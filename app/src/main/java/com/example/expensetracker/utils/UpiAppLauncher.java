package com.example.expensetracker.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Launches UPI apps with different methods.
 * APK-safe: Does NOT pre-fill UPI payment data.
 */
public class UpiAppLauncher {

    private static final String TAG = "UpiAppLauncher";

    /**
     * Launch UPI app's QR scanner (APK-safe, no pre-filled data).
     *
     * Priority:
     * 1. Try app-specific deep link (e.g., "gpay://upi/scan")
     * 2. Try generic UPI scan intent ("upi://scan")
     * 3. Launch app main screen (user navigates manually)
     */
    public static boolean launchQrScanner(Context context, UpiAppDetector.UpiApp app) {
        if (app == null || !app.isInstalled) {
            Log.w(TAG, "App not found or not installed");
            return false;
        }

        Log.d(TAG, "Attempting to launch: " + app.name + " (" + app.packageName + ")");

        // ═══════════════════════════════════════════════════════
        // METHOD 1: App-specific deep link
        // ═══════════════════════════════════════════════════════
        if (app.qrScanDeepLink != null) {
            Log.d(TAG, "Trying app-specific deep link: " + app.qrScanDeepLink);
            if (tryLaunchIntent(context, app.packageName, app.qrScanDeepLink)) {
                Log.d(TAG, "✓ Launched via app-specific deep link: " + app.qrScanDeepLink);
                return true;
            } else {
                Log.d(TAG, "✗ App-specific deep link failed");
            }
        }

        // ═══════════════════════════════════════════════════════
        // METHOD 2: Generic UPI scan intent
        // ═══════════════════════════════════════════════════════
        if (app.genericUpiScanIntent != null) {
            Log.d(TAG, "Trying generic UPI scan intent: " + app.genericUpiScanIntent);
            if (tryLaunchIntent(context, app.packageName, app.genericUpiScanIntent)) {
                Log.d(TAG, "✓ Launched via generic UPI scan intent: " + app.genericUpiScanIntent);
                return true;
            } else {
                Log.d(TAG, "✗ Generic UPI scan intent failed");
            }
        }

        // ═══════════════════════════════════════════════════════
        // METHOD 3: Launch app home screen
        // ═══════════════════════════════════════════════════════
        Log.d(TAG, "Falling back to launching main app screen");
        boolean result = launchMainApp(context, app.packageName);
        if (result) {
            Log.d(TAG, "✓ Successfully launched main app");
        } else {
            Log.e(TAG, "✗ All launch methods failed for: " + app.name);
        }
        return result;
    }

    /**
     * Launch UPI app's QR scanner by package name.
     * This is a convenience method that looks up the app first.
     */
    public static boolean launchQrScanner(Context context, String packageName) {
        Log.d(TAG, "launchQrScanner called with package: " + packageName);
        UpiAppDetector.UpiApp app = UpiAppDetector.findApp(context, packageName);
        if (app == null) {
            Log.e(TAG, "Could not find UPI app for package: " + packageName);
            return false;
        }
        if (!app.isInstalled) {
            Log.e(TAG, "UPI app is not installed: " + packageName);
            return false;
        }
        return launchQrScanner(context, app);
    }

    /**
     * Launch UPI app's main screen.
     */
    public static boolean launchMainApp(Context context, String packageName) {
        try {
            Intent launchIntent = context.getPackageManager()
                                         .getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.d(TAG, "Launched main app screen: " + packageName);
                return true;
            } else {
                Log.w(TAG, "Launch intent is null for package: " + packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch main app: " + e.getMessage());
        }
        return false;
    }

    /**
     * Try to launch an intent with given URI.
     */
    private static boolean tryLaunchIntent(Context context, String packageName, String uriString) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uriString));
            intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Intent launch failed for " + uriString + ": " + e.getMessage());
        }
        return false;
    }
}
