package com.example.expensetracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single source of truth for user-facing appearance settings.
 *
 * Keys
 * ----
 * THEME       : "light" | "dark" | "amoled"           (default "light")
 * FONT_SIZE   : "small" | "normal" | "large" | "xlarge" (default "normal")
 * PALETTE     : "blue" | "green" | "purple" | "orange" | "red" (default "blue")
 */
public class AppPreferences {

    private static final String PREFS_NAME  = "app_appearance";
    private static final String KEY_THEME   = "theme";
    private static final String KEY_FONT    = "font_size";
    private static final String KEY_PALETTE = "palette";

    // ── UPI-related preferences ───────────────────────────────────────────────
    private static final String PREFS_UPI = "upi_settings";
    private static final String KEY_PREFERRED_QR_APP = "preferred_qr_app";
    private static final String KEY_PREFERRED_DIRECT_APP = "preferred_direct_app";
    private static final String KEY_PENDING_TXN_ID = "pending_transaction_id";
    private static final String KEY_PENDING_TIMEOUT_HOURS = "pending_timeout_hours";
    private static final String KEY_QR_PRE_SCAN_ENABLED = "qr_pre_scan_enabled";

    // ── Theme constants ───────────────────────────────────────────────────────
    public static final String THEME_LIGHT  = "light";
    public static final String THEME_DARK   = "dark";
    public static final String THEME_AMOLED = "amoled";

    // ── Font-size constants ───────────────────────────────────────────────────
    public static final String FONT_SMALL   = "small";
    public static final String FONT_NORMAL  = "normal";
    public static final String FONT_LARGE   = "large";
    public static final String FONT_XLARGE  = "xlarge";

    // ── Palette constants ─────────────────────────────────────────────────────
    public static final String PALETTE_BLUE   = "blue";
    public static final String PALETTE_GREEN  = "green";
    public static final String PALETTE_PURPLE = "purple";
    public static final String PALETTE_ORANGE = "orange";
    public static final String PALETTE_RED    = "red";

    // ── Font scale values (multiplier applied to base sp via Activity config) ─
    public static float getFontScale(String fontSize) {
        switch (fontSize) {
            case FONT_SMALL:  return 0.85f;
            case FONT_LARGE:  return 1.15f;
            case FONT_XLARGE: return 1.30f;
            default:          return 1.00f; // FONT_NORMAL
        }
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                  .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getTheme(Context ctx) {
        return prefs(ctx).getString(KEY_THEME, THEME_LIGHT);
    }

    public static void setTheme(Context ctx, String theme) {
        prefs(ctx).edit().putString(KEY_THEME, theme).apply();
    }

    public static String getFontSize(Context ctx) {
        return prefs(ctx).getString(KEY_FONT, FONT_NORMAL);
    }

    public static void setFontSize(Context ctx, String fontSize) {
        prefs(ctx).edit().putString(KEY_FONT, fontSize).apply();
    }

    public static String getPalette(Context ctx) {
        return prefs(ctx).getString(KEY_PALETTE, PALETTE_BLUE);
    }

    public static void setPalette(Context ctx, String palette) {
        prefs(ctx).edit().putString(KEY_PALETTE, palette).apply();
    }

    // ── UPI App Preferences ───────────────────────────────────────────────────

    private static SharedPreferences upiPrefs(Context ctx) {
        return ctx.getApplicationContext()
                  .getSharedPreferences(PREFS_UPI, Context.MODE_PRIVATE);
    }

    /** Get preferred UPI app for QR payments */
    public static String getPreferredUpiAppForQr(Context ctx) {
        return upiPrefs(ctx).getString(KEY_PREFERRED_QR_APP, null);
    }

    public static void setPreferredUpiAppForQr(Context ctx, String packageName) {
        upiPrefs(ctx).edit().putString(KEY_PREFERRED_QR_APP, packageName).apply();
    }

    /** Get preferred UPI app for direct payments */
    public static String getPreferredUpiAppForDirect(Context ctx) {
        return upiPrefs(ctx).getString(KEY_PREFERRED_DIRECT_APP, null);
    }

    public static void setPreferredUpiAppForDirect(Context ctx, String packageName) {
        upiPrefs(ctx).edit().putString(KEY_PREFERRED_DIRECT_APP, packageName).apply();
    }

    /** Pending transaction ID tracking */
    public static long getPendingTransactionId(Context ctx) {
        return upiPrefs(ctx).getLong(KEY_PENDING_TXN_ID, -1);
    }

    public static void setPendingTransactionId(Context ctx, long txnId) {
        upiPrefs(ctx).edit().putLong(KEY_PENDING_TXN_ID, txnId).apply();
    }

    public static void clearPendingTransactionId(Context ctx) {
        upiPrefs(ctx).edit().remove(KEY_PENDING_TXN_ID).apply();
    }

    /** QR pre-scan enabled (default: true) */
    public static boolean isQrPreScanEnabled(Context ctx) {
        return upiPrefs(ctx).getBoolean(KEY_QR_PRE_SCAN_ENABLED, true);
    }

    public static void setQrPreScanEnabled(Context ctx, boolean enabled) {
        upiPrefs(ctx).edit().putBoolean(KEY_QR_PRE_SCAN_ENABLED, enabled).apply();
    }

    /** Pending transaction timeout in hours (default: 24) */
    public static int getPendingTimeoutHours(Context ctx) {
        return upiPrefs(ctx).getInt(KEY_PENDING_TIMEOUT_HOURS, 24);
    }

    public static void setPendingTimeoutHours(Context ctx, int hours) {
        upiPrefs(ctx).edit().putInt(KEY_PENDING_TIMEOUT_HOURS, hours).apply();
    }

    /** 
     * Get preferred UPI app (for general UPI payments)
     * This is an alias that defaults to the direct payment preference
     */
    public static String getPreferredUpiApp(Context ctx) {
        return getPreferredUpiAppForDirect(ctx);
    }

    /** 
     * Set preferred UPI app (for general UPI payments)
     * This is an alias that sets the direct payment preference
     */
    public static void setPreferredUpiApp(Context ctx, String packageName) {
        setPreferredUpiAppForDirect(ctx, packageName);
    }
}
