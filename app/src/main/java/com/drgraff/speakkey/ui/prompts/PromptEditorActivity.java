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
                actionBar.setTitle("Edit Prompt");
            }
            // Load the prompt
            List<Prompt> prompts = promptManager.getPrompts();
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
                Toast.makeText(this, "Error: Prompt not found.", Toast.LENGTH_SHORT).show();
                if (actionBar != null) {
                    actionBar.setTitle("Add New Prompt");
                }
                currentPromptId = INVALID_PROMPT_ID; // Reset to behave like add new
            }
        } else {
            if (actionBar != null) {
                actionBar.setTitle("Add New Prompt");
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
            editTextLabel.setError("Label cannot be empty");
            Toast.makeText(this, "Label cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPromptId != INVALID_PROMPT_ID && currentPrompt != null) {
            // Editing existing prompt
            currentPrompt.setLabel(label);
            currentPrompt.setText(text);
            // isActive state is preserved from the original currentPrompt object
            promptManager.updatePrompt(currentPrompt);
            Toast.makeText(this, "Prompt updated", Toast.LENGTH_SHORT).show();
        } else {
            // Adding new prompt
            promptManager.addPrompt(text, label); // isActive is false by default in addPrompt
            Toast.makeText(this, "Prompt saved", Toast.LENGTH_SHORT).show();
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
