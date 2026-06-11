package com.example.expensetracker.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.data.AppDatabase;
import com.example.expensetracker.ui.lock.LockActivity;
import com.example.expensetracker.ui.onboarding.OnboardingActivity;
import com.example.expensetracker.utils.AppThemeHelper;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 1500;
    private static final String PREFS_NAME    = "et_prefs";
    private static final String KEY_ONBOARDED = "onboarding_complete";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppThemeHelper.apply(this); // BEFORE super.onCreate()
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean onboarded = prefs.getBoolean(KEY_ONBOARDED, false);

            if (onboarded) {
                // User has completed onboarding → go to Lock screen
                startActivity(new Intent(this, LockActivity.class));
            } else {
                // First launch → go to Onboarding
                startActivity(new Intent(this, OnboardingActivity.class));
            }
            finish();
        }, SPLASH_DELAY_MS);
    }
}
