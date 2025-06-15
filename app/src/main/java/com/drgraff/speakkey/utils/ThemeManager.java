package com.drgraff.speakkey.utils;

import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage theme preferences
 */
public class ThemeManager {
    
    // Key for the theme selection preference
    public static final String PREF_KEY_DARK_MODE = "dark_mode";
    // Default value if the preference is not set
    public static final String THEME_DEFAULT = "default";

    /**
     * Apply the appropriate theme based on the theme preference
     * 
     * @param sharedPreferences SharedPreferences instance containing theme settings
     */
    public static void applyTheme(SharedPreferences sharedPreferences) {
        // Read the theme preference. Fallback to "default" (system default).
        String themeValue = sharedPreferences.getString(PREF_KEY_DARK_MODE, THEME_DEFAULT);
        
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // Assumes AppTheme.Dark or base dark theme is applied by the system
                break;
            case "oled":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // Assumes R.style.AppTheme_OLED is correctly parented from a DayNight theme
                // and will be picked up when MODE_NIGHT_YES is active.
                break;
            case "default":
            default: // Handles "default" or any unexpected values
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}