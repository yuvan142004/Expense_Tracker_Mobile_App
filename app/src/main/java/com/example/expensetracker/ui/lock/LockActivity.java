package com.example.expensetracker.ui.lock;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.data.entity.User;
import com.example.expensetracker.ui.MainActivity;
import com.example.expensetracker.utils.AppThemeHelper;
import com.example.expensetracker.utils.BiometricHelper;
import com.example.expensetracker.utils.PinHelper;

public class LockActivity extends AppCompatActivity {

    private LinearLayout pinLayout;
    private EditText     etPin;
    private Button       btnUnlock;
    private TextView     tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppThemeHelper.apply(this); // BEFORE super.onCreate()
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        pinLayout = findViewById(R.id.pinLayout);
        etPin     = findViewById(R.id.etPin);
        btnUnlock = findViewById(R.id.btnUnlock);
        tvError   = findViewById(R.id.tvLockError);

        loadUserAndAuthenticate();

        btnUnlock.setOnClickListener(v -> verifyPin());
    }

    private void loadUserAndAuthenticate() {
        AppDatabase.dbExecutor.execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUser();
            if (user == null) {
                // No user found — go to onboarding
                runOnUiThread(() -> {
                    startActivity(new Intent(this,
                        com.example.expensetracker.ui.onboarding.OnboardingActivity.class));
                    finish();
                });
                return;
            }

            runOnUiThread(() -> {
                if (user.useBiometric && BiometricHelper.isBiometricAvailable(this)) {
                    showBiometricPrompt(user);
                } else {
                    showPinLayout();
                }
            });
        });
    }

    private void showBiometricPrompt(User user) {
        BiometricHelper.showPrompt(this, new BiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                goToMain();
            }

            @Override
            public void onError(String errorMessage) {
                if ("USE_PIN".equals(errorMessage)) {
                    showPinLayout();
                } else {
                    showPinLayout();
                }
            }

            @Override
            public void onFailed() {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Biometric not recognised. Try again.");
            }
        });
    }

    private void showPinLayout() {
        pinLayout.setVisibility(View.VISIBLE);
    }

    private void verifyPin() {
        String entered = etPin.getText().toString().trim();
        if (entered.isEmpty()) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Please enter your PIN.");
            return;
        }

        AppDatabase.dbExecutor.execute(() -> {
            User user = AppDatabase.getInstance(this).userDao().getUser();
            boolean ok = user != null && PinHelper.verify(entered, user.pinHash);
            runOnUiThread(() -> {
                if (ok) {
                    goToMain();
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Incorrect PIN. Try again.");
                    etPin.setText("");
                }
            });
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
