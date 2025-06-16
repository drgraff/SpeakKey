package com.drgraff.speakkey;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences; // Standard import
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager; // Standard import
import android.util.Log; // Standard import
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.OpenAIModelData;
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.drgraff.speakkey.data.PhotoPromptsAdapter;
import com.drgraff.speakkey.settings.SettingsActivity;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import com.drgraff.speakkey.utils.ThemeManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PhotoPromptsActivity extends AppCompatActivity
    implements PhotoPromptsAdapter.OnPhotoPromptInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PhotoPromptsActivity";

    private RecyclerView photoPromptsRecyclerView;
    private PhotoPromptsAdapter photoPromptsAdapter;
    private PromptManager promptManager;
    private TextView emptyPhotoPromptsTextView;
    private FloatingActionButton fabAddPhotoPrompt;
    private Button btnCheckPhotoModels;
    private Spinner spinnerPhotoModels;
    private Toolbar toolbar;

    private ChatGptApi chatGptApi;
    private SharedPreferences sharedPreferences; // Class field
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<String> modelList = new ArrayList<>();
    private ArrayAdapter<String> modelAdapter;

    private static final int REQUEST_ADD_PHOTO_PROMPT = 1;
    private static final int REQUEST_EDIT_PHOTO_PROMPT = 2;

    // Member variables for theme state tracking
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ThemeManager.applyTheme(this.sharedPreferences);
        String themeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_prompts);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.photo_prompts_title));
        }

        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "PhotoPromptsActivity: Applied dynamic OLED colors.");
        }

        String apiKey = this.sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.photo_prompts_toast_api_key_not_set), Toast.LENGTH_LONG).show();
        }
        chatGptApi = new ChatGptApi(apiKey, "default_model_not_used_for_listing");
        promptManager = new PromptManager(this);

        photoPromptsRecyclerView = findViewById(R.id.photo_prompts_recycler_view);
        photoPromptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        photoPromptsAdapter = new PhotoPromptsAdapter(this, new ArrayList<>(), promptManager, this);
        photoPromptsRecyclerView.setAdapter(photoPromptsAdapter);

        emptyPhotoPromptsTextView = findViewById(R.id.empty_photo_prompts_text_view);
        fabAddPhotoPrompt = findViewById(R.id.fab_add_photo_prompt);
        btnCheckPhotoModels = findViewById(R.id.btn_check_photo_models);
        spinnerPhotoModels = findViewById(R.id.spinner_photo_models);

        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhotoModels.setAdapter(modelAdapter);

        if (apiKey.isEmpty()) {
            btnCheckPhotoModels.setEnabled(false);
        } else {
            btnCheckPhotoModels.setEnabled(true);
        }

        fabAddPhotoPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoPromptsActivity.this, com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class);
            startActivityForResult(intent, REQUEST_ADD_PHOTO_PROMPT);
        });

        btnCheckPhotoModels.setOnClickListener(v -> fetchPhotoModelsAndUpdateSpinner());

        spinnerPhotoModels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, selectedModel).apply();
                Log.d(TAG, "Selected photo model saved: " + selectedModel);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadAndPopulatePhotoModelsSpinner();
        loadPhotoPrompts();

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue;
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PhotoPromptsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PhotoPromptsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
    }

    private void fetchPhotoModelsAndUpdateSpinner() {
        if (chatGptApi == null || sharedPreferences.getString("openai_api_key", "").isEmpty()) {
            Toast.makeText(this, getString(R.string.photo_prompts_toast_api_key_cant_fetch), Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.photo_prompts_progress_fetching_models));
        progressDialog.setCancelable(false);
        progressDialog.show();
        executorService.execute(() -> {
            try {
                List<OpenAIModelData.ModelInfo> allModels = chatGptApi.listModels();
                List<String> visionModelIds = allModels.stream()
                        .filter(model -> model.id.contains("vision") || model.id.contains("dall-e") || model.id.contains("gpt-4"))
                        .map(model -> model.id)
                        .distinct().sorted().collect(Collectors.toList());
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    if (visionModelIds.isEmpty()) {
                        Toast.makeText(PhotoPromptsActivity.this, getString(R.string.photo_prompts_toast_no_specific_models), Toast.LENGTH_LONG).show();
                    }
                    modelList.clear();
                    modelList.addAll(visionModelIds.isEmpty() ? allModels.stream().map(m -> m.id).sorted().collect(Collectors.toList()) : visionModelIds);
                    modelAdapter.notifyDataSetChanged();
                    Set<String> modelSet = new HashSet<>(modelList);
                    sharedPreferences.edit().putStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, modelSet).apply();
                    Log.d(TAG, "Fetched and saved photo models: " + modelSet.size());
                    String previouslySelected = sharedPreferences.getString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, null);
                    if (previouslySelected != null) {
                        int spinnerPosition = modelAdapter.getPosition(previouslySelected);
                        if (spinnerPosition >= 0) spinnerPhotoModels.setSelection(spinnerPosition);
                        else if (!modelList.isEmpty()){
                            spinnerPhotoModels.setSelection(0);
                            sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, modelList.get(0)).apply();
                        }
                    } else if (!modelList.isEmpty()) {
                        spinnerPhotoModels.setSelection(0);
                        sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, modelList.get(0)).apply();
                    }
                    Toast.makeText(PhotoPromptsActivity.this, getString(R.string.photo_prompts_toast_models_updated), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching models: ", e);
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(PhotoPromptsActivity.this, String.format(getString(R.string.photo_prompts_toast_error_fetching_models_format), e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadAndPopulatePhotoModelsSpinner() {
        Set<String> fetchedModelIds = sharedPreferences.getStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, null);
        modelList.clear();
        if (fetchedModelIds != null && !fetchedModelIds.isEmpty()) {
            modelList.addAll(new ArrayList<>(fetchedModelIds));
            Collections.sort(modelList);
            Log.d(TAG, "Loaded photo models from Prefs: " + modelList.size());
        } else {
            Log.d(TAG, "No photo models found in Prefs. Use 'Check Models' button.");
        }
        modelAdapter.notifyDataSetChanged();
        String selectedModelId = sharedPreferences.getString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
        if (!modelList.isEmpty()) {
            int spinnerPosition = modelAdapter.getPosition(selectedModelId);
            if (spinnerPosition >= 0) spinnerPhotoModels.setSelection(spinnerPosition);
            else {
                 spinnerPhotoModels.setSelection(0);
                 sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, modelList.get(0)).apply();
            }
        }
    }

    private void loadPhotoPrompts() {
        List<Prompt> prompts = promptManager.getPromptsForMode("photo_vision");
        photoPromptsAdapter.setPrompts(prompts);
        if (prompts.isEmpty()) {
            photoPromptsRecyclerView.setVisibility(View.GONE);
            emptyPhotoPromptsTextView.setVisibility(View.VISIBLE);
        } else {
            photoPromptsRecyclerView.setVisibility(View.VISIBLE);
            emptyPhotoPromptsTextView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAppliedThemeMode != null && this.sharedPreferences != null) {
            boolean needsRecreate = false;
            String currentThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (!mAppliedThemeMode.equals(currentThemeValue)) {
                needsRecreate = true;
                Log.d(TAG, "onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue);
            } else if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
                int currentTopbarBG = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
                if (mAppliedTopbarBackgroundColor != currentTopbarBG) needsRecreate = true;
                int currentTopbarTextIcon = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
                if (mAppliedTopbarTextIconColor != currentTopbarTextIcon) needsRecreate = true;
                int currentMainBG = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
                if (mAppliedMainBackgroundColor != currentMainBG) needsRecreate = true;
                if (needsRecreate) {
                     Log.d(TAG, "onResume: OLED color(s) changed for PhotoPromptsActivity.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating PhotoPromptsActivity.");
                recreate();
                return;
            }
        }
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        loadPhotoPrompts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditPhotoPrompt(Prompt prompt) {
        Intent intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class);
        intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, prompt.getId());
        startActivityForResult(intent, REQUEST_EDIT_PHOTO_PROMPT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @androidx.annotation.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD_PHOTO_PROMPT || requestCode == REQUEST_EDIT_PHOTO_PROMPT) && resultCode == Activity.RESULT_OK) {
            loadPhotoPrompts();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged triggered for key: " + key);
        if (key == null) return;
        final String[] oledColorKeys = {
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon",
            "pref_oled_main_background", "pref_oled_surface_background",
            "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
            "pref_oled_button_background", "pref_oled_button_text_icon",
            "pref_oled_textbox_background", "pref_oled_textbox_accent",
            "pref_oled_accent_general"
        };
        boolean isOledColorKey = false;
        for (String oledKey : oledColorKeys) {
            if (oledKey.equals(key)) {
                isOledColorKey = true;
                break;
            }
        }
        if (ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
            Log.d(TAG, "Main theme preference changed. Recreating PhotoPromptsActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating PhotoPromptsActivity.");
                recreate();
            }
        }
    }
}
