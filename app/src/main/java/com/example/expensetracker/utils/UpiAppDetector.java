package com.example.expensetracker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects installed UPI apps on the device.
 * Provides metadata for known UPI apps including deep links.
 */
public class UpiAppDetector {

    private static final UpiApp[] KNOWN_UPI_APPS = {
        new UpiApp("Google Pay", "com.google.android.apps.nbu.paisa.user",
                   "gpay://upi/scan", "upi://scan"),
        new UpiApp("PhonePe", "com.phonepe.app",
                   "phonepe://scan", "upi://scan"),
        new UpiApp("Paytm", "net.one97.paytm",
                   "paytmmp://scan", null),
        new UpiApp("BHIM", "in.org.npci.upiapp",
                   "bhim://scan", "upi://scan"),
        new UpiApp("Amazon Pay", "in.amazon.mShop.android.shopping",
                   null, null),
        new UpiApp("WhatsApp Pay", "com.whatsapp",
                   null, null),
        new UpiApp("Freecharge", "com.freecharge.android",
                   null, null),
        new UpiApp("MobiKwik", "com.mobikwik_new",
                   null, null),
        new UpiApp("PayZapp", "com.itz.hdfcpayzapp",
                   null, null),
        new UpiApp("iMobile Pay", "com.csam.icici.bank.imobile",
                   null, null)
    };

    /**
     * Get list of all known UPI apps, marking which ones are installed.
     */
    public static List<UpiApp> getAllUpiApps(Context context) {
        PackageManager pm = context.getPackageManager();
        List<UpiApp> apps = new ArrayList<>();

        for (UpiApp app : KNOWN_UPI_APPS) {
            UpiApp copy = new UpiApp(app.name, app.packageName,
                                     app.qrScanDeepLink, app.genericUpiScanIntent);
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(app.packageName, 0);
                copy.isInstalled = true;
                copy.icon = pm.getApplicationIcon(appInfo);
                copy.displayName = (String) pm.getApplicationLabel(appInfo);
            } catch (PackageManager.NameNotFoundException e) {
                copy.isInstalled = false;
                copy.icon = null;
                copy.displayName = app.name; // Fallback to hardcoded name
            }
            apps.add(copy);
        }

        return apps;
    }

    /**
     * Get only installed UPI apps.
     */
    public static List<UpiApp> getInstalledUpiApps(Context context) {
        List<UpiApp> all = getAllUpiApps(context);
        List<UpiApp> installed = new ArrayList<>();
        for (UpiApp app : all) {
            if (app.isInstalled) {
                installed.add(app);
            }
        }
        return installed;
    }

    /**
     * Find app by package name.
     */
    public static UpiApp findApp(Context context, String packageName) {
        for (UpiApp app : getAllUpiApps(context)) {
            if (app.packageName.equals(packageName)) {
                return app;
            }
        }
        return null;
    }

    /**
     * Get app name by package.
     */
    public static String getAppName(Context context, String packageName) {
        UpiApp app = findApp(context, packageName);
        return app != null ? app.name : "UPI App";
    }

    /**
     * Check if app is installed.
     */
    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Data class for UPI app metadata.
     */
    public static class UpiApp {
        public String name;
        public String packageName;
        public String qrScanDeepLink;       // App-specific QR scanner deep link (e.g., "gpay://upi/scan")
        public String genericUpiScanIntent; // Generic UPI scan intent (e.g., "upi://scan")
        public boolean isInstalled;
        public Drawable icon;               // App icon drawable
        public String displayName;          // Display name from system

        public UpiApp(String name, String packageName, String qrScanDeepLink, String genericUpiScanIntent) {
            this.name = name;
            this.packageName = packageName;
            this.qrScanDeepLink = qrScanDeepLink;
            this.genericUpiScanIntent = genericUpiScanIntent;
            this.isInstalled = false;
            this.displayName = name; // Default to hardcoded name
        }
    }
}
