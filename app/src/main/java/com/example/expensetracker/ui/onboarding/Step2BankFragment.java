package com.example.expensetracker.ui.onboarding;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.BankSenderInfo;
import com.example.expensetracker.data.BankSenderRegistry;
import com.example.expensetracker.data.entity.BankAccount;
import com.example.expensetracker.data.entity.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class Step2BankFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────────────────
    private LinearLayout      accountsContainer;
    private Spinner           spinnerBank;
    private TextInputEditText etLast4, etSenderId;
    private TextInputLayout   tilSenderId;
    private TextView          tvSenderNote;
    private TextView          tvSmsWarning;      // orange banner for restricted banks
    private TextView          tvSmsReliable;     // green badge for reliable banks
    private ChipGroup         chipGroupSuggestions;
    private Button            btnAddAccount, btnNext;
    private TextView          tvNoAccounts;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<BankAccount> addedAccounts = new ArrayList<>();
    private List<String> bankNames;

    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboard_step2, container, false);

        accountsContainer    = view.findViewById(R.id.accountsContainer);
        spinnerBank          = view.findViewById(R.id.spinnerBank);
        etLast4              = view.findViewById(R.id.etLast4);
        etSenderId           = view.findViewById(R.id.etSenderId);
        tilSenderId          = view.findViewById(R.id.tilSenderId);
        tvSenderNote         = view.findViewById(R.id.tvSenderNote);
        tvSmsWarning         = view.findViewById(R.id.tvSmsWarning);
        tvSmsReliable        = view.findViewById(R.id.tvSmsReliable);
        chipGroupSuggestions = view.findViewById(R.id.chipGroupSuggestions);
        btnAddAccount        = view.findViewById(R.id.btnAddAccount);
        btnNext              = view.findViewById(R.id.btnNext);
        tvNoAccounts         = view.findViewById(R.id.tvNoAccounts);

        etSenderId.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(10)
        });

        bankNames = BankSenderRegistry.getBankNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, bankNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBank.setAdapter(adapter);

        spinnerBank.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                onBankSelected(bankNames.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        onBankSelected(bankNames.get(0));

        btnAddAccount.setOnClickListener(v -> addAccount());
        btnNext.setOnClickListener(v -> proceedToNextStep());

        return view;
    }

    // ── Bank selection ────────────────────────────────────────────────────────

    private void onBankSelected(String bankName) {
        BankSenderInfo info = BankSenderRegistry.getInfo(bankName);
        if (info == null) return;

        // 1. Auto-fill sender ID
        etSenderId.setText(info.primarySenderId);

        // 2. Helper note below field
        tvSenderNote.setText(info.helperNote);
        tvSenderNote.setVisibility(View.VISIBLE);

        // 3. SMS reliability banner
        if (info.isSmsRestricted()) {
            // Orange warning for HDFC / ICICI / Axis / Kotak
            tvSmsWarning.setText(info.debitWarning);
            tvSmsWarning.setVisibility(View.VISIBLE);
            tvSmsReliable.setVisibility(View.GONE);
        } else if (!"Other / Custom".equals(bankName)) {
            // Green badge for reliable banks
            tvSmsReliable.setVisibility(View.VISIBLE);
            tvSmsWarning.setVisibility(View.GONE);
        } else {
            tvSmsWarning.setVisibility(View.GONE);
            tvSmsReliable.setVisibility(View.GONE);
        }

        // 4. Suggestion chips
        chipGroupSuggestions.removeAllViews();
        if (info.options.size() > 1) {
            chipGroupSuggestions.setVisibility(View.VISIBLE);
            for (BankSenderInfo.SenderOption opt : info.options) {
                if (opt.senderId.isEmpty()) continue;
                Chip chip = new Chip(requireContext());
                chip.setText(opt.label + "\n" + opt.senderId);
                chip.setCheckable(true);
                chip.setChecked(opt.senderId.equals(info.primarySenderId));
                chip.setOnClickListener(v -> {
                    etSenderId.setText(opt.senderId);
                    for (int i = 0; i < chipGroupSuggestions.getChildCount(); i++) {
                        View child = chipGroupSuggestions.getChildAt(i);
                        if (child instanceof Chip && child != chip)
                            ((Chip) child).setChecked(false);
                    }
                    chip.setChecked(true);
                });
                chipGroupSuggestions.addView(chip);
            }
        } else {
            chipGroupSuggestions.setVisibility(View.GONE);
        }

        // 5. Hint text
        tilSenderId.setHint(info.primarySenderId.isEmpty()
                ? "Enter SMS Sender ID manually"
                : "SMS Sender ID (auto-filled — editable)");
    }

    // ── Add account ───────────────────────────────────────────────────────────

    private void addAccount() {
        String last4    = etLast4.getText()    != null ? etLast4.getText().toString().trim()    : "";
        String senderId = etSenderId.getText() != null ? etSenderId.getText().toString().trim() : "";
        String bankName = bankNames.get(spinnerBank.getSelectedItemPosition());

        if (last4.length() != 4 || !last4.matches("\\d{4}")) {
            Toast.makeText(requireContext(),
                    "Enter the last 4 digits of your account number",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (senderId.isEmpty()) {
            Toast.makeText(requireContext(),
                    "SMS Sender ID is required. Check your bank's transaction SMS.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        BankAccount acc = new BankAccount();
        acc.bankName    = bankName;
        acc.last4Digits = last4;
        acc.smsSenderId = senderId.toUpperCase();
        acc.userId      = 0;

        addedAccounts.add(acc);
        refreshAccountList();
        etLast4.setText("");
    }

    // ── Refresh list ──────────────────────────────────────────────────────────

    private void refreshAccountList() {
        accountsContainer.removeAllViews();
        if (addedAccounts.isEmpty()) {
            tvNoAccounts.setVisibility(View.VISIBLE);
        } else {
            tvNoAccounts.setVisibility(View.GONE);
            for (int i = 0; i < addedAccounts.size(); i++) {
                BankAccount acc = addedAccounts.get(i);
                View row = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_added_account, accountsContainer, false);

                TextView  tvLabel   = row.findViewById(R.id.tvAccountLabel);
                ImageView ivRemove  = row.findViewById(R.id.ivRemoveAccount);

                // Show reliability badge in list too
                boolean restricted = BankSenderRegistry.isRestricted(acc.bankName);
                tvLabel.setText((restricted ? "⚠ " : "✓ ") +
                        acc.bankName + "  ••••" + acc.last4Digits +
                        "  |  " + acc.smsSenderId);

                final int idx = i;
                ivRemove.setOnClickListener(v -> {
                    addedAccounts.remove(idx);
                    refreshAccountList();
                });

                accountsContainer.addView(row);
            }
        }
    }

    // ── Proceed ───────────────────────────────────────────────────────────────

    private void proceedToNextStep() {
        if (addedAccounts.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Add at least one bank account to continue.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // If any added bank is restricted, show a one-time reminder before proceeding
        boolean anyRestricted = false;
        for (BankAccount acc : addedAccounts) {
            if (BankSenderRegistry.isRestricted(acc.bankName)) {
                anyRestricted = true;
                break;
            }
        }

        if (anyRestricted) {
            new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Heads up about SMS tracking")
                .setMessage(
                    "One or more of your banks (HDFC / ICICI / Axis / Kotak) " +
                    "do not send SMS for transactions below ₹100.\n\n" +
                    "For those small payments, use the QR Scan button on the " +
                    "home screen to log them manually before paying."
                )
                .setPositiveButton("Got it, continue", (d, w) -> saveAndProceed())
                .setNegativeButton("Go back", null)
                .show();
        } else {
            saveAndProceed();
        }
    }

    private void saveAndProceed() {
        OnboardingActivity host = (OnboardingActivity) requireActivity();

        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            User user         = new User();
            user.name         = host.userName;
            user.pinHash      = null;
            user.useBiometric = false;
            long userId       = db.userDao().insert(user);

            for (BankAccount acc : addedAccounts) {
                acc.userId = (int) userId;
                db.bankAccountDao().insert(acc);
            }

            syncSmsPatterns(db);

            requireActivity().runOnUiThread(() ->
                host.showStep(new Step3PermissionsFragment()));
        });
    }

    private void syncSmsPatterns(AppDatabase db) {
        for (BankAccount acc : addedAccounts) {
            boolean exists = !db.smsPatternDao()
                    .getPatternsForSender(acc.smsSenderId).isEmpty();
            if (!exists) {
                com.example.expensetracker.data.entity.SmsPattern p =
                        new com.example.expensetracker.data.entity.SmsPattern();
                p.bankName      = acc.bankName;
                p.smsSenderId   = acc.smsSenderId;
                p.amountRegex   = "(?:Rs\\.?|INR)\\s?([\\d,]+\\.?\\d*)";
                p.merchantRegex = "(?:to|at|@)\\s+([A-Za-z0-9 &'._-]{3,40})(?:\\s|$|\\.|,)";
                p.typeRegex     = "(?i)(debited|debit|credited|credit|paid|received|withdrawn)";
                p.refRegex      = "(?:UPI[\\s:/]*Ref[\\s.#:]*|Ref\\.?\\s*No\\.?\\s*)(\\d{10,20})";
                p.isEnabled     = true;
                db.smsPatternDao().insert(p);
            }
        }
    }
}
