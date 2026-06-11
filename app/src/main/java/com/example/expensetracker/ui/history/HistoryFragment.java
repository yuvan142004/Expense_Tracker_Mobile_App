package com.example.expensetracker.ui.history;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.data.entity.Transaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class HistoryFragment extends Fragment
        implements FilterBottomSheet.OnFilterAppliedListener {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etSearch;
    private MaterialButton    btnFilter;
    private HorizontalScrollView hsvChips;
    private ChipGroup         chipGroupFilters;
    private TextView          tvResultCount;
    private TextView          tvEmpty;
    private RecyclerView      rvTransactions;

    // ── State ─────────────────────────────────────────────────────────────────
    private TransactionAdapter adapter;
    private FilterBottomSheet.FilterState currentFilter = new FilterBottomSheet.FilterState();
    private String currentQuery = "";

    // Live-data subscription handle — so we can remove the old observer before
    // re-subscribing with new parameters.
    private LiveData<List<Transaction>> currentLiveData = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch         = view.findViewById(R.id.etSearch);
        btnFilter        = view.findViewById(R.id.btnFilter);
        hsvChips         = view.findViewById(R.id.hsvChips);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters);
        tvResultCount    = view.findViewById(R.id.tvResultCount);
        tvEmpty          = view.findViewById(R.id.tvEmpty);
        rvTransactions   = view.findViewById(R.id.rvTransactions);

        // Adapter
        adapter = new TransactionAdapter(requireContext(), this::showEditCategoryDialog);
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        // Search watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                currentQuery = s.toString().trim();
                resubscribe();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter button
        btnFilter.setOnClickListener(v -> {
            FilterBottomSheet sheet = new FilterBottomSheet(currentFilter, this);
            sheet.show(getChildFragmentManager(), "filter");
        });

        // Initial load
        resubscribe();
    }

    // ── FilterBottomSheet.OnFilterAppliedListener ─────────────────────────────

    @Override
    public void onFilterApplied(FilterBottomSheet.FilterState state) {
        currentFilter = state;
        updateFilterChips();
        updateFilterButtonStyle();
        resubscribe();
    }

    // ── Re-subscribe to the combined filtered LiveData ────────────────────────

    private void resubscribe() {
        // Remove previous observer (avoids duplicate observers)
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());
        currentLiveData = db.transactionDao().getFiltered(
                currentQuery,
                currentFilter.categoryId,
                currentFilter.type,
                currentFilter.minAmount,
                currentFilter.maxAmount,
                currentFilter.fromDate,
                currentFilter.toDate
        );

        currentLiveData.observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);

            boolean empty = list == null || list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

            // Result count — only show when a filter/search is active
            boolean hasFilter = currentFilter.hasAnyFilter() || !currentQuery.isEmpty();
            if (hasFilter) {
                int count = list == null ? 0 : list.size();
                tvResultCount.setText(count + (count == 1 ? " transaction" : " transactions"));
                tvResultCount.setVisibility(View.VISIBLE);
            } else {
                tvResultCount.setVisibility(View.GONE);
            }
        });
    }

    // ── Filter chips ──────────────────────────────────────────────────────────

    private void updateFilterChips() {
        chipGroupFilters.removeAllViews();

        addChipIfNeeded(currentFilter.typeLabel, () -> {
            currentFilter.type      = null;
            currentFilter.typeLabel = null;
            onFilterChanged();
        });

        addChipIfNeeded(currentFilter.categoryLabel, () -> {
            currentFilter.categoryId    = 0;
            currentFilter.categoryLabel = null;
            onFilterChanged();
        });

        addChipIfNeeded(currentFilter.amountLabel, () -> {
            currentFilter.minAmount    = 0;
            currentFilter.maxAmount    = Double.MAX_VALUE;
            currentFilter.amountLabel  = null;
            onFilterChanged();
        });

        addChipIfNeeded(currentFilter.dateLabel, () -> {
            currentFilter.fromDate  = 0;
            currentFilter.toDate    = Long.MAX_VALUE;
            currentFilter.dateLabel = null;
            onFilterChanged();
        });

        hsvChips.setVisibility(
                chipGroupFilters.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    /** Creates a closeable chip for the given label and adds it to chipGroupFilters. */
    private void addChipIfNeeded(@Nullable String label, Runnable onClose) {
        if (label == null || label.isEmpty()) return;

        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setOnCloseIconClickListener(v -> onClose.run());
        chipGroupFilters.addView(chip);
    }

    /** Called when a chip's X is tapped — re-render chips + re-query. */
    private void onFilterChanged() {
        updateFilterChips();
        updateFilterButtonStyle();
        resubscribe();
    }

    // ── Filter button appearance ──────────────────────────────────────────────

    private void updateFilterButtonStyle() {
        boolean active = currentFilter.hasAnyFilter();

        // Resolve colors from the active theme — works in both light and dark mode
        android.util.TypedValue tv = new android.util.TypedValue();
        android.content.res.Resources.Theme theme = requireContext().getTheme();

        theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, tv, true);
        int primaryColor = tv.data;

        theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, tv, true);
        int onPrimaryColor = tv.data;

        if (active) {
            btnFilter.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(primaryColor));
            btnFilter.setTextColor(onPrimaryColor);
            btnFilter.setIconTint(
                    android.content.res.ColorStateList.valueOf(onPrimaryColor));
        } else {
            btnFilter.setBackgroundTintList(null);
            btnFilter.setTextColor(primaryColor);
            btnFilter.setIconTint(
                    android.content.res.ColorStateList.valueOf(primaryColor));
        }
    }

    // ── Edit category dialog ──────────────────────────────────────────────────

    private void showEditCategoryDialog(Transaction transaction) {
        AppDatabase.dbExecutor.execute(() -> {
            List<Category> categories = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();
            String[] names = categories.stream()
                    .map(c -> c.name).toArray(String[]::new);

            requireActivity().runOnUiThread(() -> {
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Change Category")
                        .setItems(names, (dialog, which) -> {
                            int newCatId = categories.get(which).id;
                            AppDatabase.dbExecutor.execute(() -> {
                                transaction.categoryId = newCatId;
                                transaction.isEdited   = true;
                                AppDatabase.getInstance(requireContext())
                                        .transactionDao().update(transaction);
                            });
                        })
                        .show();
            });
        });
    }
}
