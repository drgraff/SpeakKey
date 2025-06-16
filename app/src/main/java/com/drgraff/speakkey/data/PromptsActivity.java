package com.drgraff.speakkey.data;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences; // Added
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager; // Standard import
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import com.drgraff.speakkey.settings.SettingsActivity;
// import java.util.function.Consumer; // Not directly used in provided snippet, keep if needed elsewhere
import android.view.LayoutInflater;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface; // Standard import
import android.widget.Toast;
import android.widget.RadioGroup;
import android.content.res.ColorStateList; // Added for button/FAB styling

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.api.ChatGptApi;
// import com.drgraff.speakkey.api.OpenAIModelData; // Not directly used in provided snippet
// import com.drgraff.speakkey.ui.prompts.PromptEditorActivity; // Used in intent creation
// import com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity; // Used in intent creation
// import com.drgraff.speakkey.data.PromptsAdapter; // Class field
import com.drgraff.speakkey.utils.DynamicThemeApplicator; // Added
import com.drgraff.speakkey.utils.ThemeManager; // Added
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
// import java.util.HashSet; // Not directly used
import java.util.List;
import java.util.Set; // Added
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

// Implement SharedPreferences.OnSharedPreferenceChangeListener
public class PromptsActivity extends AppCompatActivity
    implements PromptsAdapter.OnPromptInteractionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private PromptsAdapter oneStepPromptsAdapter, twoStepPromptsAdapter, photoPromptsAdapter;
    private Toolbar toolbarPrompts; // Ensure this is androidx.appcompat.widget.Toolbar
    private Spinner spinnerOneStepModel, spinnerTwoStepProcessingModel, spinnerPhotoVisionModel;
    private Button btnCheckOneStepModels, btnCheckTwoStepModels, btnCheckPhotoModels;
    private RecyclerView recyclerViewOneStepPrompts, recyclerViewTwoStepPrompts, recyclerViewPhotoPrompts;
    private FloatingActionButton fabAddPrompt;
    private TextView tvEmptyOneStepPrompts, tvEmptyTwoStepPrompts, tvEmptyPhotoPrompts;

    private ArrayAdapter<String> oneStepModelAdapter, twoStepProcessingModelAdapter, photoVisionModelAdapter;
    private List<String> modelList = new ArrayList<>();

    private PromptManager promptManager;
    private ChatGptApi chatGptApi;
    private SharedPreferences sharedPreferences; // Made into a field
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProgressDialog progressDialog;
    private String currentFilterMode = null;

    private static final String TAG = "PromptsActivity"; // Added TAG
    public static final String EXTRA_FILTER_MODE_TYPE = "com.drgraff.speakkey.FILTER_MODE_TYPE";
    private static final int ADD_PROMPT_REQUEST = 1;
    private static final int REQUEST_EDIT_PROMPT = 2;

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
        setContentView(R.layout.activity_prompts);

        // Toolbar ID was changed to R.id.toolbar in activity_prompts.xml
        toolbarPrompts = findViewById(R.id.toolbar);
        setSupportActionBar(toolbarPrompts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.prompts_activity_title);
        }

        // Apply dynamic OLED colors if OLED theme is active
        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "PromptsActivity: Applied dynamic OLED colors for window/toolbar.");

            // Style specific buttons
            int buttonBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_button_background",
                DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
            );
            int buttonTextIconColor = this.sharedPreferences.getInt(
                "pref_oled_button_text_icon",
                DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
            );

            Button[] checkButtonsToStyle = {
                btnCheckOneStepModels, btnCheckTwoStepModels, btnCheckPhotoModels
            };
            String[] checkButtonNames = {
                "btnCheckOneStepModels", "btnCheckTwoStepModels", "btnCheckPhotoModels"
            };

            for (int i = 0; i < checkButtonsToStyle.length; i++) {
                Button button = checkButtonsToStyle[i];
                String buttonName = checkButtonNames[i];
                if (button != null) {
                    button.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                    button.setTextColor(buttonTextIconColor);
                    Log.d(TAG, String.format("PromptsActivity: Styled %s with BG=0x%08X, Text=0x%08X", buttonName, buttonBackgroundColor, buttonTextIconColor));
                } else {
                    Log.w(TAG, "PromptsActivity: Button " + buttonName + " is null, cannot style.");
                }
            }

            if (fabAddPrompt != null) {
                int accentGeneralColor = this.sharedPreferences.getInt(
                    "pref_oled_accent_general",
                    DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL
                );
                fabAddPrompt.setBackgroundTintList(ColorStateList.valueOf(accentGeneralColor));
                fabAddPrompt.setImageTintList(ColorStateList.valueOf(buttonTextIconColor)); // Using button text color for icon
                Log.d(TAG, String.format("PromptsActivity: Styled fabAddPrompt with BG=0x%08X, IconTint=0x%08X", accentGeneralColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "PromptsActivity: fabAddPrompt is null, cannot style.");
            }
        }


        String apiKey = this.sharedPreferences.getString("openai_api_key", "");
        chatGptApi = new ChatGptApi(apiKey, "");
        promptManager = new PromptManager(this);

        spinnerOneStepModel = findViewById(R.id.spinnerOneStepModel);
        btnCheckOneStepModels = findViewById(R.id.btnCheckOneStepModels);
        recyclerViewOneStepPrompts = findViewById(R.id.recyclerViewOneStepPrompts);
        spinnerTwoStepProcessingModel = findViewById(R.id.spinnerTwoStepProcessingModel);
        btnCheckTwoStepModels = findViewById(R.id.btnCheckTwoStepModels);
        recyclerViewTwoStepPrompts = findViewById(R.id.recyclerViewTwoStepPrompts);
        spinnerPhotoVisionModel = findViewById(R.id.spinnerPhotoVisionModel);
        btnCheckPhotoModels = findViewById(R.id.btnCheckPhotoModels);
        recyclerViewPhotoPrompts = findViewById(R.id.recyclerViewPhotoPrompts);
        tvEmptyOneStepPrompts = findViewById(R.id.tvEmptyOneStepPrompts);
        tvEmptyTwoStepPrompts = findViewById(R.id.tvEmptyTwoStepPrompts);
        tvEmptyPhotoPrompts = findViewById(R.id.tvEmptyPhotoPrompts);
        fabAddPrompt = findViewById(R.id.fabAddPrompt);

        oneStepModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        oneStepModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOneStepModel.setAdapter(oneStepModelAdapter);
        spinnerOneStepModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, selectedModel).apply();
                Log.d(TAG, "OneStep Model saved: " + selectedModel);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        btnCheckOneStepModels.setOnClickListener(v -> fetchModelsForSpinners());

        twoStepProcessingModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        twoStepProcessingModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTwoStepProcessingModel.setAdapter(twoStepProcessingModelAdapter);
        spinnerTwoStepProcessingModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, selectedModel).apply();
                Log.d(TAG, "TwoStep Processing Model saved: " + selectedModel);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        btnCheckTwoStepModels.setOnClickListener(v -> fetchModelsForSpinners());

        photoVisionModelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        photoVisionModelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhotoVisionModel.setAdapter(photoVisionModelAdapter);
        spinnerPhotoVisionModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, selectedModel).apply();
                Log.d(TAG, "PhotoVision Model saved: " + selectedModel);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        btnCheckPhotoModels.setOnClickListener(v -> fetchModelsForSpinners());

        if (apiKey.isEmpty()) {
            btnCheckOneStepModels.setEnabled(false);
            btnCheckTwoStepModels.setEnabled(false);
            btnCheckPhotoModels.setEnabled(false);
            Toast.makeText(this, "OpenAI API Key not set in app settings.", Toast.LENGTH_LONG).show();
        }

        populateAllModelSpinnersFromCache();

        recyclerViewOneStepPrompts.setLayoutManager(new LinearLayoutManager(this));
        oneStepPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewOneStepPrompts.setAdapter(oneStepPromptsAdapter);

        recyclerViewTwoStepPrompts.setLayoutManager(new LinearLayoutManager(this));
        twoStepPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewTwoStepPrompts.setAdapter(twoStepPromptsAdapter);

        recyclerViewPhotoPrompts.setLayoutManager(new LinearLayoutManager(this));
        photoPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewPhotoPrompts.setAdapter(photoPromptsAdapter);

        fabAddPrompt.setOnClickListener(v -> { // FAB listener logic ...
            if (currentFilterMode != null) {
                Intent intent;
                Class<?> editorActivityClass;
                String modeForNewPrompt = currentFilterMode;

                if ("photo_vision".equals(modeForNewPrompt)) {
                    editorActivityClass = com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class;
                    intent = new Intent(PromptsActivity.this, editorActivityClass);
                    intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, -1L);
                } else {
                    editorActivityClass = com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class;
                    intent = new Intent(PromptsActivity.this, editorActivityClass);
                    intent.putExtra(com.drgraff.speakkey.ui.prompts.PromptEditorActivity.EXTRA_PROMPT_ID, -1L);
                }
                intent.putExtra("PROMPT_MODE_TYPE", modeForNewPrompt);
                startActivityForResult(intent, REQUEST_ADD_PROMPT);
                Log.d(TAG, "FAB clicked (filtered view), launching editor for mode: " + modeForNewPrompt);

            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(PromptsActivity.this);
                LayoutInflater inflater = PromptsActivity.this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_select_prompt_mode, null);
                builder.setView(dialogView);
                builder.setTitle(getString(R.string.create_prompt_dialog_title));

                final RadioGroup rgModeSelection = dialogView.findViewById(R.id.radio_group_prompt_mode_selection);

                builder.setPositiveButton(getString(R.string.dialog_button_next), (dialog, which) -> {
                    int selectedId = rgModeSelection.getCheckedRadioButtonId();
                    String selectedModeType = null;
                    Class<?> editorActivityClass = com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class;

                    if (selectedId == R.id.radio_mode_one_step) {
                        selectedModeType = "one_step";
                    } else if (selectedId == R.id.radio_mode_two_step) {
                        selectedModeType = "two_step_processing";
                    } else if (selectedId == R.id.radio_mode_photo) {
                        selectedModeType = "photo_vision";
                        editorActivityClass = com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class;
                    }

                    if (selectedModeType != null) {
                        Intent intent = new Intent(PromptsActivity.this, editorActivityClass);
                        intent.putExtra("PROMPT_MODE_TYPE", selectedModeType);
                        if ("photo_vision".equals(selectedModeType)) {
                             intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, -1L);
                        } else {
                             intent.putExtra(com.drgraff.speakkey.ui.prompts.PromptEditorActivity.EXTRA_PROMPT_ID, -1L);
                        }
                        startActivityForResult(intent, REQUEST_ADD_PROMPT);
                        Log.d(TAG, "FAB clicked (dialog), launching editor for selected mode: " + selectedModeType);
                    } else {
                        Toast.makeText(PromptsActivity.this, getString(R.string.select_mode_toast), Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }); // End of fabAddPrompt listener

        currentFilterMode = getIntent().getStringExtra(EXTRA_FILTER_MODE_TYPE);
        Log.d(TAG, "onCreate: currentFilterMode = " + currentFilterMode);
        applyFilterVisibilityAndTitle();

        // Store applied theme state
        String finalAppliedThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        this.mAppliedThemeMode = finalAppliedThemeValue;
        if (ThemeManager.THEME_OLED.equals(finalAppliedThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PromptsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PromptsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
    } // End of onCreate

    private void setTitleBasedOnFilterMode(String filterMode) { // Unchanged
        if (getSupportActionBar() == null) return;
        if ("one_step".equals(filterMode)) {
            getSupportActionBar().setTitle(getString(R.string.title_prompts_one_step));
        } else if ("two_step_processing".equals(filterMode)) {
            getSupportActionBar().setTitle(getString(R.string.title_prompts_two_step));
        } else if ("photo_vision".equals(filterMode)) {
            getSupportActionBar().setTitle(getString(R.string.title_prompts_photo));
        } else {
            getSupportActionBar().setTitle(getString(R.string.prompts_activity_title));
        }
    }

    private void applyFilterVisibilityAndTitle() { // Unchanged
        View sectionOneStepContainer = findViewById(R.id.sectionOneStepContainer);
        View sectionTwoStepContainer = findViewById(R.id.sectionTwoStepContainer);
        View sectionPhotoContainer = findViewById(R.id.sectionPhotoContainer);

        if (sectionOneStepContainer == null || sectionTwoStepContainer == null || sectionPhotoContainer == null) {
            Log.e(TAG, "One or more section containers not found in applyFilterVisibilityAndTitle");
            return;
        }

        if (currentFilterMode != null) {
            setTitleBasedOnFilterMode(currentFilterMode);
            sectionOneStepContainer.setVisibility("one_step".equals(currentFilterMode) ? View.VISIBLE : View.GONE);
            sectionTwoStepContainer.setVisibility("two_step_processing".equals(currentFilterMode) ? View.VISIBLE : View.GONE);
            sectionPhotoContainer.setVisibility("photo_vision".equals(currentFilterMode) ? View.VISIBLE : View.GONE);
        } else {
             if (getSupportActionBar() != null) getSupportActionBar().setTitle(getString(R.string.prompts_activity_title)); // Corrected
            sectionOneStepContainer.setVisibility(View.VISIBLE);
            sectionTwoStepContainer.setVisibility(View.VISIBLE);
            sectionPhotoContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() { // New onResume
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
                     Log.d(TAG, "onResume: OLED color(s) changed for PromptsActivity.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating PromptsActivity.");
                recreate();
                return;
            }
        }
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        // Original onResume content
        populateAllModelSpinnersFromCache();
        loadAllPromptsSections();
        applyFilterVisibilityAndTitle();
    }

    @Override
    protected void onPause() { // New onPause
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { // New method
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
            Log.d(TAG, "Main theme preference changed. Recreating PromptsActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating PromptsActivity.");
                recreate();
            }
        }
    }

    private void loadAllPromptsSections() { // Unchanged
        if (promptManager == null) {
            Log.e(TAG, "PromptManager not initialized in loadAllPromptsSections");
            return;
        }
        List<Prompt> oneStepPrompts = promptManager.getPromptsForMode("one_step");
        if (oneStepPromptsAdapter != null) oneStepPromptsAdapter.setPrompts(oneStepPrompts);
        if (recyclerViewOneStepPrompts != null && tvEmptyOneStepPrompts != null) {
            recyclerViewOneStepPrompts.setVisibility(oneStepPrompts.isEmpty() ? View.GONE : View.VISIBLE);
            tvEmptyOneStepPrompts.setVisibility(oneStepPrompts.isEmpty() ? View.VISIBLE : View.GONE);
        }
        List<Prompt> twoStepPrompts = promptManager.getPromptsForMode("two_step_processing");
        if (twoStepPromptsAdapter != null) twoStepPromptsAdapter.setPrompts(twoStepPrompts);
        if (recyclerViewTwoStepPrompts != null && tvEmptyTwoStepPrompts != null) {
            recyclerViewTwoStepPrompts.setVisibility(twoStepPrompts.isEmpty() ? View.GONE : View.VISIBLE);
            tvEmptyTwoStepPrompts.setVisibility(twoStepPrompts.isEmpty() ? View.VISIBLE : View.GONE);
        }
        List<Prompt> photoPrompts = promptManager.getPromptsForMode("photo_vision");
        if (photoPromptsAdapter != null) photoPromptsAdapter.setPrompts(photoPrompts);
        if (recyclerViewPhotoPrompts != null && tvEmptyPhotoPrompts != null) {
            recyclerViewPhotoPrompts.setVisibility(photoPrompts.isEmpty() ? View.GONE : View.VISIBLE);
            tvEmptyPhotoPrompts.setVisibility(photoPrompts.isEmpty() ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "All prompt sections reloaded.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // Unchanged
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD_PROMPT || requestCode == REQUEST_EDIT_PROMPT) && resultCode == Activity.RESULT_OK) {
            loadAllPromptsSections();
            Log.d(TAG, "Returned from EditorActivity with RESULT_OK, reloaded prompts.");
        }
    }

    @Override
    public void onEditPrompt(Prompt prompt) { // Unchanged
        Intent intent;
        if ("photo_vision".equals(prompt.getPromptModeType())) {
            intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class);
            intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, prompt.getId());
        } else {
            intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class);
            intent.putExtra(com.drgraff.speakkey.ui.prompts.PromptEditorActivity.EXTRA_PROMPT_ID, prompt.getId());
            intent.putExtra("PROMPT_MODE_TYPE", prompt.getPromptModeType());
        }
        startActivityForResult(intent, REQUEST_EDIT_PROMPT);
    }

    @Override
    public void onCopyPrompt(Prompt promptToCopy) { // Unchanged logic, just ensure it compiles
        if (promptToCopy == null) {
            Toast.makeText(this, "Error: Prompt to copy is null.", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_copy_prompt, null);
        builder.setView(dialogView);
        builder.setTitle(getString(R.string.copy_prompt_dialog_title));
        final EditText etNewLabel = dialogView.findViewById(R.id.et_copy_prompt_new_label);
        final TextView tvOriginalText = dialogView.findViewById(R.id.tv_copy_prompt_original_text);
        final Spinner spinnerDestinationMode = dialogView.findViewById(R.id.spinner_copy_prompt_destination_mode);
        etNewLabel.setText("Copy of " + promptToCopy.getLabel());
        if (tvOriginalText != null) tvOriginalText.setText(promptToCopy.getText());
        final String[] modeDisplayNames = {"One Step Transcription", "Two Step Transcription", "Photo Vision"};
        final String[] modeInternalValues = {"one_step", "two_step_processing", "photo_vision"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modeDisplayNames);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDestinationMode.setAdapter(modeAdapter);
        int originalModeIndex = 0;
        if (promptToCopy.getPromptModeType() != null) {
            for (int i = 0; i < modeInternalValues.length; i++) {
                if (promptToCopy.getPromptModeType().equals(modeInternalValues[i])) {
                    originalModeIndex = i;
                    break;
                }
            }
        }
        spinnerDestinationMode.setSelection(originalModeIndex);
        builder.setPositiveButton(getString(R.string.copy_prompt_save_button), (dialog, which) -> {
            String newLabel = etNewLabel.getText().toString().trim();
            String originalText = promptToCopy.getText();
            String destinationModeType = modeInternalValues[spinnerDestinationMode.getSelectedItemPosition()];
            if (newLabel.isEmpty()) {
                Toast.makeText(PromptsActivity.this, getString(R.string.new_label_empty_error_toast), Toast.LENGTH_SHORT).show();
                return;
            }
            if (promptManager != null) {
                promptManager.addPrompt(newLabel, originalText, "", destinationModeType);
                Toast.makeText(PromptsActivity.this, getString(R.string.prompt_copied_toast_format, spinnerDestinationMode.getSelectedItem().toString()), Toast.LENGTH_SHORT).show();
                loadAllPromptsSections();
            } else {
                Toast.makeText(PromptsActivity.this, getString(R.string.prompt_manager_unavailable_error_toast), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void fetchModelsForSpinners() { // Unchanged
        if (chatGptApi == null || sharedPreferences.getString("openai_api_key", "").isEmpty()) {
            Toast.makeText(this, getString(R.string.api_key_not_set_for_models_toast), Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.fetching_models_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();
        ChatGptApi.fetchAndCacheOpenAiModels(
                chatGptApi, sharedPreferences, SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS,
                executorService, mainHandler,
                models -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(PromptsActivity.this, getString(R.string.models_updated_toast), Toast.LENGTH_SHORT).show();
                    populateAllModelSpinnersFromCache();
                },
                exception -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(PromptsActivity.this, getString(R.string.error_fetching_models_toast_format, exception.getMessage()), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error fetching models: ", exception);
                }
        );
    }

    private void populateAllModelSpinnersFromCache() { // Unchanged
        final List<String> placeholderItem = Arrays.asList(getString(R.string.no_models_loaded_placeholder));
        Set<String> fetchedModelIdsSet = sharedPreferences.getStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, null);
        boolean modelsAvailable = (fetchedModelIdsSet != null && !fetchedModelIdsSet.isEmpty());
        modelList.clear();
        if (modelsAvailable) {
            modelList.addAll(new ArrayList<>(fetchedModelIdsSet));
            Collections.sort(modelList);
            Log.d(TAG, "Populating spinners with " + modelList.size() + " cached models.");
        } else {
            Log.w(TAG, "No cached models found. Using placeholder for spinners.");
            modelList.addAll(placeholderItem);
        }
        if (oneStepModelAdapter != null) {
            oneStepModelAdapter.clear(); oneStepModelAdapter.addAll(modelList);
            oneStepModelAdapter.notifyDataSetChanged(); spinnerOneStepModel.setEnabled(modelsAvailable);
        }
        if (twoStepProcessingModelAdapter != null) {
            twoStepProcessingModelAdapter.clear(); twoStepProcessingModelAdapter.addAll(modelList);
            twoStepProcessingModelAdapter.notifyDataSetChanged(); spinnerTwoStepProcessingModel.setEnabled(modelsAvailable);
        }
        if (photoVisionModelAdapter != null) {
            photoVisionModelAdapter.clear(); photoVisionModelAdapter.addAll(modelList);
            photoVisionModelAdapter.notifyDataSetChanged(); spinnerPhotoVisionModel.setEnabled(modelsAvailable);
        }
        loadAndSetSpinnerSelection(spinnerOneStepModel, oneStepModelAdapter, SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-4o");
        loadAndSetSpinnerSelection(spinnerTwoStepProcessingModel, twoStepProcessingModelAdapter, SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, "gpt-4o");
        loadAndSetSpinnerSelection(spinnerPhotoVisionModel, photoVisionModelAdapter, SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
    }

    private void loadAndSetSpinnerSelection(Spinner spinner, ArrayAdapter<String> adapter, String prefKey, String defaultModel) { // Unchanged
        if (spinner == null || adapter == null) {
            Log.w(TAG, "Spinner or Adapter is null for prefKey: " + prefKey); return;
        }
        String selectedModel = sharedPreferences.getString(prefKey, defaultModel);
        if (adapter.getCount() == 0) {
            Log.w(TAG, "Adapter for " + prefKey + " is empty.");
            if(sharedPreferences.getString(prefKey, null) == null) {
                 sharedPreferences.edit().putString(prefKey, defaultModel).apply();
            }
            return;
        }
        int position = adapter.getPosition(selectedModel);
        if (position >= 0) {
            spinner.setSelection(position);
        } else {
            Log.w(TAG, "Saved model '" + selectedModel + "' for " + prefKey + " not found in adapter. Selecting first available.");
            if (adapter.getCount() > 0) {
                spinner.setSelection(0);
                sharedPreferences.edit().putString(prefKey, adapter.getItem(0)).apply();
            } else {
                sharedPreferences.edit().putString(prefKey, defaultModel).apply();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // Unchanged
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() { // Unchanged
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
} // End of PromptsActivity class
