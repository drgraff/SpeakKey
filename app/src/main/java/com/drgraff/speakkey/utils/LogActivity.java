package com.drgraff.speakkey.utils; // Or com.drgraff.speakkey.ui if preferred

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.res.ColorStateList; // Added for ColorStateList

import com.drgraff.speakkey.R; // Ensure R is imported correctly

import java.util.ArrayList;

public class LogActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private RecyclerView logRecyclerView;
    private LogAdapter logAdapter;
    private Button clearLogButton;

    private SharedPreferences sharedPreferences;
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;
    private int mAppliedOledButtonBackgroundColor = 0;
    private int mAppliedOledButtonTextIconColor = 0;
    private static final String TAG = "LogActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize SharedPreferences as a field
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initial theme application before super.onCreate()
        ThemeManager.applyTheme(this.sharedPreferences);
        String themeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Application Log");
        }

        // Initialize UI elements that need styling BEFORE the theme block
        logRecyclerView = findViewById(R.id.log_recycler_view);
        clearLogButton = findViewById(R.id.clear_log_button); // Now initialized

        // Apply custom OLED colors if OLED theme is active
        // Re-fetch currentThemeValue as themeValue was for pre-super.onCreate
        String currentThemeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "LogActivity: Applied dynamic OLED colors for window/toolbar."); // Consistent Log

            // Now, style the clear_log_button
            if (clearLogButton != null) {
                int buttonBackgroundColor = this.sharedPreferences.getInt(
                    "pref_oled_button_background",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
                );
                int buttonTextIconColor = this.sharedPreferences.getInt(
                    "pref_oled_button_text_icon",
                    com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
                );

                clearLogButton.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                clearLogButton.setTextColor(buttonTextIconColor);

                Log.d(TAG, String.format("LogActivity: Styled clearLogButton with BG=0x%08X, Text=0x%08X", buttonBackgroundColor, buttonTextIconColor));
            } else {
                Log.w(TAG, "LogActivity: clearLogButton is null, cannot style.");
            }
        }

        logRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(new ArrayList<>()); 
        logRecyclerView.setAdapter(logAdapter);

        clearLogButton.setOnClickListener(v -> {
            AppLogManager.getInstance().clearEntries();
            refreshLogView();
        });

        // Store the currently applied theme mode and relevant OLED colors
        this.mAppliedThemeMode = currentThemeValue; // Use the most recently fetched theme value

        if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            this.mAppliedOledButtonBackgroundColor = this.sharedPreferences.getInt("pref_oled_button_background", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND);
            this.mAppliedOledButtonTextIconColor = this.sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON);
            Log.d(TAG, "LogActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
            Log.d(TAG, "LogActivity onCreate: Stored specific OLED colors: ButtonBG=0x" + Integer.toHexString(mAppliedOledButtonBackgroundColor) + ", ButtonTextIcon=0x" + Integer.toHexString(mAppliedOledButtonTextIconColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            this.mAppliedOledButtonBackgroundColor = 0;
            this.mAppliedOledButtonTextIconColor = 0;
            Log.d(TAG, "LogActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
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

                int currentOledButtonBg = sharedPreferences.getInt("pref_oled_button_background", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND);
                if (mAppliedOledButtonBackgroundColor != currentOledButtonBg) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Button Background Color changed in LogActivity. Old=0x" + Integer.toHexString(mAppliedOledButtonBackgroundColor) + ", New=0x" + Integer.toHexString(currentOledButtonBg));
                }

                int currentOledButtonTextIcon = sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON);
                if (mAppliedOledButtonTextIconColor != currentOledButtonTextIcon) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Button Text/Icon Color changed in LogActivity. Old=0x" + Integer.toHexString(mAppliedOledButtonTextIconColor) + ", New=0x" + Integer.toHexString(currentOledButtonTextIcon));
                }

                if (needsRecreate) {
                     Log.d(TAG, "onResume: OLED color(s) changed for LogActivity.");
                }
            }

            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating LogActivity.");
                recreate();
                return;
            }
        }

        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        refreshLogView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    private void refreshLogView() {
        if (logAdapter != null) {
            logAdapter.updateLogEntries(AppLogManager.getInstance().getEntries());
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
            Log.d(TAG, "Main theme preference changed (dark_mode). Recreating LogActivity.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating LogActivity.");
                recreate();
            }
        }
    }
}
