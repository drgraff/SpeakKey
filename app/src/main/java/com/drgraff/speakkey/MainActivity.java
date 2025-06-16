package com.drgraff.speakkey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo; // Added
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu; // ADD THIS
import android.view.MenuInflater; // ADD THIS
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup; // Added import
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton; // Added for ImageButton
import android.widget.ProgressBar; // Added
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import android.widget.LinearLayout; // Added for Macro buttons
import com.google.android.material.button.MaterialButton; // Added for Macro buttons

import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.WhisperApi;
import com.drgraff.speakkey.data.AppDatabase; // Added for UploadTask
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.drgraff.speakkey.data.UploadTask; // Added for UploadTask
import com.drgraff.speakkey.service.UploadService; // Added for UploadService
import com.speakkey.data.Macro; // Added for Macro buttons
import com.speakkey.data.MacroRepository; // Added for Macro buttons
import com.speakkey.service.MacroExecutor; // Added for Macro Execution
import com.drgraff.speakkey.inputstick.InputStickBroadcast; // Added
import com.drgraff.speakkey.inputstick.InputStickManager; // Reinstated
import com.drgraff.speakkey.settings.SettingsActivity;
import com.drgraff.speakkey.formattingtags.FormattingTagsActivity; // Added for Formatting Tags
import com.drgraff.speakkey.PhotosActivity; // Added for Photos
import com.speakkey.ui.macros.MacroListActivity; // Added for Macros
import com.drgraff.speakkey.ui.AboutActivity; // ADD THIS
import com.drgraff.speakkey.utils.AppLogManager;
import com.drgraff.speakkey.utils.ThemeManager;
import com.drgraff.speakkey.utils.DynamicThemeApplicator; // Added for DynamicThemeApplicator
import com.google.android.material.navigation.NavigationView;
import android.content.res.ColorStateList; // Added for ColorStateList

import com.hualee.lame.LameControl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService; // Added for Macro Execution
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.text.TextUtils; // Added for ellipsize

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FullScreenEditTextDialogFragment.OnSaveListener, SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String mAppliedThemeMode = null;
    private int mAppliedOledButtonBackgroundColor = 0;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;
    private int mAppliedTextboxBackgroundColor = 0;
    private int mAppliedOledButtonTextIconColor = 0;
    public static final String TRANSCRIPTION_QUEUED_PLACEHOLDER = "[Transcription queued... Tap to refresh]"; // Added

    // UI elements
    private DrawerLayout drawerLayout;
    private Button btnStartRecording, btnPauseRecording, btnStopRecording;
    private Button btnSendWhisper; // Removed btnClearTranscription, Removed btnClearRecording
    private ImageButton btnClearTranscriptionIcon; // Added
    private ImageButton btnClearAllWhisperIcon; // Added to replace btnClearAll
    // private Button btnRefreshStatus; // Removed
    private Button btnSendChatGpt; // Removed btnClearChatGpt
    private ImageButton btnClearChatGptIcon; // Added
    private Button btnSendInputStick;
    private Button btnSendWhisperToInputStick; // Added
    private ImageButton btnShareWhisperText; // Added
    private ImageButton btnShareChatGptText; // Added
    private EditText whisperText, chatGptText;
    private View recordingIndicator;
    private TextView recordingTime;
    private TextView activePromptsDisplay; // ADD THIS
    private View whisperSectionContainer; // Added
    private CheckBox chkAutoSendWhisper, chkAutoSendInputStick, chkAutoSendToChatGpt; // Added chkAutoSendToChatGpt
    private CheckBox chk_auto_send_whisper_to_inputstick; // Added
    private EditText currentEditingEditText; // For FullScreenEditTextDialogFragment

    private ProgressBar progressBarWhisper; // Added
    private TextView textViewWhisperStatus; // Added

    private ProgressBar progressBarChatGpt; // Added
    private TextView textViewChatGptStatus; // Added

    // Audio recording

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean recordingThreadRunning = false;
    private String pcmFilePath; // Raw PCM recording path
    private String mp3FilePath; // Path of MP3 converted from recording
    private String audioFilePath; // Points to MP3 after conversion
    private String lastRecordedAudioPathForChatGPTDirect = null; // Added
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long recordingStartTime = 0;
    private long recordingDuration = 0;
    
    // APIs
    private WhisperApi whisperApi;
    private ChatGptApi chatGptApi;
    private InputStickManager inputStickManager; // Reinstated
    private PromptManager promptManager; // ADD THIS
    
    // Settings
    private SharedPreferences sharedPreferences;
    private MacroRepository macroRepository; // Added for Macro buttons
    private MacroExecutor macroExecutor; // Added for Macro Execution
    private ExecutorService macroExecutorService; // Added for Macro Execution
    
    // Timer
    private ScheduledExecutorService timerExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TranscriptionBroadcastReceiver transcriptionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize settings
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // String transcriptionMode = sharedPreferences.getString("transcription_mode", "two_step_transcription"); // This line is not needed here for theme application
        
        // Determine the theme value from preferences
        String themeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);

        // Apply the global AppCompatDelegate night mode (e.g., MODE_NIGHT_YES/NO) first.
        ThemeManager.applyTheme(sharedPreferences);

        // ONLY if OLED is specifically chosen, explicitly set the AppTheme.OLED.
        // Otherwise, the Activity will use its manifest theme (@style/Theme.SpeakKey),
        // which is DayNight aware and will respect the mode set by ThemeManager.applyTheme().
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }
        // NO 'else' block calling setTheme(R.style.Theme_SpeakKey)
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeUiElements(); // Initializes whisperText, chatGptText, and finds R.id.toolbar

        // Apply dynamic colors AFTER UI elements are initialized
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            DynamicThemeApplicator.applyOledColors(this, sharedPreferences); // Applies to Toolbar, Window

            // Style EditText backgrounds
            int textboxBackgroundColor = sharedPreferences.getInt(
                "pref_oled_textbox_background",
                DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND
            );
            if (whisperText != null) {
                whisperText.setBackgroundColor(textboxBackgroundColor);
                Log.d(TAG, String.format("MainActivity: Styled whisperText BG: 0x%08X", textboxBackgroundColor));
            }
            if (chatGptText != null) {
                chatGptText.setBackgroundColor(textboxBackgroundColor);
                Log.d(TAG, String.format("MainActivity: Styled chatGptText BG: 0x%08X", textboxBackgroundColor));
            }

            // Style Action Buttons
            int buttonBackgroundColor = sharedPreferences.getInt(
                "pref_oled_button_background",
                DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND
            );
            int buttonTextIconColor = sharedPreferences.getInt(
                "pref_oled_button_text_icon",
                DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON
            );

            Button[] buttonsToStyle = {
                btnStartRecording, btnPauseRecording, btnStopRecording, btnSendWhisper,
                btnSendChatGpt, btnSendInputStick, btnSendWhisperToInputStick
            };
            String[] buttonNames = {
                "btnStartRecording", "btnPauseRecording", "btnStopRecording", "btnSendWhisper",
                "btnSendChatGpt", "btnSendInputStick", "btnSendWhisperToInputStick"
            };

            for (int i = 0; i < buttonsToStyle.length; i++) {
                Button button = buttonsToStyle[i];
                String buttonName = buttonNames[i];
                if (button != null) {
                    button.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                    button.setTextColor(buttonTextIconColor);
                    // If buttons have icons that need tinting (assuming MaterialButton or AppCompatButton):
                    // if (button instanceof com.google.android.material.button.MaterialButton) {
                    //    ((com.google.android.material.button.MaterialButton) button).setIconTint(ColorStateList.valueOf(buttonTextIconColor));
                    // }
                    Log.d(TAG, String.format("MainActivity: Styled %s with BG=0x%08X, Text=0x%08X", buttonName, buttonBackgroundColor, buttonTextIconColor));
                } else {
                    Log.w(TAG, "MainActivity: Button " + buttonName + " is null, cannot style.");
                }
            }
        }

        // Re-fetch transcriptionMode if it was removed above, or ensure it's fetched after setContentView if needed by UI below.
        // For safety, let's assume it's needed by logic further down in onCreate.
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "two_step_transcription");

        // Store the currently applied theme mode and all relevant OLED colors
        this.mAppliedThemeMode = themeValue; // themeValue is from earlier in onCreate

        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            this.mAppliedTopbarBackgroundColor = sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            this.mAppliedTextboxBackgroundColor = sharedPreferences.getInt("pref_oled_textbox_background", DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND);
            this.mAppliedOledButtonBackgroundColor = sharedPreferences.getInt("pref_oled_button_background", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND);
            this.mAppliedOledButtonTextIconColor = sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON);
            // Also log all these values
            Log.d(TAG, "onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor) +
                         ", TextboxBG=0x" + Integer.toHexString(mAppliedTextboxBackgroundColor) +
                         ", ButtonBG=0x" + Integer.toHexString(mAppliedOledButtonBackgroundColor) +
                         ", ButtonTextIcon=0x" + Integer.toHexString(mAppliedOledButtonTextIconColor));
        } else {
            // Reset all applied OLED color trackers if not in OLED mode
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            this.mAppliedTextboxBackgroundColor = 0;
            this.mAppliedOledButtonBackgroundColor = 0;
            this.mAppliedOledButtonTextIconColor = 0;
            Log.d(TAG, "onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }

        macroExecutor = new MacroExecutor(this); // Initialize MacroExecutor
        macroExecutorService = Executors.newSingleThreadExecutor(); // Initialize ExecutorService
        
        // Initialize toolbar and navigation
        Toolbar toolbar = findViewById(R.id.toolbar); // This is already found in initializeUiElements
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 
                R.string.nav_header_desc, R.string.nav_header_desc);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Initialize APIs and MacroRepository
        initializeApis();
        macroRepository = new MacroRepository(getApplicationContext()); // Initialize MacroRepository
        promptManager = new PromptManager(this); // Initialize PromptManager
        transcriptionReceiver = new TranscriptionBroadcastReceiver();
        
        // Request permissions
        requestPermissions();
        
        // Set up temporary audio file path
        File audioDir = new File(getFilesDir(), "audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }

        pcmFilePath = new File(audioDir, "recording.pcm").getAbsolutePath();
        mp3FilePath = new File(audioDir, "recording.mp3").getAbsolutePath();
        audioFilePath = mp3FilePath;
        Log.i(TAG, "Recording paths -> PCM: " + pcmFilePath + ", MP3: " + mp3FilePath);


        // Display active macros
        updateUiForTranscriptionMode(transcriptionMode); // Added
        displayActiveMacros(); // Call after macroRepository is initialized
    }

    private void updateUiForTranscriptionMode(String mode) {
        Log.d(TAG, "updateUiForTranscriptionMode: Mode changed to: " + mode);
        if (whisperSectionContainer == null) {
            // This might happen if called before initializeUiElements or if ID is wrong.
            Log.e(TAG, "whisperSectionContainer is null in updateUiForTranscriptionMode. UI update skipped.");
            return;
        }
        if ("one_step_transcription".equals(mode)) {
            whisperSectionContainer.setVisibility(View.GONE);
            Log.d(TAG, "updateUiForTranscriptionMode: whisperSectionContainer visibility set to GONE");
            // Adjust hint for chatGptText if needed, e.g.,
            // chatGptText.setHint("ChatGPT Direct Transcription/Response");

            // Update activePromptsDisplay for "Direct Transcription" if no prompts are active
            // This part of the logic will also be handled/refined in updateActivePromptsDisplay()
            // For now, ensure that updateActivePromptsDisplay() is aware of the mode or is called after this.
            // The specific logic for activePrompts list here is removed,
            // as updateActivePromptsDisplay() will handle it.
            updateActivePromptsDisplay(); // Ensure it's called to reflect mode change.

        } else { // "two_step_transcription" mode (default)
            whisperSectionContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "updateUiForTranscriptionMode: whisperSectionContainer visibility set to VISIBLE");
            // chatGptText.setHint("ChatGPT Response"); // Reset hint if changed

            // Ensure activePromptsDisplay is updated by its dedicated method
            updateActivePromptsDisplay();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_toolbar_photos) {
            Intent intent = new Intent(this, PhotosActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_toolbar_record) {
            if (btnStartRecording != null) {
                btnStartRecording.performClick(); // Trigger the Start Recording button's action
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    private void initializeUiElements() {
        // Recording buttons
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnPauseRecording = findViewById(R.id.btn_pause_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        
        // Recording indicator
        recordingIndicator = findViewById(R.id.recording_indicator);
        recordingTime = findViewById(R.id.recording_time);
        
        // Whisper section
        whisperText = findViewById(R.id.whisper_text);
        btnSendWhisper = findViewById(R.id.btn_send_whisper);
        // btnClearRecording = findViewById(R.id.btn_clear_recording); // Removed
        btnClearAllWhisperIcon = findViewById(R.id.btn_clear_all_whisper_icon); // Added
        btnClearTranscriptionIcon = findViewById(R.id.btn_clear_transcription_icon); // Initialize new ImageButton
        btnShareWhisperText = findViewById(R.id.btn_share_whisper_text); // Initialize Share Whisper Button
        chkAutoSendWhisper = findViewById(R.id.chk_auto_send_whisper);
        
        // ChatGPT section
        chatGptText = findViewById(R.id.chatgpt_text);
        btnSendChatGpt = findViewById(R.id.btn_send_chatgpt);
        btnClearChatGptIcon = findViewById(R.id.btn_clear_chatgpt_icon); // Initialize new ImageButton
        btnShareChatGptText = findViewById(R.id.btn_share_chatgpt_text); // Initialize Share ChatGPT Button
        chkAutoSendToChatGpt = findViewById(R.id.chk_auto_send_to_chatgpt);
        
        // InputStick section
        btnSendInputStick = findViewById(R.id.btn_send_inputstick);
        chkAutoSendInputStick = findViewById(R.id.chk_auto_send_inputstick);
        btnSendWhisperToInputStick = findViewById(R.id.btn_send_whisper_to_inputstick);
        chk_auto_send_whisper_to_inputstick = findViewById(R.id.chk_auto_send_whisper_to_inputstick);
        // btnClearAll = findViewById(R.id.btn_clear_all); // Removed

        activePromptsDisplay = findViewById(R.id.active_prompts_display); // ADD THIS
        activePromptsDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
            // Determine current mode to pass as filter
            String currentMode = sharedPreferences.getString(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE, "two_step_transcription");
            if ("one_step_transcription".equals(currentMode)) {
                intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "one_step");
            } else { // "two_step_transcription"
                intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "two_step_processing");
            }
            startActivity(intent);
        });

        // Set up click listeners
        // setupClickListeners(); // Moved down
        
        // Set initial checkbox states from preferences
        // chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true)); // Moved down
        // chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false)); // Moved down
        // chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false)); // Moved down
        // chk_auto_send_whisper_to_inputstick.setChecked(sharedPreferences.getBoolean("auto_send_whisper_to_inputstick", false)); // Moved down
        // btnRefreshStatus = findViewById(R.id.btn_refresh_status); // Removed
        whisperSectionContainer = findViewById(R.id.whisper_section_container); // Added

        setupClickListeners(); // Moved to after all findViewById calls
        
        // Set initial checkbox states from preferences (after they are all initialized)
        chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true));
        chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false));
        chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false));
        chk_auto_send_whisper_to_inputstick.setChecked(sharedPreferences.getBoolean("auto_send_whisper_to_inputstick", false));

        progressBarWhisper = findViewById(R.id.progressBarWhisper); // Added
        textViewWhisperStatus = findViewById(R.id.textViewWhisperStatus); // Added
        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE); // Added
        if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE); // Added

        progressBarChatGpt = findViewById(R.id.progressBarChatGpt); // Added
        textViewChatGptStatus = findViewById(R.id.textViewChatGptStatus); // Added
        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE); // Added
        if (textViewChatGptStatus != null) textViewChatGptStatus.setVisibility(View.GONE); // Added

        // Listener to improve EditText scrolling within ScrollView
        View.OnTouchListener editTextTouchListener = (v, event) -> {
            // Check if the view is an EditText and can scroll vertically
            if (v.isFocusable() && v instanceof EditText) {
                // Check if the EditText can scroll up or down
                // (1 for down, -1 for up)
                boolean canScrollVertically = v.canScrollVertically(1) || v.canScrollVertically(-1);

                if (canScrollVertically) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Disallow the ScrollView to intercept touch events
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // Allow the ScrollView to intercept touch events again
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                }
            }
            // Return false so that the event is not consumed and other
            // touch listeners or default behavior (like cursor placement) still work.
            return false;
        };

        whisperText.setOnTouchListener(editTextTouchListener);
        chatGptText.setOnTouchListener(editTextTouchListener);

        // Listeners for opening full-screen editor
        whisperText.setOnClickListener(v -> {
            currentEditingEditText = whisperText;
            FullScreenEditTextDialogFragment dialogFragment = FullScreenEditTextDialogFragment.newInstance(whisperText.getText().toString());
            dialogFragment.show(getSupportFragmentManager(), "edit_whisper_text_dialog");
        });

        chatGptText.setOnClickListener(v -> {
            currentEditingEditText = chatGptText;
            FullScreenEditTextDialogFragment dialogFragment = FullScreenEditTextDialogFragment.newInstance(chatGptText.getText().toString());
            dialogFragment.show(getSupportFragmentManager(), "edit_chatgpt_text_dialog");
        });
    }
    
    private void initializeApis() {
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        String whisperEndpoint = sharedPreferences.getString("whisper_endpoint", 
                "https://api.openai.com"); // No trailing slash
        String model = sharedPreferences.getString("chatgpt_model", "gpt-3.5-turbo");
        String language = sharedPreferences.getString("language", "en");
        
        whisperApi = new WhisperApi(apiKey, whisperEndpoint, language);
        chatGptApi = new ChatGptApi(apiKey, model);
        inputStickManager = new InputStickManager(this); // Reinstated
    }
    
    private void setupClickListeners() {
        btnStartRecording.setOnClickListener(v -> startRecording());
        btnPauseRecording.setOnClickListener(v -> pauseRecording());
        btnStopRecording.setOnClickListener(v -> stopRecording());
        
        btnSendWhisper.setOnClickListener(v -> {
            showAudioTranscriptionProgressUI(); // Added
            transcribeAudio();
        });
        // btnClearRecording.setOnClickListener(v -> clearRecording()); // Removed
        // btnClearTranscription.setOnClickListener(v -> clearTranscription()); // Removed old listener

        if (btnClearTranscriptionIcon != null) {
            btnClearTranscriptionIcon.setOnClickListener(v -> clearTranscription());
        }

        // if (btnRefreshStatus != null) { // Removed
        //     btnRefreshStatus.setOnClickListener(v -> refreshTranscriptionStatus(true)); // true for user initiated
        // }
        
        btnSendChatGpt.setOnClickListener(v -> sendToChatGpt());
        // btnClearChatGpt.setOnClickListener(v -> clearChatGptResponse()); // Removed old listener

        if (btnClearChatGptIcon != null) {
            btnClearChatGptIcon.setOnClickListener(v -> clearChatGptResponse());
        }
        
        btnSendInputStick.setOnClickListener(v -> sendToInputStick());
        
        chkAutoSendWhisper.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_send_whisper", isChecked).apply();
        });
        
        chkAutoSendInputStick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_send_inputstick", isChecked).apply();
        });
        
        chkAutoSendToChatGpt.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auto_send_to_chatgpt", isChecked).apply();
        });
        btnSendWhisperToInputStick.setOnClickListener(v -> sendWhisperToInputStick()); // Added
        chk_auto_send_whisper_to_inputstick.setOnCheckedChangeListener((buttonView, isChecked) -> { // Added
            sharedPreferences.edit().putBoolean("auto_send_whisper_to_inputstick", isChecked).apply();
        });

        // Listener for the new ImageButton btnClearAllWhisperIcon
        if (btnClearAllWhisperIcon != null) {
            btnClearAllWhisperIcon.setOnClickListener(v -> {
                clearRecording(); // Clears the audio file and recording duration

                if (whisperText != null) {
                    whisperText.setText("");
                }

                if (chatGptText != null) {
                    chatGptText.setText("");
                }
                // Also reset ChatGPT progress UI
                if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                if (textViewChatGptStatus != null) textViewChatGptStatus.setVisibility(View.GONE);
                if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                
                Toast.makeText(MainActivity.this, "All fields cleared", Toast.LENGTH_SHORT).show();
            });
        }
        // The old btnClearAll listener is removed by not including it here.

        if (btnShareWhisperText != null) {
            btnShareWhisperText.setOnClickListener(v -> {
                String textToShare = whisperText.getText().toString();
                if (!textToShare.isEmpty() && !isPlaceholderOrError(textToShare)) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.main_share_chooser_title_text)));
                } else {
                    Toast.makeText(MainActivity.this, "No valid content to share", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnShareChatGptText != null) {
            btnShareChatGptText.setOnClickListener(v -> {
                String textToShare = chatGptText.getText().toString();
                if (!textToShare.isEmpty()) { // ChatGPT text is less likely to be a placeholder from the app
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.main_share_chooser_title_text)));
                } else {
                    Toast.makeText(MainActivity.this, "No content to share", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private boolean isPlaceholderOrError(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // Check against the specific placeholder string
        if (TRANSCRIPTION_QUEUED_PLACEHOLDER.equals(text)) {
            return true;
        }
        // Check for bracketed status messages like "[UPLOADING... Tap to refresh]"
        if (text.startsWith("[") && text.endsWith("... Tap to refresh]")) {
            return true;
        }
        // Check for common failure keywords
        if (text.toLowerCase().contains("failed") || text.toLowerCase().contains("error")) {
            return true;
        }
        return false;
    }
    
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    private class TranscriptionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "TranscriptionBroadcastReceiver: onReceive triggered.");
            String action = intent.getAction();
            if (UploadService.ACTION_TRANSCRIPTION_COMPLETE.equals(action)) {
                String receivedFilePath = intent.getStringExtra(UploadService.EXTRA_FILE_PATH);
                String transcriptionResult = intent.getStringExtra(UploadService.EXTRA_TRANSCRIPTION_RESULT);
                long taskId = intent.getLongExtra(UploadService.EXTRA_TASK_ID_LONG, -1); // Get task ID

                Log.d(TAG, "Received ACTION_TRANSCRIPTION_COMPLETE for task ID: " + taskId + ", file: " + receivedFilePath);
                Log.d(TAG, "Current audioFilePath in MainActivity: " + MainActivity.this.audioFilePath); // Log current path for comparison

                // Ensure whisperText is not null (it should be initialized in initializeUiElements)
                if (whisperText == null) {
                    Log.e(TAG, "whisperText EditText is null in BroadcastReceiver. Cannot update UI.");
                    return;
                }

                // Compare with the audioFilePath that triggered the transcription in *this* MainActivity instance
                if (receivedFilePath != null && receivedFilePath.equals(MainActivity.this.audioFilePath)) {
                    if (transcriptionResult != null) { // Success
                        if (whisperText != null) whisperText.setText(transcriptionResult);
                        Log.i(TAG, "Whisper transcription updated via broadcast for task ID: " + taskId);

                        if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
                        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                        if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);

                        // Optional: Auto-send to ChatGPT or InputStick if checked
                        if (chkAutoSendToChatGpt.isChecked()) {
                            Log.d(TAG, "Auto-sending to ChatGPT from broadcast receiver.");
                            sendToChatGpt();
                        }
                        if (chk_auto_send_whisper_to_inputstick.isChecked()) {
                            Log.d(TAG, "Auto-sending Whisper text to InputStick from broadcast receiver.");
                            sendWhisperToInputStick();
                        }
                    } else { // Failure (transcriptionResult is null)
                        Log.w(TAG, "Received null transcription result for matched file path: " + receivedFilePath);
                        // String errorMessage = intent.getStringExtra(UploadService.EXTRA_ERROR_MESSAGE); // Check if service sends specific error
                        String errorMessage = getString(R.string.transcription_failed_placeholder); // Default to placeholder
                        // if (errorMessage == null || errorMessage.isEmpty()) { // This check is now redundant if we always default above
                        //     errorMessage = getString(R.string.transcription_failed_placeholder);
                        // }
                        if (textViewWhisperStatus != null) {
                            textViewWhisperStatus.setText(errorMessage);
                            textViewWhisperStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                        if (whisperText != null) whisperText.setText(""); // Clear text area
                        if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                    }
                } else {
                    Log.d(TAG, "Received transcription for a different/unknown file path. Current: " + MainActivity.this.audioFilePath + ", Received: " + receivedFilePath + ". No UI update for whisperText.");
                }
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.recording_permission_required, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void startRecording() {
        if (isRecording && isPaused) {
            resumeRecording();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        
        try {
            int sampleRate = 16000;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
                    channelConfig, audioFormat, bufferSize);
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Toast.makeText(this, "AudioRecord init failed", Toast.LENGTH_SHORT).show();
                return;
            }

            audioRecord.startRecording();
            recordingThreadRunning = true;
            recordingThread = new Thread(() -> {
                try (FileOutputStream os = new FileOutputStream(pcmFilePath)) {
                    byte[] buffer = new byte[bufferSize];
                    while (recordingThreadRunning) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            os.write(buffer, 0, read);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing PCM data", e);
                }
            });
            recordingThread.start();
            Log.i(TAG, "Recording PCM to " + pcmFilePath);
            
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            Log.d(TAG, "Screen orientation locked due to recording start.");
            
            startTimer();
            updateUiForRecording(true);
            
            Log.d(TAG, "Recording started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, getString(R.string.error_recording), Toast.LENGTH_SHORT).show();
            isRecording = false;
            isPaused = false;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Log.d(TAG, "Screen orientation unlocked due to recording start failure.");
        }
    }
    
    private void pauseRecording() {
        if (!isRecording || isPaused) return;
        
        try {
            recordingThreadRunning = false;
            if (recordingThread != null) {
                recordingThread.join();
            }
            if (audioRecord != null) {
                audioRecord.stop();
            }
            isPaused = true;
            recordingDuration += System.currentTimeMillis() - recordingStartTime;
            stopTimer();
            updateUiForPausedRecording();
            Log.d(TAG, "Recording paused (orientation remains locked).");
        } catch (InterruptedException e) {
            Log.e(TAG, "Error pausing recording", e);
        }
    }
    
    private void resumeRecording() {
        if (!isRecording || !isPaused) return;
        
        try {
            int sampleRate = 16000;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            audioRecord.startRecording();
            recordingThreadRunning = true;
            recordingThread = new Thread(() -> {
                try (FileOutputStream os = new FileOutputStream(pcmFilePath, true)) {
                    byte[] buffer = new byte[bufferSize];
                    while (recordingThreadRunning) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            os.write(buffer, 0, read);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error writing PCM data", e);
                }
            });
            recordingThread.start();

            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            Log.d(TAG, "Screen orientation locked due to recording resume.");
            startTimer();
            updateUiForRecording(true);
            Log.d(TAG, "Recording resumed");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error resuming recording", e);
        }
    }
    
    private void stopRecording() {
        if (!isRecording) return;
        
        try {
            stopTimer();
            recordingThreadRunning = false;
            if (recordingThread != null) {
                try {
                    recordingThread.join();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted while stopping recording thread", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                }
            }
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            
            isRecording = false;
            isPaused = false;
            recordingDuration += System.currentTimeMillis() - recordingStartTime;
            
            updateUiForRecording(false);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Log.d(TAG, "Screen orientation unlocked due to recording stop.");
            
            Log.d(TAG, "Recording stopped, duration: " + recordingDuration + "ms");

            String transcriptionMode = sharedPreferences.getString(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE, "two_step_transcription");
            if (transcriptionMode.equals("one_step_transcription")) {
                String converted = convertToMp3(new File(pcmFilePath));
                if (converted != null) {
                    lastRecordedAudioPathForChatGPTDirect = converted;
                    audioFilePath = converted;
                    if (chkAutoSendToChatGpt.isChecked()) { // This checkbox now implies auto-send for one-step
                        transcribeAudioWithChatGpt();
                    }
                } else {
                    Toast.makeText(this, "Failed to convert recording to MP3 for one-step.", Toast.LENGTH_LONG).show();
                }
            } else { // "two_step_transcription" mode
                String step1Engine = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_ENGINE, "whisper");
                Log.d(TAG, "Two Step Mode - Step 1 Engine: " + step1Engine);

                if ("chatgpt".equals(step1Engine)) { // This now means "OpenAI API for Transcription"
                    String step1ModelName = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP1_CHATGPT_MODEL, "whisper-1");
                    // Fallback for invalid chat model is removed as this preference now points to transcription models.

                    List<Prompt> activePromptsForStep1 = promptManager.getPromptsForMode("two_step_processing").stream()
                                                             .filter(Prompt::isActive)
                                                             .collect(Collectors.toList());
                    String transcriptionHint = "";
                    if (!activePromptsForStep1.isEmpty()) {
                        String hint = activePromptsForStep1.get(0).getTranscriptionHint();
                        if (hint != null && !hint.trim().isEmpty()) {
                            transcriptionHint = hint.trim();
                        }
                    }
                    Log.d(TAG, "Two Step (Step 1 - OpenAI Transcription): Using model: " + step1ModelName + ", Hint: '" + transcriptionHint + "'");

                    String convertedFilePath = convertToMp3(new File(pcmFilePath));
                    if (convertedFilePath != null) {
                        audioFilePath = convertedFilePath; // Keep this to ensure broadcast receiver can match
                        // lastRecordedAudioPathForChatGPTDirect = convertedFilePath; // This might not be needed anymore

                        // Create UploadTask with the specific model and hint using factory method
                        UploadTask uploadTask = UploadTask.createAudioTranscriptionTask(
                            audioFilePath,
                            step1ModelName, // modelNameForTranscription
                            transcriptionHint // transcriptionHint
                        );

                        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
                        // Make variables final for use in lambda
                        final String finalStep1ModelName = step1ModelName;
                        final String finalTranscriptionHint = transcriptionHint;
                        final String finalAudioFilePath = audioFilePath;
                        final UploadTask finalUploadTask = uploadTask;

                        Executors.newSingleThreadExecutor().execute(() -> {
                            database.uploadTaskDao().insert(finalUploadTask);
                            Log.d(TAG, "Two Step (Step 1 - OpenAI Transcription): Queued UploadTask ID: " + finalUploadTask.id +
                                       " with model: " + finalStep1ModelName + " and hint: '" + finalTranscriptionHint + "'");
                            AppLogManager.getInstance().addEntry("INFO", TAG + ": OpenAI Transcription task queued in DB.",
                                                               "File: " + finalAudioFilePath + ", Model: " + finalStep1ModelName);
                            UploadService.startUploadService(MainActivity.this);
                        });

                        showAudioTranscriptionProgressUI(); // Show "Queued..." UI, same as default Whisper path

                    } else {
                        Toast.makeText(this, "Failed to convert recording to MP3 for two-step (OpenAI Transcription).", Toast.LENGTH_LONG).show();
                    }
                } else { // Default to Whisper (UploadService) for Step 1
                    String converted = convertToMp3(new File(pcmFilePath));
                    if (converted != null) {
                        audioFilePath = converted;
                        if (chkAutoSendWhisper.isChecked()) {
                            showAudioTranscriptionProgressUI(); // Added
                            transcribeAudio(); // Queues via UploadService
                        }
                    } else {
                        Toast.makeText(this, "Failed to convert recording to MP3 for two-step (Whisper).", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping recording", e);
            Toast.makeText(this, R.string.error_recording, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startTimer() {
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdownNow();
        }
        
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
        timerExecutor.scheduleAtFixedRate(() -> {
            long elapsedTime = recordingDuration + (System.currentTimeMillis() - recordingStartTime);
            updateRecordingTimeUi(elapsedTime);
        }, 0, 100, TimeUnit.MILLISECONDS);
    }
    
    private void stopTimer() {
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdownNow();
            timerExecutor = null;
        }
    }

    private String convertToMp3(File inputFile) {

        int sampleRate = 16000;
        int channel = 1;
        int bitrate = 96;
        byte[] buffer = new byte[1024];
        byte[] mp3buffer = new byte[1024];

        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(mp3FilePath)) {
            LameControl.init(sampleRate, channel, sampleRate, bitrate, 7);
            int read;
            short[] shortBuffer = new short[buffer.length / 2];
            while ((read = fis.read(buffer)) > 0) {
                for (int i = 0; i < read / 2; i++) {
                    shortBuffer[i] = (short) ((buffer[2 * i] & 0xff) | (buffer[2 * i + 1] << 8));
                }
                int encoded = LameControl.encode(shortBuffer, shortBuffer, read / 2, mp3buffer);
                if (encoded > 0) {
                    fos.write(mp3buffer, 0, encoded);
                }
            }
            int flushResult = LameControl.flush(mp3buffer);
            if (flushResult > 0) {
                fos.write(mp3buffer, 0, flushResult);
            }
            LameControl.close();
            Log.i(TAG, "MP3 conversion successful: " + mp3FilePath);
            return mp3FilePath;
        } catch (IOException e) {
            Log.e(TAG, "MP3 conversion failed", e);
            return null;
        }
    }
    
    private void updateRecordingTimeUi(long elapsedTime) {
        final int seconds = (int) (elapsedTime / 1000);
        final int minutes = seconds / 60;
        final int remainingSeconds = seconds % 60;
        
        mainHandler.post(() -> {
            recordingTime.setText(String.format(Locale.getDefault(), 
                    getString(R.string.recording_time), minutes, remainingSeconds));
            
            // Flash the recording indicator
            recordingIndicator.setVisibility(System.currentTimeMillis() % 1000 < 500 ? View.VISIBLE : View.INVISIBLE);
        });
    }
    
    private void updateUiForRecording(boolean isRecording) {
        btnStartRecording.setEnabled(!isRecording);
        btnPauseRecording.setEnabled(isRecording && !isPaused);
        btnStopRecording.setEnabled(isRecording);
        
        if (isRecording) {
            recordingIndicator.setVisibility(View.VISIBLE);
            recordingTime.setVisibility(View.VISIBLE);
        } else {
            recordingIndicator.setVisibility(View.INVISIBLE);
            recordingTime.setVisibility(View.INVISIBLE);
            recordingDuration = 0;
        }
    }
    
    private void updateUiForPausedRecording() {
        btnStartRecording.setEnabled(true);
        btnPauseRecording.setEnabled(false);
        btnStopRecording.setEnabled(true);
        recordingIndicator.setVisibility(View.INVISIBLE);
    }
    
    private void transcribeAudio() {
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            Toast.makeText(this, "No recording available to transcribe", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Queueing transcription...", Toast.LENGTH_SHORT).show(); // Changed message
        AppLogManager.getInstance().addEntry("INFO", "Whisper: Queuing transcription task...", null);

        // Create UploadTask using factory method for default Whisper path
        UploadTask uploadTask = UploadTask.createAudioTranscriptionTask(audioFilePath, "whisper-1", "");

        // Get DAO and insert in background
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        // Using a simple executor for this one-off DB operation.
        // Consider a shared executor if this pattern becomes common.
        Executors.newSingleThreadExecutor().execute(() -> {
            database.uploadTaskDao().insert(uploadTask);
            Log.d(TAG, "New UploadTask inserted with ID: " + uploadTask.id); // ID will be auto-generated
            AppLogManager.getInstance().addEntry("INFO", "Whisper: Transcription task queued in DB.", "File: " + audioFilePath);

            // Start the service to process the queue
            // It's okay to call this multiple times; IntentService handles sequential execution.
            UploadService.startUploadService(MainActivity.this);
        });

        // Update UI (e.g., clear previous transcription or show "queued" status)
        // mainHandler.post(() -> { // UI update logic moved to showAudioTranscriptionProgressUI() and called by callers
        //     // whisperText.setText(TRANSCRIPTION_QUEUED_PLACEHOLDER); // Replaced by new UI
        //     if (whisperText != null) whisperText.setText("");
        //
        //     if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.VISIBLE);
        //     if (textViewWhisperStatus != null) {
        //         textViewWhisperStatus.setVisibility(View.VISIBLE);
        //         textViewWhisperStatus.setText("Queued for transcription...");
        //     }
        //     if (btnSendWhisper != null) btnSendWhisper.setEnabled(false);
        //     // Do NOT automatically call sendToChatGpt or sendWhisperToInputStick here anymore.
        //     // That logic will move to when the task is actually completed by the service.
        // });
        Log.i(TAG, "MainActivity.transcribeAudio: Queued recording for Whisper transcription via UploadService.");
    }

    private void showAudioTranscriptionProgressUI() {
        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.VISIBLE);
        if (textViewWhisperStatus != null) {
            textViewWhisperStatus.setVisibility(View.VISIBLE);
            textViewWhisperStatus.setText("Queued for transcription...");
        }
        if (btnSendWhisper != null) btnSendWhisper.setEnabled(false);
        if (whisperText != null) whisperText.setText("");
    }

    private void showChatGptProgressUI(String message) {
        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.VISIBLE);
        if (textViewChatGptStatus != null) {
            textViewChatGptStatus.setVisibility(View.VISIBLE);
            textViewChatGptStatus.setText(message);
        }
        if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(false);
        // Optional: Clear chatGptText here if desired for all progress states.
        // if (chatGptText != null) chatGptText.setText("");
    }

    private void refreshTranscriptionStatus(boolean userInitiated) {
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            if (userInitiated) Toast.makeText(this, "No active recording session to check.", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            // We are interested in the task for the current audioFilePath, ordered by creation time descending to get the latest.
            // This assumes audioFilePath is unique per recording session before it's transcribed or discarded.
            // If multiple tasks could exist for the same file path, more specific logic might be needed.
            List<UploadTask> tasks = database.uploadTaskDao().getTasksByFilePath(audioFilePath); // Assumes getTasksByFilePath method exists

            mainHandler.post(() -> {
                if (tasks != null && !tasks.isEmpty()) {
                    UploadTask latestTaskForFile = null;
                    // Find the most recent task for this file path
                    for(UploadTask task : tasks) {
                        if (task.filePath.equals(audioFilePath)) {
                            if (latestTaskForFile == null || task.creationTimestamp > latestTaskForFile.creationTimestamp) {
                                latestTaskForFile = task;
                            }
                        }
                    }

                    if (latestTaskForFile != null) {
                        Log.d(TAG, "Refresh found task ID " + latestTaskForFile.id + " with status: " + latestTaskForFile.status + " for path: " + audioFilePath);
                        String status = latestTaskForFile.status;
                        if (status == null) status = UploadTask.STATUS_PENDING; // Default if somehow null

                        switch (status) {
                            case UploadTask.STATUS_SUCCESS:
                                if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                                if (whisperText != null) whisperText.setText(latestTaskForFile.transcriptionResult);
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                                if (userInitiated) Toast.makeText(MainActivity.this, "Transcription loaded.", Toast.LENGTH_SHORT).show();
                                // Auto-send logic
                                if (chkAutoSendToChatGpt.isChecked()) sendToChatGpt();
                                if (chk_auto_send_whisper_to_inputstick.isChecked()) sendWhisperToInputStick();
                                break;
                            case UploadTask.STATUS_FAILED:
                                String errorMsg = "Transcription failed: " + latestTaskForFile.errorMessage;
                                if (textViewWhisperStatus != null) {
                                    textViewWhisperStatus.setText(errorMsg);
                                    textViewWhisperStatus.setVisibility(View.VISIBLE);
                                }
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                                if (whisperText != null) whisperText.setText(""); // Clear text area
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                                if (userInitiated) Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                break;
                            case UploadTask.STATUS_PENDING:
                                if (textViewWhisperStatus != null) {
                                    textViewWhisperStatus.setText("Transcription queued.");
                                    textViewWhisperStatus.setVisibility(View.VISIBLE);
                                }
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.VISIBLE);
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(false);
                                if (whisperText != null) whisperText.setText("");
                                if (userInitiated) Toast.makeText(MainActivity.this, "Transcription is queued.", Toast.LENGTH_SHORT).show();
                                break;
                            case UploadTask.STATUS_UPLOADING:
                                if (textViewWhisperStatus != null) {
                                    textViewWhisperStatus.setText("Uploading audio...");
                                    textViewWhisperStatus.setVisibility(View.VISIBLE);
                                }
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.VISIBLE);
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(false);
                                if (whisperText != null) whisperText.setText("");
                                if (userInitiated) Toast.makeText(MainActivity.this, "Uploading audio...", Toast.LENGTH_SHORT).show();
                                break;
                            case UploadTask.STATUS_PROCESSING:
                                if (textViewWhisperStatus != null) {
                                    textViewWhisperStatus.setText("Processing transcription...");
                                    textViewWhisperStatus.setVisibility(View.VISIBLE);
                                }
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.VISIBLE);
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(false);
                                if (whisperText != null) whisperText.setText("");
                                if (userInitiated) Toast.makeText(MainActivity.this, "Processing transcription...", Toast.LENGTH_SHORT).show();
                                break;
                            default: // Unknown status or no task
                                if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
                                if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                                if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                                if (userInitiated) Toast.makeText(MainActivity.this, "Task status: " + latestTaskForFile.status, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    } else { // No specific task found for this audio file
                        if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
                        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                        if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                        if (userInitiated) Toast.makeText(MainActivity.this, "No transcription task found for the last recording.", Toast.LENGTH_SHORT).show();
                    }
                } else { // No tasks at all in the DB for this file path
                    if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
                    if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
                    if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
                    if (userInitiated) Toast.makeText(MainActivity.this, "No transcription tasks found in queue for this file.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void clearRecording() {
        File pcmFile = new File(pcmFilePath);
        if (pcmFile.exists()) {
            pcmFile.delete();
        }
        File mp3File = new File(mp3FilePath);
        if (mp3File.exists()) {
            mp3File.delete();
        }
        recordingDuration = 0;
        lastRecordedAudioPathForChatGPTDirect = null; // Added
        Toast.makeText(this, "Recording cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void clearTranscription() {
        if (whisperText != null) whisperText.setText("");
        clearRecording(); // This handles deleting audio files and resetting recording duration.

        // Reset Whisper specific UI elements
        if (progressBarWhisper != null) progressBarWhisper.setVisibility(View.GONE);
        if (textViewWhisperStatus != null) textViewWhisperStatus.setVisibility(View.GONE);
        if (btnSendWhisper != null) btnSendWhisper.setEnabled(true);
        // No need for a toast here as this is often called as part of a larger clear operation.
    }
    
    private void transcribeAudioWithChatGpt() {
        if (lastRecordedAudioPathForChatGPTDirect == null || lastRecordedAudioPathForChatGPTDirect.isEmpty()) {
            mainHandler.post(() -> Toast.makeText(MainActivity.this, "No audio recorded for direct processing.", Toast.LENGTH_SHORT).show());
            AppLogManager.getInstance().addEntry("WARN", TAG + ": transcribeAudioWithChatGpt called with no lastRecordedAudioPathForChatGPTDirect.", null);
            return;
        }

        final File currentAudioFile = new File(lastRecordedAudioPathForChatGPTDirect);
        if (!currentAudioFile.exists() || currentAudioFile.length() == 0) {
            mainHandler.post(() -> {
                Toast.makeText(MainActivity.this, "Audio file missing or empty for direct processing.", Toast.LENGTH_SHORT).show();
                chatGptText.setText("[DIRECT_MODE: Error - Audio file missing or empty]");
            });
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": DIRECT_MODE: Audio file missing or empty.", "Path: " + lastRecordedAudioPathForChatGPTDirect);
            return;
        }

        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            mainHandler.post(() -> {
                Toast.makeText(MainActivity.this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
                chatGptText.setText("[DIRECT_MODE: Error - API Key not set]");
            });
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": DIRECT_MODE: OpenAI API Key is not set.", null);
            return;
        }

        List<Prompt> activePrompts = promptManager.getPromptsForMode("one_step").stream().filter(Prompt::isActive).collect(Collectors.toList());
        String userPrompt = activePrompts.stream().map(Prompt::getText).collect(Collectors.joining("\n\n"));
        if (userPrompt.isEmpty()) {
            userPrompt = "Please transcribe the audio file.  Do not add anything else before or after the transcribed text."; // Default prompt
        }
        final String finalUserPrompt = userPrompt;
        final String modelName = sharedPreferences.getString(SettingsActivity.PREF_KEY_ONESTEP_PROCESSING_MODEL, "gpt-4o"); // Default to gpt-4o or another suitable model

        // mainHandler.post(() -> { // Removed old UI update
        //     chatGptText.setText("[DIRECT_MODE: Sending MP3 audio & prompt to " + modelName + "...]");
        //     Toast.makeText(MainActivity.this, "DIRECT_MODE: Processing with " + modelName + "...", Toast.LENGTH_SHORT).show();
        // });
        showChatGptProgressUI("Processing with " + modelName + "..."); // New UI update
        if (chatGptText != null) chatGptText.setText(""); // Clear previous text

        AppLogManager.getInstance().addEntry("INFO", TAG + ": DIRECT_MODE: Calling getCompletionFromAudioAndPrompt with MP3.", "Model: " + modelName + ", Audio: " + currentAudioFile.getName());

        new Thread(() -> {
            try {
                final String result = chatGptApi.getCompletionFromAudioAndPrompt(currentAudioFile, finalUserPrompt, modelName);
                AppLogManager.getInstance().addEntry("SUCCESS", TAG + ": DIRECT_MODE: Response from " + modelName + " received.", "Output Length: " + (result != null ? result.length() : "null"));
                mainHandler.post(() -> {
                    if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                    if (textViewChatGptStatus != null) textViewChatGptStatus.setVisibility(View.GONE); // Or set to "Success"
                    if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                    if (chatGptText != null) chatGptText.setText(result);
                    Toast.makeText(MainActivity.this, "DIRECT_MODE: " + modelName + " processing complete.", Toast.LENGTH_SHORT).show();
                    if (chkAutoSendInputStick.isChecked() && sharedPreferences.getBoolean("inputstick_enabled", true)) {
                        sendToInputStick();
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "DIRECT_MODE: IOException during " + modelName + " processing: " + e.getMessage(), e);
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": DIRECT_MODE: IOException in " + modelName + " processing.", "Error: " + e.getMessage());
                mainHandler.post(() -> {
                    if (textViewChatGptStatus != null) {
                        textViewChatGptStatus.setText("Error: " + e.getMessage());
                        textViewChatGptStatus.setVisibility(View.VISIBLE);
                    }
                    if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                    if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                    if (chatGptText != null) chatGptText.setText("");
                    Toast.makeText(MainActivity.this, "DIRECT_MODE: API Error - " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "DIRECT_MODE: Unexpected error during " + modelName + " processing: " + e.getMessage(), e);
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": DIRECT_MODE: Unexpected error in " + modelName + " processing.", "Error: " + e.toString());
                mainHandler.post(() -> {
                    if (textViewChatGptStatus != null) {
                        textViewChatGptStatus.setText("Unexpected error: " + e.getMessage());
                        textViewChatGptStatus.setVisibility(View.VISIBLE);
                    }
                    if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                    if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                    if (chatGptText != null) chatGptText.setText("");
                    Toast.makeText(MainActivity.this, "DIRECT_MODE: Unexpected error (" + modelName + ")", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void sendToChatGpt() {
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "two_step_transcription");
        Log.i(TAG, "sendToChatGpt called. Mode: " + transcriptionMode);

        if (transcriptionMode.equals("one_step_transcription")) {
            if (lastRecordedAudioPathForChatGPTDirect != null && new File(lastRecordedAudioPathForChatGPTDirect).exists()) {
                Log.d(TAG, "sendToChatGpt (Direct Mode): Calling transcribeAudioWithChatGpt for MP3 file " + lastRecordedAudioPathForChatGPTDirect);
                transcribeAudioWithChatGpt(); // This will use the MP3 and call getCompletionFromAudioAndPrompt
            } else {
                Toast.makeText(this, "Please record audio first for direct processing.", Toast.LENGTH_LONG).show();
                AppLogManager.getInstance().addEntry("WARN", TAG + ": sendToChatGpt (Direct Mode) called but no valid MP3 audio path.", "lastRecordedAudioPathForChatGPTDirect: " + lastRecordedAudioPathForChatGPTDirect);
            }
        } else { // "whisper" mode (two-step: `whisperText` should ideally have transcription from UploadService)
            String transcript = whisperText.getText().toString().trim();
            Log.d(TAG, "sendToChatGpt (Whisper Mode): Text from whisperText: '" + transcript.substring(0, Math.min(transcript.length(), 50)) + "...'");

            if (transcript.isEmpty() || isPlaceholderOrError(transcript)) {
                Toast.makeText(this, "No valid Whisper transcription to send. Please transcribe first or wait for completion.", Toast.LENGTH_LONG).show();
                AppLogManager.getInstance().addEntry("WARN", TAG + ": sendToChatGpt (Whisper Mode) called with no valid transcript.", "Current text: " + transcript);
                return;
            }

            String apiKey = sharedPreferences.getString("openai_api_key", "");
            if (apiKey.isEmpty()) {
                Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": sendToChatGpt (Whisper Mode) - API Key not set.", null);
                return;
            }

        List<Prompt> activePrompts = promptManager.getPromptsForMode("two_step_processing").stream().filter(Prompt::isActive).collect(Collectors.toList());
            String promptsText = activePrompts.stream().map(Prompt::getText).collect(Collectors.joining("\n\n"));
            String finalTextPayload = promptsText.isEmpty() ? transcript : promptsText + "\n\n" + transcript;
            // Use the specific model for Step 2 processing
            String step2ModelName = sharedPreferences.getString(SettingsActivity.PREF_KEY_TWOSTEP_STEP2_PROCESSING_MODEL, "gpt-4o");
            Log.d(TAG, "Using Two Step - Step 2 Processing Model: " + step2ModelName);

            // mainHandler.post(() -> { // Old UI update removed
            //     chatGptText.setText("[TWO_STEP_MODE: Sending text to " + step2ModelName + "...]");
            //     Toast.makeText(MainActivity.this, "TWO_STEP_MODE: Sending to " + step2ModelName + "...", Toast.LENGTH_SHORT).show();
            // });
            showChatGptProgressUI("Sending to " + step2ModelName + "..."); // New UI update
            if (chatGptText != null) chatGptText.setText(""); // Clear previous response

            AppLogManager.getInstance().addEntry("INFO", TAG + ": TWO_STEP_SEND_TO_CHATGPT", "Model: " + step2ModelName + ", Payload Length: " + finalTextPayload.length());

            new Thread(() -> {
                try {
                    final String result = chatGptApi.getCompletion(finalTextPayload, step2ModelName); // Pass the model
                    AppLogManager.getInstance().addEntry("SUCCESS", TAG + ": TWO_STEP_MODE: Response from " + step2ModelName + " received.", "Output Length: " + (result != null ? result.length() : "null"));
                    mainHandler.post(() -> {
                        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                        if (textViewChatGptStatus != null) textViewChatGptStatus.setVisibility(View.GONE);
                        if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                        if (chatGptText != null) chatGptText.setText(result);
                        Toast.makeText(MainActivity.this, "TWO_STEP_MODE: " + step2ModelName + " processing complete.", Toast.LENGTH_SHORT).show();
                        if (chkAutoSendInputStick.isChecked() && sharedPreferences.getBoolean("inputstick_enabled", true)) {
                            sendToInputStick();
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "TWO_STEP_MODE: IOException during " + step2ModelName + " processing: " + e.getMessage(), e);
                    AppLogManager.getInstance().addEntry("ERROR", TAG + ": TWO_STEP_MODE: IOException in " + step2ModelName + " processing.", "Error: " + e.getMessage());
                    mainHandler.post(() -> {
                        if (textViewChatGptStatus != null) {
                            textViewChatGptStatus.setText("Error: " + e.getMessage());
                            textViewChatGptStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                        if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                        if (chatGptText != null) chatGptText.setText("");
                        Toast.makeText(MainActivity.this, "TWO_STEP_MODE: API Error - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "TWO_STEP_MODE: Unexpected error during " + step2ModelName + " processing: " + e.getMessage(), e);
                    AppLogManager.getInstance().addEntry("ERROR", TAG + ": TWO_STEP_MODE: Unexpected error in " + step2ModelName + " processing.", "Error: " + e.toString());
                    mainHandler.post(() -> {
                        if (textViewChatGptStatus != null) {
                            textViewChatGptStatus.setText("Unexpected error: " + e.getMessage());
                            textViewChatGptStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
                        if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
                        if (chatGptText != null) chatGptText.setText("");
                        Toast.makeText(MainActivity.this, "TWO_STEP_MODE: Unexpected error (" + step2ModelName + ")", Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }
    }
    
    private void clearChatGptResponse() {
        if (chatGptText != null) chatGptText.setText("");
        if (progressBarChatGpt != null) progressBarChatGpt.setVisibility(View.GONE);
        if (textViewChatGptStatus != null) textViewChatGptStatus.setVisibility(View.GONE);
        if (btnSendChatGpt != null) btnSendChatGpt.setEnabled(true);
    }
    
    private void sendToInputStick() {
        if (!sharedPreferences.getBoolean("inputstick_enabled", true)) {
            Toast.makeText(this, "InputStick is disabled in settings", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String text = chatGptText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "No text to send to InputStick", Toast.LENGTH_SHORT).show(); // Clarified message
            return;
        }

        // Check if InputStickUtility is installed and supported
        if (InputStickBroadcast.isSupported(this, true)) { // No need for fully qualified name if InputStickBroadcast is imported
            if (inputStickManager != null) {
                inputStickManager.typeText(text); // Use InputStickManager
                Toast.makeText(MainActivity.this, "Text sent via InputStickManager", Toast.LENGTH_SHORT).show(); // Updated Toast
                AppLogManager.getInstance().addEntry("INFO", "InputStick: Text sent via InputStickManager", "Length: " + text.length());
            } else {
                // Fallback or error if inputStickManager is unexpectedly null
                Log.e(TAG, "InputStickManager is null. Falling back to direct broadcast.");
                AppLogManager.getInstance().addEntry("ERROR", "InputStick: InputStickManager is null. Falling back to direct broadcast.", null);
                InputStickBroadcast.type(this, text, "en-US");
                Toast.makeText(MainActivity.this, "Text sent (fallback direct broadcast)", Toast.LENGTH_SHORT).show();
            }
        } else {
            // isSupported() already shows a dialog if not installed/updated.
            AppLogManager.getInstance().addEntry("WARN", "InputStick: Utility not supported or user cancelled download.", null);
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            // Already on home, just close drawer
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_view_logs) {
            Intent intent = new Intent(this, com.drgraff.speakkey.utils.LogActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_prompts) { // Make sure R.id.nav_prompts matches the ID in menu_drawer.xml
            Intent intent = new Intent(this, com.drgraff.speakkey.data.PromptsActivity.class); // Corrected: This was already correct.
            startActivity(intent);
        } else if (id == R.id.nav_macros) {
            Intent intent = new Intent(this, MacroListActivity.class); // This line remains as is, assuming MacroListActivity is still the target for "Macros"
            startActivity(intent);
        } else if (id == R.id.nav_formatting_tags) { // New block
            Intent intent = new Intent(this, com.drgraff.speakkey.formattingtags.FormattingTagsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_photos) {
            Intent intent = new Intent(this, PhotosActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_about) { // ADD THIS BLOCK
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        if (mAppliedThemeMode != null && sharedPreferences != null) {
            boolean needsRecreate = false;
            String currentThemeValue = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);

            if (!mAppliedThemeMode.equals(currentThemeValue)) {
                needsRecreate = true;
                Log.d(TAG, "onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue);
            } else if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
                // Theme mode is still OLED, check individual OLED colors
                int currentTopbarBG = sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
                if (mAppliedTopbarBackgroundColor != currentTopbarBG) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Topbar BG changed. Old=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) + ", New=0x" + Integer.toHexString(currentTopbarBG));
                }

                int currentTopbarTextIcon = sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
                if (mAppliedTopbarTextIconColor != currentTopbarTextIcon) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Topbar Text/Icon changed. Old=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) + ", New=0x" + Integer.toHexString(currentTopbarTextIcon));
                }

                int currentMainBG = sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
                if (mAppliedMainBackgroundColor != currentMainBG) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Main BG changed. Old=0x" + Integer.toHexString(mAppliedMainBackgroundColor) + ", New=0x" + Integer.toHexString(currentMainBG));
                }

                int currentTextboxBG = sharedPreferences.getInt("pref_oled_textbox_background", DynamicThemeApplicator.DEFAULT_OLED_TEXTBOX_BACKGROUND);
                if (mAppliedTextboxBackgroundColor != currentTextboxBG) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Textbox BG changed. Old=0x" + Integer.toHexString(mAppliedTextboxBackgroundColor) + ", New=0x" + Integer.toHexString(currentTextboxBG));
                }

                int currentButtonBG = sharedPreferences.getInt("pref_oled_button_background", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND);
                if (mAppliedOledButtonBackgroundColor != currentButtonBG) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Button BG changed. Old=0x" + Integer.toHexString(mAppliedOledButtonBackgroundColor) + ", New=0x" + Integer.toHexString(currentButtonBG));
                }

                int currentButtonTextIcon = sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON);
                if (mAppliedOledButtonTextIconColor != currentButtonTextIcon) {
                    needsRecreate = true;
                    Log.d(TAG, "onResume: OLED Button Text/Icon changed. Old=0x" + Integer.toHexString(mAppliedOledButtonTextIconColor) + ", New=0x" + Integer.toHexString(currentButtonTextIcon));
                }
            }

            if (needsRecreate) {
                Log.d(TAG, "onResume: Detected configuration change. Recreating MainActivity.");
                recreate();
                return;
            }
        }

        // If not recreated, proceed with normal onResume
        ThemeManager.applyTheme(sharedPreferences);

        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        IntentFilter filter = new IntentFilter(UploadService.ACTION_TRANSCRIPTION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(transcriptionReceiver, filter);
        Log.d(TAG, "TranscriptionBroadcastReceiver registered.");
        
        // Refresh settings in case they were changed
        initializeApis(); // Refreshes API keys etc.
        chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true));
        chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false));
        chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false));
        chk_auto_send_whisper_to_inputstick.setChecked(sharedPreferences.getBoolean("auto_send_whisper_to_inputstick", false)); // Added

        // Refresh active macros
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper"); // Added
        updateUiForTranscriptionMode(transcriptionMode); // Added
        updateActivePromptsDisplay(); // Ensure this is called AFTER updateUiForTranscriptionMode
        displayActiveMacros(); // Moved here
        refreshTranscriptionStatus(false); // false because it's an automatic refresh onResume
        Log.d(TAG, "onResume: All UI setup calls complete in onResume.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sharedPreferences != null) { // Good practice to check
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(transcriptionReceiver);
        Log.d(TAG, "TranscriptionBroadcastReceiver unregistered.");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
            Log.d(TAG, "Main theme preference changed (dark_mode). Recreating MainActivity.");
            recreate();
        } else if (isOledColorKey) {
            // If an OLED color key changed, check if the current theme is OLED before recreating.
            // The sharedPreferences parameter here is the one passed to this method,
            // which should be up-to-date.
            String currentTheme = sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "OLED color preference changed: " + key + ". Recreating MainActivity.");
                recreate();
            }
        }
    }

    private void updateActivePromptsDisplay() {
        if (promptManager == null) {
            promptManager = new PromptManager(this);
        }
        if (activePromptsDisplay == null) { // Check if initialized
            Log.w(TAG, "activePromptsDisplay is null in updateActivePromptsDisplay.");
            return;
        }

        String transcriptionMode = sharedPreferences.getString(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE, "two_step_transcription");
        List<Prompt> modeSpecificPrompts;
        if ("one_step_transcription".equals(transcriptionMode)) {
            modeSpecificPrompts = promptManager.getPromptsForMode("one_step");
        } else { // two_step_transcription (or any other default)
            modeSpecificPrompts = promptManager.getPromptsForMode("two_step_processing");
        }

        List<Prompt> activeSystemPrompts = modeSpecificPrompts.stream()
                                             .filter(Prompt::isActive)
                                             .collect(Collectors.toList());

        if ("one_step_transcription".equals(transcriptionMode)) {
            if (activeSystemPrompts.isEmpty()) {
                activePromptsDisplay.setText("One Step Transcription"); // This text indicates the mode is active
                activePromptsDisplay.setVisibility(View.VISIBLE);
            } else {
                // Show active "one_step" prompts
                String promptsText = activeSystemPrompts.stream()
                                        .map(prompt -> prompt.getLabel() + (prompt.getText().isEmpty() ? "" : ": \"" + truncate(prompt.getText(), 20) + "\""))
                                        .collect(Collectors.joining(" | "));
                activePromptsDisplay.setText(promptsText);
                activePromptsDisplay.setVisibility(View.VISIBLE);
            }
        } else { // "two_step_transcription" mode
            if (activeSystemPrompts.isEmpty()) {
                // Get the default text from a string resource
                activePromptsDisplay.setText(getString(R.string.default_two_step_prompt_label));
                activePromptsDisplay.setVisibility(View.VISIBLE); // Ensure it's visible
            } else {
                // Show active "two_step_processing" prompts
                String promptsText = activeSystemPrompts.stream()
                                        .map(prompt -> prompt.getLabel() + (prompt.getText().isEmpty() ? "" : ": \"" + truncate(prompt.getText(), 20) + "\""))
                                        .collect(Collectors.joining(" | "));
                activePromptsDisplay.setText(promptsText);
                activePromptsDisplay.setVisibility(View.VISIBLE);
            }
        }
    }

    // Helper method to truncate string, if not already present
    private String truncate(String str, int length) {
        if (str.length() > length) {
            return str.substring(0, length) + "...";
        } else {
            return str;
        }
    }

    private void displayActiveMacros() {
        Log.d(TAG, "displayActiveMacros: Called");
        if (macroRepository == null) {
            Log.e(TAG, "MacroRepository not initialized in displayActiveMacros");
            return;
        }
        LinearLayout activeMacrosRowsContainer = findViewById(R.id.active_macros_rows_container);
        activeMacrosRowsContainer.removeAllViews();

        List<Macro> activeMacros = macroRepository.getActiveMacros();
        Log.d(TAG, "displayActiveMacros: Number of active macros: " + activeMacros.size());
        int macrosPerRow = macroRepository.getMacrosPerRow(2); // Default to 2 macros per row

        if (activeMacros.isEmpty()) {
            activeMacrosRowsContainer.setVisibility(View.GONE);
            Log.d(TAG, "displayActiveMacros: activeMacrosRowsContainer programmatically set to GONE");
            activeMacrosRowsContainer.post(() -> {
                Log.d(TAG, "displayActiveMacros (post-GONE): Container actual getVisibility(): " + activeMacrosRowsContainer.getVisibility());
                Log.d(TAG, "displayActiveMacros (post-GONE): Container actual height: " + activeMacrosRowsContainer.getHeight());
                Log.d(TAG, "displayActiveMacros (post-GONE): Container actual width: " + activeMacrosRowsContainer.getWidth());
            });
            return;
        } else {
            activeMacrosRowsContainer.setVisibility(View.VISIBLE);
            Log.d(TAG, "displayActiveMacros: activeMacrosRowsContainer programmatically set to VISIBLE");
            activeMacrosRowsContainer.post(() -> {
                Log.d(TAG, "displayActiveMacros (post-VISIBLE): Container actual getVisibility(): " + activeMacrosRowsContainer.getVisibility());
                Log.d(TAG, "displayActiveMacros (post-VISIBLE): Container actual height: " + activeMacrosRowsContainer.getHeight());
                Log.d(TAG, "displayActiveMacros (post-VISIBLE): Container actual width: " + activeMacrosRowsContainer.getWidth());
            });
        }

        LinearLayout currentRow = null;
        int marginHorizontalPx = (int) getResources().getDimension(R.dimen.macro_button_margin_horizontal);

        for (int i = 0; i < activeMacros.size(); i++) {
            Macro macro = activeMacros.get(i);
            if (i % macrosPerRow == 0) {
                currentRow = new LinearLayout(this);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                currentRow.setLayoutParams(rowParams);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                activeMacrosRowsContainer.addView(currentRow);
            }

            MaterialButton button = new MaterialButton(this); // Using MaterialButton for consistent styling
            button.setText(macro.getName());
            button.setEllipsize(TextUtils.TruncateAt.END); // Ellipsize long text
            button.setMaxLines(2); // Allow up to 2 lines, then ellipsize
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, // width
                    LinearLayout.LayoutParams.WRAP_CONTENT, // height
                    1.0f // weight
            );
            params.setMargins(marginHorizontalPx, 0, marginHorizontalPx, 0);
            button.setLayoutParams(params);

            button.setOnClickListener(v -> {
                if (!sharedPreferences.getBoolean("inputstick_enabled", true)) {
                    Toast.makeText(MainActivity.this, "InputStick is disabled in settings.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!InputStickBroadcast.isSupported(MainActivity.this, true)) { // true to show dialog if not supported
                    Toast.makeText(MainActivity.this, "InputStick Utility not installed or needs update.", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(MainActivity.this, "Executing: " + macro.getName(), Toast.LENGTH_SHORT).show();
                macroExecutorService.execute(() -> {
                    boolean success = macroExecutor.executeMacro(macro, MainActivity.this);
                    // Optionally, provide feedback on completion
                    mainHandler.post(() -> {
                        if (success) {
                            // Toast.makeText(MainActivity.this, macro.getName() + " finished.", Toast.LENGTH_SHORT).show();
                            AppLogManager.getInstance().addEntry("INFO", "Macro executed: " + macro.getName(), null);
                        } else {
                            // Toast.makeText(MainActivity.this, macro.getName() + " cancelled.", Toast.LENGTH_SHORT).show();
                             AppLogManager.getInstance().addEntry("WARN", "Macro cancelled or failed: " + macro.getName(), null);
                        }
                    });
                });
            });

            if (currentRow != null) {
                currentRow.addView(button);
            }
        }
        // If the last row is not full, add empty views to maintain alignment (optional, depending on desired look)
        if (currentRow != null && activeMacros.size() % macrosPerRow != 0) {
            int remainingSlots = macrosPerRow - (activeMacros.size() % macrosPerRow);
            for (int i = 0; i < remainingSlots; i++) {
                View emptyView = new View(this);
                 LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, // width
                    0, // height  <--- MODIFIED HERE
                    1.0f // weight
                );
                params.setMargins(marginHorizontalPx, 0, marginHorizontalPx, 0);
                emptyView.setLayoutParams(params);
                currentRow.addView(emptyView);
            }
        }

        // Attempt to force WRAP_CONTENT for height after buttons are added
        ViewGroup.LayoutParams params = activeMacrosRowsContainer.getLayoutParams();
        if (params != null) { // Check if params is null, though it shouldn't be for a view in layout
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            activeMacrosRowsContainer.setLayoutParams(params);
            activeMacrosRowsContainer.requestLayout();
            Log.d(TAG, "displayActiveMacros: Programmatically set height to WRAP_CONTENT and requested layout.");
        } else {
            Log.w(TAG, "displayActiveMacros: Could not get LayoutParams for activeMacrosRowsContainer to force WRAP_CONTENT.");
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (isRecording) {
            try {
                recordingThreadRunning = false;
                if (recordingThread != null) {
                    recordingThread.join();
                }
                if (audioRecord != null) {
                    audioRecord.stop();
                    audioRecord.release();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        Log.d(TAG, "Screen orientation unlocked in onDestroy.");
        if (macroExecutorService != null && !macroExecutorService.isShutdown()) {
            macroExecutorService.shutdown();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // Implementation for FullScreenEditTextDialogFragment.OnSaveListener
    @Override
    public void onSave(String editedText) {
        if (currentEditingEditText != null) {
            currentEditingEditText.setText(editedText);
            // Optionally, you can clear the reference if it's no longer needed immediately
            // currentEditingEditText = null; 
        }
    }

    private void sendWhisperToInputStick() {
        if (!sharedPreferences.getBoolean("inputstick_enabled", true)) {
            Toast.makeText(this, "InputStick is disabled in settings", Toast.LENGTH_SHORT).show();
            return;
        }

        String textToSend = whisperText.getText().toString(); // Ensure whisperText is the correct EditText for Whisper transcription
        if (textToSend.isEmpty()) {
            Toast.makeText(this, "No Whisper text to send to InputStick", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if InputStickUtility is installed and supported
        // The true parameter shows a dialog if not installed/updated.
        if (InputStickBroadcast.isSupported(this, true)) { // No need for fully qualified name
            if (inputStickManager != null) {
                inputStickManager.typeText(textToSend); // Use InputStickManager
                Toast.makeText(MainActivity.this, "Whisper text sent via InputStickManager", Toast.LENGTH_SHORT).show(); // Updated Toast
                AppLogManager.getInstance().addEntry("INFO", "InputStick: Whisper text sent via InputStickManager", "Length: " + textToSend.length());
            } else {
                // Fallback or error if inputStickManager is unexpectedly null
                Log.e(TAG, "InputStickManager is null. Falling back to direct broadcast for Whisper text.");
                AppLogManager.getInstance().addEntry("ERROR", "InputStick: InputStickManager is null. Falling back to direct broadcast for Whisper text.", null);
                InputStickBroadcast.type(this, textToSend, "en-US");
                Toast.makeText(MainActivity.this, "Whisper text sent (fallback direct broadcast)", Toast.LENGTH_SHORT).show();
            }
        } else {
            // isSupported() already shows a dialog.
            AppLogManager.getInstance().addEntry("WARN", "InputStick: Utility not supported or user cancelled download for sending Whisper text.", null);
        }
    }
}