package com.example.expensetracker.ui.add;

import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.Category;
import com.example.expensetracker.data.entity.Transaction;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class ManualEntryDialog extends DialogFragment {

    private EditText  etAmount, etNote;
    private Spinner   spinnerCategory;
    private RadioGroup rgType;
    private ChipGroup chipGroupPaymentMethod;
    private Button    btnSave, btnSaveAndPayUpi, btnCancel;

    private List<Category> categories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_manual_entry, container, false);

        etAmount        = view.findViewById(R.id.etAmount);
        etNote          = view.findViewById(R.id.etNote);
        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        rgType          = view.findViewById(R.id.rgTransactionType);
        chipGroupPaymentMethod = view.findViewById(R.id.chipGroupPaymentMethod);
        btnSave         = view.findViewById(R.id.btnSave);
        btnSaveAndPayUpi = view.findViewById(R.id.btnSaveAndPayUpi);
        btnCancel       = view.findViewById(R.id.btnCancel);

        loadCategories();

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveTransaction(false));
        btnSaveAndPayUpi.setOnClickListener(v -> saveTransaction(true));

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void loadCategories() {
        AppDatabase.dbExecutor.execute(() -> {
            categories = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();
            List<String> names = new ArrayList<>();
            for (Category c : categories) names.add(c.name);
            names.add("+ Add Category");

            requireActivity().runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, names);
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);

                spinnerCategory.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> p, View v,
                                                   int pos, long id) {
                            if (pos == categories.size()) {
                                showAddCategoryDialog();
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> p) {}
                    });
            });
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Category name");
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("Add", (d, w) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    AppDatabase.dbExecutor.execute(() -> {
                        Category cat = new Category();
                        cat.name      = name;
                        cat.colorHex  = "#BDC3C7";
                        cat.iconRes   = "ic_other";
                        cat.isDefault = false;
                        AppDatabase.getInstance(requireContext())
                            .categoryDao().insert(cat);
                        requireActivity().runOnUiThread(this::loadCategories);
                    });
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void saveTransaction(boolean openUpiApp) {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            etAmount.setError("Enter amount");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount");
            return;
        }

        int selectedPos = spinnerCategory.getSelectedItemPosition();
        if (selectedPos >= categories.size()) {
            Toast.makeText(requireContext(), "Please select a category",
                Toast.LENGTH_SHORT).show();
            return;
        }

        int categoryId = categories.get(selectedPos).id;
        String type = rgType.getCheckedRadioButtonId() == R.id.rbCredit ? "CREDIT" : "DEBIT";
        String note = etNote.getText().toString().trim();

        // Get selected payment method
        String paymentMethod = "CASH"; // default
        int selectedChipId = chipGroupPaymentMethod.getCheckedChipId();
        if (selectedChipId == R.id.chipCash) {
            paymentMethod = "CASH";
        } else if (selectedChipId == R.id.chipUPI) {
            paymentMethod = "UPI";
        } else if (selectedChipId == R.id.chipCard) {
            paymentMethod = "CARD";
        } else if (selectedChipId == R.id.chipNetBanking) {
            paymentMethod = "NET_BANKING";
        }

        Transaction txn = new Transaction();
        txn.amount          = amount;
        txn.categoryId      = categoryId;
        txn.bankAccountId   = null;
        txn.source          = "MANUAL";
        txn.transactionType = type;
        txn.merchantName    = null;
        txn.upiRef          = null;
        txn.note            = note.isEmpty() ? null : note;
        txn.rawSms          = null;
        txn.isEdited        = false;
        txn.timestamp       = System.currentTimeMillis();
        txn.paymentMethod   = paymentMethod;
        
        // Set status based on whether opening UPI app
        if (openUpiApp) {
            txn.status = "PENDING";  // Will be confirmed when user returns from UPI app
        } else {
            txn.status = "COMPLETED";  // Normal save is completed immediately
        }

        AppDatabase.dbExecutor.execute(() -> {
            long transactionId = AppDatabase.getInstance(requireContext())
                    .transactionDao().insert(txn);
            
            requireActivity().runOnUiThread(() -> {
                if (openUpiApp) {
                    // Launch UPI app flow
                    // IMPORTANT: Capture Activity reference BEFORE dismissing
                    android.app.Activity activity = getActivity();
                    if (activity == null) {
                        Toast.makeText(requireContext(), "Error: Activity not available",
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    Toast.makeText(requireContext(), "Transaction saved. Opening UPI app...",
                        Toast.LENGTH_SHORT).show();
                    dismiss();
                    
                    // Post with delay to ensure dialog is fully dismissed
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        launchUpiAppForTransaction(activity, transactionId);
                    }, 300); // 300ms delay
                } else {
                    // Normal save - just show confirmation and dismiss
                    Toast.makeText(requireContext(), "Transaction saved",
                        Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });
        });
    }

    /**
     * Launch UPI app after saving transaction
     * @param activity The Activity context (captured before fragment dismissal)
     * @param txnId Transaction ID
     */
    private void launchUpiAppForTransaction(android.app.Activity activity, long txnId) {
        // Activity is passed as parameter, so it's guaranteed to be available
        if (activity == null || activity.isFinishing()) {
            android.util.Log.e("ManualEntryDialog", "Activity is null or finishing, cannot launch UPI");
            return;
        }
        
        // Save pending transaction ID for confirmation when user returns
        com.example.expensetracker.utils.AppPreferences.setPendingTransactionId(activity, txnId);
        
        // Check for preferred UPI app
        String preferredApp = com.example.expensetracker.utils.AppPreferences.getPreferredUpiApp(activity);
        
        if (preferredApp != null && !preferredApp.isEmpty()) {
            // Launch preferred app's QR scanner directly
            android.util.Log.d("ManualEntryDialog", "Launching preferred UPI app: " + preferredApp);
            boolean success = com.example.expensetracker.utils.UpiAppLauncher.launchQrScanner(activity, preferredApp);
            if (!success) {
                Toast.makeText(activity, "Failed to open UPI app. Please try again.", 
                    Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show UPI app selector
            android.util.Log.d("ManualEntryDialog", "No preferred app, showing selector");
            showUpiAppSelector(activity, txnId);
        }
    }

    /**
     * Show UPI app selector dialog
     * @param activity The Activity context
     * @param txnId Transaction ID (for logging/debugging)
     */
    private void showUpiAppSelector(android.app.Activity activity, long txnId) {
        if (activity == null || activity.isFinishing()) {
            android.util.Log.e("ManualEntryDialog", "Activity is null or finishing, cannot show selector");
            return;
        }
        
        android.util.Log.d("ManualEntryDialog", "Showing UPI app selector for transaction: " + txnId);
        
        // Create selector dialog
        UpiAppSelectorDialog selector = new UpiAppSelectorDialog();
        selector.setOnAppSelectedListener((appPackage, rememberChoice) -> {
            android.util.Log.d("ManualEntryDialog", "UPI app selected: " + appPackage + ", remember: " + rememberChoice);
            
            if (rememberChoice) {
                com.example.expensetracker.utils.AppPreferences.setPreferredUpiApp(activity, appPackage);
            }
            
            boolean success = com.example.expensetracker.utils.UpiAppLauncher.launchQrScanner(activity, appPackage);
            if (!success) {
                Toast.makeText(activity, "Failed to open UPI app. Please try again.", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Show using Activity's fragment manager (not DialogFragment's)
        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            selector.show(((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager(), "UpiSelector");
        } else {
            android.util.Log.e("ManualEntryDialog", "Activity is not FragmentActivity, cannot show selector");
            Toast.makeText(activity, "Error showing UPI selector", Toast.LENGTH_SHORT).show();
        }
    }
}
