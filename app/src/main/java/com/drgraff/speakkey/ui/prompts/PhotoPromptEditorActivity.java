package com.drgraff.speakkey.ui.prompts;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences; // Standard import
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
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

import java.util.List;

public class PhotoPromptEditorActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String EXTRA_PHOTO_PROMPT_ID = "com.drgraff.speakkey.EXTRA_PHOTO_PROMPT_ID";
    private static final String TAG = "PhotoPromptEditorAct";

    private SharedPreferences sharedPreferences; // Class field
    private EditText editTextPhotoPromptLabel;
    private EditText editTextPhotoPromptText;
    private Button btnSavePhotoPrompt;
    private Toolbar toolbar; // Already a field

    private PromptManager promptManager;
    private long currentPromptId = -1;
    private boolean isEditMode = false;
    private Prompt currentPrompt;

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
        setContentView(R.layout.activity_photo_prompt_editor);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        // This was part of the previous step, ensure it's correctly placed
        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "PhotoPromptEditorActivity: Applied dynamic OLED colors for window/toolbar.");

            // Style Save Button
            if (btnSavePhotoPrompt != null) {
                int buttonBackgroundColor = this.sharedPreferences.getInt(
                    "pref_oled_button_background",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
                );
                int buttonTextIconColor = this.sharedPreferences.getInt(
                    "pref_oled_button_text_icon",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
                );
                btnSavePhotoPrompt.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonBackgroundColor));
                btnSavePhotoPrompt.setTextColor(buttonTextIconColor);
                Log.d(TAG, String.format("PhotoPromptEditorActivity: Styled btnSavePhotoPrompt with BG=0x%08X, Text=0x%08X", buttonBackgroundColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "PhotoPromptEditorActivity: btnSavePhotoPrompt is null, cannot style.");
            }

            // Style EditText backgrounds
            int textboxBackgroundColor = this.sharedPreferences.getInt(
                "pref_oled_textbox_background",
                com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND
            );

            EditText[] editTextsToStyle = { editTextPhotoPromptLabel, editTextPhotoPromptText };
            String[] editTextNames = { "editTextPhotoPromptLabel", "editTextPhotoPromptText" };

            for (int i = 0; i < editTextsToStyle.length; i++) {
                EditText et = editTextsToStyle[i];
                String etName = editTextNames[i];
                if (et != null) {
                    et.setBackgroundColor(textboxBackgroundColor); // Using setBackgroundColor for EditTexts
                    Log.d(TAG, String.format("PhotoPromptEditorActivity: Styled %s BG: 0x%08X", etName, textboxBackgroundColor));
                } else {
                    Log.w(TAG, "PhotoPromptEditorActivity: EditText " + etName + " is null, cannot style.");
                }
            }
        }

        editTextPhotoPromptLabel = findViewById(R.id.edittext_photo_prompt_label);
        editTextPhotoPromptText = findViewById(R.id.edittext_photo_prompt_text);
        btnSavePhotoPrompt = findViewById(R.id.btn_save_photo_prompt);

        promptManager = new PromptManager(this);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PHOTO_PROMPT_ID)) {
            currentPromptId = intent.getLongExtra(EXTRA_PHOTO_PROMPT_ID, -1);
            if (currentPromptId != -1) {
                isEditMode = true;
                List<Prompt> photoPrompts = promptManager.getPromptsForMode("photo_vision");
                for (Prompt p : photoPrompts) {
                    if (p.getId() == currentPromptId) {
                        currentPrompt = p;
                        break;
                    }
                }
                if (currentPrompt != null) {
                    editTextPhotoPromptLabel.setText(currentPrompt.getLabel());
                    editTextPhotoPromptText.setText(currentPrompt.getText());
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

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue; // Use the value fetched after super.onCreate()
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PhotoPromptEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PhotoPromptEditorActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
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

        if (isEditMode && currentPrompt != null) {
            currentPrompt.setLabel(label);
            currentPrompt.setText(text);
            currentPrompt.setPromptModeType("photo_vision");
            promptManager.updatePrompt(currentPrompt);
            Toast.makeText(this, getString(R.string.photo_prompt_editor_toast_updated), Toast.LENGTH_SHORT).show();
        } else {
            promptManager.addPrompt(label, text, "", "photo_vision");
            Toast.makeText(this, getString(R.string.photo_prompt_editor_toast_added), Toast.LENGTH_SHORT).show();
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
                     Log.d(TAG, "onResume: OLED color(s) changed for PhotoPromptEditorActivity.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating PhotoPromptEditorActivity.");
                recreate();
                return;
            }
        }
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        // Any other onResume logic for this activity would go here
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        // Any other onPause logic for this activity would go here
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
            Log.d(TAG, "Main theme preference changed. Recreating PhotoPromptEditorActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating PhotoPromptEditorActivity.");
                recreate();
            }
        }
    }
}
