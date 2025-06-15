package com.drgraff.speakkey.ui.prompts;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;

import java.util.List;

public class PromptEditorActivity extends AppCompatActivity {

    public static final String EXTRA_PROMPT_ID = "com.drgraff.speakkey.EXTRA_PROMPT_ID";
    private static final long INVALID_PROMPT_ID = -1;

    private EditText editTextLabel;
    private EditText editTextText;
    private Button buttonSave;

    private PromptManager promptManager;
    private Prompt currentPrompt;
    private long currentPromptId = INVALID_PROMPT_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_prompt);

        Toolbar toolbar = findViewById(R.id.toolbar_edit_prompt);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        editTextLabel = findViewById(R.id.prompt_edit_label);
        editTextText = findViewById(R.id.prompt_edit_text);
        buttonSave = findViewById(R.id.button_save_prompt);

        promptManager = new PromptManager(this);

        currentPromptId = getIntent().getLongExtra(EXTRA_PROMPT_ID, INVALID_PROMPT_ID);

        if (currentPromptId != INVALID_PROMPT_ID) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.edit_prompt_title);
            }
            // Load the prompt
            List<Prompt> prompts = promptManager.getAllPrompts(); // Changed to getAllPrompts
            for (Prompt p : prompts) {
                if (p.getId() == currentPromptId) {
                    currentPrompt = p;
                    break;
                }
            }

            if (currentPrompt != null) {
                editTextLabel.setText(currentPrompt.getLabel());
                editTextText.setText(currentPrompt.getText());
            } else {
                // Prompt with given ID not found, treat as error or new prompt
                Toast.makeText(this, R.string.prompt_not_found_message, Toast.LENGTH_SHORT).show();
                if (actionBar != null) {
                    actionBar.setTitle(R.string.add_prompt_title);
                }
                currentPromptId = INVALID_PROMPT_ID; // Reset to behave like add new
            }
        } else {
            if (actionBar != null) {
                actionBar.setTitle(R.string.add_prompt_title);
            }
        }

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePrompt();
            }
        });
    }

    private void savePrompt() {
        String label = editTextLabel.getText().toString().trim();
        String text = editTextText.getText().toString(); // Text can be empty, or contain just spaces

        if (label.isEmpty()) {
            editTextLabel.setError(getString(R.string.prompt_label_required_message)); // Also set error text from string
            Toast.makeText(this, R.string.prompt_label_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPromptId != INVALID_PROMPT_ID && currentPrompt != null) {
            // Editing existing prompt
            currentPrompt.setLabel(label);
            currentPrompt.setText(text);
            // isActive state is preserved from the original currentPrompt object
            if (currentPrompt.getPromptModeType() == null || currentPrompt.getPromptModeType().isEmpty()) {
                currentPrompt.setPromptModeType("two_step_processing"); // Default for safety if missing
                android.util.Log.w("PromptEditorActivity", "Prompt " + currentPrompt.getId() + " had null/empty modeType, defaulted to two_step_processing");
            }
            promptManager.updatePrompt(currentPrompt);
            Toast.makeText(this, R.string.prompt_saved_message, Toast.LENGTH_SHORT).show(); // Use "Prompt saved" for update too
        } else {
            // Adding new prompt
            String modeTypeForNewPrompt = getIntent().getStringExtra("PROMPT_MODE_TYPE");
            if (modeTypeForNewPrompt == null || modeTypeForNewPrompt.isEmpty()) {
                modeTypeForNewPrompt = "two_step_processing"; // Fallback default
                android.util.Log.w("PromptEditorActivity", "PROMPT_MODE_TYPE extra not found, defaulting to " + modeTypeForNewPrompt);
            }
        // Corrected order for addPrompt(label, text, mode)
        promptManager.addPrompt(label, text, modeTypeForNewPrompt);
            Toast.makeText(this, R.string.prompt_saved_message, Toast.LENGTH_SHORT).show();
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
