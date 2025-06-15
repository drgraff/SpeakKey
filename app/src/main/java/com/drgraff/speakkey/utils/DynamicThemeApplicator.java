package com.drgraff.speakkey.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View; // Required for findViewById if used on decorView
import androidx.appcompat.widget.Toolbar; // Ensure this is androidx.appcompat.widget.Toolbar

import com.drgraff.speakkey.R; // For R.id.toolbar if used as a generic ID

public class DynamicThemeApplicator {

    // Default color values (ARGB integers)
    // These should match the defaults used when initializing ColorPickerPreference
    public static final int DEFAULT_OLED_PRIMARY = Color.parseColor("#BB86FC");
    public static final int DEFAULT_OLED_SECONDARY = Color.parseColor("#03DAC6"); // Currently unused in this method
    public static final int DEFAULT_OLED_BACKGROUND = Color.parseColor("#000000");
    public static final int DEFAULT_OLED_SURFACE = Color.parseColor("#0D0D0D");
    public static final int DEFAULT_OLED_TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    // public static final int DEFAULT_OLED_TEXT_SECONDARY = Color.parseColor("#AAAAAA"); // Currently unused
    public static final int DEFAULT_OLED_ICON_TINT = Color.parseColor("#FFFFFF");
    public static final int DEFAULT_OLED_EDIT_TEXT_BACKGROUND = Color.parseColor("#1A1A1A");

    public static void applyOledColors(Activity activity, SharedPreferences prefs) {
        if (activity == null || prefs == null) {
            return;
        }

        int oledBackgroundColor = prefs.getInt("pref_oled_color_background", DEFAULT_OLED_BACKGROUND);
        int oledSurfaceColor = prefs.getInt("pref_oled_color_surface", DEFAULT_OLED_SURFACE);
        int oledTextColorPrimary = prefs.getInt("pref_oled_color_text_primary", DEFAULT_OLED_TEXT_PRIMARY);
        int oledIconTintColor = prefs.getInt("pref_oled_color_icon_tint", DEFAULT_OLED_ICON_TINT);
        // int primaryOledColor = prefs.getInt("pref_oled_color_primary", DEFAULT_OLED_PRIMARY); // Not directly used on toolbar bg now

        activity.getWindow().setStatusBarColor(oledBackgroundColor);
        activity.getWindow().setNavigationBarColor(oledBackgroundColor);

        // Explicitly set window background
        activity.getWindow().getDecorView().setBackgroundColor(oledBackgroundColor);

        // Assuming the activity has a Toolbar with id R.id.toolbar
        // This is a common convention but might need to be made more flexible if not all activities use this ID.
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(oledSurfaceColor); // Use surface color for toolbar background
            toolbar.setTitleTextColor(oledTextColorPrimary);
            if (toolbar.getNavigationIcon() != null) {
                // SRC_ATOP is a common mode for tinting.
                toolbar.getNavigationIcon().setColorFilter(oledIconTintColor, PorterDuff.Mode.SRC_ATOP);
            }
        }

        // Optional: Apply to root view background if needed, though windowBackground should handle it.
        // View rootView = activity.findViewById(android.R.id.content);
        // if (rootView != null) {
        //     rootView.setBackgroundColor(oledBackgroundColor);
        // }
    }
}
