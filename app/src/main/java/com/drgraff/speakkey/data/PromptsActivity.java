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
import com.drgraff.speakkey.ui.prompts.PromptEditorActivity; // Correct path for PromptEditorActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Added for FAB

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PromptsActivity extends AppCompatActivity { // Removed PromptsAdapter.OnPromptInteractionListener

    private RecyclerView promptsRecyclerView;
    private PromptsAdapter promptsAdapter;
    private PromptManager promptManager;
    // Removed promptList as adapter will hold its own list
    private TextView emptyPromptsTextView; // Added for empty state
    private FloatingActionButton fabAddPrompt; // Added for FAB

    private Button btnCheckChatGptModels; // For prompts
    private Spinner spinnerChatGptModels; // For prompts

    private ChatGptApi chatGptApi;
    private SharedPreferences sharedPreferences;
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // Or re-use if one exists
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<String> modelListPrompts = new ArrayList<>();
    private ArrayAdapter<String> modelAdapterPrompts;

    // Define new preference keys for this activity's model selection
    public static final String PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS = "selected_chatgpt_model_prompts";
    public static final String PREF_KEY_FETCHED_CHATGPT_MODEL_IDS_PROMPTS = "fetched_chatgpt_model_ids_prompts";


    // Removed promptInputText, addPromptButton, currentEditingPrompt

    private static final int ADD_PROMPT_REQUEST = 1;
    // private static final int EDIT_PROMPT_REQUEST = 2; // For future use if adapter starts activity for result

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prompts); // New layout

        Toolbar toolbar = findViewById(R.id.toolbar_prompts); // New Toolbar ID
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.prompts_activity_title);
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        String openAiModel = sharedPreferences.getString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, "gpt-3.5-turbo"); // Default model
        chatGptApi = new ChatGptApi(apiKey, openAiModel);


        promptManager = new PromptManager(this);

        promptsRecyclerView = findViewById(R.id.prompts_recycler_view);
        emptyPromptsTextView = findViewById(R.id.empty_prompts_text_view); // Initialize empty view
        fabAddPrompt = findViewById(R.id.fab_add_prompt); // Initialize FAB

        btnCheckChatGptModels = findViewById(R.id.btn_check_chatgpt_models_prompts);
        spinnerChatGptModels = findViewById(R.id.spinner_chatgpt_models_prompts);

        modelAdapterPrompts = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, modelListPrompts);
        modelAdapterPrompts.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChatGptModels.setAdapter(modelAdapterPrompts);

        if (apiKey.isEmpty()) {
            btnCheckChatGptModels.setEnabled(false);
            btnCheckChatGptModels.setText(R.string.api_key_not_set_short); // Assuming you have this string
        } else {
            btnCheckChatGptModels.setEnabled(true);
            btnCheckChatGptModels.setText("Check Models"); // Or from strings.xml
        }

        btnCheckChatGptModels.setOnClickListener(v -> fetchChatGptModelsAndUpdateSpinner());

        spinnerChatGptModels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedModel = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, selectedModel).apply();
                // Update chatGptApi if model changes, or ensure it's read fresh when used
                if (chatGptApi != null) {
                    chatGptApi.setModel(selectedModel);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        loadAndPopulateChatGptModelsSpinner();


        promptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // PromptsAdapter constructor was changed to PromptsAdapter(Context context, List<Prompt> prompts, PromptManager promptManager)
        promptsAdapter = new PromptsAdapter(this, new ArrayList<>(), promptManager);
        promptsRecyclerView.setAdapter(promptsAdapter);

        fabAddPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PromptsActivity.this, PromptEditorActivity.class);
                // For adding a new prompt, we don't pass an ID
                startActivityForResult(intent, ADD_PROMPT_REQUEST);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPrompts();
    }

    private void loadPrompts() {
        List<Prompt> loadedPrompts = promptManager.getPrompts();
        promptsAdapter.setPrompts(loadedPrompts); // Use the new setPrompts method in adapter

        if (loadedPrompts == null || loadedPrompts.isEmpty()) {
            promptsRecyclerView.setVisibility(View.GONE);
            emptyPromptsTextView.setVisibility(View.VISIBLE);
        } else {
            promptsRecyclerView.setVisibility(View.VISIBLE);
            emptyPromptsTextView.setVisibility(View.GONE);
        }
    }

    // Removed saveOrUpdatePrompt method

    // Removed onPromptActivateToggle, onEditPrompt, onDeletePrompt listener methods

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PROMPT_REQUEST && resultCode == Activity.RESULT_OK) {
            // Prompts were added/edited, onResume will call loadPrompts()
            // loadPrompts(); // Explicitly calling here is also fine, but onResume handles it
        }
        // Handle EDIT_PROMPT_REQUEST if implemented
    }

    private void fetchChatGptModelsAndUpdateSpinner() {
        if (chatGptApi == null || sharedPreferences.getString("openai_api_key", "").isEmpty()) {
            android.widget.Toast.makeText(this, "API client not initialized or API Key missing.", android.widget.Toast.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Fetching available ChatGPT models...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        executorService.execute(() -> {
            try {
                final List<OpenAIModelData.ModelInfo> models = chatGptApi.listModels(); // This is a synchronous call
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (models == null || models.isEmpty()) {
                        android.widget.Toast.makeText(PromptsActivity.this, "No models returned or error fetching.", android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }

                    modelListPrompts.clear();
                    List<String> modelIds = new ArrayList<>();
                    for (OpenAIModelData.ModelInfo model : models) {
                        String id = model.getId();
                        // Basic filtering for text-based models, can be refined
                        if (id != null && !id.trim().isEmpty() &&
                            (id.contains("gpt") || id.contains("text-davinci") || id.contains("claude") || id.contains("gemini")) && // Common text model identifiers
                            !id.contains("vision") && !id.contains("image") && !id.contains("audio") &&
                            !id.contains("tts") && !id.contains("whisper") && !id.contains("dall-e") &&
                            !id.contains("embedding") && !id.contains("moderation")) {
                            modelListPrompts.add(id);
                            modelIds.add(id);
                        }
                    }
                    Collections.sort(modelListPrompts);

                    if (modelListPrompts.isEmpty()) {
                        android.widget.Toast.makeText(PromptsActivity.this, "No suitable text-based ChatGPT models found.", android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        modelAdapterPrompts.notifyDataSetChanged();
                        sharedPreferences.edit().putStringSet(PREF_KEY_FETCHED_CHATGPT_MODEL_IDS_PROMPTS, new HashSet<>(modelIds)).apply();

                        String previouslySelectedModel = sharedPreferences.getString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, null);
                        if (previouslySelectedModel != null && modelListPrompts.contains(previouslySelectedModel)) {
                            spinnerChatGptModels.setSelection(modelListPrompts.indexOf(previouslySelectedModel));
                        } else if (!modelListPrompts.isEmpty()) {
                            spinnerChatGptModels.setSelection(0); // Select the first one
                            sharedPreferences.edit().putString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, modelListPrompts.get(0)).apply();
                             if (chatGptApi != null) chatGptApi.setModel(modelListPrompts.get(0));
                        }
                        android.widget.Toast.makeText(PromptsActivity.this, "ChatGPT models updated.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e("PromptsActivity", "Failed to fetch models", e);
                mainHandler.post(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    android.widget.Toast.makeText(PromptsActivity.this, "Error fetching models: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void loadAndPopulateChatGptModelsSpinner() {
        Set<String> fetchedModelIds = sharedPreferences.getStringSet(PREF_KEY_FETCHED_CHATGPT_MODEL_IDS_PROMPTS, null);
        modelListPrompts.clear();

        if (fetchedModelIds != null && !fetchedModelIds.isEmpty()) {
            modelListPrompts.addAll(fetchedModelIds);
            Collections.sort(modelListPrompts); // Ensure sorted order
        } else {
            // Fallback to a minimal default list if nothing is fetched or stored
            // This could be expanded or loaded from an XML array resource
            modelListPrompts.add("gpt-4-turbo");
            modelListPrompts.add("gpt-3.5-turbo");
        }
        modelAdapterPrompts.notifyDataSetChanged();

        String selectedModel = sharedPreferences.getString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, modelListPrompts.get(0));
        int selectionIndex = modelListPrompts.indexOf(selectedModel);
        if (selectionIndex >= 0) {
            spinnerChatGptModels.setSelection(selectionIndex);
        } else if (!modelListPrompts.isEmpty()) {
            spinnerChatGptModels.setSelection(0); // Default to the first item if saved one isn't in the list
            sharedPreferences.edit().putString(PREF_KEY_SELECTED_CHATGPT_MODEL_PROMPTS, modelListPrompts.get(0)).apply();
            if (chatGptApi != null) chatGptApi.setModel(modelListPrompts.get(0));
        }
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
