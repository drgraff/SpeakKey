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
        PRESET_COLORS.clear(); // Clear old presets if any

        Map<String, Integer> neonNoirColors = new HashMap<>();
        neonNoirColors.put("pref_oled_topbar_background", Color.parseColor("#FF00A0"));
        neonNoirColors.put("pref_oled_topbar_text_icon", Color.parseColor("#00FFFF"));
        neonNoirColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        neonNoirColors.put("pref_oled_surface_background", Color.parseColor("#1A1A1A"));
        neonNoirColors.put("pref_oled_general_text_primary", Color.parseColor("#00FFFF"));
        neonNoirColors.put("pref_oled_general_text_secondary", Color.parseColor("#9D00FF"));
        neonNoirColors.put("pref_oled_button_background", Color.parseColor("#FF00A0"));
        neonNoirColors.put("pref_oled_button_text_icon", Color.parseColor("#00FFFF"));
        neonNoirColors.put("pref_oled_textbox_background", Color.parseColor("#B6FF00"));
        neonNoirColors.put("pref_oled_textbox_accent", Color.parseColor("#9D00FF"));
        neonNoirColors.put("pref_oled_accent_general", Color.parseColor("#B6FF00"));
        PRESET_COLORS.put("neon_noir", neonNoirColors);

        Map<String, Integer> frostedGlassColors = new HashMap<>();
        frostedGlassColors.put("pref_oled_topbar_background", Color.parseColor("#E0E0E0"));
        frostedGlassColors.put("pref_oled_topbar_text_icon", Color.parseColor("#000000"));
        frostedGlassColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        frostedGlassColors.put("pref_oled_surface_background", Color.parseColor("#202020"));
        frostedGlassColors.put("pref_oled_general_text_primary", Color.parseColor("#FFFFFF"));
        frostedGlassColors.put("pref_oled_general_text_secondary", Color.parseColor("#B0B0B0"));
        frostedGlassColors.put("pref_oled_button_background", Color.parseColor("#757575"));
        frostedGlassColors.put("pref_oled_button_text_icon", Color.parseColor("#FFFFFF"));
        frostedGlassColors.put("pref_oled_textbox_background", Color.parseColor("#252525"));
        frostedGlassColors.put("pref_oled_textbox_accent", Color.parseColor("#E0E0E0"));
        frostedGlassColors.put("pref_oled_accent_general", Color.parseColor("#9E9E9E"));
        PRESET_COLORS.put("frosted_glass", frostedGlassColors);

        Map<String, Integer> pastelsColors = new HashMap<>();
        pastelsColors.put("pref_oled_topbar_background", Color.parseColor("#FFCDD2"));
        pastelsColors.put("pref_oled_topbar_text_icon", Color.parseColor("#4E342E"));
        pastelsColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        pastelsColors.put("pref_oled_surface_background", Color.parseColor("#1C1C1C"));
        pastelsColors.put("pref_oled_general_text_primary", Color.parseColor("#E1BEE7"));
        pastelsColors.put("pref_oled_general_text_secondary", Color.parseColor("#CFD8DC"));
        pastelsColors.put("pref_oled_button_background", Color.parseColor("#C8E6C9"));
        pastelsColors.put("pref_oled_button_text_icon", Color.parseColor("#3E2723"));
        pastelsColors.put("pref_oled_textbox_background", Color.parseColor("#151515"));
        pastelsColors.put("pref_oled_textbox_accent", Color.parseColor("#B3E5FC"));
        pastelsColors.put("pref_oled_accent_general", Color.parseColor("#FFF59D"));
        PRESET_COLORS.put("pastels", pastelsColors);

        Map<String, Integer> earthtonesColors = new HashMap<>();
        earthtonesColors.put("pref_oled_topbar_background", Color.parseColor("#5D4037"));
        earthtonesColors.put("pref_oled_topbar_text_icon", Color.parseColor("#D7CCC8"));
        earthtonesColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        earthtonesColors.put("pref_oled_surface_background", Color.parseColor("#1E1E1E"));
        earthtonesColors.put("pref_oled_general_text_primary", Color.parseColor("#A1887F"));
        earthtonesColors.put("pref_oled_general_text_secondary", Color.parseColor("#BCAAA4"));
        earthtonesColors.put("pref_oled_button_background", Color.parseColor("#795548"));
        earthtonesColors.put("pref_oled_button_text_icon", Color.parseColor("#FFFFFF"));
        earthtonesColors.put("pref_oled_textbox_background", Color.parseColor("#121212"));
        earthtonesColors.put("pref_oled_textbox_accent", Color.parseColor("#8D6E63"));
        earthtonesColors.put("pref_oled_accent_general", Color.parseColor("#FFAB40"));
        PRESET_COLORS.put("earthtones", earthtonesColors);

        Map<String, Integer> imperialJadeColors = new HashMap<>();
        imperialJadeColors.put("pref_oled_topbar_background", Color.parseColor("#2E7D32"));
        imperialJadeColors.put("pref_oled_topbar_text_icon", Color.parseColor("#FFEB3B"));
        imperialJadeColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        imperialJadeColors.put("pref_oled_surface_background", Color.parseColor("#0A0A0A"));
        imperialJadeColors.put("pref_oled_general_text_primary", Color.parseColor("#C8E6C9"));
        imperialJadeColors.put("pref_oled_general_text_secondary", Color.parseColor("#A5D6A7"));
        imperialJadeColors.put("pref_oled_button_background", Color.parseColor("#FFC107"));
        imperialJadeColors.put("pref_oled_button_text_icon", Color.parseColor("#000000"));
        imperialJadeColors.put("pref_oled_textbox_background", Color.parseColor("#050505"));
        imperialJadeColors.put("pref_oled_textbox_accent", Color.parseColor("#FFEB3B"));
        imperialJadeColors.put("pref_oled_accent_general", Color.parseColor("#B71C1C"));
        PRESET_COLORS.put("imperial_jade", imperialJadeColors);

        Map<String, Integer> monoAccentColors = new HashMap<>();
        monoAccentColors.put("pref_oled_topbar_background", Color.parseColor("#212121"));
        monoAccentColors.put("pref_oled_topbar_text_icon", Color.parseColor("#FFFFFF"));
        monoAccentColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        monoAccentColors.put("pref_oled_surface_background", Color.parseColor("#101010"));
        monoAccentColors.put("pref_oled_general_text_primary", Color.parseColor("#E0E0E0"));
        monoAccentColors.put("pref_oled_general_text_secondary", Color.parseColor("#9E9E9E"));
        monoAccentColors.put("pref_oled_button_background", Color.parseColor("#FBC02D"));
        monoAccentColors.put("pref_oled_button_text_icon", Color.parseColor("#000000"));
        monoAccentColors.put("pref_oled_textbox_background", Color.parseColor("#080808"));
        monoAccentColors.put("pref_oled_textbox_accent", Color.parseColor("#FBC02D"));
        monoAccentColors.put("pref_oled_accent_general", Color.parseColor("#FBC02D"));
        PRESET_COLORS.put("mono_accent", monoAccentColors);

        Map<String, Integer> retroCrtColors = new HashMap<>();
        retroCrtColors.put("pref_oled_topbar_background", Color.parseColor("#001000"));
        retroCrtColors.put("pref_oled_topbar_text_icon", Color.parseColor("#00FF00"));
        retroCrtColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        retroCrtColors.put("pref_oled_surface_background", Color.parseColor("#000500"));
        retroCrtColors.put("pref_oled_general_text_primary", Color.parseColor("#00C000"));
        retroCrtColors.put("pref_oled_general_text_secondary", Color.parseColor("#00A000"));
        retroCrtColors.put("pref_oled_button_background", Color.parseColor("#003000"));
        retroCrtColors.put("pref_oled_button_text_icon", Color.parseColor("#00FF00"));
        retroCrtColors.put("pref_oled_textbox_background", Color.parseColor("#000300"));
        retroCrtColors.put("pref_oled_textbox_accent", Color.parseColor("#00FF00"));
        retroCrtColors.put("pref_oled_accent_general", Color.parseColor("#00FF00"));
        PRESET_COLORS.put("retro_crt", retroCrtColors);

        Map<String, Integer> mutedModernColors = new HashMap<>();
        mutedModernColors.put("pref_oled_topbar_background", Color.parseColor("#455A64"));
        mutedModernColors.put("pref_oled_topbar_text_icon", Color.parseColor("#CFD8DC"));
        mutedModernColors.put("pref_oled_main_background", Color.parseColor("#000000"));
        mutedModernColors.put("pref_oled_surface_background", Color.parseColor("#102027"));
        mutedModernColors.put("pref_oled_general_text_primary", Color.parseColor("#B0BEC5"));
        mutedModernColors.put("pref_oled_general_text_secondary", Color.parseColor("#78909C"));
        mutedModernColors.put("pref_oled_button_background", Color.parseColor("#546E7A"));
        mutedModernColors.put("pref_oled_button_text_icon", Color.parseColor("#ECEFF1"));
        mutedModernColors.put("pref_oled_textbox_background", Color.parseColor("#0A1014"));
        mutedModernColors.put("pref_oled_textbox_accent", Color.parseColor("#B0BEC5"));
        mutedModernColors.put("pref_oled_accent_general", Color.parseColor("#FF7043"));
        PRESET_COLORS.put("muted_modern", mutedModernColors);
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
