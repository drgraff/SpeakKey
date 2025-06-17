package com.drgraff.speakkey.ui.prompts;

import android.app.Activity;
import android.content.SharedPreferences; // Standard import
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log; // Standard import

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager; // Standard import

import com.drgraff.speakkey.R;
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import com.drgraff.speakkey.utils.ThemeManager;
import android.content.res.ColorStateList; // Added for ColorStateList

import java.util.List; // Keep if getAllPrompts() returns List

public class PromptEditorActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_PROMPT_ID = "com.drgraff.speakkey.EXTRA_PROMPT_ID";
    private static final long INVALID_PROMPT_ID = -1;
    private static final String TAG = "PromptEditorActivity";

    private SharedPreferences sharedPreferences; // Class field
    private EditText editTextLabel;
    private EditText editTextText;
    private EditText editTextTranscriptionHint;
    private Button buttonSave;

    private PromptManager promptManager;
    private Prompt currentPrompt;
    private long currentPromptId = INVALID_PROMPT_ID;

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
        setContentView(R.layout.activity_edit_prompt);

        // Initialize UI elements FIRST
        editTextLabel = findViewById(R.id.prompt_edit_label);
        editTextText = findViewById(R.id.prompt_edit_text);
        editTextTranscriptionHint = findViewById(R.id.prompt_edit_transcription_hint);
        buttonSave = findViewById(R.id.button_save_prompt);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "PromptEditorActivity: Applied dynamic OLED colors for window/toolbar.");

            // Style Save Button
            if (buttonSave != null) {
                int buttonBackgroundColor = this.sharedPreferences.getInt(
                    "pref_oled_button_background",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
                );
                int buttonTextIconColor = this.sharedPreferences.getInt(
                    "pref_oled_button_text_icon",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
                );
                buttonSave.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                buttonSave.setTextColor(buttonTextIconColor);
                Log.d(TAG, String.format("PromptEditorActivity: Styled buttonSave with BG=0x%08X, Text=0x%08X", buttonBackgroundColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "PromptEditorActivity: buttonSave is null, cannot style.");
            }

            // Style EditText backgrounds
            int textboxBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_textbox_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND
            );

            EditText[] editTextsToStyle = { editTextLabel, editTextText, editTextTranscriptionHint };
            String[] editTextNames = { "editTextLabel", "editTextText", "editTextTranscriptionHint" };

            for (int i = 0; i < editTextsToStyle.length; i++) {
                EditText et = editTextsToStyle[i];
                String etName = editTextNames[i];
                if (et != null) {
                    et.setBackgroundColor(textboxBackgroundColor); // Using setBackgroundColor for EditTexts
                    Log.d(TAG, String.format("PromptEditorActivity: Styled %s BG: 0x%08X", etName, textboxBackgroundColor));
                } else {
                    Log.w(TAG, "PromptEditorActivity: EditText " + etName + " is null, cannot style.");
                }
            }
        }

        promptManager = new PromptManager(this);
        currentPromptId = getIntent().getLongExtra(EXTRA_PROMPT_ID, INVALID_PROMPT_ID);

        if (currentPromptId != INVALID_PROMPT_ID) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.edit_prompt_title);
            }
            List<Prompt> prompts = promptManager.getAllPrompts();
            for (Prompt p : prompts) {
                if (p.getId() == currentPromptId) {
                    currentPrompt = p;
                    break;
                }
            }
            if (currentPrompt != null) {
                editTextLabel.setText(currentPrompt.getLabel());
                editTextText.setText(currentPrompt.getText());
                editTextTranscriptionHint.setText(currentPrompt.getTranscriptionHint() != null ? currentPrompt.getTranscriptionHint() : "");
            } else {
                Toast.makeText(this, R.string.prompt_not_found_message, Toast.LENGTH_SHORT).show();
                if (actionBar != null) {
                    actionBar.setTitle(R.string.add_prompt_title);
                }
                currentPromptId = INVALID_PROMPT_ID;
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

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue;
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PromptEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PromptEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
    }

    private void savePrompt() {
        String label = editTextLabel.getText().toString().trim();
        String text = editTextText.getText().toString();
        String transcriptionHint = editTextTranscriptionHint.getText().toString().trim();

        if (label.isEmpty()) {
            editTextLabel.setError(getString(R.string.prompt_label_required_message));
            Toast.makeText(this, R.string.prompt_label_required_message, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPromptId != INVALID_PROMPT_ID && currentPrompt != null) {
            currentPrompt.setLabel(label);
            currentPrompt.setText(text);
            currentPrompt.setTranscriptionHint(transcriptionHint);
            if (currentPrompt.getPromptModeType() == null || currentPrompt.getPromptModeType().isEmpty()) {
                currentPrompt.setPromptModeType("two_step_processing");
                Log.w(TAG, "Prompt " + currentPrompt.getId() + " had null/empty modeType, defaulted to two_step_processing");
            }
            promptManager.updatePrompt(currentPrompt);
            Toast.makeText(this, R.string.prompt_saved_message, Toast.LENGTH_SHORT).show();
        } else {
            String modeTypeForNewPrompt = getIntent().getStringExtra("PROMPT_MODE_TYPE");
            if (modeTypeForNewPrompt == null || modeTypeForNewPrompt.isEmpty()) {
                modeTypeForNewPrompt = "two_step_processing";
                Log.w(TAG, "PROMPT_MODE_TYPE extra not found, defaulting to " + modeTypeForNewPrompt);
            }
            promptManager.addPrompt(label, text, transcriptionHint, modeTypeForNewPrompt);
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
                     Log.d(TAG, "onResume: OLED color(s) changed for PromptEditorActivity.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating PromptEditorActivity.");
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
            Log.d(TAG, "Main theme preference changed. Recreating PromptEditorActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating PromptEditorActivity.");
                recreate();
            }
        }
    }
}
