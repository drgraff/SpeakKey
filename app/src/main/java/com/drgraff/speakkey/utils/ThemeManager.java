package com.drgraff.speakkey.utils;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage theme preferences
 */
public class ThemeManager {
    
    private static final String DARK_MODE_PREF = "dark_mode";
    
    /**
     * Apply the appropriate theme based on the dark_mode preference
     * 
     * @param preferences SharedPreferences instance containing theme settings
     */
    public static void applyTheme(SharedPreferences preferences) {
        boolean darkMode = preferences.getBoolean(DARK_MODE_PREF, false);
        
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}