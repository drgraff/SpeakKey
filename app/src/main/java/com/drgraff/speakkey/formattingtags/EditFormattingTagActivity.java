package com.drgraff.speakkey.formattingtags;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView; // Added for Spinner error
import android.widget.Toast;

import androidx.annotation.NonNull;
import java.util.ArrayList; // Added
import java.util.Arrays; // Added
import java.util.List; // Added
import java.util.Map; // Added
import java.util.LinkedHashMap; // Added To maintain insertion order for Spinner
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.drgraff.speakkey.R;
import com.google.android.material.textfield.TextInputEditText;


public class EditFormattingTagActivity extends AppCompatActivity {

    public static final String EXTRA_TAG_ID = "com.drgraff.speakkey.EXTRA_TAG_ID";
    private static final long INVALID_TAG_ID = -1;

    private TextInputEditText editTextName, editTextOpeningTag; // editTextKeystrokeSequence removed
    private CheckBox checkBoxCtrl, checkBoxAlt, checkBoxShift, checkBoxMeta; // Added
    private Spinner spinnerMainKey; // Added
    private List<KeystrokeDisplay> keySpinnerItems; // Added For Spinner data
    private Button buttonSave;
    private FormattingTagManager tagManager;
    private FormattingTag currentTag;
    private long currentTagId = INVALID_TAG_ID;

    // Simple class to hold display name and actual key value for the Spinner
    static class KeystrokeDisplay {
        public String displayName;
        public String keyValue; // e.g., "KEY_A", "KEY_ENTER"

        public KeystrokeDisplay(String displayName, String keyValue) {
            this.displayName = displayName;
            this.keyValue = keyValue;
        }

        @NonNull
        @Override
        public String toString() {
            return displayName; // This is what will be shown in the Spinner
        }
    }

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
        // editTextKeystrokeSequence = findViewById(R.id.edit_tag_keystroke_sequence); // Removed

        checkBoxCtrl = findViewById(R.id.checkbox_modifier_ctrl); // Added
        checkBoxAlt = findViewById(R.id.checkbox_modifier_alt); // Added
        checkBoxShift = findViewById(R.id.checkbox_modifier_shift); // Added
        checkBoxMeta = findViewById(R.id.checkbox_modifier_meta); // Added
        spinnerMainKey = findViewById(R.id.spinner_main_key); // Added

        buttonSave = findViewById(R.id.button_save_formatting_tag);

        populateMainKeySpinner(); // Added call

        tagManager = new FormattingTagManager(this);
        tagManager.open();

        currentTagId = getIntent().getLongExtra(EXTRA_TAG_ID, INVALID_TAG_ID);

        if (currentTagId != INVALID_TAG_ID) {
            currentTag = tagManager.getTag(currentTagId);
            if (currentTag != null) {
                editTextName.setText(currentTag.getName());
                editTextOpeningTag.setText(currentTag.getOpeningTagText());
                // editTextKeystrokeSequence.setText(currentTag.getKeystrokeSequence()); // Removed
                parseAndSetKeystrokeUI(currentTag.getKeystrokeSequence()); // Added call
                // Ensure Activity title is also set for editing here
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.edit_formatting_tag_title);
                }
            } else {
                // Handle case where tag with ID is not found (current logic seems to Toast and set title to Add)
                // This part of the existing logic can remain.
                Toast.makeText(this, "Formatting Tag not found, creating a new one.", Toast.LENGTH_SHORT).show();
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(R.string.add_new_formatting_tag_title);
                }
                currentTagId = INVALID_TAG_ID; // Reset to ensure it behaves as 'add new'
            }
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.add_new_formatting_tag_title);
            }
        }

        buttonSave.setOnClickListener(v -> saveFormattingTag());
    }

    private void saveFormattingTag() {
        String name = editTextName.getText().toString().trim();
        String openingText = editTextOpeningTag.getText().toString().trim();
        // String keystrokes = editTextKeystrokeSequence.getText().toString().trim(); // Removed

        // Construct Keystroke String from New UI
        StringBuilder keystrokesBuilder = new StringBuilder();
        if (checkBoxCtrl.isChecked()) {
            keystrokesBuilder.append("CTRL_LEFT+");
        }
        if (checkBoxAlt.isChecked()) {
            keystrokesBuilder.append("ALT_LEFT+");
        }
        if (checkBoxShift.isChecked()) {
            keystrokesBuilder.append("SHIFT_LEFT+");
        }
        if (checkBoxMeta.isChecked()) {
            keystrokesBuilder.append("GUI_LEFT+");
        }

        KeystrokeDisplay selectedKeyItem = (KeystrokeDisplay) spinnerMainKey.getSelectedItem();
        String mainKeyValue = "";
        if (selectedKeyItem != null && selectedKeyItem.keyValue != null && !selectedKeyItem.keyValue.isEmpty()) {
            mainKeyValue = selectedKeyItem.keyValue;
            keystrokesBuilder.append(mainKeyValue);
        } else if (keystrokesBuilder.length() > 0) {
            // Modifiers selected but no main key: remove trailing '+'
             keystrokesBuilder.setLength(keystrokesBuilder.length() - 1);
        }

        String keystrokes = keystrokesBuilder.toString();
        // New Validation for keystrokes (main key must be selected)
        if (mainKeyValue.isEmpty()) {
            Toast.makeText(this, "Main key must be selected.", Toast.LENGTH_SHORT).show();
            // Optionally, set an error on the spinner (not straightforward) or a general Toast.
            // ((TextView)spinnerMainKey.getSelectedView()).setError("Main key required"); // This might not work well or look good
            if (spinnerMainKey.getSelectedView() instanceof TextView) {
                 ((TextView)spinnerMainKey.getSelectedView()).setError("Main key required");
            }
            return;
        }
        // End of new keystroke construction and validation

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

        // Old keystrokes.isEmpty() validation removed

        if (currentTagId != INVALID_TAG_ID && currentTag != null) {
            // Editing existing tag
            currentTag.setName(name);
            currentTag.setOpeningTagText(openingText);
            // currentTag.setClosingTagText(closingText); // Removed
            currentTag.setKeystrokeSequence(keystrokes);
            // currentTag.setActive(true); // Assuming active, or add a switch
            tagManager.updateTag(currentTag);
        } else {
            // Adding new tag
            // ID 0 is fine as SQLite will auto-increment. isActive defaults to true in DB.
            FormattingTag newTag = new FormattingTag(0, name, openingText, keystrokes, true); // Removed closingText
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

    private void populateMainKeySpinner() {
        keySpinnerItems = new ArrayList<>();
        // Add a "None" or "Select Key..." option as the first item
        keySpinnerItems.add(new KeystrokeDisplay(getString(R.string.prompt_select_key), "")); // Empty value for no key

        // --- Populate with actual keys (This list should be comprehensive) ---
        // Example:
        keySpinnerItems.add(new KeystrokeDisplay("Enter", "KEY_ENTER"));
        keySpinnerItems.add(new KeystrokeDisplay("Tab", "KEY_TAB"));
        keySpinnerItems.add(new KeystrokeDisplay("Space", "KEY_SPACEBAR"));
        keySpinnerItems.add(new KeystrokeDisplay("Backspace", "KEY_BACKSPACE"));
        keySpinnerItems.add(new KeystrokeDisplay("Esc", "KEY_ESCAPE"));
        keySpinnerItems.add(new KeystrokeDisplay("Delete", "KEY_DELETE"));
        keySpinnerItems.add(new KeystrokeDisplay("Insert", "KEY_INSERT"));
        keySpinnerItems.add(new KeystrokeDisplay("Home", "KEY_HOME"));
        keySpinnerItems.add(new KeystrokeDisplay("End", "KEY_END"));
        keySpinnerItems.add(new KeystrokeDisplay("Page Up", "KEY_PAGE_UP"));
        keySpinnerItems.add(new KeystrokeDisplay("Page Down", "KEY_PAGE_DOWN"));
        keySpinnerItems.add(new KeystrokeDisplay("Up Arrow", "KEY_ARROW_UP"));
        keySpinnerItems.add(new KeystrokeDisplay("Down Arrow", "KEY_ARROW_DOWN"));
        keySpinnerItems.add(new KeystrokeDisplay("Left Arrow", "KEY_ARROW_LEFT"));
        keySpinnerItems.add(new KeystrokeDisplay("Right Arrow", "KEY_ARROW_RIGHT"));

        for (char c = 'A'; c <= 'Z'; c++) {
            keySpinnerItems.add(new KeystrokeDisplay(String.valueOf(c), "KEY_" + c));
        }
        for (char c = '0'; c <= '9'; c++) {
            keySpinnerItems.add(new KeystrokeDisplay(String.valueOf(c), "KEY_" + c));
        }
        for (int i = 1; i <= 12; i++) {
            keySpinnerItems.add(new KeystrokeDisplay("F" + i, "KEY_F" + i));
        }
        // Add other keys as defined in TextTagFormatter.getHidKeyCode's switch cases
        keySpinnerItems.add(new KeystrokeDisplay("- (Minus)", "KEY_MINUS"));
        keySpinnerItems.add(new KeystrokeDisplay("= (Equals)", "KEY_EQUALS"));
        keySpinnerItems.add(new KeystrokeDisplay("[ (Left Bracket)", "KEY_LEFT_BRACKET"));
        keySpinnerItems.add(new KeystrokeDisplay("] (Right Bracket)", "KEY_RIGHT_BRACKET"));
        keySpinnerItems.add(new KeystrokeDisplay("\\ (Backslash)", "KEY_BACKSLASH"));
        keySpinnerItems.add(new KeystrokeDisplay("; (Semicolon)", "KEY_SEMICOLON"));
        keySpinnerItems.add(new KeystrokeDisplay("' (Apostrophe)", "KEY_APOSTROPHE"));
        keySpinnerItems.add(new KeystrokeDisplay("` (Grave Accent)", "KEY_GRAVE"));
        keySpinnerItems.add(new KeystrokeDisplay(", (Comma)", "KEY_COMA"));
        keySpinnerItems.add(new KeystrokeDisplay(". (Period)", "KEY_DOT"));
        keySpinnerItems.add(new KeystrokeDisplay("/ (Slash)", "KEY_SLASH"));
        keySpinnerItems.add(new KeystrokeDisplay("Caps Lock", "KEY_CAPS_LOCK"));
        keySpinnerItems.add(new KeystrokeDisplay("Print Screen", "KEY_PRINT_SCREEN"));
        keySpinnerItems.add(new KeystrokeDisplay("Scroll Lock", "KEY_SCROLL_LOCK"));
        keySpinnerItems.add(new KeystrokeDisplay("Pause", "KEY_PAUSE")); // Or KEY_PASUE if that's the actual constant
        keySpinnerItems.add(new KeystrokeDisplay("Application", "KEY_APPLICATION"));
        // Numpad keys
        keySpinnerItems.add(new KeystrokeDisplay("Num Lock", "KEY_NUM_LOCK"));
        keySpinnerItems.add(new KeystrokeDisplay("Num /", "KEY_NUM_SLASH"));
        keySpinnerItems.add(new KeystrokeDisplay("Num *", "KEY_NUM_STAR"));
        keySpinnerItems.add(new KeystrokeDisplay("Num -", "KEY_NUM_MINUS"));
        keySpinnerItems.add(new KeystrokeDisplay("Num +", "KEY_NUM_PLUS"));
        keySpinnerItems.add(new KeystrokeDisplay("Num Enter", "KEY_NUM_ENTER"));
        for (int i = 0; i <= 9; i++) {
             keySpinnerItems.add(new KeystrokeDisplay("Num " + i, "KEY_NUM_" + i));
        }
        keySpinnerItems.add(new KeystrokeDisplay("Num . (Dot)", "KEY_NUM_DOT"));


        ArrayAdapter<KeystrokeDisplay> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, keySpinnerItems);
        spinnerMainKey.setAdapter(adapter);
    }

    private void parseAndSetKeystrokeUI(String keystrokeSequence) {
        if (keystrokeSequence == null || keystrokeSequence.isEmpty()) {
            return;
        }
        // Reset UI elements first
        checkBoxCtrl.setChecked(false);
        checkBoxAlt.setChecked(false);
        checkBoxShift.setChecked(false);
        checkBoxMeta.setChecked(false);
        spinnerMainKey.setSelection(0); // Default to "Select Key..."

        String[] parts = keystrokeSequence.split("\\+"); // Use "\\+" to split by literal '+'
        String mainKeyPart = null;

        for (String part : parts) {
            part = part.trim().toUpperCase(); // Match TextTagFormatter.getHidKeyCode logic
            if (part.equals("CTRL_LEFT") || part.equals("CTRL_RIGHT") || part.equals("CTRL")) {
                checkBoxCtrl.setChecked(true);
            } else if (part.equals("ALT_LEFT") || part.equals("ALT_RIGHT") || part.equals("ALT")) {
                checkBoxAlt.setChecked(true);
            } else if (part.equals("SHIFT_LEFT") || part.equals("SHIFT_RIGHT") || part.equals("SHIFT")) {
                checkBoxShift.setChecked(true);
            } else if (part.equals("GUI_LEFT") || part.equals("GUI_RIGHT") || part.equals("META")) {
                checkBoxMeta.setChecked(true);
            } else {
                mainKeyPart = part; 
            }
        }

        if (mainKeyPart != null) {
            for (int i = 0; i < keySpinnerItems.size(); i++) {
                if (keySpinnerItems.get(i).keyValue.equalsIgnoreCase(mainKeyPart)) {
                    spinnerMainKey.setSelection(i);
                    break;
                }
            }
        }
    }
}
