package com.drgraff.speakkey.formattingtags;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences; // Standard import
import android.os.Bundle;
import android.util.Log; // Standard import
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager; // Standard import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.drgraff.speakkey.R;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import com.drgraff.speakkey.utils.ThemeManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.content.res.ColorStateList; // Added for ColorStateList, just in case

import java.util.ArrayList;
import java.util.List;

public class FormattingTagsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private RecyclerView recyclerView;
    private FormattingTagAdapter adapter;
    private FormattingTagManager tagManager;
    private TextView emptyView;
    private FloatingActionButton fabAddTag;

    private SharedPreferences sharedPreferences; // Class field
    private static final String TAG = "FormattingTagsActivity";

    public static final int REQUEST_CODE_ADD_TAG = 1;
    public static final int REQUEST_CODE_EDIT_TAG = 2;

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
        setContentView(R.layout.activity_formatting_tags);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.formatting_tags_activity_title));
        }

        // Initialize UI elements FIRST
        recyclerView = findViewById(R.id.formatting_tags_recycler_view);
        emptyView = findViewById(R.id.empty_formatting_tags_text_view);
        fabAddTag = findViewById(R.id.fab_add_formatting_tag);

        String currentActivityThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "FormattingTagsActivity: Applied dynamic OLED colors for window/toolbar.");

            // Style FloatingActionButton
            if (fabAddTag != null) {
                int accentGeneralColor = this.sharedPreferences.getInt(
                    "pref_oled_accent_general",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL
                );
                int buttonTextIconColor = this.sharedPreferences.getInt(
                    "pref_oled_button_text_icon",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
                );

                fabAddTag.setBackgroundTintList(ColorStateList.valueOf(accentGeneralColor));
                fabAddTag.setImageTintList(ColorStateList.valueOf(buttonTextIconColor));

                Log.d(TAG, String.format("FormattingTagsActivity: Styled fabAddTag with BG=0x%08X, IconTint=0x%08X", accentGeneralColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "FormattingTagsActivity: fabAddTag is null, cannot style.");
            }
        }

        tagManager = new FormattingTagManager(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FormattingTagAdapter(this, new ArrayList<>(), tagManager);
        recyclerView.setAdapter(adapter);

        fabAddTag.setOnClickListener(v -> {
            Intent intent = new Intent(FormattingTagsActivity.this, EditFormattingTagActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_TAG);
        });

        // Store applied theme state
        this.mAppliedThemeMode = currentActivityThemeValue;
        if (ThemeManager.THEME_OLED.equals(currentActivityThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "FormattingTagsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "FormattingTagsActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
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
                     Log.d(TAG, "onResume: OLED color(s) changed for FormattingTagsActivity.");
                }
            }

            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating FormattingTagsActivity.");
                recreate();
                return;
            }
        }

        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        loadFormattingTags();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    private void loadFormattingTags() {
        List<FormattingTag> tags = new ArrayList<>();
        try {
            tagManager.open();
             if (tagManager.isOpen()) {
                tags = tagManager.getAllTags();
             }
        } catch (Exception e) {
            Log.e(TAG, "Error loading tags", e);
        } finally {
            if (tagManager.isOpen()) {
                tagManager.close();
            }
        }
        adapter.setFormattingTags(tags);
        if (tags.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_ADD_TAG || requestCode == REQUEST_CODE_EDIT_TAG) {
                // loadFormattingTags(); // Implicitly called by onResume
            }
        }
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
            Log.d(TAG, "Main theme preference changed. Recreating FormattingTagsActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating FormattingTagsActivity.");
                recreate();
            }
        }
    }
}
