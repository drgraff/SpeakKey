package com.drgraff.speakkey;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.speakkey.data.Macro; // Added for Macro buttons
import com.speakkey.data.MacroRepository; // Added for Macro buttons
import com.speakkey.service.MacroExecutor; // Added for Macro Execution
import com.drgraff.speakkey.inputstick.InputStickBroadcast; // Added
// import com.drgraff.speakkey.inputstick.InputStickManager; // Removed
import com.drgraff.speakkey.settings.SettingsActivity;
import com.drgraff.speakkey.formattingtags.FormattingTagsActivity; // Added for Formatting Tags
import com.speakkey.ui.macros.MacroListActivity; // Added for Macros
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

    // UI elements
    private DrawerLayout drawerLayout;
    private Button btnStartRecording, btnPauseRecording, btnStopRecording;
    private Button btnSendWhisper; // Removed btnClearTranscription, Removed btnClearRecording
    private ImageButton btnClearTranscriptionIcon; // Added
    private ImageButton btnClearAllWhisperIcon; // Added to replace btnClearAll
    private Button btnSendChatGpt; // Removed btnClearChatGpt
    private ImageButton btnClearChatGptIcon; // Added
    private Button btnSendInputStick;
    private Button btnSendWhisperToInputStick; // Added
    private EditText whisperText, chatGptText;
    private View recordingIndicator;
    private TextView recordingTime;
    private CheckBox chkAutoSendWhisper, chkAutoSendInputStick, chkAutoSendToChatGpt; // Added chkAutoSendToChatGpt
    private CheckBox chk_auto_send_whisper_to_inputstick; // Added
    private EditText currentEditingEditText; // For FullScreenEditTextDialogFragment

    // Audio recording
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private boolean isRecording = false;
    private boolean isPaused = false;
    private long recordingStartTime = 0;
    private long recordingDuration = 0;
    
    // APIs
    private WhisperApi whisperApi;
    private ChatGptApi chatGptApi;
    // private InputStickManager inputStickManager; // Removed
    
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
        
        // Request permissions
        requestPermissions();
        
        // Set up temporary audio file path
        File audioDir = new File(getFilesDir(), "audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        audioFilePath = new File(audioDir, "recording.mp3").getAbsolutePath();

        // Display active macros
        displayActiveMacros(); // Call after macroRepository is initialized
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
        chkAutoSendWhisper = findViewById(R.id.chk_auto_send_whisper);
        
        // ChatGPT section
        chatGptText = findViewById(R.id.chatgpt_text);
        btnSendChatGpt = findViewById(R.id.btn_send_chatgpt);
        btnClearChatGptIcon = findViewById(R.id.btn_clear_chatgpt_icon); // Initialize new ImageButton
        chkAutoSendToChatGpt = findViewById(R.id.chk_auto_send_to_chatgpt);
        
        // InputStick section
        btnSendInputStick = findViewById(R.id.btn_send_inputstick);
        chkAutoSendInputStick = findViewById(R.id.chk_auto_send_inputstick);
        btnSendWhisperToInputStick = findViewById(R.id.btn_send_whisper_to_inputstick);
        chk_auto_send_whisper_to_inputstick = findViewById(R.id.chk_auto_send_whisper_to_inputstick);
        // btnClearAll = findViewById(R.id.btn_clear_all); // Removed

        // Set up click listeners
        // setupClickListeners(); // Moved down
        
        // Set initial checkbox states from preferences
        // chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true)); // Moved down
        // chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false)); // Moved down
        // chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false)); // Moved down
        // chk_auto_send_whisper_to_inputstick.setChecked(sharedPreferences.getBoolean("auto_send_whisper_to_inputstick", false)); // Moved down

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
        // inputStickManager = new InputStickManager(this); // Removed
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
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            isPaused = false;
            recordingStartTime = System.currentTimeMillis();
            
            startTimer();
            updateUiForRecording(true);
            
            Log.d(TAG, "Recording started");
        } catch (IOException e) {
            Log.e(TAG, "Error starting recording", e);
            Toast.makeText(this, R.string.error_recording, Toast.LENGTH_SHORT).show();
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
                Log.d(TAG, "Recording paused");
            } else {
                // For devices that don't support pause/resume
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
            
            Log.d(TAG, "Recording stopped, duration: " + recordingDuration + "ms");
            
            if (chkAutoSendWhisper.isChecked()) {
                transcribeAudio();
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
        
        Toast.makeText(this, "Transcribing audio...", Toast.LENGTH_SHORT).show();
        AppLogManager.getInstance().addEntry("INFO", "Whisper: Starting transcription...", null);
        
        // Perform transcription in background
        new Thread(() -> {
            try {
                String transcription = whisperApi.transcribe(audioFile);
                AppLogManager.getInstance().addEntry("SUCCESS", "Whisper: Transcription successful", "Length: " + transcription.length());
                mainHandler.post(() -> {
                    whisperText.setText(transcription);
                    Toast.makeText(MainActivity.this, "Transcription complete", Toast.LENGTH_SHORT).show();

                    // Existing auto-send to ChatGPT logic (should remain)
                    if (chkAutoSendToChatGpt.isChecked()) {
                        AppLogManager.getInstance().addEntry("INFO", "Auto-sending transcript to ChatGPT...", null);
                        sendToChatGpt(); 
                    }

                    // New: Auto-send Whisper text directly to InputStick
                    if (chk_auto_send_whisper_to_inputstick.isChecked()) { // Use the new CheckBox ID
                        AppLogManager.getInstance().addEntry("INFO", "Auto-sending Whisper transcript directly to InputStick...", null);
                        sendWhisperToInputStick(); // This is the method created in the previous task
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error transcribing audio", e);
                AppLogManager.getInstance().addEntry("ERROR", "Whisper: Transcription failed", e.toString());
                mainHandler.post(() -> {
                    // Show a more specific toast
                    String detailedErrorMessage = getString(R.string.error_transcribing);
                    if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                        // Append only the first part of the message if it's too long or has newlines.
                        // Let's take up to the first newline or a certain character limit.
                        String userFriendlyMessage = e.getMessage();
                        int newlineIndex = userFriendlyMessage.indexOf('\n');
                        if (newlineIndex != -1) {
                            userFriendlyMessage = userFriendlyMessage.substring(0, newlineIndex);
                        }
                        if (userFriendlyMessage.length() > 150) { // Arbitrary limit for toast length
                            userFriendlyMessage = userFriendlyMessage.substring(0, 147) + "...";
                        }
                        detailedErrorMessage += ": " + userFriendlyMessage;
                    }
                    Toast.makeText(MainActivity.this, detailedErrorMessage, Toast.LENGTH_LONG).show(); // Use LENGTH_LONG
                });
            }
        }).start();
    }
    
    private void clearRecording() {
        File audioFile = new File(audioFilePath);
        if (audioFile.exists()) {
            audioFile.delete();
        }
        recordingDuration = 0;
        Toast.makeText(this, "Recording cleared", Toast.LENGTH_SHORT).show();
    }
    
    private void clearTranscription() {
        whisperText.setText("");
        clearRecording();
    }
    
    private void sendToChatGpt() {
        String transcript = whisperText.getText().toString().trim();
        if (transcript.isEmpty()) {
            Toast.makeText(this, "No transcription to send", Toast.LENGTH_SHORT).show();
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
        if (com.drgraff.speakkey.inputstick.InputStickBroadcast.isSupported(this, true)) {
            // Send the text using InputStickBroadcast
            com.drgraff.speakkey.inputstick.InputStickBroadcast.type(this, text, "en-US"); // Using "en-US" as default layout
            Toast.makeText(MainActivity.this, "Text sent via InputStick broadcast", Toast.LENGTH_SHORT).show();
            AppLogManager.getInstance().addEntry("INFO", "InputStick: Text broadcasted", "Length: " + text.length());
        } else {
            // isSupported() already shows a dialog if not installed/updated.
            // Optionally, add another toast or log if needed, but DownloadDialog should handle user notification.
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
        if (com.drgraff.speakkey.inputstick.InputStickBroadcast.isSupported(this, true)) {
            // Send the text using InputStickBroadcast
            com.drgraff.speakkey.inputstick.InputStickBroadcast.type(this, textToSend, "en-US"); // Using "en-US" as default layout
            Toast.makeText(MainActivity.this, "Whisper text sent via InputStick broadcast", Toast.LENGTH_SHORT).show();
            // Ensure AppLogManager is accessible or initialized if you use it here
            AppLogManager.getInstance().addEntry("INFO", "InputStick: Whisper text broadcasted", "Length: " + textToSend.length());
        } else {
            // isSupported() already shows a dialog.
            AppLogManager.getInstance().addEntry("WARN", "InputStick: Utility not supported or user cancelled download for sending Whisper text.", null);
        }
    }
}