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

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.OpenAIModelData; // Added
import com.drgraff.speakkey.utils.ThemeManager;

import java.util.ArrayList; // Added
import java.util.Arrays;
import java.util.HashSet; // Added
import java.util.List; // Added
import java.util.Set;
import java.util.concurrent.ExecutorService; // Added
import java.util.concurrent.Executors; // Added

public class SettingsActivity extends AppCompatActivity {

    public static final String PREF_KEY_TRANSCRIPTION_MODE = "transcription_mode"; // Existing key, good to have a constant
    public static final String PREF_KEY_ONESTEP_PROCESSING_MODEL = "pref_onestep_processing_model"; // New key for old "chatgpt_model"
    public static final String PREF_KEY_TWOSTEP_STEP1_ENGINE = "pref_twostep_step1_engine";
    public static final String PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL = "pref_twostep_step1_chatgpt_model";
    public static final String PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL = "pref_twostep_step2_processing_model";
    public static final String PREF_KEY_PHOTOVISION_PROCESSING_MODEL = "pref_photovision_processing_model";
    // The old key "chatgpt_model" will be replaced by PREF_KEY_ONESTEP_PROCESSING_MODEL in root_preferences.xml next.
    // The old key "pref_key_selected_photo_model" from PhotoPromptsActivity will be superseded by PREF_KEY_PHOTOVISION_PROCESSING_MODEL.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
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
    }

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
        private static final String PREF_KEY_FETCHED_MODEL_IDS = "fetched_model_ids";
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

            executorService.execute(() -> {
                try {
                    final List<OpenAIModelData.ModelInfo> models = chatGptApi.listModels();
                    mainHandler.post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (models == null || models.isEmpty()) {
                            Toast.makeText(getContext(), "No models returned or error in fetching.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        List<String> modelIdsList = new ArrayList<>();
                        for (OpenAIModelData.ModelInfo model : models) {
                            if (model.getId() != null && !model.getId().trim().isEmpty()) {
                                 modelIdsList.add(model.getId());
                            }
                        }
                        
                        if (modelIdsList.isEmpty()) {
                            Toast.makeText(getContext(), "No suitable model IDs found in the response.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String[] modelIdsArray = modelIdsList.toArray(new String[0]);
                        Arrays.sort(modelIdsArray); 

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putStringSet(PREF_KEY_FETCHED_MODEL_IDS, new HashSet<>(Arrays.asList(modelIdsArray)));
                        
                        String currentSelectedModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, null); // Updated key
                        boolean currentSelectionStillValid = false;
                        if (currentSelectedModel != null) {
                            for (String id : modelIdsArray) {
                                if (id.equals(currentSelectedModel)) {
                                    currentSelectionStillValid = true;
                                    break;
                                }
                            }
                        }

                        if (!currentSelectionStillValid && modelIdsArray.length > 0) {
                            currentSelectedModel = modelIdsArray[0]; 
                            editor.putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, currentSelectedModel); // Updated key
                        }
                        editor.apply();

                        loadAndPopulateModels(); 

                        Toast.makeText(getContext(), "ChatGPT models updated successfully.", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Failed to fetch models", e);
                    mainHandler.post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getContext(), "Error fetching models: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        }

        private void loadAndPopulateModels() {
            if (chatGptModelPreference == null) return;

            Set<String> modelIdsSet = sharedPreferences.getStringSet(PREF_KEY_FETCHED_MODEL_IDS, null);
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

            // Helper lambda to update summary for a ListPreference
            BiConsumer<ListPreference, String> updateSummary = (pref, defaultSummary) -> {
                if (pref != null) {
                    if (pref.getEntry() != null) {
                        pref.setSummary(pref.getEntry());
                    } else {
                        CharSequence value = pref.getValue();
                        int index = pref.findIndexOfValue(value != null ? value.toString() : "");
                        if (index >= 0) {
                            pref.setSummary(pref.getEntries()[index]);
                        } else {
                            pref.setSummary(defaultSummary);
                        }
                    }
                }
            };

            ListPreference transcriptionModePreference = findPreference(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE);
            updateSummary.accept(transcriptionModePreference, "Select transcription service");

            ListPreference twoStepStep1EnginePref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE);
            updateSummary.accept(twoStepStep1EnginePref, "Choose Step 1 engine");

            ListPreference twoStepStep1ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL);
            updateSummary.accept(twoStepStep1ModelPref, "Select Step 1 ChatGPT model");
            // For this one, also populate with dynamic models if available, like chatGptModelPreference
            if (twoStepStep1ModelPref != null && modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                String[] modelNames = modelIdsSet.toArray(new String[0]); // Assuming names are IDs
                Arrays.sort(modelIds);
                Arrays.sort(modelNames);
                twoStepStep1ModelPref.setEntries(modelNames);
                twoStepStep1ModelPref.setEntryValues(modelIds);
                // Set value and summary based on current selection or default
                String currentStep1Model = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, "gpt-3.5-turbo");
                boolean step1ModelFound = Arrays.asList(modelIds).contains(currentStep1Model);
                if (step1ModelFound) {
                    twoStepStep1ModelPref.setValue(currentStep1Model);
                } else if (modelIds.length > 0) {
                    twoStepStep1ModelPref.setValue(modelIds[0]);
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, modelIds[0]).apply();
                }
                if (twoStepStep1ModelPref.getEntry() != null) { // Re-check after setting value
                     twoStepStep1ModelPref.setSummary(twoStepStep1ModelPref.getEntry());
                }
            }


            ListPreference twoStepStep2ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL);
            updateSummary.accept(twoStepStep2ModelPref, "Select Step 2 processing model");
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


            ListPreference photoVisionModelPref = findPreference(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL);
            updateSummary.accept(photoVisionModelPref, "Select photo vision model");
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
            if (key.equals("dark_mode")) {
                ThemeManager.applyTheme(sharedPreferences);
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
                    // Add this call for the specific key:
                    if (key.equals(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE)) {
                        updateTwoStepStep1ModelVisibility(listPref.getValue());
                    }
                }
            }
            // Potentially add logic here to show/hide model preferences based on transcription_mode or step1_engine
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