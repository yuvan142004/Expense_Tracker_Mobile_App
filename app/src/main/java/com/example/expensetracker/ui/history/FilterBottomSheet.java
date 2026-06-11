package com.example.expensetracker.ui.history;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.Category;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    // ── Filter state passed back to HistoryFragment ───────────────────────────
    public static class FilterState {
        public String  type        = null;   // null = all, "DEBIT", "CREDIT"
        public int     categoryId  = 0;      // 0 = any
        public double  minAmount   = 0;
        public double  maxAmount   = Double.MAX_VALUE;
        public long    fromDate    = 0;
        public long    toDate      = Long.MAX_VALUE;

        // Display labels for chips
        public String typeLabel     = null;
        public String categoryLabel = null;
        public String amountLabel   = null;
        public String dateLabel     = null;

        public boolean hasAnyFilter() {
            return type != null || categoryId != 0
                || minAmount > 0 || maxAmount < Double.MAX_VALUE
                || fromDate > 0  || toDate < Long.MAX_VALUE;
        }
    }

    public interface OnFilterAppliedListener {
        void onFilterApplied(FilterState state);
    }

    private static final String TAG_FROM = "FROM";
    private static final String TAG_TO   = "TO";

    private final FilterState            currentState;
    private final OnFilterAppliedListener listener;
    private final SimpleDateFormat        sdf =
        new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private List<Category> categories = new ArrayList<>();
    // "All Categories" is index 0; real categories start at index 1
    private String[] categoryNames;

    // Date calendars
    private final Calendar calFrom = Calendar.getInstance();
    private final Calendar calTo   = Calendar.getInstance();
    private boolean fromDateSet = false;
    private boolean toDateSet   = false;

    public FilterBottomSheet(FilterState current, OnFilterAppliedListener listener) {
        this.currentState = current != null ? current : new FilterState();
        this.listener     = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ── Type chips ────────────────────────────────────────────────────────
        ChipGroup cgType     = view.findViewById(R.id.chipGroupType);
        Chip      chipAll    = view.findViewById(R.id.chipTypeAll);
        Chip      chipDebit  = view.findViewById(R.id.chipTypeDebit);
        Chip      chipCredit = view.findViewById(R.id.chipTypeCredit);

        if ("DEBIT".equals(currentState.type))       chipDebit.setChecked(true);
        else if ("CREDIT".equals(currentState.type)) chipCredit.setChecked(true);
        else                                          chipAll.setChecked(true);

        // ── Category spinner ──────────────────────────────────────────────────
        Spinner spCat = view.findViewById(R.id.spinnerFilterCategory);

        // ── Amount fields ─────────────────────────────────────────────────────
        TextInputEditText etMin = view.findViewById(R.id.etMinAmount);
        TextInputEditText etMax = view.findViewById(R.id.etMaxAmount);

        if (currentState.minAmount > 0)
            etMin.setText(String.valueOf((int) currentState.minAmount));
        if (currentState.maxAmount < Double.MAX_VALUE)
            etMax.setText(String.valueOf((int) currentState.maxAmount));

        // ── Date fields ───────────────────────────────────────────────────────
        TextInputEditText etFrom = view.findViewById(R.id.etFromDate);
        TextInputEditText etTo   = view.findViewById(R.id.etToDate);

        if (currentState.fromDate > 0) {
            calFrom.setTimeInMillis(currentState.fromDate);
            etFrom.setText(sdf.format(new Date(currentState.fromDate)));
            fromDateSet = true;
        }
        if (currentState.toDate < Long.MAX_VALUE) {
            calTo.setTimeInMillis(currentState.toDate);
            etTo.setText(sdf.format(new Date(currentState.toDate)));
            toDateSet = true;
        }

        etFrom.setOnClickListener(v -> showDatePicker(TAG_FROM, etFrom));
        etTo.setOnClickListener(v   -> showDatePicker(TAG_TO,   etTo));

        // ── Quick date preset chips ───────────────────────────────────────────
        ChipGroup cgPreset = view.findViewById(R.id.chipGroupDatePreset);
        cgPreset.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            Calendar start = Calendar.getInstance();
            Calendar end   = Calendar.getInstance();

            if (id == R.id.chipToday) {
                startOfDay(start); endOfDay(end);
            } else if (id == R.id.chipWeek) {
                start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                startOfDay(start); endOfDay(end);
            } else if (id == R.id.chipMonth) {
                start.set(Calendar.DAY_OF_MONTH, 1);
                startOfDay(start); endOfDay(end);
            } else if (id == R.id.chipLast3) {
                start.add(Calendar.MONTH, -3);
                startOfDay(start); endOfDay(end);
            }

            calFrom.setTimeInMillis(start.getTimeInMillis());
            calTo.setTimeInMillis(end.getTimeInMillis());
            fromDateSet = true;
            toDateSet   = true;
            etFrom.setText(sdf.format(calFrom.getTime()));
            etTo.setText(sdf.format(calTo.getTime()));
        });

        // ── Load categories async then set spinner ────────────────────────────
        AppDatabase.dbExecutor.execute(() -> {
            categories = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();
            categoryNames = new String[categories.size() + 1];
            categoryNames[0] = "All Categories";
            for (int i = 0; i < categories.size(); i++)
                categoryNames[i + 1] = categories.get(i).name;

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                spCat.setAdapter(adapter);

                // Restore selection
                if (currentState.categoryId != 0) {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).id == currentState.categoryId) {
                            spCat.setSelection(i + 1);
                            break;
                        }
                    }
                }
            });
        });

        // ── Clear All ─────────────────────────────────────────────────────────
        view.findViewById(R.id.btnClearFilters).setOnClickListener(v -> {
            chipAll.setChecked(true);
            spCat.setSelection(0);
            etMin.setText("");
            etMax.setText("");
            etFrom.setText("");
            etTo.setText("");
            cgPreset.clearCheck();
            fromDateSet = false;
            toDateSet   = false;
        });

        // ── Apply ─────────────────────────────────────────────────────────────
        view.findViewById(R.id.btnApplyFilter).setOnClickListener(v -> {
            FilterState result = new FilterState();

            // Type
            int typeChecked = cgType.getCheckedChipId();
            if (typeChecked == R.id.chipTypeDebit) {
                result.type      = "DEBIT";
                result.typeLabel = "Expense";
            } else if (typeChecked == R.id.chipTypeCredit) {
                result.type      = "CREDIT";
                result.typeLabel = "Income";
            }

            // Category
            int catPos = spCat.getSelectedItemPosition();
            if (catPos > 0 && catPos - 1 < categories.size()) {
                result.categoryId    = categories.get(catPos - 1).id;
                result.categoryLabel = categories.get(catPos - 1).name;
            }

            // Amount
            String minStr = etMin.getText() != null ? etMin.getText().toString().trim() : "";
            String maxStr = etMax.getText() != null ? etMax.getText().toString().trim() : "";
            if (!minStr.isEmpty()) result.minAmount = Double.parseDouble(minStr);
            if (!maxStr.isEmpty()) result.maxAmount = Double.parseDouble(maxStr);
            if (result.minAmount > 0 || result.maxAmount < Double.MAX_VALUE) {
                String lo = result.minAmount > 0      ? "\u20B9" + (int) result.minAmount : "0";
                String hi = result.maxAmount < Double.MAX_VALUE ? "\u20B9" + (int) result.maxAmount : "\u221E";
                result.amountLabel = lo + " – " + hi;
            }

            // Dates
            if (fromDateSet) result.fromDate = calFrom.getTimeInMillis();
            if (toDateSet)   result.toDate   = calTo.getTimeInMillis();
            if (fromDateSet || toDateSet) {
                String f = fromDateSet ? sdf.format(calFrom.getTime()) : "any";
                String t = toDateSet   ? sdf.format(calTo.getTime())   : "any";
                result.dateLabel = f.equals(t) ? f : f + " – " + t;
            }

            listener.onFilterApplied(result);
            dismiss();
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showDatePicker(String tag, TextInputEditText target) {
        Calendar base = TAG_FROM.equals(tag) ? calFrom : calTo;
        DatePickerDialog dpd = new DatePickerDialog(requireContext(),
            (dp, year, month, day) -> {
                base.set(year, month, day);
                if (TAG_FROM.equals(tag)) { startOfDay(base); fromDateSet = true; }
                else                      { endOfDay(base);   toDateSet   = true; }
                target.setText(sdf.format(base.getTime()));
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private static void startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static void endOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
    }
}
