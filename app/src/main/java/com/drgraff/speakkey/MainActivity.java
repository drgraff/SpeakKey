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

import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.WhisperApi;
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.drgraff.speakkey.inputstick.InputStickManager;
import com.drgraff.speakkey.settings.SettingsActivity;
import com.drgraff.speakkey.utils.AppLogManager;
import com.drgraff.speakkey.utils.ThemeManager;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FullScreenEditTextDialogFragment.OnSaveListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // UI elements
    private DrawerLayout drawerLayout;
    private Button btnStartRecording, btnPauseRecording, btnStopRecording;
    private Button btnSendWhisper, btnClearRecording, btnClearTranscription;
    private Button btnSendChatGpt, btnClearChatGpt;
    private Button btnSendInputStick;
    private EditText whisperText, chatGptText;
    private View recordingIndicator;
    private TextView recordingTime;
    private CheckBox chkAutoSendWhisper, chkAutoSendInputStick, chkAutoSendToChatGpt; // Added chkAutoSendToChatGpt
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
    private InputStickManager inputStickManager;
    
    // Settings
    private SharedPreferences sharedPreferences;
    
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
        
        // Initialize APIs
        initializeApis();
        
        // Request permissions
        requestPermissions();
        
        // Set up temporary audio file path
        File audioDir = new File(getFilesDir(), "audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        audioFilePath = new File(audioDir, "recording.mp3").getAbsolutePath();
    }
    
    private void initializeUiElements() {
        // Recording buttons
        btnStartRecording = findViewById(R.id.btn_start_recording);
        btnPauseRecording = findViewById(R.id.btn_pause_recording);
        btnStopRecording = findViewById(R.id.btn_stop_recording);
        
        // Whisper section
        whisperText = findViewById(R.id.whisper_text);
        btnSendWhisper = findViewById(R.id.btn_send_whisper);
        btnClearRecording = findViewById(R.id.btn_clear_recording);
        btnClearTranscription = findViewById(R.id.btn_clear_transcription);
        chkAutoSendWhisper = findViewById(R.id.chk_auto_send_whisper);
        
        // ChatGPT section
        chatGptText = findViewById(R.id.chatgpt_text);
        btnSendChatGpt = findViewById(R.id.btn_send_chatgpt);
        btnClearChatGpt = findViewById(R.id.btn_clear_chatgpt);
        
        // InputStick section
        btnSendInputStick = findViewById(R.id.btn_send_inputstick);
        chkAutoSendInputStick = findViewById(R.id.chk_auto_send_inputstick);
        
        // Recording indicator
        recordingIndicator = findViewById(R.id.recording_indicator);
        recordingTime = findViewById(R.id.recording_time);

        // ChatGPT section (added checkbox)
        chkAutoSendToChatGpt = findViewById(R.id.chk_auto_send_to_chatgpt);
        
        // Set up click listeners
        setupClickListeners();
        
        // Set initial checkbox states from preferences
        chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true));
        chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false));
        chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false));

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
        inputStickManager = new InputStickManager(this);
    }
    
    private void setupClickListeners() {
        btnStartRecording.setOnClickListener(v -> startRecording());
        btnPauseRecording.setOnClickListener(v -> pauseRecording());
        btnStopRecording.setOnClickListener(v -> stopRecording());
        
        btnSendWhisper.setOnClickListener(v -> transcribeAudio());
        btnClearRecording.setOnClickListener(v -> clearRecording());
        btnClearTranscription.setOnClickListener(v -> clearTranscription());
        
        btnSendChatGpt.setOnClickListener(v -> sendToChatGpt());
        btnClearChatGpt.setOnClickListener(v -> clearChatGptResponse());
        
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

                    // Add this:
                    if (chkAutoSendToChatGpt.isChecked()) {
                        // Log this action
                        AppLogManager.getInstance().addEntry("INFO", "Auto-sending transcript to ChatGPT...", null);
                        sendToChatGpt(); // Call sendToChatGpt automatically
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
            Toast.makeText(this, "No text to send", Toast.LENGTH_SHORT).show();
            return;
        }
        
        inputStickManager.connect(connected -> {
            if (connected) {
                inputStickManager.typeText(text);
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Text sent to InputStick", Toast.LENGTH_SHORT).show();
                });
            } else {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, R.string.error_inputstick, Toast.LENGTH_SHORT).show();
                });
            }
        });
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
            Intent intent = new Intent(this, com.drgraff.speakkey.data.PromptsActivity.class); // Use fully qualified name
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
        initializeApis();
        chkAutoSendWhisper.setChecked(sharedPreferences.getBoolean("auto_send_whisper", true));
        chkAutoSendInputStick.setChecked(sharedPreferences.getBoolean("auto_send_inputstick", false));
        chkAutoSendToChatGpt.setChecked(sharedPreferences.getBoolean("auto_send_to_chatgpt", false));
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
}