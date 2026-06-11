package com.example.expensetracker.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.expensetracker.R;
import com.example.expensetracker.ui.MainActivity;
import com.example.expensetracker.utils.AppPreferences;
import com.example.expensetracker.utils.AppThemeHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class SettingsFragment extends Fragment {

    // ── Pending selections (written to prefs only on "Apply") ─────────────────
    private String pendingFont;
    private String pendingTheme;
    private String pendingPalette;

    // ── Views ─────────────────────────────────────────────────────────────────
    private ChipGroup cgFont;
    private TextView  tvFontPreview;

    // Theme cards
    private MaterialCardView cvLight, cvDark, cvAmoled;

    // Palette circles
    private ImageView ivPalBlue, ivPalGreen, ivPalPurple, ivPalOrange, ivPalRed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Read saved preferences as starting values
        pendingFont    = AppPreferences.getFontSize(requireContext());
        pendingTheme   = AppPreferences.getTheme(requireContext());
        pendingPalette = AppPreferences.getPalette(requireContext());

        bindViews(view);
        restoreSelections();

        // Font chips
        cgFont.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipFontSmall)  pendingFont = AppPreferences.FONT_SMALL;
            else if (id == R.id.chipFontLarge)  pendingFont = AppPreferences.FONT_LARGE;
            else if (id == R.id.chipFontXLarge) pendingFont = AppPreferences.FONT_XLARGE;
            else                                pendingFont = AppPreferences.FONT_NORMAL;
            updateFontPreview();
        });

        // Theme card clicks
        view.findViewById(R.id.cardThemeLight) .setOnClickListener(v -> selectTheme(AppPreferences.THEME_LIGHT));
        view.findViewById(R.id.cardThemeDark)  .setOnClickListener(v -> selectTheme(AppPreferences.THEME_DARK));
        view.findViewById(R.id.cardThemeAmoled).setOnClickListener(v -> selectTheme(AppPreferences.THEME_AMOLED));

        // Palette clicks
        view.findViewById(R.id.palBlue)  .setOnClickListener(v -> selectPalette(AppPreferences.PALETTE_BLUE));
        view.findViewById(R.id.palGreen) .setOnClickListener(v -> selectPalette(AppPreferences.PALETTE_GREEN));
        view.findViewById(R.id.palPurple).setOnClickListener(v -> selectPalette(AppPreferences.PALETTE_PURPLE));
        view.findViewById(R.id.palOrange).setOnClickListener(v -> selectPalette(AppPreferences.PALETTE_ORANGE));
        view.findViewById(R.id.palRed)   .setOnClickListener(v -> selectPalette(AppPreferences.PALETTE_RED));

        // Apply button
        MaterialButton btnApply = view.findViewById(R.id.btnApplySettings);
        btnApply.setOnClickListener(v -> applyAndRestart());
    }

    // ── Bind view references ──────────────────────────────────────────────────

    private void bindViews(View root) {
        cgFont       = root.findViewById(R.id.chipGroupFont);
        tvFontPreview= root.findViewById(R.id.tvFontPreview);

        cvLight  = root.findViewById(R.id.themePreviewLight);
        cvDark   = root.findViewById(R.id.themePreviewDark);
        cvAmoled = root.findViewById(R.id.themePreviewAmoled);

        ivPalBlue   = root.findViewById(R.id.ivPalBlue);
        ivPalGreen  = root.findViewById(R.id.ivPalGreen);
        ivPalPurple = root.findViewById(R.id.ivPalPurple);
        ivPalOrange = root.findViewById(R.id.ivPalOrange);
        ivPalRed    = root.findViewById(R.id.ivPalRed);
    }

    // ── Restore saved state into UI ───────────────────────────────────────────

    private void restoreSelections() {
        // Font chip
        int chipId;
        switch (pendingFont) {
            case AppPreferences.FONT_SMALL:  chipId = R.id.chipFontSmall;  break;
            case AppPreferences.FONT_LARGE:  chipId = R.id.chipFontLarge;  break;
            case AppPreferences.FONT_XLARGE: chipId = R.id.chipFontXLarge; break;
            default:                          chipId = R.id.chipFontNormal; break;
        }
        ((Chip) requireView().findViewById(chipId)).setChecked(true);
        updateFontPreview();

        // Theme stroke
        highlightThemeCard(pendingTheme);

        // Palette checkmark
        highlightPalette(pendingPalette);
    }

    // ── Font preview ──────────────────────────────────────────────────────────

    private void updateFontPreview() {
        float scale = AppPreferences.getFontScale(pendingFont);
        // Base size is 15sp; multiply by scale
        tvFontPreview.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f * scale);
    }

    // ── Theme selection ───────────────────────────────────────────────────────

    private void selectTheme(String theme) {
        pendingTheme = theme;
        highlightThemeCard(theme);
    }

    private void highlightThemeCard(String theme) {
        int strokePx = dpToPx(2);
        int primary  = androidx.core.content.ContextCompat.getColor(
                requireContext(), R.color.primary);
        int none     = 0x00000000;

        cvLight .setStrokeColor(AppPreferences.THEME_LIGHT .equals(theme) ? primary : none);
        cvDark  .setStrokeColor(AppPreferences.THEME_DARK  .equals(theme) ? primary : none);
        cvAmoled.setStrokeColor(AppPreferences.THEME_AMOLED.equals(theme) ? primary : none);

        cvLight .setStrokeWidth(AppPreferences.THEME_LIGHT .equals(theme) ? strokePx : 0);
        cvDark  .setStrokeWidth(AppPreferences.THEME_DARK  .equals(theme) ? strokePx : 0);
        cvAmoled.setStrokeWidth(AppPreferences.THEME_AMOLED.equals(theme) ? strokePx : 0);
    }

    // ── Palette selection ─────────────────────────────────────────────────────

    private void selectPalette(String palette) {
        pendingPalette = palette;
        highlightPalette(palette);
    }

    private void highlightPalette(String palette) {
        // Show a checkmark overlay on the selected circle
        int check = R.drawable.ic_check_white;
        ivPalBlue  .setImageResource(AppPreferences.PALETTE_BLUE  .equals(palette) ? check : 0);
        ivPalGreen .setImageResource(AppPreferences.PALETTE_GREEN .equals(palette) ? check : 0);
        ivPalPurple.setImageResource(AppPreferences.PALETTE_PURPLE.equals(palette) ? check : 0);
        ivPalOrange.setImageResource(AppPreferences.PALETTE_ORANGE.equals(palette) ? check : 0);
        ivPalRed   .setImageResource(AppPreferences.PALETTE_RED   .equals(palette) ? check : 0);
    }

    // ── Apply & restart ───────────────────────────────────────────────────────

    private void applyAndRestart() {
        AppPreferences.setFontSize(requireContext(), pendingFont);
        AppPreferences.setTheme   (requireContext(), pendingTheme);
        AppPreferences.setPalette (requireContext(), pendingPalette);

        // Apply night mode now so the restarted Activity inflates with the correct DayNight mode.
        // (AppThemeHelper.apply() will also call this again in the new Activity's onCreate,
        //  but calling it here ensures the mode is set before we even launch the new task.)
        AppThemeHelper.applyNightMode(requireContext());

        // Fully restart the app task — new MainActivity will call AppThemeHelper.apply()
        // before super.onCreate(), picking up the updated night mode + palette + font.
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        requireContext().startActivity(intent);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int dpToPx(float dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
