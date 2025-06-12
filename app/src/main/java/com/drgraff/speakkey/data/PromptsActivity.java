package com.drgraff.speakkey.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
// Removed Button, EditText, Toast (Toast might be re-added if needed for other purposes)
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import com.drgraff.speakkey.settings.SettingsActivity;
import java.util.function.Consumer;
import android.view.LayoutInflater; // Added
import android.widget.EditText; // Added
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import android.widget.RadioGroup; // Added

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Added for Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.OpenAIModelData;
import com.drgraff.speakkey.ui.prompts.PromptEditorActivity;
import com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity; // Added for intent
import com.drgraff.speakkey.data.PromptsAdapter; // Added
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays; // Added
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PromptsActivity extends AppCompatActivity implements PromptsAdapter.OnPromptInteractionListener {

    // Adapters for RecyclerViews
    private PromptsAdapter oneStepPromptsAdapter, twoStepPromptsAdapter, photoPromptsAdapter;

    // private RecyclerView promptsRecyclerView; // Old single RecyclerView
    // private PromptsAdapter promptsAdapter; // Old single adapter
    // private TextView emptyPromptsTextView; // Old empty view

    // New UI Elements
    private Toolbar toolbarPrompts;
    private Spinner spinnerOneStepModel, spinnerTwoStepProcessingModel, spinnerPhotoVisionModel;
    private Button btnCheckOneStepModels, btnCheckTwoStepModels, btnCheckPhotoModels;
    private RecyclerView recyclerViewOneStepPrompts, recyclerViewTwoStepPrompts, recyclerViewPhotoPrompts;
    private FloatingActionButton fabAddPrompt;
    private TextView tvEmptyOneStepPrompts, tvEmptyTwoStepPrompts, tvEmptyPhotoPrompts; // Added

    // Adapters for Spinners
    private ArrayAdapter<String> oneStepModelAdapter, twoStepProcessingModelAdapter, photoVisionModelAdapter;
    private List<String> modelList = new ArrayList<>(); // Common list for all spinners

    // Core components
    private PromptManager promptManager;
    private ChatGptApi chatGptApi;
    private SharedPreferences sharedPreferences;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProgressDialog progressDialog;
    private String currentFilterMode = null; // Added

    private static final String TAG = "PromptsActivity";
    public static final String EXTRA_FILTER_MODE_TYPE = "com.drgraff.speakkey.FILTER_MODE_TYPE";

    // Removed local PREF_KEY constants

    private static final int ADD_PROMPT_REQUEST = 1;
    private static final int REQUEST_ADD_PROMPT = ADD_PROMPT_REQUEST; // Alias for clarity if used elsewhere
    private static final int REQUEST_EDIT_PROMPT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompts);

        toolbarPrompts = findViewById(R.id.toolbar_prompts);
        setSupportActionBar(toolbarPrompts);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.prompts_activity_title); // Assuming this string resource exists
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        // Initialize chatGptApi without a default model for this screen, as each section has its own
        chatGptApi = new ChatGptApi(apiKey, ""); // Model will be set based on spinner/section

        promptManager = new PromptManager(this);

        // Initialize new UI elements
        spinnerOneStepModel = findViewById(R.id.spinnerOneStepModel);
        btnCheckOneStepModels = findViewById(R.id.btnCheckOneStepModels);
        recyclerViewOneStepPrompts = findViewById(R.id.recyclerViewOneStepPrompts);

        spinnerTwoStepProcessingModel = findViewById(R.id.spinnerTwoStepProcessingModel);
        btnCheckTwoStepModels = findViewById(R.id.btnCheckTwoStepModels);
        recyclerViewTwoStepPrompts = findViewById(R.id.recyclerViewTwoStepPrompts);

        spinnerPhotoVisionModel = findViewById(R.id.spinnerPhotoVisionModel);
        btnCheckPhotoModels = findViewById(R.id.btnCheckPhotoModels);
        recyclerViewPhotoPrompts = findViewById(R.id.recyclerViewPhotoPrompts);

        tvEmptyOneStepPrompts = findViewById(R.id.tvEmptyOneStepPrompts); // Added
        tvEmptyTwoStepPrompts = findViewById(R.id.tvEmptyTwoStepPrompts); // Added
        tvEmptyPhotoPrompts = findViewById(R.id.tvEmptyPhotoPrompts);   // Added

        fabAddPrompt = findViewById(R.id.fabAddPrompt);

        // Setup Spinners
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
             android.widget.Toast.makeText(this, "OpenAI API Key not set in app settings.", android.widget.Toast.LENGTH_LONG).show();
        }


        populateAllModelSpinnersFromCache(); // Load initial data into spinners

        // Setup RecyclerViews
        recyclerViewOneStepPrompts.setLayoutManager(new LinearLayoutManager(this));
        oneStepPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewOneStepPrompts.setAdapter(oneStepPromptsAdapter);

        recyclerViewTwoStepPrompts.setLayoutManager(new LinearLayoutManager(this));
        twoStepPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewTwoStepPrompts.setAdapter(twoStepPromptsAdapter);

        recyclerViewPhotoPrompts.setLayoutManager(new LinearLayoutManager(this));
        photoPromptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager, this);
        recyclerViewPhotoPrompts.setAdapter(photoPromptsAdapter);

        fabAddPrompt.setOnClickListener(v -> {
            if (currentFilterMode != null) {
                // Activity is filtered, launch editor for the current mode
                Intent intent;
                Class<?> editorActivityClass;
                String modeForNewPrompt = currentFilterMode;

                if ("photo_vision".equals(modeForNewPrompt)) {
                    editorActivityClass = com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class;
                    intent = new Intent(PromptsActivity.this, editorActivityClass);
                    intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, -1L); // Indicate new prompt
                } else { // "one_step" or "two_step_processing"
                    editorActivityClass = com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class;
                    intent = new Intent(PromptsActivity.this, editorActivityClass);
                    intent.putExtra(com.drgraff.speakkey.ui.prompts.PromptEditorActivity.EXTRA_PROMPT_ID, -1L); // Indicate new prompt
                }
                intent.putExtra("PROMPT_MODE_TYPE", modeForNewPrompt);
                startActivityForResult(intent, REQUEST_ADD_PROMPT);
                Log.d(TAG, "FAB clicked (filtered view), launching editor for mode: " + modeForNewPrompt);

            } else {
                // Activity is not filtered (showing all sections), show mode selection dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(PromptsActivity.this);
                LayoutInflater inflater = PromptsActivity.this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_select_prompt_mode, null);
                builder.setView(dialogView);
                builder.setTitle(getString(R.string.create_prompt_dialog_title));

                final RadioGroup rgModeSelection = dialogView.findViewById(R.id.radio_group_prompt_mode_selection);
                // Optional: Pre-select a default radio button, e.g., Two Step
                // RadioButton radioTwoStep = dialogView.findViewById(R.id.radio_mode_two_step);
                // if (radioTwoStep != null) radioTwoStep.setChecked(true);


                builder.setPositiveButton(getString(R.string.dialog_button_next), (dialog, which) -> {
                    int selectedId = rgModeSelection.getCheckedRadioButtonId();
                    String selectedModeType = null;
                    Class<?> editorActivityClass = com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class; // Default

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
                        // Note: To prevent dialog dismissal on error, more complex dialog handling is needed.
                    }
                });
                builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        // Filter logic
        currentFilterMode = getIntent().getStringExtra(EXTRA_FILTER_MODE_TYPE);
        Log.d(TAG, "onCreate: currentFilterMode = " + currentFilterMode);
        applyFilterVisibilityAndTitle();
    }

    private void setTitleBasedOnFilterMode(String filterMode) {
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

    private void applyFilterVisibilityAndTitle() {
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
            setTitle(getString(R.string.prompts_activity_title));
            sectionOneStepContainer.setVisibility(View.VISIBLE);
            sectionTwoStepContainer.setVisibility(View.VISIBLE);
            sectionPhotoContainer.setVisibility(View.VISIBLE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        populateAllModelSpinnersFromCache(); // Refresh spinner selections
        loadAllPromptsSections(); // Load/refresh prompts for all RecyclerViews
        applyFilterVisibilityAndTitle(); // Re-apply filter on resume
    }

    private void loadAllPromptsSections() {
        if (promptManager == null) {
            Log.e(TAG, "PromptManager not initialized in loadAllPromptsSections");
            return;
        }

        List<Prompt> oneStepPrompts = promptManager.getPromptsForMode("one_step");
        if (oneStepPromptsAdapter != null) {
            oneStepPromptsAdapter.setPrompts(oneStepPrompts);
        }
        if (recyclerViewOneStepPrompts != null && tvEmptyOneStepPrompts != null) {
            if (oneStepPrompts.isEmpty()) {
                recyclerViewOneStepPrompts.setVisibility(View.GONE);
                tvEmptyOneStepPrompts.setVisibility(View.VISIBLE);
            } else {
                recyclerViewOneStepPrompts.setVisibility(View.VISIBLE);
                tvEmptyOneStepPrompts.setVisibility(View.GONE);
            }
        }

        List<Prompt> twoStepPrompts = promptManager.getPromptsForMode("two_step_processing");
        if (twoStepPromptsAdapter != null) {
            twoStepPromptsAdapter.setPrompts(twoStepPrompts);
        }
        if (recyclerViewTwoStepPrompts != null && tvEmptyTwoStepPrompts != null) {
            if (twoStepPrompts.isEmpty()) {
                recyclerViewTwoStepPrompts.setVisibility(View.GONE);
                tvEmptyTwoStepPrompts.setVisibility(View.VISIBLE);
            } else {
                recyclerViewTwoStepPrompts.setVisibility(View.VISIBLE);
                tvEmptyTwoStepPrompts.setVisibility(View.GONE);
            }
        }

        List<Prompt> photoPrompts = promptManager.getPromptsForMode("photo_vision");
        if (photoPromptsAdapter != null) {
            photoPromptsAdapter.setPrompts(photoPrompts);
        }
        if (recyclerViewPhotoPrompts != null && tvEmptyPhotoPrompts != null) {
            if (photoPrompts.isEmpty()) {
                recyclerViewPhotoPrompts.setVisibility(View.GONE);
                tvEmptyPhotoPrompts.setVisibility(View.VISIBLE);
            } else {
                recyclerViewPhotoPrompts.setVisibility(View.VISIBLE);
                tvEmptyPhotoPrompts.setVisibility(View.GONE);
            }
        }
        Log.d(TAG, "All prompt sections reloaded.");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_ADD_PROMPT || requestCode == REQUEST_EDIT_PROMPT) && resultCode == Activity.RESULT_OK) {
            loadAllPromptsSections(); // Refresh the relevant list(s)
            Log.d(TAG, "Returned from EditorActivity with RESULT_OK, reloaded prompts.");
        }
    }

    // Implementation for PromptsAdapter.OnPromptInteractionListener
    @Override
    public void onEditPrompt(Prompt prompt) {
        Intent intent;
        if ("photo_vision".equals(prompt.getPromptModeType())) {
            intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class);
            intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, prompt.getId());
        } else { // "one_step" or "two_step_processing"
            intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PromptEditorActivity.class);
            intent.putExtra(com.drgraff.speakkey.ui.prompts.PromptEditorActivity.EXTRA_PROMPT_ID, prompt.getId());
            intent.putExtra("PROMPT_MODE_TYPE", prompt.getPromptModeType());
        }
        startActivityForResult(intent, REQUEST_EDIT_PROMPT);
    }

    @Override
    public void onCopyPrompt(Prompt promptToCopy) {
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
        if (tvOriginalText != null) {
             tvOriginalText.setText(promptToCopy.getText());
        }

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

        builder.setPositiveButton(getString(R.string.copy_prompt_save_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newLabel = etNewLabel.getText().toString().trim();
                String originalText = promptToCopy.getText();
                String destinationModeType = modeInternalValues[spinnerDestinationMode.getSelectedItemPosition()];

                if (newLabel.isEmpty()) {
                    Toast.makeText(PromptsActivity.this, getString(R.string.new_label_empty_error_toast), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (promptManager != null) {
                    promptManager.addPrompt(originalText, newLabel, destinationModeType);
                    Toast.makeText(PromptsActivity.this, getString(R.string.prompt_copied_toast_format, spinnerDestinationMode.getSelectedItem().toString()), Toast.LENGTH_SHORT).show();
                    loadAllPromptsSections();
                } else {
                    Toast.makeText(PromptsActivity.this, getString(R.string.prompt_manager_unavailable_error_toast), Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void fetchModelsForSpinners() {
        if (chatGptApi == null || sharedPreferences.getString("openai_api_key", "").isEmpty()) {
            android.widget.Toast.makeText(this, getString(R.string.api_key_not_set_for_models_toast), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.fetching_models_progress));
        progressDialog.setCancelable(false);
        progressDialog.show();

        ChatGptApi.fetchAndCacheOpenAiModels(
                chatGptApi,
                sharedPreferences,
                SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, // Key to save/read all fetched model IDs
                executorService,
                mainHandler,
                models -> { // onSuccess
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    android.widget.Toast.makeText(PromptsActivity.this, getString(R.string.models_updated_toast), android.widget.Toast.LENGTH_SHORT).show();
                    populateAllModelSpinnersFromCache(); // Repopulate all spinners
                },
                exception -> { // onError
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    android.widget.Toast.makeText(PromptsActivity.this, getString(R.string.error_fetching_models_toast_format, exception.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error fetching models: ", exception);
                }
        );
    }

    private void populateAllModelSpinnersFromCache() {
        final List<String> placeholderItem = Arrays.asList(getString(R.string.no_models_loaded_placeholder));
        Set<String> fetchedModelIdsSet = sharedPreferences.getStringSet(SettingsActivity.PREF_KEY_FETCHED_MODEL_IDS, null);
        boolean modelsAvailable = (fetchedModelIdsSet != null && !fetchedModelIdsSet.isEmpty());

        modelList.clear(); // Clear the shared backing list first

        if (modelsAvailable) {
            modelList.addAll(new ArrayList<>(fetchedModelIdsSet));
            Collections.sort(modelList);
            Log.d(TAG, "Populating spinners with " + modelList.size() + " cached models.");
        } else {
            Log.w(TAG, "No cached models found. Using placeholder for spinners.");
            modelList.addAll(placeholderItem); // Add placeholder to the shared list
        }

        // Update all adapters with the content of modelList (which is either real models or the placeholder)
        if (oneStepModelAdapter != null) {
            oneStepModelAdapter.clear();
            oneStepModelAdapter.addAll(modelList);
            oneStepModelAdapter.notifyDataSetChanged();
            spinnerOneStepModel.setEnabled(modelsAvailable);
        }
        if (twoStepProcessingModelAdapter != null) {
            twoStepProcessingModelAdapter.clear();
            twoStepProcessingModelAdapter.addAll(modelList);
            twoStepProcessingModelAdapter.notifyDataSetChanged();
            spinnerTwoStepProcessingModel.setEnabled(modelsAvailable);
        }
        if (photoVisionModelAdapter != null) {
            photoVisionModelAdapter.clear();
            photoVisionModelAdapter.addAll(modelList);
            photoVisionModelAdapter.notifyDataSetChanged();
            spinnerPhotoVisionModel.setEnabled(modelsAvailable);
        }

        // After repopulating adapters, re-apply saved selections for each spinner
        // If models are not available, this will select the placeholder.
        loadAndSetSpinnerSelection(spinnerOneStepModel, oneStepModelAdapter, SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-4o");
        loadAndSetSpinnerSelection(spinnerTwoStepProcessingModel, twoStepProcessingModelAdapter, SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, "gpt-4o");
        loadAndSetSpinnerSelection(spinnerPhotoVisionModel, photoVisionModelAdapter, SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
    }

    private void loadAndSetSpinnerSelection(Spinner spinner, ArrayAdapter<String> adapter, String prefKey, String defaultModel) {
        if (spinner == null || adapter == null) {
            Log.w(TAG, "Spinner or Adapter is null for prefKey: " + prefKey);
            return;
        }
        String selectedModel = sharedPreferences.getString(prefKey, defaultModel);
        // Ensure the adapter has items before trying to get a position or item.
        if (adapter.getCount() == 0) {
            Log.w(TAG, "Adapter for " + prefKey + " is empty. Cannot set selection. Default model " + selectedModel + " might be saved if not present.");
            // If adapter is empty, it might be that models haven't been fetched yet.
            // We could save the default to prefs if no specific selection for this key exists yet.
            if(sharedPreferences.getString(prefKey, null) == null) { // only if no selection ever made for this key
                 sharedPreferences.edit().putString(prefKey, defaultModel).apply();
            }
            return;
        }

        int position = adapter.getPosition(selectedModel);
        if (position >= 0) {
            spinner.setSelection(position);
        } else { // Saved model not in the current list
            Log.w(TAG, "Saved model '" + selectedModel + "' for " + prefKey + " not found in adapter. Selecting first available.");
            if (adapter.getCount() > 0) {
                spinner.setSelection(0);
                sharedPreferences.edit().putString(prefKey, adapter.getItem(0)).apply(); // Save this new default
            } else {
                Log.w(TAG, "Adapter for " + prefKey + " is empty after attempting to select first item.");
                // If nothing is in adapter, and saved model is not there, ensure default is in prefs
                sharedPreferences.edit().putString(prefKey, defaultModel).apply();
            }
        }
        // Log.d(TAG, "Spinner for " + prefKey + " set to: " + spinner.getSelectedItem() + " (intended: " + selectedModel + ")");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Go back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
