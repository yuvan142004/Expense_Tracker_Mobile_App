package com.example.expensetracker.ui.home;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.dao.TransactionDao;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.data.entity.Transaction;
import com.example.expensetracker.ml.AnomalyDetector;
import com.example.expensetracker.ml.SmartCategoryClassifier;
import com.example.expensetracker.ml.SpendingPredictor;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HomeFragment extends Fragment {

    // View references
    private TextView  tvGreeting, tvToday, tvWeek;
    private PieChart  pieChart;
    private BarChart  barChart;
    private LineChart lineChart;
    private Spinner   spinnerChart;

    // Date filter
    private TabLayout    tabDateFilter;
    private LinearLayout layoutCustomDatePicker;
    private Button       btnFromDate, btnToDate;

    // Category list
    private RecyclerView           rvCategoryExpenses;
    private TextView               tvCategoryListHeader;
    private CategoryExpenseAdapter categoryAdapter;

    // AI insights card
    private MaterialCardView cardAiInsights;
    private LinearLayout     layoutAiHeader, layoutAiContent;
    private ImageView        ivExpandCollapse;
    private TextView         tvAiInsights;
    private boolean          isAiExpanded = false;

    // Custom date range
    private long customFromDate = 0;
    private long customToDate   = 0;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    // TFLite model instances — initialised in onCreateView, released in onDestroyView
    private SpendingPredictor       spendingPredictor;
    private AnomalyDetector         anomalyDetector;
    private SmartCategoryClassifier smartClassifier;

    // Category names (index 0 = categoryId 1)
    private static final String[] CATEGORY_NAMES = {
        "Food", "Shopping", "Fund Transfer", "Friend",
        "Bills & Utilities", "Transport", "Health",
        "Entertainment", "Education", "Other"
    };

    // ── Fragment lifecycle ────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvGreeting   = view.findViewById(R.id.tvGreeting);
        tvToday      = view.findViewById(R.id.tvToday);
        tvWeek       = view.findViewById(R.id.tvWeek);
        pieChart     = view.findViewById(R.id.pieChart);
        barChart     = view.findViewById(R.id.barChart);
        lineChart    = view.findViewById(R.id.lineChart);
        spinnerChart = view.findViewById(R.id.spinnerChartType);

        tabDateFilter          = view.findViewById(R.id.tabDateFilter);
        layoutCustomDatePicker = view.findViewById(R.id.layoutCustomDatePicker);
        btnFromDate            = view.findViewById(R.id.btnFromDate);
        btnToDate              = view.findViewById(R.id.btnToDate);

        rvCategoryExpenses   = view.findViewById(R.id.rvCategoryExpenses);
        tvCategoryListHeader = view.findViewById(R.id.tvCategoryListHeader);

        cardAiInsights   = view.findViewById(R.id.cardAiInsights);
        layoutAiHeader   = view.findViewById(R.id.layoutAiHeader);
        layoutAiContent  = view.findViewById(R.id.layoutAiContent);
        ivExpandCollapse = view.findViewById(R.id.ivExpandCollapse);
        tvAiInsights     = view.findViewById(R.id.tvAiInsights);

        if (tabDateFilter == null || pieChart == null) {
            android.util.Log.e("HomeFragment", "Critical views are null!");
            return view;
        }

        // Theme-aware chart text colour
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme()
            .resolveAttribute(com.google.android.material.R.attr.colorOnSurface, tv, true);
        int textColor = tv.data;

        for (com.github.mikephil.charting.charts.Chart<?> chart :
                new com.github.mikephil.charting.charts.Chart[]{pieChart, barChart, lineChart}) {
            chart.setBackgroundColor(Color.TRANSPARENT);
            chart.getDescription().setTextColor(textColor);
            chart.getLegend().setTextColor(textColor);
        }
        barChart.getXAxis().setTextColor(textColor);
        barChart.getAxisLeft().setTextColor(textColor);
        barChart.getAxisRight().setTextColor(textColor);
        lineChart.getXAxis().setTextColor(textColor);
        lineChart.getAxisLeft().setTextColor(textColor);
        lineChart.getAxisRight().setTextColor(textColor);

        // RecyclerView
        categoryAdapter = new CategoryExpenseAdapter(requireContext());
        rvCategoryExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryExpenses.setAdapter(categoryAdapter);

        // Initialise TFLite models (constructors never throw — errors are caught internally)
        spendingPredictor = new SpendingPredictor(requireContext());
        anomalyDetector   = new AnomalyDetector(requireContext());
        smartClassifier   = new SmartCategoryClassifier(requireContext());

        setupDateFilterTabs();
        setupAiInsights();
        setupChartDropdown();
        loadGreeting();
        loadSummaryCards();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (spendingPredictor != null) { spendingPredictor.close(); spendingPredictor = null; }
        if (anomalyDetector   != null) { anomalyDetector.close();   anomalyDetector   = null; }
        if (smartClassifier   != null) { smartClassifier.close();   smartClassifier   = null; }
    }

    // ── Chart type dropdown ───────────────────────────────────────────────────

    private void setupChartDropdown() {
        String[] types = {"Pie Chart", "Bar Chart", "Line Chart"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChart.setAdapter(adapter);

        spinnerChart.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                pieChart.setVisibility(View.GONE);
                barChart.setVisibility(View.GONE);
                lineChart.setVisibility(View.GONE);

                if (pos != 0) {
                    tabDateFilter.setVisibility(View.GONE);
                    layoutCustomDatePicker.setVisibility(View.GONE);
                    tvCategoryListHeader.setVisibility(View.GONE);
                    rvCategoryExpenses.setVisibility(View.GONE);
                    cardAiInsights.setVisibility(View.GONE);
                }

                switch (pos) {
                    case 0:
                        pieChart.setVisibility(View.VISIBLE);
                        tabDateFilter.setVisibility(View.VISIBLE);
                        loadPieChartWithDateFilter(tabDateFilter.getSelectedTabPosition());
                        break;
                    case 1:
                        barChart.setVisibility(View.VISIBLE);
                        loadBarChart();
                        break;
                    case 2:
                        lineChart.setVisibility(View.VISIBLE);
                        loadLineChart();
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ── Greeting ──────────────────────────────────────────────────────────────

    private void loadGreeting() {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db   = AppDatabase.getInstance(requireContext());
            String      name = "User";
            try {
                com.example.expensetracker.data.entity.User user = db.userDao().getUser();
                if (user != null && user.name != null && !user.name.isEmpty()) {
                    name = user.name;
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "loadGreeting error: " + e.getMessage());
            }
            final String finalName = name;
            requireActivity().runOnUiThread(() -> tvGreeting.setText("Hi " + finalName));
        });
    }

    // ── Date filter tabs ──────────────────────────────────────────────────────

    private void setupDateFilterTabs() {
        TabLayout.Tab defaultTab = tabDateFilter.getTabAt(1);
        if (defaultTab != null) defaultTab.select();

        tabDateFilter.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 2) {
                    layoutCustomDatePicker.setVisibility(View.VISIBLE);
                    if (customFromDate > 0 && customToDate > 0) {
                        loadPieChartWithDateFilter(position);
                    }
                } else {
                    layoutCustomDatePicker.setVisibility(View.GONE);
                    loadPieChartWithDateFilter(position);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnFromDate.setOnClickListener(v -> showDatePickerDialog(true));
        btnToDate.setOnClickListener(v -> showDatePickerDialog(false));
    }

    private void showDatePickerDialog(boolean isFromDate) {
        Calendar cal = Calendar.getInstance();
        if (isFromDate && customFromDate > 0) cal.setTimeInMillis(customFromDate);
        else if (!isFromDate && customToDate > 0) cal.setTimeInMillis(customToDate);

        new DatePickerDialog(
            requireContext(),
            (datePicker, year, month, dayOfMonth) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, dayOfMonth, 0, 0, 0);
                selected.set(Calendar.MILLISECOND, 0);
                if (isFromDate) {
                    customFromDate = selected.getTimeInMillis();
                    btnFromDate.setText(dateFormat.format(new Date(customFromDate)));
                } else {
                    selected.set(Calendar.HOUR_OF_DAY, 23);
                    selected.set(Calendar.MINUTE, 59);
                    selected.set(Calendar.SECOND, 59);
                    customToDate = selected.getTimeInMillis();
                    btnToDate.setText(dateFormat.format(new Date(customToDate)));
                }
                if (customFromDate > 0 && customToDate > 0 && customFromDate <= customToDate) {
                    loadPieChartWithDateFilter(2);
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void loadPieChartWithDateFilter(int filterType) {
        Calendar cal = Calendar.getInstance();
        long fromDate, toDate;

        switch (filterType) {
            case 0: // This Week
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                fromDate = cal.getTimeInMillis();
                cal.add(Calendar.WEEK_OF_YEAR, 1);
                toDate = cal.getTimeInMillis();
                break;
            case 1: // This Month (default)
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                fromDate = cal.getTimeInMillis();
                cal.add(Calendar.MONTH, 1);
                toDate = cal.getTimeInMillis();
                break;
            case 2: // Custom
                if (customFromDate <= 0 || customToDate <= 0) return;
                fromDate = customFromDate;
                toDate   = customToDate;
                break;
            default:
                return;
        }

        loadPieChartWithRange(fromDate, toDate, filterType);
    }

    // ── AI Insights card ─────────────────────────────────────────────────────

    private void setupAiInsights() {
        layoutAiHeader.setOnClickListener(v -> {
            isAiExpanded = !isAiExpanded;
            layoutAiContent.setVisibility(isAiExpanded ? View.VISIBLE : View.GONE);
            ivExpandCollapse.setRotation(isAiExpanded ? 180f : 0f);
        });
    }

    // ── Summary cards ─────────────────────────────────────────────────────────

    private void loadSummaryCards() {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            cal.add(Calendar.DAY_OF_MONTH, 1);
            long endOfDay = cal.getTimeInMillis();

            cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfWeek = cal.getTimeInMillis();
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            long endOfWeek = cal.getTimeInMillis();

            double todayTotal = db.transactionDao().getTodayTotal(startOfDay, endOfDay);
            double weekTotal  = db.transactionDao().getWeekTotal(startOfWeek, endOfWeek);

            requireActivity().runOnUiThread(() -> {
                tvToday.setText(String.format(Locale.getDefault(), "Today: \u20B9%.0f", todayTotal));
                tvWeek.setText(String.format(Locale.getDefault(), "This Week: \u20B9%.0f", weekTotal));
            });
        });
    }

    // ── Pie Chart (main data loader) ──────────────────────────────────────────

    private void loadPieChartWithRange(long fromDate, long toDate, int filterType) {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            // Current-period category totals
            List<TransactionDao.CategorySum> sums =
                db.transactionDao().getMonthlyCategoryTotals(fromDate, toDate);

            List<PieEntry> pieEntries = new ArrayList<>();
            List<CategoryExpenseAdapter.CategoryExpenseItem> categoryItems = new ArrayList<>();
            Map<String, Double> categoryMap = new HashMap<>();
            double totalAmount = 0;
            int    colorIndex  = 0;

            for (TransactionDao.CategorySum cs : sums) {
                if (cs.total > 0) {
                    Category cat   = db.categoryDao().getCategoryByIdSync(cs.categoryId);
                    String   label = (cat != null) ? cat.name : "Other";
                    pieEntries.add(new PieEntry((float) cs.total, label));

                    int    color    = getColorForIndex(colorIndex);
                    String colorHex = String.format("#%06X", (0xFFFFFF & color));
                    categoryItems.add(new CategoryExpenseAdapter.CategoryExpenseItem(
                        cs.categoryId, label, cs.total, colorHex));
                    categoryMap.put(label, cs.total);
                    totalAmount += cs.total;
                    colorIndex++;
                }
            }

            // Previous period for comparison
            long periodLen = toDate - fromDate;
            List<TransactionDao.CategorySum> prevSums =
                db.transactionDao().getMonthlyCategoryTotals(fromDate - periodLen, fromDate);
            double prevTotalAmount = 0;
            for (TransactionDao.CategorySum cs : prevSums) prevTotalAmount += cs.total;

            // Feature 1: SpendingPredictor
            float[] nextMonthPredictions = null;
            if (spendingPredictor != null && spendingPredictor.isReady()) {
                List<List<TransactionDao.CategorySum>> last3 = new ArrayList<>();
                for (int i = 3; i >= 1; i--) {
                    long mFrom = fromDate - (long) i       * periodLen;
                    long mTo   = fromDate - (long) (i - 1) * periodLen;
                    last3.add(db.transactionDao().getMonthlyCategoryTotals(mFrom, mTo));
                }
                nextMonthPredictions = spendingPredictor.predictNextMonth(last3);
            }

            // Feature 2: AnomalyDetector
            List<Transaction> periodTxns =
                db.transactionDao().getTransactionsForPeriodSync(fromDate, toDate);
            List<AnomalyDetector.AnomalyResult> anomalies = new ArrayList<>();
            if (anomalyDetector != null && anomalyDetector.isReady()) {
                anomalies = anomalyDetector.detectAnomalies(periodTxns);
            }

            // Feature 3: SmartCategoryClassifier
            int reclassifiedCount = 0;
            int totalOtherCount   = 0;
            Map<String, Integer> reclassMap = new LinkedHashMap<>();

            if (smartClassifier != null && smartClassifier.isReady()) {
                for (Transaction txn : periodTxns) {
                    if (!"DEBIT".equals(txn.transactionType)) continue;
                    if (txn.categoryId == 10) {
                        totalOtherCount++;
                        float[] result     = smartClassifier.classifyWithConfidence(txn.amount, txn.timestamp);
                        int     newCatId   = (int) result[0];
                        float   confidence = result[1];
                        if (newCatId != 10 && confidence >= 0.45f) {
                            reclassifiedCount++;
                            String catName = (newCatId >= 1 && newCatId <= 10)
                                ? CATEGORY_NAMES[newCatId - 1] : "Other";
                            reclassMap.merge(catName, 1, Integer::sum);
                        }
                    }
                }
            }

            // Build insight text
            String periodName = filterType == 0 ? "this week"
                              : filterType == 1 ? "this month" : "this period";
            String insights = buildTfliteInsights(
                nextMonthPredictions, anomalies,
                reclassifiedCount, totalOtherCount, reclassMap,
                categoryMap, totalAmount, prevTotalAmount, periodName);

            final double finalTotal   = totalAmount;
            final String finalInsight = insights;

            requireActivity().runOnUiThread(() -> {
                if (pieEntries.isEmpty()) {
                    pieChart.setNoDataText("No expenses for selected period");
                    pieChart.clear();
                    pieChart.invalidate();
                    tvCategoryListHeader.setVisibility(View.GONE);
                    rvCategoryExpenses.setVisibility(View.GONE);
                    cardAiInsights.setVisibility(View.GONE);
                    return;
                }

                PieDataSet dataSet = new PieDataSet(pieEntries, "");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                dataSet.setValueTextSize(14f);
                dataSet.setValueTextColor(Color.WHITE);
                dataSet.setValueFormatter(new PercentFormatter(pieChart));

                PieData data = new PieData(dataSet);
                pieChart.setData(data);
                pieChart.setUsePercentValues(true);
                pieChart.getDescription().setEnabled(false);
                pieChart.setHoleRadius(40f);
                pieChart.setTransparentCircleRadius(45f);
                pieChart.animateY(800);
                pieChart.invalidate();

                tvCategoryListHeader.setVisibility(View.VISIBLE);
                tvCategoryListHeader.setText(
                    String.format(Locale.getDefault(), "Total: \u20B9%.0f", finalTotal));
                rvCategoryExpenses.setVisibility(View.VISIBLE);
                categoryAdapter.setItems(categoryItems);

                cardAiInsights.setVisibility(View.VISIBLE);
                tvAiInsights.setText(finalInsight);
                isAiExpanded = false;
                layoutAiContent.setVisibility(View.GONE);
                ivExpandCollapse.setRotation(0f);
            });
        });
    }

    // ── AI insight builder ────────────────────────────────────────────────────

    private String buildTfliteInsights(
            float[]                             predictions,
            List<AnomalyDetector.AnomalyResult> anomalies,
            int                                 reclassifiedCount,
            int                                 totalOtherCount,
            Map<String, Integer>                reclassMap,
            Map<String, Double>                 categoryMap,
            double                              totalSpent,
            double                              prevTotal,
            String                              periodName) {

        StringBuilder sb = new StringBuilder();

        // Section 0: Basic spending summary
        if (!categoryMap.isEmpty() && totalSpent > 0) {
            String topCat = "";
            double topAmt = 0;
            for (Map.Entry<String, Double> e : categoryMap.entrySet()) {
                if (e.getValue() > topAmt) { topAmt = e.getValue(); topCat = e.getKey(); }
            }
            double topPct = topAmt / totalSpent * 100;
            sb.append("\uD83D\uDCB0 Top spending: ").append(topCat)
              .append(String.format(Locale.getDefault(),
                  " (\u20B9%.0f, %.0f%% of total)\n\n", topAmt, topPct));

            if (prevTotal > 0) {
                double change = (totalSpent - prevTotal) / prevTotal * 100;
                if (change > 20) {
                    sb.append(String.format(Locale.getDefault(),
                        "\u26A0\uFE0F  Spending rose %.0f%% vs the previous %s.\n\n",
                        change, periodName));
                } else if (change < -20) {
                    sb.append(String.format(Locale.getDefault(),
                        "\u2705  Spending fell %.0f%% vs the previous %s. Great!\n\n",
                        Math.abs(change), periodName));
                } else {
                    sb.append("\uD83D\uDCCA  Spending is stable compared to the previous ")
                      .append(periodName).append(".\n\n");
                }
            }
        } else {
            sb.append("\uD83D\uDCED  No spending data for ").append(periodName).append(".\n\n");
        }

        // Section 1: SpendingPredictor
        sb.append("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");
        sb.append("\uD83D\uDD2E  AI Forecast \u2014 Next Month\n");
        sb.append("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");

        if (predictions != null) {
            double predictedTotal = 0;
            for (float p : predictions) predictedTotal += p;

            if (predictedTotal > 0) {
                sb.append(String.format(Locale.getDefault(),
                    "  Estimated total: \u20B9%.0f\n\n", predictedTotal));

                Integer[] order = new Integer[SpendingPredictor.NUM_CATEGORIES];
                for (int i = 0; i < order.length; i++) order[i] = i;
                final float[] pred = predictions;
                Arrays.sort(order, (a, b) -> Float.compare(pred[b], pred[a]));

                sb.append("  Top predicted categories:\n");
                for (int rank = 0; rank < Math.min(3, order.length); rank++) {
                    int   idx  = order[rank];
                    float amt  = predictions[idx];
                    if (amt < 1f) continue;
                    String name = (idx < CATEGORY_NAMES.length) ? CATEGORY_NAMES[idx] : "Other";
                    sb.append(String.format(Locale.getDefault(),
                        "    %d. %-20s \u20B9%.0f\n", rank + 1, name, amt));
                }

                if (predictedTotal > totalSpent * 1.15 && totalSpent > 0) {
                    sb.append("\n  \u26A1 Prediction is 15%+ above current \u2014 consider reviewing budget.\n");
                }
            } else {
                sb.append("  Insufficient history for prediction.\n");
            }
        } else {
            sb.append("  Model not ready (add spending_predictor.tflite to assets).\n");
        }

        // Section 2: AnomalyDetector
        sb.append("\n\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");
        sb.append("\uD83D\uDEA8  Anomaly Detection\n");
        sb.append("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");

        if (!anomalies.isEmpty()) {
            sb.append("  Unusually high-spend days detected:\n");
            SimpleDateFormat dayFmt =
                new SimpleDateFormat("dd MMM (EEE)", Locale.getDefault());
            int showMax = Math.min(3, anomalies.size());
            for (int i = 0; i < showMax; i++) {
                AnomalyDetector.AnomalyResult r = anomalies.get(i);
                sb.append(String.format(Locale.getDefault(),
                    "    \u2022 %s \u2014 \u20B9%.0f  (score %.2f)\n",
                    dayFmt.format(new Date(r.dateMs)), r.totalAmount, r.anomalyScore));
            }
            if (anomalies.size() > 3) {
                sb.append(String.format(Locale.getDefault(),
                    "    \u2026 and %d more high-spend days.\n", anomalies.size() - 3));
            }
            sb.append("\n  \uD83D\uDCA1 Review these days to ensure no unexpected charges.\n");
        } else if (anomalyDetector != null && anomalyDetector.isReady()) {
            sb.append("  \u2705  No anomalous days detected \u2014 spending looks normal!\n");
        } else {
            sb.append("  Model not ready (add anomaly_detector.tflite to assets).\n");
        }

        // Section 3: SmartCategoryClassifier
        sb.append("\n\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");
        sb.append("\uD83E\uDD16  Smart Auto-Classify\n");
        sb.append("\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");

        if (smartClassifier != null && smartClassifier.isReady()) {
            if (totalOtherCount == 0) {
                sb.append("  No \"Other\" transactions to reclassify this period.\n");
            } else {
                sb.append(String.format(Locale.getDefault(),
                    "  Scanned %d 'Other' transactions.\n", totalOtherCount));
                if (reclassifiedCount > 0) {
                    sb.append(String.format(Locale.getDefault(),
                        "  AI suggested categories for %d of them:\n", reclassifiedCount));
                    for (Map.Entry<String, Integer> e : reclassMap.entrySet()) {
                        sb.append(String.format(Locale.getDefault(),
                            "    \u2022 %s  \u00D7%d\n", e.getKey(), e.getValue()));
                    }
                    sb.append("  (Tap a transaction in History to confirm.)\n");
                } else {
                    sb.append("  No confident reclassifications found this period.\n");
                }
            }
        } else {
            sb.append("  Model not ready (add category_classifier.tflite to assets).\n");
        }

        sb.append("\n\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\n");
        sb.append("\uD83D\uDCF1  All insights computed on-device \u2014 no internet required.");

        return sb.toString();
    }

    // ── Bar Chart (last 6 months) ─────────────────────────────────────────────

    private void loadBarChart() {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase     db      = AppDatabase.getInstance(requireContext());
            List<BarEntry>  entries = new ArrayList<>();
            Calendar        cal     = Calendar.getInstance();

            for (int i = 5; i >= 0; i--) {
                Calendar c = (Calendar) cal.clone();
                c.add(Calendar.MONTH, -i);
                c.set(Calendar.DAY_OF_MONTH, 1);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                long from = c.getTimeInMillis();
                c.add(Calendar.MONTH, 1);
                long to = c.getTimeInMillis();

                double total = db.transactionDao().getTotalForPeriod(from, to);
                entries.add(new BarEntry(5 - i, (float) total));
            }

            requireActivity().runOnUiThread(() -> {
                BarDataSet dataSet = new BarDataSet(entries, "Monthly Spend");
                dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
                BarData data = new BarData(dataSet);
                barChart.setData(data);
                barChart.getDescription().setEnabled(false);
                barChart.animateY(800);
                barChart.invalidate();
            });
        });
    }

    // ── Line Chart (daily this month) ─────────────────────────────────────────

    private void loadLineChart() {
        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long from = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            long to = cal.getTimeInMillis();

            List<Transaction> txns = db.transactionDao().getTransactionsForPeriodSync(from, to);

            Map<Integer, Double> dailyTotals = new TreeMap<>();
            Calendar c = Calendar.getInstance();
            for (Transaction txn : txns) {
                c.setTimeInMillis(txn.timestamp);
                int day = c.get(Calendar.DAY_OF_MONTH);
                dailyTotals.merge(day, txn.amount, Double::sum);
            }

            List<Entry> entries = new ArrayList<>();
            for (Map.Entry<Integer, Double> e : dailyTotals.entrySet()) {
                entries.add(new Entry(e.getKey(), e.getValue().floatValue()));
            }

            requireActivity().runOnUiThread(() -> {
                if (entries.isEmpty()) {
                    lineChart.setNoDataText("No expenses this month");
                    lineChart.invalidate();
                    return;
                }
                LineDataSet dataSet = new LineDataSet(entries, "Daily Spend");
                android.util.TypedValue tv = new android.util.TypedValue();
                requireContext().getTheme().resolveAttribute(
                    com.google.android.material.R.attr.colorPrimary, tv, true);
                int primaryColor = tv.data;
                dataSet.setColor(primaryColor);
                dataSet.setCircleColor(primaryColor);
                dataSet.setLineWidth(2f);
                dataSet.setDrawValues(false);
                LineData data = new LineData(dataSet);
                lineChart.setData(data);
                lineChart.getDescription().setEnabled(false);
                lineChart.animateX(800);
                lineChart.invalidate();
            });
        });
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private int getColorForIndex(int index) {
        int[] colors = ColorTemplate.MATERIAL_COLORS;
        return colors[index % colors.length];
    }
}
