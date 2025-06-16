package com.drgraff.speakkey.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.Log;
import android.view.View;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;

public class DynamicThemeApplicator {
    private static final String TAG = "DynamicThemeApplicator";

    // New Default color values (ARGB integers)
    public static final int DEFAULT_OLED_TOPBAR_BACKGROUND = Color.parseColor("#03DAC6"); // Cyan
    public static final int DEFAULT_OLED_TOPBAR_TEXT_ICON = Color.parseColor("#000000");  // Black
    public static final int DEFAULT_OLED_MAIN_BACKGROUND = Color.parseColor("#000000");   // Black
    public static final int DEFAULT_OLED_SURFACE_BACKGROUND = Color.parseColor("#0D0D0D"); // Dark Grey
    public static final int DEFAULT_OLED_GENERAL_TEXT_PRIMARY = Color.parseColor("#FFFFFF"); // White
    public static final int DEFAULT_OLED_GENERAL_TEXT_SECONDARY = Color.parseColor("#AAAAAA"); // Light Grey
    public static final int DEFAULT_OLED_BUTTON_BACKGROUND = Color.parseColor("#03DAC6");  // Cyan
    public static final int DEFAULT_OLED_BUTTON_TEXT_ICON = Color.parseColor("#000000");   // Black
    public static final int DEFAULT_OLED_TEXTBOX_BACKGROUND = Color.parseColor("#1A1A1A"); // Darker Grey
    public static final int DEFAULT_OLED_TEXTBOX_ACCENT = Color.parseColor("#03DAC6");    // Cyan
    public static final int DEFAULT_OLED_ACCENT_GENERAL = Color.parseColor("#03DAC6");      // Cyan

    public static void applyOledColors(Activity activity, SharedPreferences prefs) {
        if (activity == null || prefs == null) {
            Log.e(TAG, "Activity or SharedPreferences is null. Cannot apply OLED colors.");
            return;
        }

        Log.d(TAG, "Applying custom OLED colors using new grouped keys...");

        // Retrieve all new grouped preference values
        int topbarBackgroundColor = prefs.getInt("pref_oled_topbar_background", DEFAULT_OLED_TOPBAR_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_topbar_background: Value=0x%08X, Default=0x%08X", topbarBackgroundColor, DEFAULT_OLED_TOPBAR_BACKGROUND));

        int topbarTextIconColor = prefs.getInt("pref_oled_topbar_text_icon", DEFAULT_OLED_TOPBAR_TEXT_ICON);
        Log.d(TAG, String.format("pref_oled_topbar_text_icon: Value=0x%08X, Default=0x%08X", topbarTextIconColor, DEFAULT_OLED_TOPBAR_TEXT_ICON));

        int mainBackgroundColor = prefs.getInt("pref_oled_main_background", DEFAULT_OLED_MAIN_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_main_background: Value=0x%08X, Default=0x%08X", mainBackgroundColor, DEFAULT_OLED_MAIN_BACKGROUND));

        int surfaceBackgroundColor = prefs.getInt("pref_oled_surface_background", DEFAULT_OLED_SURFACE_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_surface_background: Value=0x%08X, Default=0x%08X", surfaceBackgroundColor, DEFAULT_OLED_SURFACE_BACKGROUND));

        int generalTextPrimaryColor = prefs.getInt("pref_oled_general_text_primary", DEFAULT_OLED_GENERAL_TEXT_PRIMARY);
        Log.d(TAG, String.format("pref_oled_general_text_primary: Value=0x%08X, Default=0x%08X", generalTextPrimaryColor, DEFAULT_OLED_GENERAL_TEXT_PRIMARY));

        int generalTextSecondaryColor = prefs.getInt("pref_oled_general_text_secondary", DEFAULT_OLED_GENERAL_TEXT_SECONDARY);
        Log.d(TAG, String.format("pref_oled_general_text_secondary: Value=0x%08X, Default=0x%08X", generalTextSecondaryColor, DEFAULT_OLED_GENERAL_TEXT_SECONDARY));

        int buttonBackgroundColor = prefs.getInt("pref_oled_button_background", DEFAULT_OLED_BUTTON_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_button_background: Value=0x%08X, Default=0x%08X", buttonBackgroundColor, DEFAULT_OLED_BUTTON_BACKGROUND));

        int buttonTextIconColor = prefs.getInt("pref_oled_button_text_icon", DEFAULT_OLED_BUTTON_TEXT_ICON);
        Log.d(TAG, String.format("pref_oled_button_text_icon: Value=0x%08X, Default=0x%08X", buttonTextIconColor, DEFAULT_OLED_BUTTON_TEXT_ICON));

        int textboxBackgroundColor = prefs.getInt("pref_oled_textbox_background", DEFAULT_OLED_TEXTBOX_BACKGROUND);
        Log.d(TAG, String.format("pref_oled_textbox_background: Value=0x%08X, Default=0x%08X", textboxBackgroundColor, DEFAULT_OLED_TEXTBOX_BACKGROUND));

        int textboxAccentColor = prefs.getInt("pref_oled_textbox_accent", DEFAULT_OLED_TEXTBOX_ACCENT);
        Log.d(TAG, String.format("pref_oled_textbox_accent: Value=0x%08X, Default=0x%08X", textboxAccentColor, DEFAULT_OLED_TEXTBOX_ACCENT));

        int accentGeneralColor = prefs.getInt("pref_oled_accent_general", DEFAULT_OLED_ACCENT_GENERAL);
        Log.d(TAG, String.format("pref_oled_accent_general: Value=0x%08X, Default=0x%08X", accentGeneralColor, DEFAULT_OLED_ACCENT_GENERAL));

        // Apply colors to Window elements
        activity.getWindow().setStatusBarColor(mainBackgroundColor);
        activity.getWindow().setNavigationBarColor(mainBackgroundColor);
        activity.getWindow().getDecorView().setBackgroundColor(mainBackgroundColor);

        // Apply colors to Toolbar
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            Log.d(TAG, "Toolbar found, applying new grouped colors.");
            toolbar.setBackgroundColor(topbarBackgroundColor);
            toolbar.setTitleTextColor(topbarTextIconColor);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setColorFilter(topbarTextIconColor, PorterDuff.Mode.SRC_ATOP);
            }
            Log.d(TAG, String.format("Toolbar colors applied. BG: 0x%08X, Text/Icon: 0x%08X", topbarBackgroundColor, topbarTextIconColor));
        } else {
            Log.w(TAG, "Toolbar not found (R.id.toolbar). Cannot apply toolbar specific colors.");
        }
        Log.d(TAG, "Finished applying custom OLED colors via DynamicThemeApplicator.");
    }
}
