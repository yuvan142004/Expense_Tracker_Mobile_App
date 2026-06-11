package com.example.expensetracker.utils;

import android.content.Context;
import android.os.Build;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

public class BiometricHelper {

    private BiometricHelper() {}

    public interface BiometricCallback {
        void onSuccess();
        void onError(String errorMessage);
        void onFailed();
    }

    /** Check if biometric authentication is available on this device. */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager manager = BiometricManager.from(context);
        int result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG |
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /** Show the biometric prompt. Must be called from a FragmentActivity. */
    public static void showPrompt(FragmentActivity activity, BiometricCallback callback) {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Verify Identity")
                .setSubtitle("Use your fingerprint or face to unlock")
                .setNegativeButtonText("Use PIN")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                        BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    callback.onSuccess();
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        // User tapped "Use PIN" — treat as an error so UI can show PIN entry
                        callback.onError("USE_PIN");
                    } else {
                        callback.onError(errString.toString());
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    callback.onFailed();
                }
            }
        );

        biometricPrompt.authenticate(promptInfo);
    }
}
