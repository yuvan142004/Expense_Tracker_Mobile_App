package com.example.expensetracker.ui.alerts;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.BudgetLimit;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.data.entity.RecurringExpense;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsFragment extends Fragment {

    private TabLayout       tabLayout;
    private RecyclerView    rvAlerts;
    private FloatingActionButton fab;

    private BudgetLimitAdapter      budgetAdapter;
    private RecurringExpenseAdapter recurringAdapter;

    private int currentTab = 0; // 0 = Budget Limits, 1 = Recurring

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alerts, container, false);

        tabLayout = view.findViewById(R.id.tabLayoutAlerts);
        rvAlerts  = view.findViewById(R.id.rvAlerts);
        fab       = view.findViewById(R.id.fabAddAlert);

        budgetAdapter = new BudgetLimitAdapter(
            requireContext(),
            this::deleteBudgetLimit,
            this::showEditBudgetDialog
        );
        recurringAdapter = new RecurringExpenseAdapter(
            requireContext(),
            this::deleteRecurring,
            this::showEditRecurringDialog
        );

        rvAlerts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlerts.setAdapter(budgetAdapter);

        AppDatabase.getInstance(requireContext()).budgetLimitDao().getAllLimits()
            .observe(getViewLifecycleOwner(), budgetAdapter::submitList);
        AppDatabase.getInstance(requireContext()).recurringExpenseDao().getAllRecurring()
            .observe(getViewLifecycleOwner(), recurringAdapter::submitList);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                rvAlerts.setAdapter(currentTab == 0 ? budgetAdapter : recurringAdapter);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fab.setOnClickListener(v -> {
            if (currentTab == 0) showAddBudgetDialog(null);
            else showAddRecurringDialog(null);
        });

        return view;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BUDGET LIMIT — Add / Edit
    // ═══════════════════════════════════════════════════════════════════════════

    /** Pass null for add mode, existing BudgetLimit for edit mode. */
    private void showAddBudgetDialog(@Nullable BudgetLimit existing) {
        AppDatabase.dbExecutor.execute(() -> {
            List<Category> cats = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();
            String[] names = cats.stream().map(c -> c.name).toArray(String[]::new);

            requireActivity().runOnUiThread(() -> {
                View dv = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_add_budget, null);

                Spinner   spCat    = dv.findViewById(R.id.spinnerCategory);
                EditText  etAmt    = dv.findViewById(R.id.etLimitAmount);
                Spinner   spPeriod = dv.findViewById(R.id.spinnerPeriod);
                View      toggleRow = dv.findViewById(R.id.layoutActiveToggle);
                SwitchMaterial switchActive = dv.findViewById(R.id.switchActive);

                // Category
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, names);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCat.setAdapter(catAdapter);

                // Period
                String[] periods = {"MONTHLY", "WEEKLY", "CUSTOM"};
                ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, periods);
                periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spPeriod.setAdapter(periodAdapter);

                boolean isEdit = (existing != null);

                if (isEdit) {
                    // Pre-fill fields
                    etAmt.setText(String.valueOf((int) existing.limitAmount));

                    // Select category spinner
                    for (int i = 0; i < cats.size(); i++) {
                        if (cats.get(i).id == existing.categoryId) {
                            spCat.setSelection(i);
                            break;
                        }
                    }
                    // Select period spinner
                    for (int i = 0; i < periods.length; i++) {
                        if (periods[i].equals(existing.period)) {
                            spPeriod.setSelection(i);
                            break;
                        }
                    }
                    // Show active toggle in edit mode
                    toggleRow.setVisibility(View.VISIBLE);
                    switchActive.setChecked(existing.isActive);
                }

                new AlertDialog.Builder(requireContext())
                    .setTitle(isEdit ? "Edit Budget Limit" : "Set Budget Limit")
                    .setView(dv)
                    .setPositiveButton("Save", (d, w) -> {
                        String amtStr = etAmt.getText().toString().trim();
                        if (amtStr.isEmpty()) return;
                        double amt    = Double.parseDouble(amtStr);
                        int    catId  = cats.get(spCat.getSelectedItemPosition()).id;
                        String period = periods[spPeriod.getSelectedItemPosition()];

                        if (isEdit) {
                            existing.limitAmount = amt;
                            existing.categoryId  = catId;
                            existing.period      = period;
                            existing.isActive    = switchActive.isChecked();
                            AppDatabase.dbExecutor.execute(() ->
                                AppDatabase.getInstance(requireContext())
                                    .budgetLimitDao().update(existing));
                        } else {
                            BudgetLimit limit = new BudgetLimit();
                            limit.categoryId  = catId;
                            limit.limitAmount = amt;
                            limit.period      = period;
                            limit.isActive    = true;
                            AppDatabase.dbExecutor.execute(() ->
                                AppDatabase.getInstance(requireContext())
                                    .budgetLimitDao().insert(limit));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        });
    }

    private void showEditBudgetDialog(BudgetLimit limit) {
        showAddBudgetDialog(limit);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RECURRING EXPENSE — Add / Edit
    // ═══════════════════════════════════════════════════════════════════════════

    /** Pass null for add mode, existing RecurringExpense for edit mode. */
    private void showAddRecurringDialog(@Nullable RecurringExpense existing) {
        AppDatabase.dbExecutor.execute(() -> {
            List<Category> cats = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();
            String[] names = cats.stream().map(c -> c.name).toArray(String[]::new);

            requireActivity().runOnUiThread(() -> {
                View dv = LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_add_recurring, null);

                EditText   etName   = dv.findViewById(R.id.etRecurringName);
                EditText   etAmt    = dv.findViewById(R.id.etRecurringAmount);
                Spinner    spCat    = dv.findViewById(R.id.spinnerCategory);
                Spinner    spFreq   = dv.findViewById(R.id.spinnerFrequency);
                TextView   tvDate   = dv.findViewById(R.id.tvDueDateDisplay);
                View       btnPick  = dv.findViewById(R.id.btnPickDueDate);
                View       toggleRow = dv.findViewById(R.id.layoutActiveToggle);
                SwitchMaterial switchActive = dv.findViewById(R.id.switchActive);

                // Category
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, names);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCat.setAdapter(catAdapter);

                // Frequency
                String[] freqs = {"MONTHLY", "WEEKLY"};
                ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, freqs);
                freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFreq.setAdapter(freqAdapter);

                // Mutable due-date holder (Calendar used inside lambda)
                final Calendar pickedCal = Calendar.getInstance();

                boolean isEdit = (existing != null);

                if (isEdit) {
                    // Pre-fill
                    etName.setText(existing.name);
                    etAmt.setText(String.valueOf((int) existing.amount));

                    for (int i = 0; i < cats.size(); i++) {
                        if (cats.get(i).id == existing.categoryId) {
                            spCat.setSelection(i);
                            break;
                        }
                    }
                    for (int i = 0; i < freqs.length; i++) {
                        if (freqs[i].equals(existing.frequency)) {
                            spFreq.setSelection(i);
                            break;
                        }
                    }
                    pickedCal.setTimeInMillis(existing.nextDueDate);
                    tvDate.setText(sdf.format(new Date(existing.nextDueDate)));

                    toggleRow.setVisibility(View.VISIBLE);
                    switchActive.setChecked(existing.isActive);
                } else {
                    // Default: 1 month from today
                    pickedCal.add(Calendar.MONTH, 1);
                    tvDate.setText(sdf.format(pickedCal.getTime()));
                }

                // Date picker
                btnPick.setOnClickListener(v -> {
                    DatePickerDialog dpd = new DatePickerDialog(
                        requireContext(),
                        (dp, year, month, day) -> {
                            pickedCal.set(year, month, day, 0, 0, 0);
                            pickedCal.set(Calendar.MILLISECOND, 0);
                            tvDate.setText(sdf.format(pickedCal.getTime()));
                        },
                        pickedCal.get(Calendar.YEAR),
                        pickedCal.get(Calendar.MONTH),
                        pickedCal.get(Calendar.DAY_OF_MONTH)
                    );
                    // Don't allow past dates for new items; allow any date for edit
                    if (!isEdit) dpd.getDatePicker().setMinDate(System.currentTimeMillis());
                    dpd.show();
                });

                new AlertDialog.Builder(requireContext())
                    .setTitle(isEdit ? "Edit Recurring Expense" : "Add Recurring Expense")
                    .setView(dv)
                    .setPositiveButton("Save", (d, w) -> {
                        String name   = etName.getText().toString().trim();
                        String amtStr = etAmt.getText().toString().trim();
                        if (name.isEmpty() || amtStr.isEmpty()) return;
                        double amt    = Double.parseDouble(amtStr);
                        int    catId  = cats.get(spCat.getSelectedItemPosition()).id;
                        String freq   = freqs[spFreq.getSelectedItemPosition()];
                        long   dueMs  = pickedCal.getTimeInMillis();

                        if (isEdit) {
                            existing.name        = name;
                            existing.amount      = amt;
                            existing.categoryId  = catId;
                            existing.frequency   = freq;
                            existing.nextDueDate = dueMs;
                            existing.isActive    = switchActive.isChecked();
                            AppDatabase.dbExecutor.execute(() ->
                                AppDatabase.getInstance(requireContext())
                                    .recurringExpenseDao().update(existing));
                        } else {
                            RecurringExpense re = new RecurringExpense();
                            re.name        = name;
                            re.amount      = amt;
                            re.categoryId  = catId;
                            re.frequency   = freq;
                            re.nextDueDate = dueMs;
                            re.isActive    = true;
                            AppDatabase.dbExecutor.execute(() ->
                                AppDatabase.getInstance(requireContext())
                                    .recurringExpenseDao().insert(re));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        });
    }

    private void showEditRecurringDialog(RecurringExpense re) {
        showAddRecurringDialog(re);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Delete helpers
    // ═══════════════════════════════════════════════════════════════════════════

    private void deleteBudgetLimit(BudgetLimit limit) {
        AppDatabase.dbExecutor.execute(() ->
            AppDatabase.getInstance(requireContext()).budgetLimitDao().delete(limit));
    }

    private void deleteRecurring(RecurringExpense re) {
        AppDatabase.dbExecutor.execute(() ->
            AppDatabase.getInstance(requireContext()).recurringExpenseDao().delete(re));
    }
}
