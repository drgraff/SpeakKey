package com.drgraff.speakkey.settings;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.content.SharedPreferences;
import android.util.Log; // Added for logging
import com.drgraff.speakkey.R;
// Make sure ColorPickerPreference is imported if needed for instanceof, or use FQDN.
// For now, assuming findPreference returns Preference and direct casting isn't strictly needed
// if ColorPickerPreference extends Preference and doesn't need specific methods called on it here.
// import com.drgraff.speakkey.settings.ColorPickerPreference;

import java.util.HashMap;
import java.util.Map;

public class OledThemeSettingsFragment extends PreferenceFragmentCompat {

    public static final String PREF_KEY_OLED_COLOR_PRESET = "pref_oled_color_preset";

    private static final Map<String, Map<String, Integer>> PRESET_COLORS = new HashMap<>();

    static {
        Map<String, Integer> neonColors = new HashMap<>();
        neonColors.put("pref_oled_topbar_background", Color.parseColor("#FF00FF")); // Magenta
        neonColors.put("pref_oled_topbar_text_icon", Color.parseColor("#000000"));    // Black
        neonColors.put("pref_oled_main_background", Color.parseColor("#000000"));      // Black
        neonColors.put("pref_oled_surface_background", Color.parseColor("#1A1A1A"));  // Dark Grey
        neonColors.put("pref_oled_general_text_primary", Color.parseColor("#00FF00")); // Lime Green
        neonColors.put("pref_oled_general_text_secondary", Color.parseColor("#A0FFA0"));// Light Lime
        neonColors.put("pref_oled_button_background", Color.parseColor("#FFFF00"));    // Yellow
        neonColors.put("pref_oled_button_text_icon", Color.parseColor("#000000"));    // Black
        neonColors.put("pref_oled_textbox_background", Color.parseColor("#101010"));  // Very Dark Grey
        neonColors.put("pref_oled_textbox_accent", Color.parseColor("#FF00FF"));       // Magenta
        neonColors.put("pref_oled_accent_general", Color.parseColor("#00FFFF"));       // Cyan accent
        PRESET_COLORS.put("neon", neonColors);

        Map<String, Integer> forestColors = new HashMap<>();
        forestColors.put("pref_oled_topbar_background", Color.parseColor("#2E7D32")); // Dark Green
        forestColors.put("pref_oled_topbar_text_icon", Color.parseColor("#FFFFFF"));    // White
        forestColors.put("pref_oled_main_background", Color.parseColor("#000000"));      // Black
        forestColors.put("pref_oled_surface_background", Color.parseColor("#101010"));  // Very Dark Grey
        forestColors.put("pref_oled_general_text_primary", Color.parseColor("#A5D6A7")); // Light Green
        forestColors.put("pref_oled_general_text_secondary", Color.parseColor("#81C784"));// Medium Light Green
        forestColors.put("pref_oled_button_background", Color.parseColor("#558B2F"));    // Olive Green
        forestColors.put("pref_oled_button_text_icon", Color.parseColor("#FFFFFF"));    // White
        forestColors.put("pref_oled_textbox_background", Color.parseColor("#0A0A0A"));  // Near Black
        forestColors.put("pref_oled_textbox_accent", Color.parseColor("#A5D6A7"));       // Light Green
        forestColors.put("pref_oled_accent_general", Color.parseColor("#FFB300"));       // Amber accent
        PRESET_COLORS.put("forest", forestColors);

        Map<String, Integer> oceanColors = new HashMap<>();
        oceanColors.put("pref_oled_topbar_background", Color.parseColor("#0277BD")); // Deep Blue
        oceanColors.put("pref_oled_topbar_text_icon", Color.parseColor("#FFFFFF"));    // White
        oceanColors.put("pref_oled_main_background", Color.parseColor("#000000"));      // Black
        oceanColors.put("pref_oled_surface_background", Color.parseColor("#0D0D0D"));  // Dark Grey
        oceanColors.put("pref_oled_general_text_primary", Color.parseColor("#B3E5FC")); // Light Sky Blue
        oceanColors.put("pref_oled_general_text_secondary", Color.parseColor("#81D4FA"));// Medium Sky Blue
        oceanColors.put("pref_oled_button_background", Color.parseColor("#039BE5"));    // Bright Blue
        oceanColors.put("pref_oled_button_text_icon", Color.parseColor("#FFFFFF"));    // White
        oceanColors.put("pref_oled_textbox_background", Color.parseColor("#0A0A0A"));  // Near Black
        oceanColors.put("pref_oled_textbox_accent", Color.parseColor("#81D4FA"));       // Medium Sky Blue
        oceanColors.put("pref_oled_accent_general", Color.parseColor("#FFCA28"));       // Amber/Gold accent
        PRESET_COLORS.put("ocean", oceanColors);
    }

    public static Map<String, Integer> getPresetColors(String presetName) {
        return PRESET_COLORS.get(presetName);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.oled_theme_preferences, rootKey);

        ListPreference presetPreference = findPreference(PREF_KEY_OLED_COLOR_PRESET);
        if (presetPreference != null) {
            // Set initial summary based on current value
            updatePresetSummary(presetPreference, getPreferenceManager().getSharedPreferences().getString(PREF_KEY_OLED_COLOR_PRESET, "custom"));

            presetPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedPresetKey = (String) newValue;
                updatePresetSummary((ListPreference) preference, selectedPresetKey);

                if (!"custom".equals(selectedPresetKey)) {
                    Map<String, Integer> colors = getPresetColors(selectedPresetKey);
                    if (colors != null) {
                        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                        for (Map.Entry<String, Integer> entry : colors.entrySet()) {
                            editor.putInt(entry.getKey(), entry.getValue());
                            // Individual ColorPickerPreferences will be updated when the activity/fragment is recreated
                            // due to the SharedPreferences change triggering listeners in the hosting activity.
                        }
                        editor.apply();
                        // The apply() will trigger onSharedPreferenceChanged in SettingsActivity/OledThemeSettingsActivity,
                        // which should lead to a recreate() call, refreshing the entire UI including ColorPickerPreferences.
                    }
                }
                return true; // Value is persisted
            });
        }

        // Logic to update preset dropdown when individual colors change
        final String[] oledColorKeys = {
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
            "pref_oled_main_background", "pref_oled_surface_background",
            "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
            "pref_oled_button_background", "pref_oled_button_text_icon",
            "pref_oled_textbox_background", "pref_oled_textbox_accent",
            "pref_oled_accent_general"
        };

        if (presetPreference == null) { // Check again in case it was null earlier and we returned from that block
            Log.e("OledThemeSettingsFrag", "Preset ListPreference is null, cannot attach individual color listeners.");
            return; // Cannot proceed
        }
        // Re-assign to a final variable for use in lambda, if not already effectively final
        final ListPreference finalPresetPreference = presetPreference;

        Preference.OnPreferenceChangeListener individualColorChangeListener = (preference, newValue) -> {
            if (!"custom".equals(finalPresetPreference.getValue())) {
                finalPresetPreference.setValue("custom"); // This will trigger its own listener, which updates summary
            } else {
                // If already "custom", ensure summary reflects this.
                // The presetPreference's own listener should handle this when its value is set.
                // However, if a color changes while "custom" is already selected,
                // the preset's listener won't fire again. So, explicitly update summary here too.
                updatePresetSummary(finalPresetPreference, "custom");
            }
            return true; // Allow the individual color change to be persisted
        };

        for (String key : oledColorKeys) {
            Preference colorPickerPref = findPreference(key);
            if (colorPickerPref != null) { // Check if the preference itself exists
                 // Optional: Check instanceof if ColorPickerPreference has specific methods to call
                 // if (colorPickerPref instanceof com.drgraff.speakkey.settings.ColorPickerPreference) {
                    colorPickerPref.setOnPreferenceChangeListener(individualColorChangeListener);
                 // }
            } else {
                Log.w("OledThemeSettingsFrag", "ColorPickerPreference with key '" + key + "' not found.");
            }
        }
    }

    private void updatePresetSummary(ListPreference listPreference, String value) {
        int index = listPreference.findIndexOfValue(value);
        if (index >= 0) {
            listPreference.setSummary(listPreference.getEntries()[index]);
        } else {
            listPreference.setSummary("Custom"); // Fallback for "custom" or if value not found
        }
    }
}
