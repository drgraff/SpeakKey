package com.drgraff.speakkey.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.app.ProgressDialog;
// import android.content.SharedPreferences; // Duplicate (already handled by previous import)
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View; // Added for DecorView logging
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.graphics.Color;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.OpenAIModelData;
import com.drgraff.speakkey.utils.ThemeManager;
import com.drgraff.speakkey.settings.ColorPickerPreference;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SettingsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String PREF_KEY_TRANSCRIPTION_MODE = "transcription_mode";
    public static final String PREF_KEY_ONESTEP_PROCESSING_MODEL = "pref_onestep_processing_model";
    public static final String PREF_KEY_TWOSTEP_STEP1_ENGINE = "pref_twostep_step1_engine";
    public static final String PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL = "pref_twostep_step1_chatgpt_model";
    public static final String PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL = "pref_twostep_step2_processing_model";
    public static final String PREF_KEY_PHOTOVISION_PROCESSING_MODEL = "pref_photovision_processing_model";
    public static final String PREF_KEY_FETCHED_MODEL_IDS = "fetched_model_ids";

    private SharedPreferences sharedPreferences;
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;
    private static final String TAG_ACTIVITY = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ThemeManager.applyTheme(this.sharedPreferences);
        String themeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        View decorView = getWindow().getDecorView();
        android.graphics.drawable.Drawable background = decorView.getBackground();
        String actualBackgroundColor = "#UNKNOWN";
        if (background instanceof android.graphics.drawable.ColorDrawable) {
            actualBackgroundColor = String.format("#%08X", ((android.graphics.drawable.ColorDrawable) background).getColor());
        }
        Log.d(TAG_ACTIVITY, "onCreate after setContentView: DecorView background color: " + actualBackgroundColor);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        this.mAppliedThemeMode = currentActivityThemeValue; // Store the theme mode that is currently active

        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            Log.d(TAG_ACTIVITY, "onCreate: OLED mode confirmed. Calling DynamicThemeApplicator.applyOledColors(). Prefs: TopbarBG=0x" + Integer.toHexString(sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)) + ", MainBG=0x" + Integer.toHexString(sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)));
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);

            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG_ACTIVITY, "onCreate: Stored mApplied OLED colors for activity frame. TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) + ", TopbarText=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) + ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG_ACTIVITY, "onCreate: Not OLED mode, mApplied OLED-specific colors reset for activity frame.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.sharedPreferences == null) {
            this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (mAppliedThemeMode != null) {
            boolean needsRecreate = false;
            String currentThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);

            if (!mAppliedThemeMode.equals(currentThemeValue)) {
                needsRecreate = true;
                Log.d(TAG_ACTIVITY, "onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue);
            } else if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
                int currentTopbarBgFromPrefs = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
                if (mAppliedTopbarBackgroundColor != currentTopbarBgFromPrefs) {
                    needsRecreate = true;
                    Log.d(TAG_ACTIVITY, "onResume: OLED Topbar Background Color changed. Stored: 0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) + ", Current Pref: 0x" + Integer.toHexString(currentTopbarBgFromPrefs));
                }
                int currentTopbarTextIconFromPrefs = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
                if (mAppliedTopbarTextIconColor != currentTopbarTextIconFromPrefs) {
                    needsRecreate = true;
                    Log.d(TAG_ACTIVITY, "onResume: OLED Topbar Text/Icon Color changed. Stored: 0x" + Integer.toHexString(mAppliedTopbarTextIconColor) + ", Current Pref: 0x" + Integer.toHexString(currentTopbarTextIconFromPrefs));
                }
                int currentMainBgFromPrefs = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
                if (mAppliedMainBackgroundColor != currentMainBgFromPrefs) {
                    needsRecreate = true;
                    Log.d(TAG_ACTIVITY, "onResume: OLED Main Background Color changed. Stored: 0x" + Integer.toHexString(mAppliedMainBackgroundColor) + ", Current Pref: 0x" + Integer.toHexString(currentMainBgFromPrefs));
                }
            }

            if (needsRecreate) {
                Log.d(TAG_ACTIVITY, "onResume: Detected configuration change. Recreating SettingsActivity.");
                recreate();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG_ACTIVITY, "onSharedPreferenceChanged triggered for key: " + key);
        if (key == null) return;

        final String[] oledColorKeysForActivityFrame = {
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
            "pref_oled_main_background"
        };
        boolean isRelevantOledColorKey = false;
        for (String oledKey : oledColorKeysForActivityFrame) {
            if (oledKey.equals(key)) {
                isRelevantOledColorKey = true;
                break;
            }
        }

        if (ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
            Log.d(TAG_ACTIVITY, "Main theme preference changed. Recreating SettingsActivity.");
            recreate();
        } else if (isRelevantOledColorKey) {
            String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG_ACTIVITY, "onSharedPreferenceChanged: Relevant OLED color key '" + key + "' changed. Recreating SettingsActivity.");
                recreate();
            }
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

        private ExecutorService executorService = Executors.newSingleThreadExecutor();
        private Handler mainHandler = new Handler(Looper.getMainLooper());
        private ProgressDialog progressDialog;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            sharedPreferences = getPreferenceManager().getSharedPreferences();
            chatGptModelPreference = findPreference(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL);
            prefCheckModelsButton = findPreference("pref_check_models_button");

            String apiKey = sharedPreferences.getString("openai_api_key", "");
            if (!apiKey.isEmpty()) {
                chatGptApi = new ChatGptApi(apiKey, sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-3.5-turbo"));
            } else {
                if (prefCheckModelsButton != null) {
                    prefCheckModelsButton.setEnabled(false);
                    prefCheckModelsButton.setSummary("OpenAI API Key not set.");
                }
            }

            final String[] oledColorKeys = {
                "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
                "pref_oled_main_background", "pref_oled_surface_background",
                "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
                "pref_oled_button_background", "pref_oled_button_text_icon",
                "pref_oled_textbox_background", "pref_oled_textbox_accent",
                "pref_oled_accent_general"
            };

            final String[] oledDefaultColorsHex = {
                "#03DAC6", "#000000", "#000000", "#0D0D0D", "#FFFFFF", "#AAAAAA",
                "#03DAC6", "#000000", "#1A1A1A", "#03DAC6", "#03DAC6"
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
                    fetchModelsAndUpdatePreference();
                    return true;
                });
            }

            ListPreference twoStepStep1EnginePrefOnCreate = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE);
            if (twoStepStep1EnginePrefOnCreate != null) {
                updateTwoStepStep1ModelVisibility(twoStepStep1EnginePrefOnCreate.getValue());
            }

            Preference oledSettingsScreenPreference = findPreference("pref_oled_theme_settings_screen");
            if (oledSettingsScreenPreference != null) {
                oledSettingsScreenPreference.setOnPreferenceClickListener(preference -> {
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), OledThemeSettingsActivity.class);
                        startActivity(intent);
                    }
                    return true;
                });
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

            ChatGptApi.fetchAndCacheOpenAiModels(
                    chatGptApi,
                    sharedPreferences,
                    SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS,
                    executorService,
                    mainHandler,
                    models -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        if (models == null || models.isEmpty()) {
                            Toast.makeText(getContext(), "No models returned or error in fetching (check logs).", Toast.LENGTH_LONG).show();
                        }
                        loadAndPopulateModels(); 
                        Toast.makeText(getContext(), "ChatGPT models updated.", Toast.LENGTH_SHORT).show();
                    },
                    exception -> {
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

            Set<String> modelIdsSet = sharedPreferences.getStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, null);
            Set<String> modelNamesSet = modelIdsSet; 
            String currentSelectedModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-3.5-turbo");

            if (modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                String[] modelNames = modelNamesSet.toArray(new String[0]);
                Arrays.sort(modelIds);
                Arrays.sort(modelNames);

                chatGptModelPreference.setEntries(modelNames);
                chatGptModelPreference.setEntryValues(modelIds);
                
                boolean found = Arrays.asList(modelIds).contains(currentSelectedModel);
                if (found) {
                    chatGptModelPreference.setValue(currentSelectedModel);
                } else if (modelIds.length > 0) {
                    chatGptModelPreference.setValue(modelIds[0]);
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, modelIds[0]).apply();
                }
            } else {
                CharSequence[] entries = getResources().getTextArray(R.array.chatgpt_model_entries);
                CharSequence[] entryValues = getResources().getTextArray(R.array.chatgpt_model_values);
                chatGptModelPreference.setEntries(entries);
                chatGptModelPreference.setEntryValues(entryValues);
                boolean isDefaultValid = Arrays.asList(entryValues).contains(currentSelectedModel);
                if(!isDefaultValid && entryValues.length > 0) {
                    currentSelectedModel = entryValues[0].toString();
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, currentSelectedModel).apply();
                }
                chatGptModelPreference.setValue(currentSelectedModel);
            }

            if (chatGptModelPreference.getEntry() != null) {
                chatGptModelPreference.setSummary(chatGptModelPreference.getEntry());
            }

            ListPreference transcriptionModePreference = findPreference(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE);
            if (transcriptionModePreference != null) {
                transcriptionModePreference.setSummary(transcriptionModePreference.getEntry() != null ? transcriptionModePreference.getEntry() : "Select transcription service");
            }

            ListPreference twoStepStep1EnginePref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE);
            if (twoStepStep1EnginePref != null) {
                 twoStepStep1EnginePref.setSummary(twoStepStep1EnginePref.getEntry() != null ? twoStepStep1EnginePref.getEntry() : "Choose Step 1 engine");
            }

            ListPreference twoStepStep1ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL);
            if (twoStepStep1ModelPref != null) {
                String defaultTranscriptionModel = "whisper-1";
                if (modelIdsSet != null && !modelIdsSet.isEmpty()) {
                    String[] modelIds = modelIdsSet.toArray(new String[0]);
                    Arrays.sort(modelIds);
                    twoStepStep1ModelPref.setEntries(modelIds);
                    twoStepStep1ModelPref.setEntryValues(modelIds);
                    String currentStep1Model = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel);
                    boolean step1ModelFound = Arrays.asList(modelIds).contains(currentStep1Model);
                    if (step1ModelFound) {
                        twoStepStep1ModelPref.setValue(currentStep1Model);
                    } else if (Arrays.asList(modelIds).contains(defaultTranscriptionModel)) {
                        twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    } else if (modelIds.length > 0) {
                        twoStepStep1ModelPref.setValue(modelIds[0]);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, modelIds[0]).apply();
                    } else {
                        twoStepStep1ModelPref.setEntries(new String[]{defaultTranscriptionModel});
                        twoStepStep1ModelPref.setEntryValues(new String[]{defaultTranscriptionModel});
                        twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    }
                } else {
                    Log.w("SettingsFragment", "No models for PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL. Using default.");
                    twoStepStep1ModelPref.setEntries(new String[]{defaultTranscriptionModel});
                    twoStepStep1ModelPref.setEntryValues(new String[]{defaultTranscriptionModel});
                    twoStepStep1ModelPref.setValue(defaultTranscriptionModel);
                    if (sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, null) == null) {
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, defaultTranscriptionModel).apply();
                    }
                }
                twoStepStep1ModelPref.setSummary(twoStepStep1ModelPref.getEntry() != null ? twoStepStep1ModelPref.getEntry() : twoStepStep1ModelPref.getValue());
            }

            ListPreference twoStepStep2ModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL);
             if (twoStepStep2ModelPref != null && modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                Arrays.sort(modelIds);
                twoStepStep2ModelPref.setEntries(modelIds);
                twoStepStep2ModelPref.setEntryValues(modelIds);
                String currentStep2Model = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, "gpt-4o");
                boolean step2ModelFound = Arrays.asList(modelIds).contains(currentStep2Model);
                 if (step2ModelFound) {
                    twoStepStep2ModelPref.setValue(currentStep2Model);
                } else if (modelIds.length > 0) {
                    twoStepStep2ModelPref.setValue(modelIds[0]);
                    sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, modelIds[0]).apply();
                }
                twoStepStep2ModelPref.setSummary(twoStepStep2ModelPref.getEntry() != null ? twoStepStep2ModelPref.getEntry() : "Select Step 2 model");
            } else if (twoStepStep2ModelPref != null) {
                 twoStepStep2ModelPref.setSummary("Select Step 2 model (models not loaded)");
            }


            ListPreference photoVisionModelPref = findPreference(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL);
            if (photoVisionModelPref != null && modelIdsSet != null && !modelIdsSet.isEmpty()) {
                String[] modelIds = modelIdsSet.toArray(new String[0]);
                Arrays.sort(modelIds);
                photoVisionModelPref.setEntries(modelIds);
                photoVisionModelPref.setEntryValues(modelIds);
                String currentPhotoModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
                boolean photoModelFound = Arrays.asList(modelIds).contains(currentPhotoModel);
                if (photoModelFound) {
                    photoVisionModelPref.setValue(currentPhotoModel);
                } else if (modelIds.length > 0) {
                    photoVisionModelPref.setValue(modelIds[0]);
                     sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, modelIds[0]).apply();
                }
                 photoVisionModelPref.setSummary(photoVisionModelPref.getEntry() != null ? photoVisionModelPref.getEntry() : "Select Photo Vision model");
            } else if (photoVisionModelPref != null) {
                photoVisionModelPref.setSummary("Select Photo Vision model (models not loaded)");
            }
        }

        private void updateTwoStepStep1ModelVisibility(String engineValue) {
            ListPreference twoStepStep1ChatGptModelPref = findPreference(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL);
            if (twoStepStep1ChatGptModelPref != null) {
                if ("chatgpt".equals(engineValue)) {
                    twoStepStep1ChatGptModelPref.setVisible(true);
                } else {
                    twoStepStep1ChatGptModelPref.setVisible(false);
                }
                Log.d("SettingsFragment", "Two Step Step 1 ChatGPT Model visibility set to: " + twoStepStep1ChatGptModelPref.isVisible());
            } else {
                Log.w("SettingsFragment", "Could not find pref_twostep_step1_chatgpt_model.");
            }
        }
        
        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }
        
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            final String[] oledColorKeys = {
                "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
                "pref_oled_main_background", "pref_oled_surface_background",
                "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
                "pref_oled_button_background", "pref_oled_button_text_icon",
                "pref_oled_textbox_background", "pref_oled_textbox_accent",
                "pref_oled_accent_general"
            };
            boolean isOledColorKey = Arrays.asList(oledColorKeys).contains(key);

            if (ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
                // ThemeManager.applyTheme(sharedPreferences); // This is handled by Activity's listener now
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            } else if (isOledColorKey) {
                String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
                if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                    if (getActivity() != null) {
                        Log.d("SettingsFragment", "OLED color preference changed in Fragment: " + key + ". Recreating activity.");
                        getActivity().recreate();
                    }
                }
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