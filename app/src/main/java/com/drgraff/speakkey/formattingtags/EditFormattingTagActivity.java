package com.drgraff.speakkey.formattingtags;

import android.app.Activity;
import android.content.Intent; // Keep this for getIntent()
import android.content.SharedPreferences; // Standard import
import android.os.Bundle;
import android.text.TextUtils; // Keep for TextUtils.isEmpty
import android.util.Log; // Standard import
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager; // Standard import

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.data.Prompt; // Not used here, but keep if PromptManager might return it
import com.drgraff.speakkey.formattingtags.FormattingTag; // Corrected
import com.drgraff.speakkey.formattingtags.FormattingTagManager; // Corrected
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import com.drgraff.speakkey.utils.ThemeManager;
import com.google.android.material.textfield.TextInputEditText;
import android.content.res.ColorStateList; // Added for ColorStateList
import androidx.core.widget.CompoundButtonCompat; // Added for CheckBox tinting


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
// import java.util.Map; // Not directly used in this class
// import java.util.LinkedHashMap; // Not directly used in this class

public class EditFormattingTagActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_TAG_ID = "com.drgraff.speakkey.EXTRA_TAG_ID";
    private static final long INVALID_TAG_ID = -1;
    private static final String DEFAULT_DELAY_MS = "0";
    private static final String TAG = "EditFormatTagAct";

    private SharedPreferences sharedPreferences; // Class field
    private TextInputEditText editTextName, editTextOpeningTag, editTextTagDelayMs;
    private CheckBox checkBoxCtrl, checkBoxAlt, checkBoxShift, checkBoxMeta;
    private Spinner spinnerMainKey;
    private List<KeystrokeDisplay> keySpinnerItems;
    private Button buttonSave;
    private FormattingTagManager tagManager;
    private FormattingTag currentTag;
    private long currentTagId = INVALID_TAG_ID;

    // Member variables for theme state tracking
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;

    static class KeystrokeDisplay {
        public String displayName;
        public String keyValue;
        public KeystrokeDisplay(String displayName, String keyValue) {
            this.displayName = displayName;
            this.keyValue = keyValue;
        }
        @NonNull @Override public String toString() { return displayName; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ThemeManager.applyTheme(this.sharedPreferences);
        String themeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_formatting_tag);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (getSupportActionBar() != null) { // Redundant check, actionBar will be null if getSupportActionBar is null
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize UI elements FIRST
        editTextName = findViewById(R.id.edit_tag_name);
        editTextOpeningTag = findViewById(R.id.edit_tag_opening_text);
        editTextTagDelayMs = findViewById(R.id.editTextTagDelayMs);
        checkBoxCtrl = findViewById(R.id.checkbox_modifier_ctrl);
        checkBoxAlt = findViewById(R.id.checkbox_modifier_alt);
        checkBoxShift = findViewById(R.id.checkbox_modifier_shift);
        checkBoxMeta = findViewById(R.id.checkbox_modifier_meta);
        spinnerMainKey = findViewById(R.id.spinner_main_key);
        buttonSave = findViewById(R.id.button_save_formatting_tag);

        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "EditFormattingTagActivity: Applied dynamic OLED colors for window/toolbar.");

            // Retrieve common colors
            int buttonBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_button_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
            );
            int buttonTextIconColor = this.sharedPreferences.getInt(
                "pref_oled_button_text_icon",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
            );
            int textboxBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_textbox_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND
            );
            int accentGeneralColor = this.sharedPreferences.getInt(
                "pref_oled_accent_general",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL
            );

            // Style Save Button
            if (buttonSave != null) {
                buttonSave.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                buttonSave.setTextColor(buttonTextIconColor);
                Log.d(TAG, String.format("EditFormattingTagActivity: Styled buttonSave with BG=0x%08X, Text=0x%08X", buttonBackgroundColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "EditFormattingTagActivity: buttonSave is null, cannot style.");
            }

            // Style EditText backgrounds
            TextInputEditText[] editTextsToStyle = { editTextName, editTextOpeningTag, editTextTagDelayMs };
            String[] editTextNames = { "editTextName", "editTextOpeningTag", "editTextTagDelayMs" };

            for (int i = 0; i < editTextsToStyle.length; i++) {
                TextInputEditText et = editTextsToStyle[i];
                String etName = editTextNames[i];
                if (et != null) {
                    et.setBackgroundColor(textboxBackgroundColor);
                    Log.d(TAG, String.format("EditFormattingTagActivity: Styled %s BG: 0x%08X", etName, textboxBackgroundColor));
                } else {
                    Log.w(TAG, "EditFormattingTagActivity: EditText " + etName + " is null, cannot style.");
                }
            }

            // Style CheckBoxes
            CheckBox[] checkBoxesToStyle = { checkBoxCtrl, checkBoxAlt, checkBoxShift, checkBoxMeta };
            String[] checkBoxNames = { "checkBoxCtrl", "checkBoxAlt", "checkBoxShift", "checkBoxMeta" };
            ColorStateList accentColorStateList = ColorStateList.valueOf(accentGeneralColor);

            for (int i = 0; i < checkBoxesToStyle.length; i++) {
                CheckBox cb = checkBoxesToStyle[i];
                String cbName = checkBoxNames[i];
                if (cb != null) {
                    CompoundButtonCompat.setButtonTintList(cb, accentColorStateList);
                    Log.d(TAG, String.format("EditFormattingTagActivity: Styled %s Tint: 0x%08X", cbName, accentGeneralColor));
                } else {
                    Log.w(TAG, "EditFormattingTagActivity: CheckBox " + cbName + " is null, cannot style.");
                }
            }
        }

        populateMainKeySpinner();

        tagManager = new FormattingTagManager(this);
        tagManager.open();

        currentTagId = getIntent().getLongExtra(EXTRA_TAG_ID, INVALID_TAG_ID);

        if (currentTagId != INVALID_TAG_ID) {
            currentTag = tagManager.getTag(currentTagId);
            if (currentTag != null) {
                editTextName.setText(currentTag.getName());
                editTextOpeningTag.setText(currentTag.getOpeningTagText());
                editTextTagDelayMs.setText(String.valueOf(currentTag.getDelayMs()));
                parseAndSetKeystrokeUI(currentTag.getKeystrokeSequence());
                if (actionBar != null) actionBar.setTitle(R.string.edit_formatting_tag_title);
            } else {
                Toast.makeText(this, "Formatting Tag not found, creating a new one.", Toast.LENGTH_SHORT).show();
                if (actionBar != null) actionBar.setTitle(R.string.add_new_formatting_tag_title);
                editTextTagDelayMs.setText(DEFAULT_DELAY_MS);
                currentTagId = INVALID_TAG_ID;
            }
        } else {
            if (actionBar != null) actionBar.setTitle(R.string.add_new_formatting_tag_title);
            editTextTagDelayMs.setText(DEFAULT_DELAY_MS);
        }

        buttonSave.setOnClickListener(v -> saveFormattingTag());

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue;
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "EditFormattingTagActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "EditFormattingTagActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
    }

    private void saveFormattingTag() {
        String name = editTextName.getText().toString().trim();
        String openingText = editTextOpeningTag.getText().toString().trim();
        String delayString = editTextTagDelayMs.getText().toString().trim();
        int delayMs = 0;

        if (delayString.isEmpty()) {
            editTextTagDelayMs.setError("Delay cannot be empty. Enter 0 if no delay is needed.");
            editTextTagDelayMs.requestFocus();
            Toast.makeText(this, "Delay cannot be empty. Enter 0 if no delay is needed.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            delayMs = Integer.parseInt(delayString);
            if (delayMs < 0) {
                editTextTagDelayMs.setError("Delay must be a non-negative number.");
                editTextTagDelayMs.requestFocus();
                Toast.makeText(this, "Delay must be a non-negative number.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            editTextTagDelayMs.setError("Invalid number format for delay.");
            editTextTagDelayMs.requestFocus();
            Toast.makeText(this, "Invalid number format for delay.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder keystrokesBuilder = new StringBuilder();
        if (checkBoxCtrl.isChecked()) keystrokesBuilder.append("CTRL_LEFT+");
        if (checkBoxAlt.isChecked()) keystrokesBuilder.append("ALT_LEFT+");
        if (checkBoxShift.isChecked()) keystrokesBuilder.append("SHIFT_LEFT+");
        if (checkBoxMeta.isChecked()) keystrokesBuilder.append("GUI_LEFT+");

        KeystrokeDisplay selectedKeyItem = (KeystrokeDisplay) spinnerMainKey.getSelectedItem();
        String mainKeyValue = "";
        if (selectedKeyItem != null && selectedKeyItem.keyValue != null && !selectedKeyItem.keyValue.isEmpty()) {
            mainKeyValue = selectedKeyItem.keyValue;
            keystrokesBuilder.append(mainKeyValue);
        } else if (keystrokesBuilder.length() > 0) {
             keystrokesBuilder.setLength(keystrokesBuilder.length() - 1);
        }
        String keystrokes = keystrokesBuilder.toString();
        if (mainKeyValue.isEmpty()) {
            Toast.makeText(this, "Main key must be selected.", Toast.LENGTH_SHORT).show();
            if (spinnerMainKey.getSelectedView() instanceof TextView) {
                 ((TextView)spinnerMainKey.getSelectedView()).setError("Main key required");
            }
            return;
        }

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

        if (currentTagId != INVALID_TAG_ID && currentTag != null) {
            currentTag.setName(name);
            currentTag.setOpeningTagText(openingText);
            currentTag.setDelayMs(delayMs);
            currentTag.setKeystrokeSequence(keystrokes);
            tagManager.updateTag(currentTag);
        } else {
            FormattingTag newTag = new FormattingTag(0, name, openingText, keystrokes, true, delayMs);
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
        keySpinnerItems.add(new KeystrokeDisplay(getString(R.string.prompt_select_key), ""));
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
        for (char c = 'A'; c <= 'Z'; c++) keySpinnerItems.add(new KeystrokeDisplay(String.valueOf(c), "KEY_" + c));
        for (char c = '0'; c <= '9'; c++) keySpinnerItems.add(new KeystrokeDisplay(String.valueOf(c), "KEY_" + c));
        for (int i = 1; i <= 12; i++) keySpinnerItems.add(new KeystrokeDisplay("F" + i, "KEY_F" + i));
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
        keySpinnerItems.add(new KeystrokeDisplay("Pause", "KEY_PAUSE"));
        keySpinnerItems.add(new KeystrokeDisplay("Application", "KEY_APPLICATION"));
        keySpinnerItems.add(new KeystrokeDisplay("Num Lock", "KEY_NUM_LOCK"));
        keySpinnerItems.add(new KeystrokeDisplay("Num /", "KEY_NUM_SLASH"));
        keySpinnerItems.add(new KeystrokeDisplay("Num *", "KEY_NUM_STAR"));
        keySpinnerItems.add(new KeystrokeDisplay("Num -", "KEY_NUM_MINUS"));
        keySpinnerItems.add(new KeystrokeDisplay("Num +", "KEY_NUM_PLUS"));
        keySpinnerItems.add(new KeystrokeDisplay("Num Enter", "KEY_NUM_ENTER"));
        for (int i = 0; i <= 9; i++) keySpinnerItems.add(new KeystrokeDisplay("Num " + i, "KEY_NUM_" + i));
        keySpinnerItems.add(new KeystrokeDisplay("Num . (Dot)", "KEY_NUM_DOT"));
        ArrayAdapter<KeystrokeDisplay> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, keySpinnerItems);
        spinnerMainKey.setAdapter(adapter);
    }

    private void parseAndSetKeystrokeUI(String keystrokeSequence) {
        if (keystrokeSequence == null || keystrokeSequence.isEmpty()) return;
        checkBoxCtrl.setChecked(false);
        checkBoxAlt.setChecked(false);
        checkBoxShift.setChecked(false);
        checkBoxMeta.setChecked(false);
        spinnerMainKey.setSelection(0);
        String[] parts = keystrokeSequence.split("\\+");
        String mainKeyPart = null;
        for (String part : parts) {
            part = part.trim().toUpperCase();
            if (part.equals("CTRL_LEFT") || part.equals("CTRL_RIGHT") || part.equals("CTRL")) checkBoxCtrl.setChecked(true);
            else if (part.equals("ALT_LEFT") || part.equals("ALT_RIGHT") || part.equals("ALT")) checkBoxAlt.setChecked(true);
            else if (part.equals("SHIFT_LEFT") || part.equals("SHIFT_RIGHT") || part.equals("SHIFT")) checkBoxShift.setChecked(true);
            else if (part.equals("GUI_LEFT") || part.equals("GUI_RIGHT") || part.equals("META")) checkBoxMeta.setChecked(true);
            else mainKeyPart = part;
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
                     Log.d(TAG, "onResume: OLED color(s) changed for EditFormattingTagActivity.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating EditFormattingTagActivity.");
                recreate();
                return;
            }
        }
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
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
            Log.d(TAG, "Main theme preference changed. Recreating EditFormattingTagActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating EditFormattingTagActivity.");
                recreate();
            }
        }
    }
}
