package com.example.expensetracker.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.User;
import com.example.expensetracker.utils.BiometricHelper;
import com.example.expensetracker.utils.PinHelper;

public class Step4LockSetupFragment extends Fragment {

    private RadioGroup   rgLockType;
    private LinearLayout pinLayout;
    private EditText     etPin, etConfirmPin;
    private Button       btnFinish;
    private TextView     tvError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboard_step4, container, false);

        rgLockType   = view.findViewById(R.id.rgLockType);
        pinLayout    = view.findViewById(R.id.pinInputLayout);
        etPin        = view.findViewById(R.id.etPin);
        etConfirmPin = view.findViewById(R.id.etConfirmPin);
        btnFinish    = view.findViewById(R.id.btnFinish);
        tvError      = view.findViewById(R.id.tvError);

        // If biometric not available, hide that option
        if (!BiometricHelper.isBiometricAvailable(requireContext())) {
            view.findViewById(R.id.rbBiometric).setVisibility(View.GONE);
        }

        rgLockType.setOnCheckedChangeListener((group, checkedId) -> {
            pinLayout.setVisibility(
                checkedId == R.id.rbPin ? View.VISIBLE : View.GONE);
        });

        btnFinish.setOnClickListener(v -> saveLockAndFinish());

        return view;
    }

    private void saveLockAndFinish() {
        int checkedId = rgLockType.getCheckedRadioButtonId();
        if (checkedId == -1) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Please choose a lock method.");
            return;
        }

        boolean useBiometric = (checkedId == R.id.rbBiometric);
        String  pinHash      = null;

        if (!useBiometric) {
            String pin        = etPin.getText().toString().trim();
            String confirmPin = etConfirmPin.getText().toString().trim();

            if (!PinHelper.isValidFormat(pin)) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("PIN must be 4-6 digits.");
                return;
            }
            if (!pin.equals(confirmPin)) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("PINs do not match.");
                return;
            }
            pinHash = PinHelper.hash(pin);
        }

        final String finalPinHash = pinHash;
        final boolean finalBiometric = useBiometric;

        AppDatabase.dbExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            User user = db.userDao().getUser();
            if (user != null) {
                user.pinHash      = finalPinHash;
                user.useBiometric = finalBiometric;
                db.userDao().update(user);
            }
            requireActivity().runOnUiThread(() ->
                ((OnboardingActivity) requireActivity()).finishOnboarding());
        });
    }
}
