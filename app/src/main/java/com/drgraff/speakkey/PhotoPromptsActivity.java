package com.drgraff.speakkey;

import android.app.Activity; // Added
import android.app.ProgressDialog;
import android.content.Intent; // Added
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
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
import com.drgraff.speakkey.data.PhotoPrompt;
import com.drgraff.speakkey.data.PhotoPromptManager;
import com.drgraff.speakkey.data.PhotoPromptsAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections; // Added
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PhotoPromptsActivity extends AppCompatActivity implements PhotoPromptsAdapter.OnPhotoPromptInteractionListener {

    public static final String PREF_KEY_SELECTED_PHOTO_MODEL = "selected_photo_model";
    public static final String PREF_KEY_FETCHED_PHOTO_MODEL_IDS = "fetched_photo_model_ids";
    private static final String TAG = "PhotoPromptsActivity";

    private RecyclerView photoPromptsRecyclerView;
    private PhotoPromptsAdapter photoPromptsAdapter;
    private PhotoPromptManager photoPromptManager;
    private TextView emptyPhotoPromptsTextView;
    private FloatingActionButton fabAddPhotoPrompt;
    private Button btnCheckPhotoModels;
    private Spinner spinnerPhotoModels;
    private Toolbar toolbar;

    private ChatGptApi chatGptApi;
    private SharedPreferences sharedPreferences;
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<String> modelList = new ArrayList<>();
    private ArrayAdapter<String> modelAdapter;

    private static final int REQUEST_ADD_PHOTO_PROMPT = 1;
    private static final int REQUEST_EDIT_PHOTO_PROMPT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_prompts);

        toolbar = findViewById(R.id.toolbar_photo_prompts);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.photo_prompts_title));
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.photo_prompts_toast_api_key_not_set), Toast.LENGTH_LONG).show();
            // Consider disabling model fetching functionality
        }
        // Initialize ChatGptApi - assuming default model is not critical here or handled by ChatGptApi constructor
        chatGptApi = new ChatGptApi(apiKey, "default_model_not_used_for_listing");


        photoPromptManager = new PhotoPromptManager(this);

        photoPromptsRecyclerView = findViewById(R.id.photo_prompts_recycler_view);
        photoPromptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        photoPromptsAdapter = new PhotoPromptsAdapter(this, new ArrayList<>(), photoPromptManager, this);
        photoPromptsRecyclerView.setAdapter(photoPromptsAdapter);

        emptyPhotoPromptsTextView = findViewById(R.id.empty_photo_prompts_text_view);
        fabAddPhotoPrompt = findViewById(R.id.fab_add_photo_prompt);
        btnCheckPhotoModels = findViewById(R.id.btn_check_photo_models);
        spinnerPhotoModels = findViewById(R.id.spinner_photo_models);

        modelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelList);
        modelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPhotoModels.setAdapter(modelAdapter);

        if (apiKey.isEmpty()) {
            // Toast is already shown a few lines above.
            btnCheckPhotoModels.setEnabled(false);
        } else {
            btnCheckPhotoModels.setEnabled(true); // Ensure it's enabled if key IS present
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
                sharedPreferences.edit().putString(PREF_KEY_SELECTED_PHOTO_MODEL, selectedModel).apply();
                Log.d(TAG, "Selected photo model saved: " + selectedModel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadAndPopulatePhotoModelsSpinner();
        loadPhotoPrompts();
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
                // Basic filtering for "vision" or "dall-e" models as a starting point
                // This might need to be more sophisticated based on actual API responses or capabilities.
                List<String> visionModelIds = allModels.stream()
                        .filter(model -> model.id.contains("vision") || model.id.contains("dall-e") || model.id.contains("gpt-4")) // Example filter
                        .map(model -> model.id)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (visionModelIds.isEmpty()) {
                        Toast.makeText(PhotoPromptsActivity.this, getString(R.string.photo_prompts_toast_no_specific_models), Toast.LENGTH_LONG).show();
                        // Fallback to showing all models if no specific vision ones are identified by simple filter
                        // Or handle as an error/empty list. For now, let's populate with what we have or all.
                        // If visionModelIds is empty, this will clear the spinner if it had old data.
                    }

                    modelList.clear();
                    modelList.addAll(visionModelIds.isEmpty() ? allModels.stream().map(m -> m.id).sorted().collect(Collectors.toList()) : visionModelIds);

                    modelAdapter.notifyDataSetChanged();

                    // Save fetched models to SharedPreferences
                    Set<String> modelSet = new HashSet<>(modelList);
                    sharedPreferences.edit().putStringSet(PREF_KEY_FETCHED_PHOTO_MODEL_IDS, modelSet).apply();
                    Log.d(TAG, "Fetched and saved photo models: " + modelSet.size());

                    // Restore selection
                    String previouslySelected = sharedPreferences.getString(PREF_KEY_SELECTED_PHOTO_MODEL, null);
                    if (previouslySelected != null) {
                        int spinnerPosition = modelAdapter.getPosition(previouslySelected);
                        if (spinnerPosition >= 0) {
                            spinnerPhotoModels.setSelection(spinnerPosition);
                        } else if (!modelList.isEmpty()){
                            spinnerPhotoModels.setSelection(0); // Select first if previous not found
                             sharedPreferences.edit().putString(PREF_KEY_SELECTED_PHOTO_MODEL, modelList.get(0)).apply();
                        }
                    } else if (!modelList.isEmpty()) {
                        spinnerPhotoModels.setSelection(0); // Select first if nothing was previously selected
                        sharedPreferences.edit().putString(PREF_KEY_SELECTED_PHOTO_MODEL, modelList.get(0)).apply();
                    }
                    Toast.makeText(PhotoPromptsActivity.this, getString(R.string.photo_prompts_toast_models_updated), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching models: ", e);
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(PhotoPromptsActivity.this, String.format(getString(R.string.photo_prompts_toast_error_fetching_models_format), e.getMessage()), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadAndPopulatePhotoModelsSpinner() {
        Set<String> fetchedModelIds = sharedPreferences.getStringSet(PREF_KEY_FETCHED_PHOTO_MODEL_IDS, null);
        modelList.clear();
        if (fetchedModelIds != null && !fetchedModelIds.isEmpty()) {
            modelList.addAll(new ArrayList<>(fetchedModelIds));
            Collections.sort(modelList); // Ensure consistent order
            Log.d(TAG, "Loaded photo models from Prefs: " + modelList.size());
        } else {
            Log.d(TAG, "No photo models found in Prefs. Use 'Check Models' button.");
            // Optionally add a default "dummy" or "select model" item if list is empty
            // modelList.add("No models loaded - Check Models");
        }
        modelAdapter.notifyDataSetChanged();

        String selectedModelId = sharedPreferences.getString(PREF_KEY_SELECTED_PHOTO_MODEL, "gpt-4-vision-preview");
        if (selectedModelId != null && !modelList.isEmpty()) { // selectedModelId will not be null due to default
            int spinnerPosition = modelAdapter.getPosition(selectedModelId);
            if (spinnerPosition >= 0) {
                spinnerPhotoModels.setSelection(spinnerPosition);
            } else if (!modelList.isEmpty()) { // If saved selection not in current list, select first
                 spinnerPhotoModels.setSelection(0);
                 sharedPreferences.edit().putString(PREF_KEY_SELECTED_PHOTO_MODEL, modelList.get(0)).apply();
            }
        } else if (!modelList.isEmpty()) { // If no selection saved, select first
            spinnerPhotoModels.setSelection(0);
            sharedPreferences.edit().putString(PREF_KEY_SELECTED_PHOTO_MODEL, modelList.get(0)).apply();
        }
    }

    private void loadPhotoPrompts() {
        List<PhotoPrompt> prompts = photoPromptManager.getPhotoPrompts();
        photoPromptsAdapter.setPhotoPrompts(prompts);

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
        loadPhotoPrompts();
        // Consider re-checking API key or refreshing models if settings could change elsewhere
        // For now, loadAndPopulatePhotoModelsSpinner() in onCreate should suffice for initial setup.
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Or onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEditPhotoPrompt(PhotoPrompt photoPrompt) {
        Intent intent = new Intent(this, com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.class);
        intent.putExtra(com.drgraff.speakkey.ui.prompts.PhotoPromptEditorActivity.EXTRA_PHOTO_PROMPT_ID, photoPrompt.getId());
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
}
