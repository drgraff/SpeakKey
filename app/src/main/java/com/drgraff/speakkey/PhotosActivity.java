package com.drgraff.speakkey;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Handler; // Added
import android.os.Looper; // Added
import android.preference.PreferenceManager; // Added
import android.util.Base64; // Added
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton; // Added
import android.widget.ImageView;
import android.widget.ProgressBar; // Added
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.drgraff.speakkey.utils.ThemeManager; // Added for ThemeManager
import com.drgraff.speakkey.utils.DynamicThemeApplicator; // Added for DynamicThemeApplicator
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream; // Added
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList; // Added
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // Added
import java.util.concurrent.Executors; // Added
import java.util.stream.Collectors;

import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.ChatGptRequest;
import com.drgraff.speakkey.data.AppDatabase; // Added for UploadTask
import com.drgraff.speakkey.data.Prompt; // Added
import com.drgraff.speakkey.data.PromptManager; // Added
import com.drgraff.speakkey.data.UploadTask; // Added for UploadTask
import com.drgraff.speakkey.inputstick.InputStickBroadcast; // Added
import com.drgraff.speakkey.service.UploadService; // Added for UploadService
import com.drgraff.speakkey.inputstick.InputStickManager;
import com.drgraff.speakkey.settings.SettingsActivity; // Added import
import com.drgraff.speakkey.utils.AppLogManager;
import com.drgraff.speakkey.FullScreenEditTextDialogFragment; // Added

public class PhotosActivity extends AppCompatActivity implements FullScreenEditTextDialogFragment.OnSaveListener, SharedPreferences.OnSharedPreferenceChangeListener { // Added interface

    private static final String TAG = "PhotosActivity";
    // private SharedPreferences sharedPreferences; // Already exists
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;
    // Add others if DynamicThemeApplicator applies more that are visually distinct in PhotosActivity

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final String KEY_PHOTO_PATH = "currentPhotoPath";
    private static final String PREF_AUTO_SEND_CHATGPT_PHOTO = "auto_send_chatgpt_photo_enabled";
    private static final String PREF_AUTO_SEND_INPUTSTICK_PHOTO = "auto_send_inputstick_photo_enabled";
    public static final String PHOTO_PROCESSING_QUEUED_PLACEHOLDER = "[Photo processing queued... Tap to refresh]"; // Added

    private ImageView imageViewPhoto;
    private ImageButton btnTakePhotoArea; // Changed from Button to ImageButton
    private Button btnClearPhoto;
    private Button btnPhotoPrompts;
    private TextView textActivePhotoPromptsDisplay;
    private PromptManager promptManager; // Changed

    private Button btnSendToChatGptPhoto; // Added
    private EditText editTextChatGptResponsePhoto; // Added
    private ChatGptApi chatGptApi; // Added
    private SharedPreferences sharedPreferences; // Added
    private ProgressDialog progressDialog; // Added
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CheckBox chkAutoSendChatGptPhoto;
    private ImageButton btnClearChatGptResponsePhoto;
    private ImageButton btnShareChatGptResponsePhoto; // Added for share button
    private Button btnSendToInputStickPhoto;
    private CheckBox chkAutoSendInputStickPhoto;
    private InputStickManager inputStickManager; // Added

    private String currentPhotoPath;
    private PhotoVisionBroadcastReceiver photoVisionReceiver;

    private ProgressBar progressBarPhotoProcessing; // Added
    private TextView textViewPhotoStatus; // Added
    private boolean isNewPhotoTaskJustQueued = false; // Added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize MEMBER sharedPreferences ONCE at the top
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Theme application logic uses the MEMBER variable
        com.drgraff.speakkey.utils.ThemeManager.applyTheme(this.sharedPreferences); // Or just sharedPreferences
        String themeValue = this.sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);
        if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(themeValue)) {
            setTheme(R.style.AppTheme_OLED);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.photos_title_toolbar));
        }

        // Apply custom OLED colors if OLED theme is active
        String currentThemeValue = this.sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);
        if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(currentThemeValue)) {
            com.drgraff.speakkey.utils.DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            // Log that colors are being applied for PhotosActivity
            Log.d(TAG, "PhotosActivity: Applied dynamic OLED colors.");
        }

        imageViewPhoto = findViewById(R.id.image_view_photo);
        btnTakePhotoArea = findViewById(R.id.btn_take_photo_area); // Initial Take Photo Button
        btnClearPhoto = findViewById(R.id.btn_clear_photo);
        btnPhotoPrompts = findViewById(R.id.btn_photo_prompts);
        textActivePhotoPromptsDisplay = findViewById(R.id.text_active_photo_prompts_display);
        promptManager = new PromptManager(this); // Changed

        btnSendToChatGptPhoto = findViewById(R.id.btn_send_to_chatgpt_photo);
        editTextChatGptResponsePhoto = findViewById(R.id.edittext_chatgpt_response_photo);
        // Member sharedPreferences is already initialized at the top.
        // This line is redundant: sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        chkAutoSendChatGptPhoto = findViewById(R.id.chk_auto_send_chatgpt_photo);
        btnClearChatGptResponsePhoto = findViewById(R.id.btn_clear_chatgpt_response_photo);
        btnShareChatGptResponsePhoto = findViewById(R.id.btn_share_chatgpt_response_photo); // Initialize share button
        btnSendToInputStickPhoto = findViewById(R.id.btn_send_to_inputstick_photo); // Added
        chkAutoSendInputStickPhoto = findViewById(R.id.chk_auto_send_inputstick_photo);
        inputStickManager = new InputStickManager(this); // Added
        photoVisionReceiver = new PhotoVisionBroadcastReceiver();

        progressBarPhotoProcessing = findViewById(R.id.progressBarPhotoProcessing); // Added
        textViewPhotoStatus = findViewById(R.id.textViewPhotoStatus); // Added
        progressBarPhotoProcessing.setVisibility(View.GONE); // Added
        textViewPhotoStatus.setVisibility(View.GONE); // Added

        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_api_key_not_set_chatgpt), Toast.LENGTH_LONG).show();
            btnSendToChatGptPhoto.setEnabled(false);
        }
        // Model name will be retrieved from shared prefs when sending
        chatGptApi = new ChatGptApi(apiKey, ""); // Model set per request in getVisionCompletion

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.photos_progress_sending_to_chatgpt_message));
        progressDialog.setCancelable(false);

        chkAutoSendChatGptPhoto.setChecked(this.sharedPreferences.getBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, false));
        chkAutoSendChatGptPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            this.sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, isChecked).apply();
        });

        btnClearChatGptResponsePhoto.setOnClickListener(v -> {
            editTextChatGptResponsePhoto.setText("");
        });

        chkAutoSendInputStickPhoto.setChecked(this.sharedPreferences.getBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, false)); // Added
        chkAutoSendInputStickPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> { // Added
            this.sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, isChecked).apply();
        });

        btnSendToInputStickPhoto.setOnClickListener(v -> {
            sendTextToInputStick(); // Changed from placeholder
        });

        btnTakePhotoArea.setOnClickListener(v -> checkCameraPermissionAndDispatch());
        imageViewPhoto.setOnClickListener(v -> {
            if (currentPhotoPath != null) {
                File oldFile = new File(currentPhotoPath);
                if (oldFile.exists()) {
                    if (oldFile.delete()) {
                        Log.d(TAG, "Old photo deleted: " + currentPhotoPath);
                    } else {
                        Log.e(TAG, "Failed to delete old photo: " + currentPhotoPath);
                    }
                }
                currentPhotoPath = null;
            }
            checkCameraPermissionAndDispatch();
        });

        btnClearPhoto.setOnClickListener(v -> {
            clearPhoto();
        });

        btnPhotoPrompts.setOnClickListener(v -> {
            Intent intent = new Intent(PhotosActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
            intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "photo_vision");
            startActivity(intent);
        });

        textActivePhotoPromptsDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(PhotosActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
            intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "photo_vision");
            startActivity(intent);
        });

        btnSendToChatGptPhoto.setOnClickListener(v -> {
            showPhotoUploadProgressUI(); // Added
            sendPhotoAndPromptsToChatGpt();
        });

        editTextChatGptResponsePhoto.setOnClickListener(v -> { // Added listener
            FullScreenEditTextDialogFragment dialogFragment = FullScreenEditTextDialogFragment.newInstance(editTextChatGptResponsePhoto.getText().toString());
            dialogFragment.show(getSupportFragmentManager(), "edit_chatgpt_response_photo_dialog");
        });

        // Retain the OnTouchListener for refresh that was added in the previous subtask
        editTextChatGptResponsePhoto.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                // If textViewPhotoStatus is visible, it means there might be an ongoing or failed task.
                // User tapping the EditText (which might be empty or show old results) can trigger a manual refresh.
                if (textViewPhotoStatus.getVisibility() == View.VISIBLE || progressBarPhotoProcessing.getVisibility() == View.VISIBLE) {
                    refreshPhotoProcessingStatus(true);
                    // We don't consume the event here (return true) to allow the OnClickListener to still fire
                    // for opening the full screen editor.
                }
            }
            return false; // Important: return false to not consume the event.
        });

        btnShareChatGptResponsePhoto.setOnClickListener(v -> {
            String textToShare = editTextChatGptResponsePhoto.getText().toString();
            if (!textToShare.isEmpty() &&
                !textToShare.equals(PHOTO_PROCESSING_QUEUED_PLACEHOLDER) &&
                !(textToShare.startsWith("[") && textToShare.endsWith("... Tap to refresh]")) &&
                !textToShare.toLowerCase().contains("failed") && // Avoid sharing "Photo processing failed:..."
                !textToShare.toLowerCase().startsWith("photo processing failed")) { // More specific check

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.photos_share_chooser_title_text)));
            } else {
                Toast.makeText(PhotosActivity.this, "No valid content to share.", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState != null) {
            currentPhotoPath = savedInstanceState.getString(KEY_PHOTO_PATH);
            if (currentPhotoPath != null) {
                setPic();
            }
        }

        // Store the currently applied theme mode and relevant OLED colors
        String currentAppliedThemeValue = this.sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);
        this.mAppliedThemeMode = currentAppliedThemeValue;

        if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(currentAppliedThemeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PhotosActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0;
            this.mAppliedTopbarTextIconColor = 0;
            this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PhotosActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode, OLED colors reset.");
        }
    }

    private void updateActivePhotoPromptsDisplay() { // Added method
        if (promptManager == null) promptManager = new PromptManager(this); // Defensive init
        List<Prompt> activePrompts = promptManager.getPromptsForMode("photo_vision").stream()
                .filter(Prompt::isActive) // Assumes Prompt.isActive() exists
                .collect(Collectors.toList());

        if (activePrompts.isEmpty()) {
            textActivePhotoPromptsDisplay.setText(getString(R.string.photos_text_no_active_prompts));
        } else {
            String displayText = activePrompts.stream()
                    .map(Prompt::getLabel) // Assumes Prompt.getLabel() exists
                    .collect(Collectors.joining("\n")); // Using newline as separator
            textActivePhotoPromptsDisplay.setText(displayText);
        }
        textActivePhotoPromptsDisplay.setVisibility(View.VISIBLE); // Ensure it's visible
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Perform check for theme/color changes
        if (mAppliedThemeMode != null && this.sharedPreferences != null) {
            boolean needsRecreate = false;
            String currentThemeValue = this.sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);

            if (!mAppliedThemeMode.equals(currentThemeValue)) {
                needsRecreate = true;
                Log.d(TAG, "PhotosActivity onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue);
            } else if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(currentThemeValue)) {
                // Check specific OLED colors relevant to PhotosActivity's appearance via DynamicThemeApplicator
                int currentTopbarBG = this.sharedPreferences.getInt("pref_oled_topbar_background", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
                if (mAppliedTopbarBackgroundColor != currentTopbarBG) needsRecreate = true;

                int currentTopbarTextIcon = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
                if (mAppliedTopbarTextIconColor != currentTopbarTextIcon) needsRecreate = true;

                int currentMainBG = this.sharedPreferences.getInt("pref_oled_main_background", com.drgraff.speakkey.utils.DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
                if (mAppliedMainBackgroundColor != currentMainBG) needsRecreate = true;

                if (needsRecreate) { // Log only if a specific color changed
                     Log.d(TAG, "PhotosActivity onResume: OLED color(s) changed.");
                }
            }

            if (needsRecreate) {
                Log.d(TAG, "PhotosActivity onResume: Detected configuration change. Recreating activity.");
                recreate();
                return;
            }
        }

        // Register SharedPreferences listener
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }

        // Existing onResume content
        IntentFilter filter = new IntentFilter(UploadService.ACTION_PHOTO_VISION_COMPLETE); // Keep this
        LocalBroadcastManager.getInstance(this).registerReceiver(photoVisionReceiver, filter);
        Log.d(TAG, "PhotoVisionBroadcastReceiver registered.");
        updateActivePhotoPromptsDisplay();

        if (isNewPhotoTaskJustQueued) {
            Log.d(TAG, "onResume: isNewPhotoTaskJustQueued is true, attempting to show 'Queued...' UI synchronously.");
            if (textViewPhotoStatus != null) {
                textViewPhotoStatus.setText("Queued for processing...");
                textViewPhotoStatus.setVisibility(View.VISIBLE);
            }
            if (progressBarPhotoProcessing != null) {
                progressBarPhotoProcessing.setIndeterminate(true);
                progressBarPhotoProcessing.setVisibility(View.VISIBLE);
            }
            if (btnSendToChatGptPhoto != null) {
                btnSendToChatGptPhoto.setEnabled(false);
            }
            if (editTextChatGptResponsePhoto != null) {
                editTextChatGptResponsePhoto.setText("");
            }
            // Do not reset isNewPhotoTaskJustQueued here;
            // refreshPhotoProcessingStatus or the broadcast receiver will handle it.
        }
        refreshPhotoProcessingStatus(false); // Add this call
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister SharedPreferences listener
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }

        // Existing onPause content
        LocalBroadcastManager.getInstance(this).unregisterReceiver(photoVisionReceiver);
        Log.d(TAG, "PhotoVisionBroadcastReceiver unregistered.");
    }

    // The existing OnTouchListener for editTextChatGptResponsePhoto for refresh should be here.
    // It was added in a previous step. I'll ensure the new listener for btnShare is added correctly
    // without interfering with it. The previous diff attempt failed because the OnTouchListener was not
    // in the search block. I'll add the new listener at the end of onCreate.

    private void refreshPhotoProcessingStatus(boolean userInitiated) {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            if (userInitiated) Toast.makeText(this, "No active photo to check status for.", Toast.LENGTH_SHORT).show();
            // Ensure UI is reset if no photo path
            if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
            if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
            if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
            isNewPhotoTaskJustQueued = false; // No active photo, so no task is "just queued" for it
            return;
        }

        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<UploadTask> tasks = database.uploadTaskDao().getTasksByFilePath(currentPhotoPath);
            UploadTask latestTaskForFile = null; // Initialize here

            if (tasks != null && !tasks.isEmpty()) {
                for (UploadTask task : tasks) {
                    if (task.filePath.equals(currentPhotoPath) && UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                        if (latestTaskForFile == null || task.creationTimestamp > latestTaskForFile.creationTimestamp) {
                            latestTaskForFile = task;
                        }
                    }
                }
            }

            final UploadTask finalLatestTaskForFile = latestTaskForFile; // effectively final for lambda
            mainHandler.post(() -> {
                if (finalLatestTaskForFile != null) {
                    Log.d(TAG, "Refresh found photo task ID " + finalLatestTaskForFile.id + " with status: " + finalLatestTaskForFile.status + " for path: " + currentPhotoPath);
                    String status = finalLatestTaskForFile.status != null ? finalLatestTaskForFile.status : UploadTask.STATUS_PENDING;

                    if (UploadTask.STATUS_PENDING.equals(status) ||
                        UploadTask.STATUS_UPLOADING.equals(status) ||
                        UploadTask.STATUS_PROCESSING.equals(status)) {

                        isNewPhotoTaskJustQueued = false; // DB has caught up to an active task.

                        if (textViewPhotoStatus != null) {
                            if(UploadTask.STATUS_PENDING.equals(status)) textViewPhotoStatus.setText("Photo processing is queued.");
                            else if(UploadTask.STATUS_UPLOADING.equals(status)) textViewPhotoStatus.setText("Uploading photo...");
                            else textViewPhotoStatus.setText("Processing photo..."); // STATUS_PROCESSING
                            textViewPhotoStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarPhotoProcessing != null) {
                            progressBarPhotoProcessing.setIndeterminate(true);
                            progressBarPhotoProcessing.setVisibility(View.VISIBLE);
                        }
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                        if (userInitiated && textViewPhotoStatus != null) Toast.makeText(PhotosActivity.this, textViewPhotoStatus.getText().toString(), Toast.LENGTH_SHORT).show();

                    } else if (UploadTask.STATUS_SUCCESS.equals(status)) {
                        isNewPhotoTaskJustQueued = false;
                        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText(finalLatestTaskForFile.visionApiResponse);
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if (userInitiated) Toast.makeText(PhotosActivity.this, "Photo processing complete.", Toast.LENGTH_SHORT).show();
                        if (chkAutoSendInputStickPhoto != null && chkAutoSendInputStickPhoto.isChecked()) {
                            sendTextToInputStick();
                        }
                    } else if (UploadTask.STATUS_FAILED.equals(status)) {
                        isNewPhotoTaskJustQueued = false;
                        String errorMsg = "Photo processing failed: " + finalLatestTaskForFile.errorMessage;
                        if (textViewPhotoStatus != null) {
                            textViewPhotoStatus.setText(errorMsg);
                            textViewPhotoStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if (userInitiated) Toast.makeText(PhotosActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } else { // Other unknown status from DB
                         isNewPhotoTaskJustQueued = false;
                         if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                         if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                         if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                         if (userInitiated) Toast.makeText(PhotosActivity.this, "Unknown task status: " + status, Toast.LENGTH_SHORT).show();
                    }
                } else { // finalLatestTaskForFile is null (no task of TYPE_PHOTO_VISION for currentPhotoPath)
                    if (isNewPhotoTaskJustQueued) {
                        // Keep "Queued..." UI visible as the new task might not be in DB yet
                        if (textViewPhotoStatus != null) {
                            textViewPhotoStatus.setText("Queued for processing...");
                            textViewPhotoStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarPhotoProcessing != null) {
                            progressBarPhotoProcessing.setIndeterminate(true);
                            progressBarPhotoProcessing.setVisibility(View.VISIBLE);
                        }
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                    } else {
                        // No task just queued, and no task found. Reset UI.
                        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if (userInitiated) {
                            Toast.makeText(PhotosActivity.this, "No active processing task found for this photo.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        });
    }

    private String encodeImageToBase64(String imagePath) {
        if (imagePath == null) return null;
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) return null;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Adjust quality as needed. JPEG is generally preferred for photos for size.
        // Consider PNG if alpha transparency or lossless is critical, but size will be larger.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream); // 80 is a good starting quality
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void sendPhotoAndPromptsToChatGpt() {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_no_photo_selected_text), Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = encodeImageToBase64(currentPhotoPath);
        if (base64Image == null) {
            Toast.makeText(this, getString(R.string.photos_toast_failed_encode_image_text), Toast.LENGTH_SHORT).show();
            return;
        }
        String dataUri = "data:image/jpeg;base64," + base64Image;

        List<Prompt> activePhotoPrompts = promptManager.getPromptsForMode("photo_vision").stream()
                .filter(Prompt::isActive)
                .collect(Collectors.toList());

        String concatenatedPromptText = "";
        if (!activePhotoPrompts.isEmpty()) {
            concatenatedPromptText = activePhotoPrompts.stream()
                                    .map(Prompt::getText) // Assumes Prompt.getText() exists
                                    .collect(Collectors.joining("\n\n"));
        } else {
            concatenatedPromptText = getString(R.string.photos_default_image_description_prompt);
        }

        String selectedPhotoModel = sharedPreferences.getString(SettingsActivity.PREF_KEY_PHOTOVISION_PROCESSING_MODEL, "gpt-4-vision-preview");
        if (selectedPhotoModel == null || selectedPhotoModel.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_model_not_selected_text), Toast.LENGTH_LONG).show();
            return;
        }

        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_api_key_not_set_send_text), Toast.LENGTH_LONG).show();
            return;
        }
        // chatGptApi is initialized in onCreate with the API key.
        // If key changes, activity would typically be recreated or API re-initialized on resume if critical.

        // The base64 encoding and data URI creation are now done in UploadService.
        // We just need to pass the file path and other parameters.

        // progressDialog.show(); // No longer show progress here, service handles it.
        // UI update logic moved to showPhotoUploadProgressUI() and called by callers.
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Queuing photo processing task.", "File: " + currentPhotoPath);

        UploadTask uploadTask = UploadTask.createPhotoVisionTask(
                currentPhotoPath,
                concatenatedPromptText, // This is the visionPrompt
                selectedPhotoModel      // This is the visionModelName
        );

        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        final String finalConcatenatedPromptText = concatenatedPromptText; // Create final copy
        Executors.newSingleThreadExecutor().execute(() -> {
            database.uploadTaskDao().insert(uploadTask);
            Log.d(TAG, "New Photo Vision UploadTask inserted with ID: " + uploadTask.id + " for file: " + currentPhotoPath);
            // Use finalConcatenatedPromptText in the lambda
            AppLogManager.getInstance().addEntry("INFO", TAG + ": Photo processing task queued in DB.", "File: " + currentPhotoPath + ", Prompt: " + finalConcatenatedPromptText.substring(0, Math.min(finalConcatenatedPromptText.length(), 50)) + "...");

            UploadService.startUploadService(PhotosActivity.this);
        });
        // Do NOT automatically call sendTextToInputStick here. This will be handled when the task completes.
    }

    private void showPhotoUploadProgressUI() {
        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.VISIBLE);
        if (textViewPhotoStatus != null) {
            textViewPhotoStatus.setVisibility(View.VISIBLE);
            textViewPhotoStatus.setText("Queued for processing..."); // Or a more generic "Processing..."
        }
        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
        isNewPhotoTaskJustQueued = true; // Added
    }

    private void checkCameraPermissionAndDispatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, getString(R.string.photos_toast_camera_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error occurred while creating the File", ex);
                Toast.makeText(this, getString(R.string.photos_toast_error_creating_image_file_text), Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Log.w(TAG, "No activity found to handle ACTION_IMAGE_CAPTURE intent. Ensure a camera app is installed and enabled in the emulator/device.");
            Toast.makeText(this, getString(R.string.photos_toast_no_camera_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                setPic(); // This will update currentPhotoPath and UI
                if (chkAutoSendChatGptPhoto.isChecked() && currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                    showPhotoUploadProgressUI(); // Call directly
                    mainHandler.post(new Runnable() { // Post sendPhotoAndPromptsToChatGpt
                        @Override
                        public void run() {
                            sendPhotoAndPromptsToChatGpt();
                        }
                    });
                }
            } else {
                // If the user cancelled or an error occurred, delete the empty file.
                if (currentPhotoPath != null) {
                    File photoFile = new File(currentPhotoPath);
                    if (photoFile.exists() && photoFile.length() == 0) {
                        photoFile.delete();
                    }
                }
                // Optionally, if currentPhotoPath was from a previous successful capture,
                // you might want to keep it. For this flow, we assume a new capture attempt.
                // If no new image, and an old one was there, clearPhoto() might be too aggressive.
                // For now, just deleting the empty file is fine.
            }
        }
    }

    private void setPic() {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            Log.w(TAG, "currentPhotoPath is null or empty in setPic");
            clearPhoto(); // Ensure UI is in a consistent state if path is invalid
            return;
        }

        final String path = currentPhotoPath; // Use a final variable for lambda/runnable

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                File imgFile = new File(path);
                if (imgFile.exists()) {
                    // Consider adding BitmapFactory.Options for scaling large images
                    // to prevent OutOfMemoryError, e.g.:
                    // BitmapFactory.Options options = new BitmapFactory.Options();
                    // options.inSampleSize = 2; // Adjust as needed
                    // final Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    final Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (myBitmap != null) {
                                if (imageViewPhoto != null) { // Add null check for UI elements
                                    imageViewPhoto.setImageBitmap(myBitmap);
                                    imageViewPhoto.setVisibility(View.VISIBLE);
                                }
                                if (btnTakePhotoArea != null) btnTakePhotoArea.setVisibility(View.GONE);
                                if (btnClearPhoto != null) btnClearPhoto.setVisibility(View.VISIBLE);
                                if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText(""); // Clear previous/status messages

                                // Only hide progress if auto-send is NOT checked.
                                // If auto-send IS checked, showPhotoUploadProgressUI() will soon be called
                                // to manage the visibility of these elements.
                                if (chkAutoSendChatGptPhoto != null && !chkAutoSendChatGptPhoto.isChecked()) {
                                    if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                                    if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                                    if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                                }
                                // If chkAutoSendChatGptPhoto IS checked, we intentionally leave the
                                // progress bar and status text as they are, because showPhotoUploadProgressUI()
                                // will handle them. The btnSendToChatGptPhoto will also be managed by it.
                            } else {
                                Log.e(TAG, "Failed to decode bitmap from path: " + path);
                                Toast.makeText(PhotosActivity.this, getString(R.string.photos_toast_failed_load_image_text), Toast.LENGTH_SHORT).show();
                                clearPhoto(); // Reset UI if bitmap is null
                            }
                        }
                    });
                } else {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.w(TAG, "Image file does not exist at path: " + path);
                            Toast.makeText(PhotosActivity.this, "Image file no longer exists.", Toast.LENGTH_SHORT).show();
                            clearPhoto(); // Reset UI if file doesn't exist
                        }
                    });
                }
            }
        });
    }

    private void clearPhoto() {
        if (currentPhotoPath != null) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) {
                photoFile.delete();
            }
            currentPhotoPath = null;
        }
        imageViewPhoto.setImageBitmap(null); // Clear the image
        imageViewPhoto.setVisibility(View.GONE);
        btnTakePhotoArea.setVisibility(View.VISIBLE); // Show initial button
        btnClearPhoto.setVisibility(View.GONE);
        editTextChatGptResponsePhoto.setText(""); // Clear any previous messages

        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE); // Added
        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE); // Added
        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true); // Added
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentPhotoPath != null) {
            outState.putString(KEY_PHOTO_PATH, currentPhotoPath);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Navigate back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void sendTextToInputStick() { // Added method
        if (!sharedPreferences.getBoolean("inputstick_enabled", true)) {
            Toast.makeText(this, getString(R.string.photos_toast_inputstick_disabled_text), Toast.LENGTH_SHORT).show();
            return;
        }

        String textToSend = editTextChatGptResponsePhoto.getText().toString();
        if (textToSend.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_no_text_to_send_inputstick_text), Toast.LENGTH_SHORT).show();
            return;
        }

        if (InputStickBroadcast.isSupported(this, true)) {
            if (inputStickManager != null) {
                inputStickManager.typeText(textToSend);
                Toast.makeText(this, getString(R.string.photos_toast_text_sent_to_inputstick_text), Toast.LENGTH_SHORT).show();
                AppLogManager.getInstance().addEntry("INFO", "PhotosActivity: Text sent to InputStick", "Length: " + textToSend.length());
            } else {
                Log.e(TAG, "inputStickManager is null. Cannot send text.");
                AppLogManager.getInstance().addEntry("ERROR", "PhotosActivity: inputStickManager is null", null);
                Toast.makeText(this, getString(R.string.photos_toast_error_inputstick_manager_null_text), Toast.LENGTH_SHORT).show();
            }
        } else {
            AppLogManager.getInstance().addEntry("WARN", "PhotosActivity: InputStick Utility not supported or user cancelled download.", null);
            // isSupported() already shows a dialog if not installed/updated.
        }
    }

    @Override // Added method
    public void onSave(String editedText) {
        if (editTextChatGptResponsePhoto != null) {
            editTextChatGptResponsePhoto.setText(editedText);
        }
    }

    private class PhotoVisionBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "PhotoVisionBroadcastReceiver: onReceive triggered.");
            String action = intent.getAction();
            if (UploadService.ACTION_PHOTO_VISION_COMPLETE.equals(action)) {
                String receivedFilePath = intent.getStringExtra(UploadService.EXTRA_PHOTO_FILE_PATH);
                String visionResult = intent.getStringExtra(UploadService.EXTRA_VISION_RESULT);
                long taskId = intent.getLongExtra(UploadService.EXTRA_PHOTO_TASK_ID_LONG, -1);

                Log.d(TAG, "Received ACTION_PHOTO_VISION_COMPLETE for task ID: " + taskId + ", file: " + receivedFilePath);
                Log.d(TAG, "Current photo path in PhotosActivity: " + PhotosActivity.this.currentPhotoPath);

                if (editTextChatGptResponsePhoto == null) {
                    Log.e(TAG, "editTextChatGptResponsePhoto is null in BroadcastReceiver. Cannot update UI.");
                    return;
                }

                // Compare with the currentPhotoPath that this PhotosActivity instance is displaying/waiting for
                if (receivedFilePath != null && receivedFilePath.equals(PhotosActivity.this.currentPhotoPath)) {
                    if (visionResult != null) { // Success case
                        textViewPhotoStatus.setVisibility(View.GONE); // Or setText("Processing complete.") then hide after delay
                        progressBarPhotoProcessing.setVisibility(View.GONE);
                        btnSendToChatGptPhoto.setEnabled(true);
                        editTextChatGptResponsePhoto.setText(visionResult);
                        Log.i(TAG, "Photo vision result updated via broadcast for task ID: " + taskId);

                        if (chkAutoSendInputStickPhoto != null && chkAutoSendInputStickPhoto.isChecked()) {
                            Log.d(TAG, "Auto-sending photo vision result to InputStick from broadcast receiver.");
                            sendTextToInputStick();
                        }
                    } else { // Failure case (visionResult is null)
                        // Attempt to get error message from intent, if UploadService provides it
                        // String errorMessage = intent.getStringExtra(UploadService.EXTRA_ERROR_MESSAGE); // Removed
                        String errorMessage = getString(R.string.photos_vision_failed_placeholder); // Default to placeholder
                        // if (errorMessage == null || errorMessage.isEmpty()) { // This check is now redundant
                        //     errorMessage = getString(R.string.photos_vision_failed_placeholder); // Generic failure
                        // }
                        Log.w(TAG, "Received null vision result or error for matched file path: " + receivedFilePath + ". Error: " + errorMessage);

                        textViewPhotoStatus.setText(errorMessage);
                        textViewPhotoStatus.setVisibility(View.VISIBLE);
                        progressBarPhotoProcessing.setVisibility(View.GONE);
                        btnSendToChatGptPhoto.setEnabled(true);
                        editTextChatGptResponsePhoto.setText(""); // Clear any old results
                        // Optionally, call refreshPhotoProcessingStatus(false) to ensure UI is based on DB which should have the failure.
                        // However, the broadcast should be authoritative for the result of *this specific task*.
                    }
                    isNewPhotoTaskJustQueued = false; // Reset flag as this task is now handled
                } else {
                    Log.d(TAG, "Received vision result for a different/unknown file path. Current: " + PhotosActivity.this.currentPhotoPath + ", Received: " + receivedFilePath + ". No UI update for this activity instance.");
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "PhotosActivity onSharedPreferenceChanged triggered for key: " + key);
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

        if (com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
            Log.d(TAG, "PhotosActivity: Main theme preference changed (dark_mode). Recreating.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = sharedPreferences.getString(com.drgraff.speakkey.utils.ThemeManager.PREF_KEY_DARK_MODE, com.drgraff.speakkey.utils.ThemeManager.THEME_DEFAULT);
            if (com.drgraff.speakkey.utils.ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "PhotosActivity: OLED color preference changed: " + key + ". Recreating.");
                recreate();
            }
        }
    }
}
