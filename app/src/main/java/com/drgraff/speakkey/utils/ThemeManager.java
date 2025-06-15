package com.drgraff.speakkey.utils;

import android.content.SharedPreferences;
import android.util.Log; // For logging migration
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage theme preferences
 */
public class ThemeManager {
    
    // Key for the theme selection preference
    public static final String PREF_KEY_DARK_MODE = "dark_mode";

    // Theme values
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_OLED = "oled";
    public static final String THEME_DEFAULT = "default"; // System default

    /**
     * Apply the appropriate theme based on the theme preference.
     * Handles migration from an old boolean preference to the new string-based preference.
     * 
     * @param sharedPreferences SharedPreferences instance containing theme settings
     */
    public static void applyTheme(SharedPreferences sharedPreferences) {
        String themeValue;
        try {
            // Try to get it as a String first (new format)
            themeValue = sharedPreferences.getString(PREF_KEY_DARK_MODE, THEME_DEFAULT);
        } catch (ClassCastException e) {
            // If ClassCastException, it's an old boolean preference
            Log.w("ThemeManager", "Old boolean 'dark_mode' preference found. Migrating to string.");
            boolean oldDarkMode = sharedPreferences.getBoolean(PREF_KEY_DARK_MODE, false); // false was default for SwitchPreference

            if (oldDarkMode) {
                themeValue = THEME_DARK;
            } else {
                themeValue = THEME_LIGHT;
                // Mapping old 'false' (which meant light theme) to the new explicit "light".
                // If it should map to system default, THEME_DEFAULT would be used here.
            }
            // Save the migrated value back as a string
            sharedPreferences.edit().putString(PREF_KEY_DARK_MODE, themeValue).apply();
        }
        
        switch (themeValue) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // Assumes AppTheme.Dark or base dark theme is applied by the system
                break;
            case THEME_OLED:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                // Assumes R.style.AppTheme_OLED is correctly parented from a DayNight theme
                // and will be picked up when MODE_NIGHT_YES is active.
                break;
            case THEME_DEFAULT:
            default: // Handles "default" or any unexpected/new values gracefully by falling to system
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}