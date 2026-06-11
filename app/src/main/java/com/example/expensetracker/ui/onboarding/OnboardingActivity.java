package com.example.expensetracker.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.expensetracker.R;
import com.example.expensetracker.ui.MainActivity;
import com.example.expensetracker.utils.AppThemeHelper;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS_NAME    = "et_prefs";
    public static final String KEY_ONBOARDED = "onboarding_complete";

    // Shared data collected across steps
    public String  userName;
    public String  pinHash;
    public boolean useBiometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppThemeHelper.apply(this); // BEFORE super.onCreate()
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        if (savedInstanceState == null) {
            showStep(new Step1NameFragment());
        }
    }

    public void showStep(Fragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        ft.replace(R.id.onboardingContainer, fragment);
        ft.addToBackStack(null);
        ft.commit();
    }

    /** Called when all onboarding steps are complete. */
    public void finishOnboarding() {
        SharedPreferences.Editor editor =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_ONBOARDED, true);
        editor.apply();

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
