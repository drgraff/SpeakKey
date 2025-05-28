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
            chatGptModelPreference = findPreference("chatgpt_model");
            prefCheckModelsButton = findPreference("pref_check_models_button");
            // Preference checkUpdatesPreference = findPreference("pref_check_for_updates"); // Removed

            EditTextPreference formatDelayPreference = findPreference("pref_inputstick_format_delay_ms");
            if (formatDelayPreference != null) {
                String currentValue = sharedPreferences.getString("pref_inputstick_format_delay_ms", "100");
                formatDelayPreference.setSummary(currentValue + " ms");
            }

            String apiKey = sharedPreferences.getString("openai_api_key", "");
            if (!apiKey.isEmpty()) {
                chatGptApi = new ChatGptApi(apiKey, sharedPreferences.getString("chatgpt_model", "gpt-3.5-turbo"));
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
                        
                        String currentSelectedModel = sharedPreferences.getString("chatgpt_model", null);
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
                            editor.putString("chatgpt_model", currentSelectedModel);
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
            String currentSelectedModel = sharedPreferences.getString("chatgpt_model", "gpt-3.5-turbo");

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
                    sharedPreferences.edit().putString("chatgpt_model", modelIds[0]).apply();
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
                    sharedPreferences.edit().putString("chatgpt_model", currentSelectedModel).apply();
                }
                chatGptModelPreference.setValue(currentSelectedModel);
            }

            // Update summary to reflect current selection
            if (chatGptModelPreference.getEntry() != null) {
                chatGptModelPreference.setSummary(chatGptModelPreference.getEntry());
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
            } else if (key.equals("chatgpt_model")) {
                Preference modelPref = findPreference(key);
                if (modelPref instanceof ListPreference) {
                    ListPreference listPref = (ListPreference) modelPref;
                    if (listPref.getEntry() != null) {
                        listPref.setSummary(listPref.getEntry());
                    }
                }
            } else if (key.equals("pref_inputstick_format_delay_ms")) {
                Preference delayPref = findPreference(key);
                if (delayPref instanceof EditTextPreference) {
                    String currentValue = sharedPreferences.getString(key, "100");
                    delayPref.setSummary(currentValue + " ms");
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