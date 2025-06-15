package com.drgraff.speakkey.settings;

import android.content.SharedPreferences;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference; // Added for explicit type check
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.graphics.Color; // Added for Color parsing
// PorterDuff import removed as it's now used in DynamicThemeApplicator

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.OpenAIModelData; // Added
import com.drgraff.speakkey.utils.ThemeManager;
import com.drgraff.speakkey.settings.ColorPickerPreference; // Added for ColorPickerPreference
import com.drgraff.speakkey.utils.DynamicThemeApplicator; // Added for DynamicThemeApplicator

import java.util.ArrayList; // Added
import java.util.Arrays;
import java.util.HashSet; // Added
import java.util.List; // Added
import java.util.Set;
import java.util.concurrent.ExecutorService; // Added
import java.util.concurrent.Executors; // Added
import java.util.function.Consumer; // Added for API 24+ or with desugaring
// android.util.Log will be imported by the FQN in the code block

public class SettingsActivity extends AppCompatActivity {

    public static final String PREF_KEY_TRANSCRIPTION_MODE = "transcription_mode"; // Existing key, good to have a constant
    public static final String PREF_KEY_ONESTEP_PROCESSING_MODEL = "pref_onestep_processing_model"; // New key for old "chatgpt_model"
    public static final String PREF_KEY_TWOSTEP_STEP1_ENGINE = "pref_twostep_step1_engine";
    public static final String PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL = "pref_twostep_step1_chatgpt_model";
    public static final String PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL = "pref_twostep_step2_processing_model";
    public static final String PREF_KEY_PHOTOVISION_PROCESSING_MODEL = "pref_photovision_processing_model";
    public static final String PREF_KEY_FETCHED_MODEL_IDS = "fetched_model_ids"; // Added
    // The old key "chatgpt_model" will be replaced by PREF_KEY_ONESTEP_PROCESSING_MODEL in root_preferences.xml next.
    // The old key "pref_key_selected_photo_model" from PhotoPromptsActivity will be superseded by PREF_KEY_PHOTOVISION_PROCESSING_MODEL.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Apply AppCompatDelegate default night mode FIRST
        com.drgraff.speakkey.utils.ThemeManager.applyTheme(sharedPreferences);

        // Then, if OLED is specifically chosen, override with the specific OLED theme
        String themeValue = sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);
        if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Show the Up button in the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        
        // Load the settings fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        // Apply custom OLED colors if OLED theme is active
        // Re-check themeValue as it's local to the original block
        String currentActivityThemeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, sharedPreferences);
        }
    }

    // Removed applyOledCustomizations method

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        private ListPreference chatGptModelPreference;
        private Preference prefCheckModelsButton;
        private ChatGptApi chatGptApi;
        private SharedPreferences sharedPreferences;
        // Removed: private static final String PREF_KEY_FETCHED_MODEL_IDS = "fetched_model_ids";
        // private static final String PREF_KEY_FETCHED_MODEL_NAMES = "fetched_model_names"; // Not strictly needed if IDs are names

        private ExecutorService executorService = Executors.newSingleThreadExecutor();
        private Handler mainHandler = new Handler(Looper.getMainLooper());
        private ProgressDialog progressDialog;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            sharedPreferences = getPreferenceManager().getSharedPreferences();
            chatGptModelPreference = findPreference(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL); // Updated key
            prefCheckModelsButton = findPreference("pref_check_models_button");
            // Preference checkUpdatesPreference = findPreference("pref_check_for_updates"); // Removed

            String apiKey = sharedPreferences.getString("openai_api_key", "");
            if (!apiKey.isEmpty()) {
                chatGptApi = new ChatGptApi(apiKey, sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-3.5-turbo")); // Updated key
            } else {
                if (prefCheckModelsButton != null) {
                    prefCheckModelsButton.setEnabled(false);
                    prefCheckModelsButton.setSummary("OpenAI API Key not set.");
                }
            }

            // Set default colors for OLED ColorPickerPreferences
            String[] oledColorKeys = {
                "pref_oled_color_primary", "pref_oled_color_secondary",
                "pref_oled_color_background", "pref_oled_color_surface",
                "pref_oled_color_text_primary", "pref_oled_color_text_secondary",
                "pref_oled_color_icon_tint", "pref_oled_color_edit_text_background"
            };

            String[] oledDefaultColorsHex = {
                "#03DAC6", "#03DAC6", "#000000", "#0D0D0D",
                "#FFFFFF", "#AAAAAA", "#FFFFFF", "#1A1A1A"
            };

            for (int i = 0; i < oledColorKeys.length; i++) {
                ColorPickerPreference colorPref = findPreference(oledColorKeys[i]);
                if (colorPref != null) {
                    colorPref.setDefaultColorValue(Color.parseColor(oledDefaultColorsHex[i]));
                }
            }

            loadAndPopulateModels();

            if (prefCheckModelsButton != null && chatGptApi != null) {
                prefCheckModelsButton.setOnPreferenceClickListener(preference -> {
                    fetchModelsAndUpdatePreference(); // Call the actual method
                    return true;
                });
            }

            // Removed the if (checkUpdatesPreference != null) block

            ListPreference twoStepStep1EnginePrefOnCreate = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE);
            if (twoStepStep1EnginePrefOnCreate != null) {
                updateTwoStepStep1ModelVisibility(twoStepStep1EnginePrefOnCreate.getValue());
            }
        }

        private void fetchModelsAndUpdatePreference() {
            if (chatGptApi == null) {
                Toast.makeText(getContext(), "API client not initialized (check API key).", Toast.LENGTH_LONG).show();
                return;
            }

            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("Fetching available models...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            if (chatGptApi == null) {
                Toast.makeText(getContext(), "API client not initialized (check API key).", Toast.LENGTH_LONG).show();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                return;
            }
            if (sharedPreferences == null) {
                 Toast.makeText(getContext(), "SharedPreferences not available.", Toast.LENGTH_LONG).show();
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                return;
            }

            ChatGptApi.fetchAndCacheOpenAiModels(
                    chatGptApi,
                    sharedPreferences,
                    SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, // New argument
                    executorService,
                    mainHandler,
                    models -> { // onSuccess callback (Consumer<List<OpenAIModelData.ModelInfo>>)
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (models == null || models.isEmpty()) {
                            // The static method logs warnings, but we can also toast here if needed
                            // or rely on the static method to have informed the user via a generic error if it passed one to onError
                            Toast.makeText(getContext(), "No models returned or error in fetching (check logs).", Toast.LENGTH_LONG).show();
                            // return; // No return here, still call loadAndPopulate to reflect empty cache
                        }
                        // The saving of PREF_KEY_FETCHED_MODEL_IDS is now done inside fetchAndCacheOpenAiModels.
                        // The logic for checking current selection and updating default for PREF_KEY_ONESTEP_PROCESSING_MODEL
                        // is also now inside fetchAndCacheOpenAiModels if we adapt it, or it can be handled by loadAndPopulateModels.
                        // For now, just reload and repopulate.
                        loadAndPopulateModels(); 
                        Toast.makeText(getContext(), "ChatGPT models updated.", Toast.LENGTH_SHORT).show();
                    },
                    exception -> { // onError callback (Consumer<Exception>)
                        Log.e("SettingsFragment", "Failed to fetch models via utility", exception);
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getContext(), "Error fetching models: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );
        }

        private void loadAndPopulateModels() {
            if (chatGptModelPreference == null) return;

            Set<String> modelIdsSet = sharedPreferences.getStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, null); // Changed to use SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS
            // Use IDs for names too, as PREF_KEY_FETCHED_MODEL_NAMES is not saved by fetchModelsAndUpdatePreference
            Set<String> modelNamesSet = modelIdsSet; 
            String currentSelectedModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-3.5-turbo"); // Updated key

            if (modelIdsSet != null && !modelIdsSet.isEmpty()) { // modelNamesSet will be the same
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                String[] modelNames = modelNamesSet.toArray(new String[0]); // Will be same as modelIds
                Arrays.sort(modelIds); // Sort IDs
                Arrays.sort(modelNames); // Sort names (which are IDs)

                chatGptModelPreference.setEntries(modelNames); // Use sorted names (IDs)
                chatGptModelPreference.setEntryValues(modelIds); // Use sorted IDs
                
                boolean found = false;
                for (String id : modelIds) {
                    if (id.equals(currentSelectedModel)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    chatGptModelPreference.setValue(currentSelectedModel);
                } else if (modelIds.length > 0) { // Default to first in the new sorted list
                    chatGptModelPreference.setValue(modelIds[0]);
                    // Persist this change if the previously selected model is no longer valid
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, modelIds[0]).apply(); // Updated key
                }
            } else {
                 // Fallback to default XML values if nothing is stored
                CharSequence[] entries = getResources().getTextArray(R.array.chatgpt_model_entries);
                CharSequence[] entryValues = getResources().getTextArray(R.array.chatgpt_model_values);
                chatGptModelPreference.setEntries(entries);
                chatGptModelPreference.setEntryValues(entryValues);
                // Ensure currentSelectedModel is one of the defaults or set to the first default
                boolean isDefaultValid = false;
                for(CharSequence val : entryValues) {
                    if(val.toString().equals(currentSelectedModel)) {
                        isDefaultValid = true;
                        break;
                    }
                }
                if(!isDefaultValid && entryValues.length > 0) {
                    currentSelectedModel = entryValues[0].toString();
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, currentSelectedModel).apply(); // Updated key
                }
                chatGptModelPreference.setValue(currentSelectedModel);
            }

            // Update summary to reflect current selection
            if (chatGptModelPreference.getEntry() != null) {
                chatGptModelPreference.setSummary(chatGptModelPreference.getEntry());
            }

            ListPreference transcriptionModePreference = findPreference(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE);
            if (transcriptionModePreference != null) {
                if (transcriptionModePreference.getEntry() != null) {
                    transcriptionModePreference.setSummary(transcriptionModePreference.getEntry());
                } else {
                    CharSequence value = transcriptionModePreference.getValue();
                    int index = transcriptionModePreference.findIndexOfValue(value != null ? value.toString() : "");
                    if (index >= 0) {
                        transcriptionModePreference.setSummary(transcriptionModePreference.getEntries()[index]);
                    } else {
                        transcriptionModePreference.setSummary("Select transcription service");
                    }
                }
            }

            ListPreference twoStepStep1EnginePref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE);
            if (twoStepStep1EnginePref != null) {
                if (twoStepStep1EnginePref.getEntry() != null) {
                    twoStepStep1EnginePref.setSummary(twoStepStep1EnginePref.getEntry());
                } else {
                    CharSequence value = twoStepStep1EnginePref.getValue();
                    int index = twoStepStep1EnginePref.findIndexOfValue(value != null ? value.toString() : "");
                    if (index >= 0) {
                        twoStepStep1EnginePref.setSummary(twoStepStep1EnginePref.getEntries()[index]);
                    } else {
                        twoStepStep1EnginePref.setSummary("Choose Step 1 engine");
                    }
                }
            }

            ListPreference twoStepStep1ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL);
            if (twoStepStep1ModelPref != null) {
                String defaultTranscriptionModel = "whisper-1";
                if (modelIdsSet != null && !modelIdsSet.isEmpty()) {
                    String[] modelIds = modelIdsSet.toArray(new String[0]);
                    // String[] modelNames = modelIdsSet.toArray(new String[0]); // Assuming IDs are used as names
                    Arrays.sort(modelIds);
                    // Arrays.sort(modelNames);

                    twoStepStep1ModelPref.setEntries(modelIds); // Use sorted model IDs for entries
                    twoStepStep1ModelPref.setEntryValues(modelIds); // Use sorted model IDs for values

                    String currentStep1Model = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel);
                    boolean step1ModelFound = Arrays.asList(modelIds).contains(currentStep1Model);

                    if (step1ModelFound) {
                        twoStepStep1ModelPref.setValue(currentStep1Model);
                    } else if (Arrays.asList(modelIds).contains(defaultTranscriptionModel)) {
                        // If current selection not found but default is in the list
                        twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    } else if (modelIds.length > 0) {
                        // If current and default not found, use first available model
                        twoStepStep1ModelPref.setValue(modelIds[0]);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, modelIds[0]).apply();
                    } else {
                        // This case should ideally not be reached if modelIdsSet is not empty
                        // but as a fallback, if modelIds becomes empty after all.
                        twoStepStep1ModelPref.setEntries(new String[]{defaultTranscriptionModel});
                        twoStepStep1ModelPref.setEntryValues(new String[]{defaultTranscriptionModel});
                        twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    }
                } else { // modelIdsSet is null or empty
                    Log.w("SettingsFragment", "No models in modelIdsSet for PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL. Using default.");
                    twoStepStep1ModelPref.setEntries(new String[]{defaultTranscriptionModel});
                    twoStepStep1ModelPref.setEntryValues(new String[]{defaultTranscriptionModel});
                    twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                    // Ensure default is saved if not present
                    if (sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, null) == null) {
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    }
                }
                // Update summary
                if (twoStepStep1ModelPref.getEntry() != null) {
                    twoStepStep1ModelPref.setSummary(twoStepStep1ModelPref.getEntry());
                } else {
                    twoStepStep1ModelPref.setSummary(twoStepStep1ModelPref.getValue()); // Fallback to value if entry is null
                }
            }


            ListPreference twoStepStep2ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL);
            // Populate with dynamic models
             if (twoStepStep2ModelPref != null && modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                String[] modelNames = modelIdsSet.toArray(new String[0]);
                Arrays.sort(modelIds);
                Arrays.sort(modelNames);
                twoStepStep2ModelPref.setEntries(modelNames);
                twoStepStep2ModelPref.setEntryValues(modelIds);
                String currentStep2Model = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, "gpt-4o");
                boolean step2ModelFound = Arrays.asList(modelIds).contains(currentStep2Model);
                 if (step2ModelFound) {
                    twoStepStep2ModelPref.setValue(currentStep2Model);
                } else if (modelIds.length > 0) {
                    twoStepStep2ModelPref.setValue(modelIds[0]);
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, modelIds[0]).apply();
                }
                if (twoStepStep2ModelPref.getEntry() != null) {
                     twoStepStep2ModelPref.setSummary(twoStepStep2ModelPref.getEntry());
                }
            }
            // Update summary for twoStepStep2ModelPref after potential population
            if (twoStepStep2ModelPref != null) {
                if (twoStepStep2ModelPref.getEntry() != null) {
                    twoStepStep2ModelPref.setSummary(twoStepStep2ModelPref.getEntry());
                } else {
                    CharSequence value = twoStepStep2ModelPref.getValue();
                    int index = twoStepStep2ModelPref.findIndexOfValue(value != null ? value.toString() : "");
                    if (index >= 0) {
                        twoStepStep2ModelPref.setSummary(twoStepStep2ModelPref.getEntries()[index]);
                    } else {
                        twoStepStep2ModelPref.setSummary("Select Step 2 processing model");
                    }
                }
            }


            ListPreference photoVisionModelPref = findPreference(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL);
            // Populate with dynamic models
            if (photoVisionModelPref != null && modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                String[] modelNames = modelIdsSet.toArray(new String[0]);
                Arrays.sort(modelIds);
                Arrays.sort(modelNames);
                photoVisionModelPref.setEntries(modelNames);
                photoVisionModelPref.setEntryValues(modelIds);
                String currentPhotoModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
                boolean photoModelFound = Arrays.asList(modelIds).contains(currentPhotoModel);
                if (photoModelFound) {
                    photoVisionModelPref.setValue(currentPhotoModel);
                } else if (modelIds.length > 0) {
                    photoVisionModelPref.setValue(modelIds[0]);
                     sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, modelIds[0]).apply();
                }
                 if (photoVisionModelPref.getEntry() != null) {
                     photoVisionModelPref.setSummary(photoVisionModelPref.getEntry());
                }
            }
        }

        private void updateTwoStepStep1ModelVisibility(String engineValue) {
            ListPreference twoStepStep1ChatGptModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL);
            if (twoStepStep1ChatGptModelPref != null) {
                if ("chatgpt".equals(engineValue)) { // Use the actual value stored for ChatGPT engine
                    twoStepStep1ChatGptModelPref.setVisible(true);
                    // twoStepStep1ChatGptModelPref.setEnabled(true); // setVisible is usually enough
                } else {
                    twoStepStep1ChatGptModelPref.setVisible(false);
                    // twoStepStep1ChatGptModelPref.setEnabled(false);
                }
                Log.d("SettingsFragment", "Two Step Step 1 ChatGPT Model visibility set to: " + twoStepStep1ChatGptModelPref.isVisible());
            } else {
                Log.w("SettingsFragment", "Could not find pref_twostep_step1_chatgpt_model preference to update visibility.");
            }
        }
        
        @Override
        public void onResume() {
            super.onResume();
            // Register the preference change listener
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onPause() {
            super.onPause();
            // Unregister the preference change listener
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final String[] oledColorKeys = {
                "pref_oled_color_primary", "pref_oled_color_secondary",
                "pref_oled_color_background", "pref_oled_color_surface",
                "pref_oled_color_text_primary", "pref_oled_color_text_secondary",
                "pref_oled_color_icon_tint", "pref_oled_color_edit_text_background"
            };
            boolean isOledColorKey = false;
            for (String oledKey : oledColorKeys) {
                if (oledKey.equals(key)) {
                    isOledColorKey = true;
                    break;
                }
            }

            if (ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
                ThemeManager.applyTheme(sharedPreferences);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } else if (isOledColorKey) {
                // If an OLED color key changed, and the current theme is OLED, recreate.
                String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
                if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                    if (getActivity() != null) {
                        android.util.Log.d("SettingsFragment", "OLED color preference changed: " + key + ". Recreating activity.");
                        getActivity().recreate();
                    }
                }
                // Also, update the summary of the ColorPickerPreference itself if possible (to reflect new color string or similar)
                // ColorPickerPreference changedPref = findPreference(key);
                // if (changedPref != null) {
                //     // The ColorPickerPreference itself should update its preview via notifyChanged() when color is saved.
                //     // No explicit summary update might be needed here unless we want to show hex string.
                // }
            } else if (key.equals(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL) ||
                       key.equals(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE) ||
                       key.equals(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE) ||
                       key.equals(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL) ||
                       key.equals(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL) ||
                       key.equals(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL)) {
                Preference pref = findPreference(key);
                if (pref instanceof ListPreference) {
                    ListPreference listPref = (ListPreference) pref;
                    if (listPref.getEntry() != null) {
                        listPref.setSummary(listPref.getEntry());
                    }
                    if (key.equals(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE)) {
                        updateTwoStepStep1ModelVisibility(listPref.getValue());
                    }
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
        }
    }
}