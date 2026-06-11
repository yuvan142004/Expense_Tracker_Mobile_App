package com.example.expensetracker.utils;

import android.app.Activity;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.expensetracker.R;

/**
 * Applies the user-chosen theme + color-palette + font-scale to an Activity.
 *
 * Call order (CRITICAL):
 *   1. AppThemeHelper.apply(this)   ← BEFORE super.onCreate()
 *   2. super.onCreate(savedInstanceState)
 *   3. setContentView(...)
 *
 * Night mode is driven by AppCompatDelegate — it works globally and survives
 * the Activity recreation triggered by apply-and-restart.
 *
 * The palette overlay is applied via getTheme().applyStyle() which layers
 * colorPrimary overrides on top of the base theme.
 */
public class AppThemeHelper {

    /**
     * Must be called in EVERY Activity.onCreate(), BEFORE super.onCreate().
     *
     * Also re-applies night mode here (not just in Application.onCreate) so that
     * when the user changes the theme and the Activity restarts, the new DayNight
     * mode is set before the window is inflated.
     */
    public static void apply(Activity activity) {
        String theme   = AppPreferences.getTheme(activity);
        String palette = AppPreferences.getPalette(activity);
        String font    = AppPreferences.getFontSize(activity);

        // 0. Night mode — must be set before super.onCreate() so DayNight resolves correctly
        applyNightMode(activity);

        // 1. AMOLED overlay — applied on top of the dark DayNight theme
        //    (only when user selected AMOLED; normal dark uses DayNight defaults)
        if (AppPreferences.THEME_AMOLED.equals(theme)) {
            activity.getTheme().applyStyle(R.style.ThemeOverlay_ExpenseTracker_Amoled, true);
        }

        // 2. Palette overlay — overrides colorPrimary / statusBarColor
        int paletteStyle = paletteStyleFor(palette);
        if (paletteStyle != 0) {
            activity.getTheme().applyStyle(paletteStyle, true);
        }

        // 3. Font scale — applied via Configuration override.
        float scale = AppPreferences.getFontScale(font);
        Configuration config = new Configuration(
                activity.getResources().getConfiguration());
        config.fontScale = scale;
        //noinspection deprecation
        activity.getResources().updateConfiguration(
                config, activity.getResources().getDisplayMetrics());
    }

    /**
     * Call this once from Application.onCreate() to set night-mode globally.
     * AppCompatDelegate persists the mode across Activity recreations.
     */
    public static void applyNightMode(android.content.Context ctx) {
        String theme = AppPreferences.getTheme(ctx);
        switch (theme) {
            case AppPreferences.THEME_DARK:
            case AppPreferences.THEME_AMOLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    private static int paletteStyleFor(String palette) {
        switch (palette) {
            case AppPreferences.PALETTE_GREEN:  return R.style.Palette_Green;
            case AppPreferences.PALETTE_PURPLE: return R.style.Palette_Purple;
            case AppPreferences.PALETTE_ORANGE: return R.style.Palette_Orange;
            case AppPreferences.PALETTE_RED:    return R.style.Palette_Red;
            default:                            return R.style.Palette_Blue;
        }
    }
}
