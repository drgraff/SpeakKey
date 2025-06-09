package com.drgraff.speakkey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo; // Added
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu; // ADD THIS
import android.view.MenuInflater; // ADD THIS
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton; // Added for ImageButton
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
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService; // Added for Macro Execution
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import android.text.TextUtils; // Added for ellipsize

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FullScreenEditTextDialogFragment.OnSaveListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
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

    // Audio recording
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize settings
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper"); // Added
        
        // Apply the theme before setting content view
        ThemeManager.applyTheme(sharedPreferences);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        macroExecutor = new MacroExecutor(this); // Initialize MacroExecutor
        macroExecutorService = Executors.newSingleThreadExecutor(); // Initialize ExecutorService
        
        // Initialize toolbar and navigation
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 
                R.string.nav_header_desc, R.string.nav_header_desc);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        
        // Initialize UI elements
        initializeUiElements();
        
        // Initialize APIs and MacroRepository
        initializeApis();
        macroRepository = new MacroRepository(getApplicationContext()); // Initialize MacroRepository
        promptManager = new PromptManager(this); // Initialize PromptManager
        
        // Request permissions
        requestPermissions();
        
        // Set up temporary audio file path
        File audioDir = new File(getFilesDir(), "audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        audioFilePath = new File(audioDir, "recording.mp3").getAbsolutePath(); // Changed to .mp3
        Log.i(TAG, "Attempting MP3 recording (AudioEncoder.MP3 API 29+). Path: " + audioFilePath);

        // Display active macros
        displayActiveMacros(); // Call after macroRepository is initialized
        updateUiForTranscriptionMode(transcriptionMode); // Added
    }

    private void updateUiForTranscriptionMode(String mode) {
        if (whisperSectionContainer == null) {
            // This might happen if called before initializeUiElements or if ID is wrong.
            Log.e(TAG, "whisperSectionContainer is null in updateUiForTranscriptionMode. UI update skipped.");
            return;
        }
        if ("chatgpt_direct".equals(mode)) {
            whisperSectionContainer.setVisibility(View.GONE);
            // Adjust hint for chatGptText if needed, e.g.,
            // chatGptText.setHint("ChatGPT Direct Transcription/Response");

            // Update activePromptsDisplay for "Direct Transcription" if no prompts are active
            // This part of the logic will also be handled/refined in updateActivePromptsDisplay()
            // For now, ensure that updateActivePromptsDisplay() is aware of the mode or is called after this.
            if (promptManager != null) { // promptManager should be initialized
                List<Prompt> activePrompts = promptManager.getPrompts().stream()
                                                 .filter(Prompt::isActive)
                                                 .collect(Collectors.toList());
                if (activePrompts.isEmpty() && activePromptsDisplay != null) {
                    activePromptsDisplay.setText("Direct Transcription");
                    activePromptsDisplay.setVisibility(View.VISIBLE); // Ensure it's visible
                } else if (activePromptsDisplay != null) {
                    // If there are active prompts, updateActivePromptsDisplay will handle showing them.
                    // Call it to refresh, as it contains the full logic.
                    updateActivePromptsDisplay();
                }
            }

        } else { // "whisper" mode (default)
            whisperSectionContainer.setVisibility(View.VISIBLE);
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
        activePromptsDisplay.setOnClickListener(v -> { // Optional: make it clickable to go to Prompts screen
            Intent intent = new Intent(MainActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
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
        
        btnSendWhisper.setOnClickListener(v -> transcribeAudio());
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
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.MP3); // Requires API 29+
            mediaRecorder.setAudioSamplingRate(16000);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            Log.d(TAG, "Screen orientation locked due to recording start.");
            
            startTimer();
            updateUiForRecording(true);
            
            Log.d(TAG, "Recording started");
        } catch (IOException e) {
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mediaRecorder.pause();
                isPaused = true;
                recordingDuration += System.currentTimeMillis() - recordingStartTime;
                stopTimer();
                updateUiForPausedRecording();
                Log.d(TAG, "Recording paused (orientation remains locked).");
            } else {
                // For devices that don't support pause/resume
                // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); // Removed, stopRecording will handle it
                stopRecording();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing recording", e);
        }
    }
    
    private void resumeRecording() {
        if (!isRecording || !isPaused) return;
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                mediaRecorder.resume();
                isPaused = false;
                recordingStartTime = System.currentTimeMillis();
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                Log.d(TAG, "Screen orientation locked due to recording resume.");
                startTimer();
                updateUiForRecording(true);
                Log.d(TAG, "Recording resumed");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error resuming recording", e);
        }
    }
    
    private void stopRecording() {
        if (!isRecording) return;
        
        try {
            stopTimer();
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            
            isRecording = false;
            isPaused = false;
            recordingDuration += System.currentTimeMillis() - recordingStartTime;
            
            updateUiForRecording(false);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Log.d(TAG, "Screen orientation unlocked due to recording stop.");
            
            Log.d(TAG, "Recording stopped, duration: " + recordingDuration + "ms");

            String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper");
            if (transcriptionMode.equals("chatgpt_direct")) {
                lastRecordedAudioPathForChatGPTDirect = audioFilePath;
                if (chkAutoSendToChatGpt.isChecked()) { // Using chkAutoSendToChatGpt as the equivalent
                    transcribeAudioWithChatGpt();
                }
            } else { // "whisper" mode
                if (chkAutoSendWhisper.isChecked()) {
                    transcribeAudio();
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

        // Create UploadTask
        UploadTask uploadTask = new UploadTask(audioFilePath, UploadTask.TYPE_AUDIO_TRANSCRIPTION);
        // All other fields (status, retryCount, creationTimestamp) are set in the constructor

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
        mainHandler.post(() -> {
            whisperText.setText(TRANSCRIPTION_QUEUED_PLACEHOLDER); // Update placeholder text
            // Do NOT automatically call sendToChatGpt or sendWhisperToInputStick here anymore.
            // That logic will move to when the task is actually completed by the service.
        });
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
                        if (UploadTask.STATUS_SUCCESS.equals(latestTaskForFile.status)) {
                            whisperText.setText(latestTaskForFile.transcriptionResult);
                            Toast.makeText(MainActivity.this, "Transcription loaded.", Toast.LENGTH_SHORT).show();
                            // Potentially trigger auto-send actions if they were pending on this result
                            if (chkAutoSendToChatGpt.isChecked()) {
                                AppLogManager.getInstance().addEntry("INFO", "Auto-sending refreshed transcript to ChatGPT...", null);
                                sendToChatGpt();
                            }
                            if (chk_auto_send_whisper_to_inputstick.isChecked()) {
                                AppLogManager.getInstance().addEntry("INFO", "Auto-sending refreshed Whisper transcript to InputStick...", null);
                                sendWhisperToInputStick();
                            }
                        } else if (UploadTask.STATUS_FAILED.equals(latestTaskForFile.status)) {
                            String errorMsg = "Transcription failed: " + latestTaskForFile.errorMessage;
                            whisperText.setText(errorMsg);
                            Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        } else if (UploadTask.STATUS_PENDING.equals(latestTaskForFile.status) || UploadTask.STATUS_UPLOADING.equals(latestTaskForFile.status)) {
                            whisperText.setText("[" + latestTaskForFile.status + "... Tap to refresh]");
                            if (userInitiated) Toast.makeText(MainActivity.this, "Transcription is " + latestTaskForFile.status.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                        } else {
                             if (userInitiated) Toast.makeText(MainActivity.this, "Task status: " + latestTaskForFile.status, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (userInitiated) Toast.makeText(MainActivity.this, "No transcription task found for the last recording.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (userInitiated) Toast.makeText(MainActivity.this, "No transcription tasks found in queue for this file.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void clearRecording() {
        File audioFile = new File(audioFilePath);
        if (audioFile.exists()) {
            audioFile.delete();
        }
        recordingDuration = 0;
        lastRecordedAudioPathForChatGPTDirect = null; // Added
        Toast.makeText(this, "Recording cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void clearTranscription() {
        whisperText.setText("");
        clearRecording();
    }
    
    private void transcribeAudioWithChatGpt() {
    if (lastRecordedAudioPathForChatGPTDirect == null || lastRecordedAudioPathForChatGPTDirect.isEmpty()) {
        mainHandler.post(() -> Toast.makeText(MainActivity.this, "No audio recording available for direct processing.", Toast.LENGTH_SHORT).show());
        return;
    }

    final File currentAudioFile = new File(lastRecordedAudioPathForChatGPTDirect); // Make effectively final for thread
    if (!currentAudioFile.exists() || currentAudioFile.length() == 0) {
        mainHandler.post(() -> {
            Toast.makeText(MainActivity.this, "Audio file is missing or empty.", Toast.LENGTH_SHORT).show();
            chatGptText.setText("Audio file missing.");
        });
        AppLogManager.getInstance().addEntry("ERROR", TAG + ": Audio file missing or empty for direct processing.", "Path: " + lastRecordedAudioPathForChatGPTDirect);
        return;
    }

    String apiKey = sharedPreferences.getString("openai_api_key", "");
    if (apiKey.isEmpty()) {
        mainHandler.post(() -> Toast.makeText(MainActivity.this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show());
        return;
    }

    List<Prompt> currentUserActivePrompts = promptManager.getPrompts().stream()
                                               .filter(Prompt::isActive)
                                               .collect(Collectors.toList());
    String userDefinedPromptText = currentUserActivePrompts.stream()
                                       .map(Prompt::getText)
                                       .collect(Collectors.joining("\n\n"));

    if (userDefinedPromptText.isEmpty()) {
        userDefinedPromptText = "Process the audio. If it's speech, provide a transcription. If it's music or other sounds, describe it. Follow any instructions if present in the speech.";
    }

    // Use the model selected in settings, ensure "gpt-4o-audio-preview" or "gpt-4o" can be selected there.
    final String modelName = sharedPreferences.getString("chatgpt_model", "gpt-4o-audio-preview");
    final String finalUserDefinedPromptText = userDefinedPromptText; // Effectively final for thread

    mainHandler.post(() -> {
        chatGptText.setText("[Sending audio and prompt to " + modelName + "...]");
        Toast.makeText(MainActivity.this, "Processing audio with " + modelName + "...", Toast.LENGTH_SHORT).show();
    });
    AppLogManager.getInstance().addEntry("INFO", TAG + ": Calling getCompletionFromAudioAndPrompt.",
        "Model: " + modelName + ", File: " + currentAudioFile.getAbsolutePath() + ", Prompt Length: " + finalUserDefinedPromptText.length());

    new Thread(() -> {
        try {
            final String processedText = chatGptApi.getCompletionFromAudioAndPrompt(currentAudioFile, finalUserDefinedPromptText, modelName);
            AppLogManager.getInstance().addEntry("SUCCESS", TAG + ": Response from " + modelName + " received.", "Output Length: " + processedText.length());

            mainHandler.post(() -> {
                chatGptText.setText(processedText);
                Toast.makeText(MainActivity.this, modelName + " processing complete.", Toast.LENGTH_SHORT).show();
                if (chkAutoSendInputStick.isChecked() && sharedPreferences.getBoolean("inputstick_enabled", true)) {
                    sendToInputStick();
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "IOException during " + modelName + " processing: " + e.getMessage(), e);
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": IOException in " + modelName + " processing.", e.getMessage());
            mainHandler.post(() -> {
                chatGptText.setText("Error (" + modelName + "): " + e.getMessage());
                Toast.makeText(MainActivity.this, "API Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during " + modelName + " processing: " + e.getMessage(), e);
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": Unexpected error in " + modelName + " processing.", e.toString());
            mainHandler.post(() -> {
                chatGptText.setText("Unexpected error with " + modelName + ".");
                Toast.makeText(MainActivity.this, "Unexpected error (" + modelName + ").", Toast.LENGTH_SHORT).show();
            });
        }
    }).start();
}

    private void sendToChatGpt() {
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper");

        if (transcriptionMode.equals("chatgpt_direct")) {
            if (lastRecordedAudioPathForChatGPTDirect != null && new File(lastRecordedAudioPathForChatGPTDirect).exists()) {
                transcribeAudioWithChatGpt();
                return; // Exit here to prevent text-based sending
            } else {
                // If btnSendChatGpt is clicked in direct mode but no audio recorded yet for this session
                Toast.makeText(this, "Please record audio first for direct transcription.", Toast.LENGTH_LONG).show();
                // Optionally, if whisperText has some old content, you might allow sending that.
                // But for a clean "direct" mode, requiring fresh audio makes sense.
                // If you want to allow sending existing whisperText content, remove the return and the else.
                return;
            }
        }

        // Existing logic for "whisper" mode (or if direct mode had no audio and didn't return)
        String transcript = whisperText.getText().toString().trim();
        if (transcript.isEmpty() || isPlaceholderOrError(transcript)) { // Also check for placeholder
            Toast.makeText(this, "No valid transcription to send to ChatGPT", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, R.string.error_no_api_key, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add this block:
        PromptManager promptManager = new PromptManager(this); // Initialize PromptManager
        List<Prompt> allPrompts = promptManager.getPrompts();
        String activePromptsString = allPrompts.stream()
                                         .filter(Prompt::isActive)
                                         .map(Prompt::getText)
                                         .collect(Collectors.joining("\n\n")); // Join active prompts with double newline

        String finalPromptPayload;
        if (!activePromptsString.isEmpty()) {
            finalPromptPayload = activePromptsString + "\n\n" + transcript;
        } else {
            finalPromptPayload = transcript;
        }
        // End of new block
        
        Toast.makeText(this, "Sending to ChatGPT...", Toast.LENGTH_SHORT).show();
        AppLogManager.getInstance().addEntry("INFO", "ChatGPT: Sending request...", "Payload: " + finalPromptPayload); // Log the payload
        
        // Send to ChatGPT in background
        new Thread(() -> {
            try {
                String response = chatGptApi.getCompletion(finalPromptPayload);
                AppLogManager.getInstance().addEntry("SUCCESS", "ChatGPT: Response received", "Length: " + response.length());
                mainHandler.post(() -> {
                    chatGptText.setText(response);
                    Toast.makeText(MainActivity.this, "Response received", Toast.LENGTH_SHORT).show();
                    
                    if (chkAutoSendInputStick.isChecked() && 
                        sharedPreferences.getBoolean("inputstick_enabled", true)) {
                        sendToInputStick();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error getting ChatGPT response", e);
                AppLogManager.getInstance().addEntry("ERROR", "ChatGPT: Request failed", e.toString());
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, R.string.error_chatgpt, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void clearChatGptResponse() {
        chatGptText.setText("");
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
        // Apply theme in case it was changed in settings
        ThemeManager.applyTheme(sharedPreferences);
        
        // Refresh settings in case they were changed
        initializeApis(); // Refreshes API keys etc.
        chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true));
        chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false));
        chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false));
        chk_auto_send_whisper_to_inputstick.setChecked(sharedPreferences.getBoolean("auto_send_whisper_to_inputstick", false)); // Added

        // Refresh active macros
        displayActiveMacros();
        String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper"); // Added
        updateUiForTranscriptionMode(transcriptionMode); // Added
        updateActivePromptsDisplay(); // Ensure this is called AFTER updateUiForTranscriptionMode
        refreshTranscriptionStatus(false); // false because it's an automatic refresh onResume
    }

    private void updateActivePromptsDisplay() {
        if (promptManager == null) {
            promptManager = new PromptManager(this);
        }
        if (activePromptsDisplay == null) { // Check if initialized
            Log.w(TAG, "activePromptsDisplay is null in updateActivePromptsDisplay.");
            return;
        }

        String transcriptionMode = sharedPreferences.getString("transcription_mode", "whisper");
        List<Prompt> allPrompts = promptManager.getPrompts();
        List<Prompt> activeSystemPrompts = allPrompts.stream()
                                             .filter(Prompt::isActive)
                                             .collect(Collectors.toList());

        if ("chatgpt_direct".equals(transcriptionMode)) {
            if (activeSystemPrompts.isEmpty()) {
                activePromptsDisplay.setText("Direct Transcription");
                activePromptsDisplay.setVisibility(View.VISIBLE);
            } else {
                // Show actual active prompts
                String promptsText = activeSystemPrompts.stream()
                                        .map(Prompt::getLabel)
                                        .collect(Collectors.joining("\n"));
                activePromptsDisplay.setText(promptsText);
                activePromptsDisplay.setVisibility(View.VISIBLE);
            }
        } else { // Whisper mode
            if (activeSystemPrompts.isEmpty()) {
                activePromptsDisplay.setText(""); // Or "No active prompts"
                activePromptsDisplay.setVisibility(View.GONE);
            } else {
                String promptsText = activeSystemPrompts.stream()
                                        .map(Prompt::getLabel)
                                        .collect(Collectors.joining("\n"));
                activePromptsDisplay.setText(promptsText);
                activePromptsDisplay.setVisibility(View.VISIBLE);
            }
        }
    }

    private void displayActiveMacros() {
        if (macroRepository == null) {
            Log.e(TAG, "MacroRepository not initialized in displayActiveMacros");
            return;
        }
        LinearLayout activeMacrosRowsContainer = findViewById(R.id.active_macros_rows_container);
        activeMacrosRowsContainer.removeAllViews();

        List<Macro> activeMacros = macroRepository.getActiveMacros();
        int macrosPerRow = macroRepository.getMacrosPerRow(2); // Default to 2 macros per row

        if (activeMacros.isEmpty()) {
            activeMacrosRowsContainer.setVisibility(View.GONE);
            return;
        } else {
            activeMacrosRowsContainer.setVisibility(View.VISIBLE);
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
                    LinearLayout.LayoutParams.WRAP_CONTENT, // height
                    1.0f // weight
                );
                params.setMargins(marginHorizontalPx, 0, marginHorizontalPx, 0);
                emptyView.setLayoutParams(params);
                currentRow.addView(emptyView);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing MediaRecorder", e);
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