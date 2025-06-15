package com.drgraff.speakkey.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View; // Keep this import, it's used by activity.getWindow().getDecorView()
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R; // For R.id.toolbar

public class DynamicThemeApplicator {
    private static final String TAG = "DynamicThemeApplicator";

    // Default color values (ARGB integers)
    public static final int DEFAULT_OLED_PRIMARY = Color.parseColor("#BB86FC");
    public static final int DEFAULT_OLED_SECONDARY = Color.parseColor("#03DAC6");
    public static final int DEFAULT_OLED_BACKGROUND = Color.parseColor("#000000");
    public static final int DEFAULT_OLED_SURFACE = Color.parseColor("#0D0D0D");
    public static final int DEFAULT_OLED_TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    public static final int DEFAULT_OLED_ICON_TINT = Color.parseColor("#FFFFFF");
    public static final int DEFAULT_OLED_EDIT_TEXT_BACKGROUND = Color.parseColor("#1A1A1A");

    public static void applyOledColors(Activity activity, SharedPreferences prefs) {
        if (activity == null || prefs == null) {
            Log.e(TAG, "Activity or SharedPreferences is null. Cannot apply OLED colors.");
            return;
        }

        Log.d(TAG, "Applying custom OLED colors...");

        int oledBackgroundColor = prefs.getInt("pref_oled_color_background", DEFAULT_OLED_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_color_background: Value=0x%08X, Default=0x%08X", oledBackgroundColor, DEFAULT_OLED_BACKGROUND));

        int oledSurfaceColor = prefs.getInt("pref_oled_color_surface", DEFAULT_OLED_SURFACE);
        Log.d(TAG, String.format("pref_oled_color_surface: Value=0x%08X, Default=0x%08X", oledSurfaceColor, DEFAULT_OLED_SURFACE));

        int oledTextColorPrimary = prefs.getInt("pref_oled_color_text_primary", DEFAULT_OLED_TEXT_PRIMARY);
        Log.d(TAG, String.format("pref_oled_color_text_primary: Value=0x%08X, Default=0x%08X", oledTextColorPrimary, DEFAULT_OLED_TEXT_PRIMARY));

        int oledIconTintColor = prefs.getInt("pref_oled_color_icon_tint", DEFAULT_OLED_ICON_TINT);
        Log.d(TAG, String.format("pref_oled_color_icon_tint: Value=0x%08X, Default=0x%08X", oledIconTintColor, DEFAULT_OLED_ICON_TINT));

        int primaryOledColor = prefs.getInt("pref_oled_color_primary", DEFAULT_OLED_PRIMARY);
        Log.d(TAG, String.format("pref_oled_color_primary: Value=0x%08X, Default=0x%08X", primaryOledColor, DEFAULT_OLED_PRIMARY));

        int secondaryOledColor = prefs.getInt("pref_oled_color_secondary", DEFAULT_OLED_SECONDARY);
        Log.d(TAG, String.format("pref_oled_color_secondary: Value=0x%08X, Default=0x%08X", secondaryOledColor, DEFAULT_OLED_SECONDARY));

        int oledEditTextBackgroundColor = prefs.getInt("pref_oled_edit_text_background", DEFAULT_OLED_EDIT_TEXT_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_edit_text_background: Value=0x%08X, Default=0x%08X", oledEditTextBackgroundColor, DEFAULT_OLED_EDIT_TEXT_BACKGROUND));

        activity.getWindow().setStatusBarColor(oledBackgroundColor);
        activity.getWindow().setNavigationBarColor(oledBackgroundColor);
        activity.getWindow().getDecorView().setBackgroundColor(oledBackgroundColor);

        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Log.d(TAG, "Toolbar found, applying colors."); // Removed current background as it might be complex object
            // Use primaryOledColor for the Toolbar background
            toolbar.setBackgroundColor(primaryOledColor);
            toolbar.setTitleTextColor(oledTextColorPrimary);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setColorFilter(oledIconTintColor, PorterDuff.Mode.SRC_ATOP);
            }
            // Log the primaryOledColor that was applied to the background
            Log.d(TAG, String.format("Toolbar colors applied. New Toolbar BG color: 0x%08X", primaryOledColor));
        } else {
            Log.w(TAG, "Toolbar not found (R.id.toolbar). Cannot apply toolbar specific colors.");
        }
        Log.d(TAG, "Finished applying custom OLED colors.");
    } // End of applyOledColors method
} // End of DynamicThemeApplicator class
