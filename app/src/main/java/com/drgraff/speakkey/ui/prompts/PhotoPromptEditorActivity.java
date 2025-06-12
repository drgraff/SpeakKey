package com.drgraff.speakkey.ui.prompts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.data.Prompt; // Changed
import com.drgraff.speakkey.data.PromptManager; // Changed
import java.util.List; // Added

public class PhotoPromptEditorActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_PROMPT_ID = "com.drgraff.speakkey.EXTRA_PHOTO_PROMPT_ID";
    // Removed LABEL and TEXT extras as we load the whole prompt by ID for editing

    private EditText editTextPhotoPromptLabel;
    private EditText editTextPhotoPromptText;
    private Button btnSavePhotoPrompt;
    private Toolbar toolbar;

    private PromptManager promptManager; // Changed
    private long currentPromptId = -1; // Default to -1 or 0 if 0 is not a valid ID
    private boolean isEditMode = false;
    private Prompt currentPrompt; // Changed // To store the loaded prompt in edit mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_prompt_editor);

        toolbar = findViewById(R.id.toolbar_photo_prompt_editor);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        editTextPhotoPromptLabel = findViewById(R.id.edittext_photo_prompt_label);
        editTextPhotoPromptText = findViewById(R.id.edittext_photo_prompt_text);
        btnSavePhotoPrompt = findViewById(R.id.btn_save_photo_prompt);

        promptManager = new PromptManager(this); // Changed

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PHOTO_PROMPT_ID)) {
            currentPromptId = intent.getLongExtra(EXTRA_PHOTO_PROMPT_ID, -1);
            if (currentPromptId != -1) {
                isEditMode = true;
                // currentPhotoPrompt = photoPromptManager.getPhotoPromptById(currentPromptId);
                List<Prompt> photoPrompts = promptManager.getPromptsForMode("photo_vision");
                for (Prompt p : photoPrompts) {
                    if (p.getId() == currentPromptId) {
                        currentPrompt = p; // Changed
                        break;
                    }
                }
                if (currentPrompt != null) { // Changed
                    editTextPhotoPromptLabel.setText(currentPrompt.getLabel()); // Changed
                    editTextPhotoPromptText.setText(currentPrompt.getText()); // Changed
                    // activeSwitch.setChecked(currentPrompt.isActive()); // activeSwitch not in this file
                } else {
                    Toast.makeText(this, getString(R.string.photo_prompt_editor_toast_not_found), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (isEditMode) {
                actionBar.setTitle(getString(R.string.photo_prompt_editor_title_edit));
            } else {
                actionBar.setTitle(getString(R.string.photo_prompt_editor_title_add));
            }
        }

        btnSavePhotoPrompt.setOnClickListener(v -> savePhotoPrompt());
    }

    private void savePhotoPrompt() {
        String label = editTextPhotoPromptLabel.getText().toString().trim();
        String text = editTextPhotoPromptText.getText().toString().trim();

        if (TextUtils.isEmpty(label)) {
            editTextPhotoPromptLabel.setError(getString(R.string.photo_prompt_editor_error_label_empty));
            return;
        }
        if (TextUtils.isEmpty(text)) {
            editTextPhotoPromptText.setError(getString(R.string.photo_prompt_editor_error_text_empty));
            return;
        }

        if (isEditMode && currentPrompt != null) { // Changed
            currentPrompt.setLabel(label); // Changed
            currentPrompt.setText(text); // Changed
            currentPrompt.setPromptModeType("photo_vision"); // Ensure mode type
            promptManager.updatePrompt(currentPrompt); // Changed
            Toast.makeText(this, getString(R.string.photo_prompt_editor_toast_updated), Toast.LENGTH_SHORT).show();
        } else {
            promptManager.addPrompt(label, text, "photo_vision"); // Changed
            Toast.makeText(this, getString(R.string.photo_prompt_editor_toast_added), Toast.LENGTH_SHORT).show();
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Consider showing a discard confirmation dialog if there are unsaved changes
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
