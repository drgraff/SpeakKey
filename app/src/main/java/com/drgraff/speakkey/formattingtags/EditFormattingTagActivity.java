package com.drgraff.speakkey.formattingtags;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;
import com.google.android.material.textfield.TextInputEditText;


public class EditFormattingTagActivity extends AppCompatActivity {

    public static final String EXTRA_TAG_ID = "com.drgraff.speakkey.EXTRA_TAG_ID";
    private static final long INVALID_TAG_ID = -1;

    private TextInputEditText editTextName, editTextOpeningTag, editTextClosingTag, editTextKeystrokeSequence;
    private Button buttonSave;
    private FormattingTagManager tagManager;
    private FormattingTag currentTag;
    private long currentTagId = INVALID_TAG_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_formatting_tag);

        Toolbar toolbar = findViewById(R.id.toolbar_edit_formatting_tag);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // Title will be set below based on add/edit mode
        }

        editTextName = findViewById(R.id.edit_tag_name);
        editTextOpeningTag = findViewById(R.id.edit_tag_opening_text);
        editTextClosingTag = findViewById(R.id.edit_tag_closing_text);
        editTextKeystrokeSequence = findViewById(R.id.edit_tag_keystroke_sequence);
        buttonSave = findViewById(R.id.button_save_formatting_tag);

        tagManager = new FormattingTagManager(this);
        tagManager.open();

        currentTagId = getIntent().getLongExtra(EXTRA_TAG_ID, INVALID_TAG_ID);

        if (currentTagId != INVALID_TAG_ID) {
            currentTag = tagManager.getTag(currentTagId);
            if (currentTag != null) {
                editTextName.setText(currentTag.getName());
                editTextOpeningTag.setText(currentTag.getOpeningTagText());
                editTextClosingTag.setText(currentTag.getClosingTagText());
                editTextKeystrokeSequence.setText(currentTag.getKeystrokeSequence());
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.edit_formatting_tag_title));
                }
            } else {
                Toast.makeText(this, "Error: Formatting tag not found.", Toast.LENGTH_LONG).show();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getString(R.string.add_new_formatting_tag_title));
                }
                // Treat as new if tag not found, currentTagId remains invalid for save logic
                currentTagId = INVALID_TAG_ID;
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.add_new_formatting_tag_title));
            }
        }

        buttonSave.setOnClickListener(v -> saveFormattingTag());
    }

    private void saveFormattingTag() {
        String name = editTextName.getText().toString().trim();
        String openingText = editTextOpeningTag.getText().toString().trim();
        String closingText = editTextClosingTag.getText().toString().trim(); // Can be empty
        String keystrokes = editTextKeystrokeSequence.getText().toString().trim();

        if (name.isEmpty()) {
            editTextName.setError(getString(R.string.tag_name_required_message));
            editTextName.requestFocus();
            Toast.makeText(this, getString(R.string.tag_name_required_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (openingText.isEmpty()) {
            editTextOpeningTag.setError(getString(R.string.opening_tag_text_required_message));
            editTextOpeningTag.requestFocus();
            Toast.makeText(this, getString(R.string.opening_tag_text_required_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (keystrokes.isEmpty()) {
            editTextKeystrokeSequence.setError(getString(R.string.keystroke_sequence_required_message));
            editTextKeystrokeSequence.requestFocus();
            Toast.makeText(this, getString(R.string.keystroke_sequence_required_message), Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentTagId != INVALID_TAG_ID && currentTag != null) {
            // Editing existing tag
            currentTag.setName(name);
            currentTag.setOpeningTagText(openingText);
            currentTag.setClosingTagText(closingText);
            currentTag.setKeystrokeSequence(keystrokes);
            // currentTag.setActive(true); // Assuming active, or add a switch
            tagManager.updateTag(currentTag);
        } else {
            // Adding new tag
            // ID 0 is fine as SQLite will auto-increment. isActive defaults to true in DB.
            FormattingTag newTag = new FormattingTag(0, name, openingText, closingText, keystrokes, true);
            tagManager.addTag(newTag);
        }

        Toast.makeText(this, getString(R.string.formatting_tag_saved_message), Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tagManager != null) {
            tagManager.close();
        }
    }
}
