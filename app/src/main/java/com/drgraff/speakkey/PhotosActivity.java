package com.drgraff.speakkey;

import android.Manifest;
import android.app.Activity; // Import for Activity.RESULT_OK
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
import android.os.Handler;
import android.os.Looper;
import androidx.preference.PreferenceManager; // Standard import
import android.util.Base64;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.drgraff.speakkey.utils.ThemeManager;
import com.drgraff.speakkey.utils.DynamicThemeApplicator;
import androidx.core.content.FileProvider;
import android.content.res.ColorStateList;
import androidx.core.widget.CompoundButtonCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.drgraff.speakkey.api.ChatGptApi;
// import com.drgraff.speakkey.api.ChatGptRequest; // Not used directly
import com.drgraff.speakkey.data.AppDatabase;
import com.drgraff.speakkey.data.Prompt;
import com.drgraff.speakkey.data.PromptManager;
import com.drgraff.speakkey.data.PhotoPromptsAdapter; // Assuming this is the correct adapter
import com.drgraff.speakkey.data.UploadTask;
import com.drgraff.speakkey.inputstick.InputStickBroadcast;
import com.drgraff.speakkey.service.UploadService;
import com.drgraff.speakkey.inputstick.InputStickManager;
import com.drgraff.speakkey.settings.SettingsActivity;
import com.drgraff.speakkey.utils.AppLogManager;
// import com.drgraff.speakkey.FullScreenEditTextDialogFragment; // REMOVED

public class PhotosActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener { // FullScreenEditTextDialogFragment.OnSaveListener REMOVED

    private static final String TAG = "PhotosActivity";
    private SharedPreferences sharedPreferences;
    private String mAppliedThemeMode = null;
    private int mAppliedTopbarBackgroundColor = 0;
    private int mAppliedTopbarTextIconColor = 0;
    private int mAppliedMainBackgroundColor = 0;

    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final String KEY_PHOTO_PATH = "currentPhotoPath";
    private static final String PREF_AUTO_SEND_CHATGPT_PHOTO = "auto_send_chatgpt_photo_enabled";
    private static final String PREF_AUTO_SEND_INPUTSTICK_PHOTO = "auto_send_inputstick_photo_enabled";
    public static final String PHOTO_PROCESSING_QUEUED_PLACEHOLDER = "[Photo processing queued... Tap to refresh]";

    private static final int REQUEST_ADD_PHOTO_PROMPT = 3;
    private static final int REQUEST_EDIT_PHOTO_PROMPT = 4;

    private ImageView imageViewPhoto;
    private ImageButton btnTakePhotoArea;
    private Button btnClearPhoto;
    private Button btnPhotoPrompts;
    private TextView textActivePhotoPromptsDisplay;
    private PromptManager promptManager;
    private PhotoPromptsAdapter photoPromptsAdapter;

    private Button btnSendToChatGptPhoto;
    private EditText editTextChatGptResponsePhoto;
    private ChatGptApi chatGptApi;
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private CheckBox chkAutoSendChatGptPhoto;
    private ImageButton btnClearChatGptResponsePhoto;
    private ImageButton btnShareChatGptResponsePhoto;
    private Button btnSendToInputStickPhoto;
    private CheckBox chkAutoSendInputStickPhoto;
    private InputStickManager inputStickManager;

    private String currentPhotoPath;
    private PhotoVisionBroadcastReceiver photoVisionReceiver;
    private IntentFilter photoVisionReceiverFilter;

    private ProgressBar progressBarPhotoProcessing;
    private TextView textViewPhotoStatus;
    private boolean isNewPhotoTaskJustQueued = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ThemeManager.applyTheme(this.sharedPreferences);
        String themeValue = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
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

        // Initialize ALL UI elements first
        imageViewPhoto = findViewById(R.id.image_view_photo);
        btnTakePhotoArea = findViewById(R.id.btn_take_photo_area);
        btnClearPhoto = findViewById(R.id.btn_clear_photo);
        btnPhotoPrompts = findViewById(R.id.btn_photo_prompts);
        textActivePhotoPromptsDisplay = findViewById(R.id.text_active_photo_prompts_display);
        btnSendToChatGptPhoto = findViewById(R.id.btn_send_to_chatgpt_photo);
        editTextChatGptResponsePhoto = findViewById(R.id.edittext_chatgpt_response_photo);
        chkAutoSendChatGptPhoto = findViewById(R.id.chk_auto_send_chatgpt_photo);
        btnClearChatGptResponsePhoto = findViewById(R.id.btn_clear_chatgpt_response_photo);
        btnShareChatGptResponsePhoto = findViewById(R.id.btn_share_chatgpt_response_photo);
        btnSendToInputStickPhoto = findViewById(R.id.btn_send_to_inputstick_photo);
        chkAutoSendInputStickPhoto = findViewById(R.id.chk_auto_send_inputstick_photo);
        progressBarPhotoProcessing = findViewById(R.id.progressBarPhotoProcessing);
        textViewPhotoStatus = findViewById(R.id.textViewPhotoStatus);
        // fabAddPhotoPrompt is not in PhotosActivity layout

        // Initialize other components
        promptManager = new PromptManager(this);
        inputStickManager = new InputStickManager(this);
        photoVisionReceiver = new PhotoVisionBroadcastReceiver();
        photoVisionReceiverFilter = new IntentFilter(UploadService.ACTION_PHOTO_VISION_COMPLETE);

        String apiKey = sharedPreferences.getString("openai_api_key", "");
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_api_key_not_set_chatgpt), Toast.LENGTH_LONG).show();
            if(btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
        }
        chatGptApi = new ChatGptApi(apiKey, "");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.photos_progress_sending_to_chatgpt_message));
        progressDialog.setCancelable(false);

        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            DynamicThemeApplicator.applyOledColors(this, this.sharedPreferences);
            Log.d(TAG, "PhotosActivity: Applied dynamic OLED colors for window/toolbar.");

            int buttonBackgroundColor = this.sharedPreferences.getInt("pref_oled_button_background", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_BACKGROUND);
            int buttonTextIconColor = this.sharedPreferences.getInt("pref_oled_button_text_icon", DynamicThemeApplicator.DEFAULT_OLED_BUTTON_TEXT_ICON);

            Button[] buttonsToStyle = { btnClearPhoto, btnPhotoPrompts, btnSendToChatGptPhoto, btnSendToInputStickPhoto };
            String[] buttonNames = { "btnClearPhoto", "btnPhotoPrompts", "btnSendToChatGptPhoto", "btnSendToInputStickPhoto" };

            for (int i = 0; i < buttonsToStyle.length; i++) {
                Button button = buttonsToStyle[i];
                String buttonName = buttonNames[i];
                if (button != null) {
                    button.setBackgroundTintList(ColorStateList.valueOf(buttonBackgroundColor));
                    button.setTextColor(buttonTextIconColor);
                    Log.d(TAG, String.format("PhotosActivity: Styled %s with BG=0x%08X, Text=0x%08X", buttonName, buttonBackgroundColor, buttonTextIconColor));
                } else {
                    Log.w(TAG, "PhotosActivity: Button " + buttonName + " is null, cannot style.");
                }
            }

            // Apply OLED theme to CheckBoxes
            ColorStateList checkBoxTintList = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked}, // checked
                    new int[]{-android.R.attr.state_checked}  // unchecked
                },
                new int[]{
                    this.sharedPreferences.getInt("pref_oled_accent_general", DynamicThemeApplicator.DEFAULT_OLED_ACCENT_GENERAL), // Color for checked state
                    this.sharedPreferences.getInt("pref_oled_general_text_secondary", DynamicThemeApplicator.DEFAULT_OLED_GENERAL_TEXT_SECONDARY)  // Color for unchecked state
                }
            );

            if (chkAutoSendChatGptPhoto != null) {
                CompoundButtonCompat.setButtonTintList(chkAutoSendChatGptPhoto, checkBoxTintList);
            }
            if (chkAutoSendInputStickPhoto != null) {
                CompoundButtonCompat.setButtonTintList(chkAutoSendInputStickPhoto, checkBoxTintList);
            }
        }

        if (chkAutoSendChatGptPhoto != null) {
            chkAutoSendChatGptPhoto.setChecked(this.sharedPreferences.getBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, false));
            chkAutoSendChatGptPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                this.sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, isChecked).apply();
            });
        }
        if (btnClearChatGptResponsePhoto != null) btnClearChatGptResponsePhoto.setOnClickListener(v -> {
            if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
        });
        if (chkAutoSendInputStickPhoto != null) {
            chkAutoSendInputStickPhoto.setChecked(this.sharedPreferences.getBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, false));
            chkAutoSendInputStickPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
                this.sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, isChecked).apply();
            });
        }
        if (btnSendToInputStickPhoto != null) btnSendToInputStickPhoto.setOnClickListener(v -> sendTextToInputStick());
        if (btnTakePhotoArea != null) btnTakePhotoArea.setOnClickListener(v -> checkCameraPermissionAndDispatch());
        if (imageViewPhoto != null) imageViewPhoto.setOnClickListener(v -> {
             if (currentPhotoPath != null) {
                File oldFile = new File(currentPhotoPath);
                if (oldFile.exists()) {
                    if (oldFile.delete()) Log.d(TAG, "Old photo deleted: " + currentPhotoPath);
                    else Log.e(TAG, "Failed to delete old photo: " + currentPhotoPath);
                }
                currentPhotoPath = null;
            }
            checkCameraPermissionAndDispatch();
        });
        if (btnClearPhoto != null) btnClearPhoto.setOnClickListener(v -> clearPhoto());
        if (btnPhotoPrompts != null) btnPhotoPrompts.setOnClickListener(v -> {
            Intent intent = new Intent(PhotosActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
            intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "photo_vision");
            startActivity(intent);
        });
        if (textActivePhotoPromptsDisplay != null) textActivePhotoPromptsDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(PhotosActivity.this, com.drgraff.speakkey.data.PromptsActivity.class);
            intent.putExtra(com.drgraff.speakkey.data.PromptsActivity.EXTRA_FILTER_MODE_TYPE, "photo_vision");
            startActivity(intent);
        });
        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setOnClickListener(v -> {
            showPhotoUploadProgressUI();
            sendPhotoAndPromptsToChatGpt();
        });
        if (editTextChatGptResponsePhoto != null) {
            // editTextChatGptResponsePhoto.setOnClickListener(v -> {
            //     FullScreenEditTextDialogFragment dialogFragment = FullScreenEditTextDialogFragment.newInstance(editTextChatGptResponsePhoto.getText().toString());
            //     dialogFragment.show(getSupportFragmentManager(), "edit_chatgpt_response_photo_dialog");
            // });
            editTextChatGptResponsePhoto.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (textViewPhotoStatus != null && progressBarPhotoProcessing != null &&
                        (textViewPhotoStatus.getVisibility() == View.VISIBLE || progressBarPhotoProcessing.getVisibility() == View.VISIBLE)) {
                        refreshPhotoProcessingStatus(true);
                    }
                }
                return false;
            });
        }
        if (btnShareChatGptResponsePhoto != null) btnShareChatGptResponsePhoto.setOnClickListener(v -> {
            String textToShare = editTextChatGptResponsePhoto.getText().toString();
            if (!textToShare.isEmpty() && !textToShare.equals(PHOTO_PROCESSING_QUEUED_PLACEHOLDER) &&
                !(textToShare.startsWith("[") && textToShare.endsWith("... Tap to refresh]")) &&
                !textToShare.toLowerCase().contains("failed") &&
                !textToShare.toLowerCase().startsWith("photo processing failed")) {
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
            if (currentPhotoPath != null) setPic();
        }

        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);

        // Handle incoming shared image
        if (getIntent().hasExtra("SHARED_IMAGE_PATH")) {
            currentPhotoPath = getIntent().getStringExtra("SHARED_IMAGE_PATH");
            Log.d(TAG, "Received shared image path: " + currentPhotoPath);
            if (currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                setPic(); // Load the picture
                // Simulate auto-send
                // Ensure UI elements are ready before calling showPhotoUploadProgressUI if it relies on them.
                // Post to handler to ensure layout is complete if called very early in onCreate.
                mainHandler.post(() -> {
                    if (imageViewPhoto.getVisibility() == View.VISIBLE) { // Check if setPic was successful
                        showPhotoUploadProgressUI();
                        sendPhotoAndPromptsToChatGpt();
                    } else {
                        Log.e(TAG, "Shared image loaded via setPic but imageViewPhoto not visible, cannot auto-send.");
                        Toast.makeText(PhotosActivity.this, "Error loading shared image for auto-send.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                 Log.e(TAG, "SHARED_IMAGE_PATH extra was present but null or empty.");
                 Toast.makeText(this, "Error receiving shared image path.", Toast.LENGTH_SHORT).show();
            }
        }


        this.mAppliedThemeMode = themeValue;
        if (ThemeManager.THEME_OLED.equals(themeValue)) {
            this.mAppliedTopbarBackgroundColor = this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND);
            this.mAppliedTopbarTextIconColor = this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON);
            this.mAppliedMainBackgroundColor = this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND);
            Log.d(TAG, "PhotosActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode +
                         ", TopbarBG=0x" + Integer.toHexString(mAppliedTopbarBackgroundColor) +
                         ", TopbarTextIcon=0x" + Integer.toHexString(mAppliedTopbarTextIconColor) +
                         ", MainBG=0x" + Integer.toHexString(mAppliedMainBackgroundColor));
        } else {
            this.mAppliedTopbarBackgroundColor = 0; this.mAppliedTopbarTextIconColor = 0; this.mAppliedMainBackgroundColor = 0;
            Log.d(TAG, "PhotosActivity onCreate: Stored mAppliedThemeMode=" + mAppliedThemeMode + ". Not OLED mode.");
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
                Log.d(TAG, "PhotosActivity onResume: Theme mode changed. OldMode=" + mAppliedThemeMode + ", NewMode=" + currentThemeValue);
            } else if (ThemeManager.THEME_OLED.equals(currentThemeValue)) {
                if (mAppliedTopbarBackgroundColor != this.sharedPreferences.getInt("pref_oled_topbar_background", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_BACKGROUND)) needsRecreate = true;
                if (mAppliedTopbarTextIconColor != this.sharedPreferences.getInt("pref_oled_topbar_text_icon", DynamicThemeApplicator.DEFAULT_OLED_TOPBAR_TEXT_ICON)) needsRecreate = true;
                if (mAppliedMainBackgroundColor != this.sharedPreferences.getInt("pref_oled_main_background", DynamicThemeApplicator.DEFAULT_OLED_MAIN_BACKGROUND)) needsRecreate = true;
                if (needsRecreate) {
                     Log.d(TAG, "PhotosActivity onResume: OLED color(s) changed.");
                }
            }
            if (needsRecreate) {
                Log.d(TAG, "PhotosActivity onResume: Detected configuration change. Recreating activity.");
                recreate();
                return;
            }
        }
        if (this.sharedPreferences != null) {
            this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
        if (photoVisionReceiver != null && photoVisionReceiverFilter != null) {
             LocalBroadcastManager.getInstance(this).registerReceiver(photoVisionReceiver, photoVisionReceiverFilter);
             Log.d(TAG, "PhotoVisionBroadcastReceiver registered.");
        }
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
            if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
            if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
        }
        refreshPhotoProcessingStatus(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (this.sharedPreferences != null) {
            this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        if (photoVisionReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(photoVisionReceiver);
            Log.d(TAG, "PhotoVisionBroadcastReceiver unregistered.");
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "PhotosActivity onSharedPreferenceChanged triggered for key: " + key);
        if (key == null) return;
        final String[] oledColorKeys = {
            "pref_oled_topbar_background", "pref_oled_topbar_text_icon", "pref_oled_main_background",
            "pref_oled_surface_background", "pref_oled_general_text_primary", "pref_oled_general_text_secondary",
            "pref_oled_button_background", "pref_oled_button_text_icon", "pref_oled_textbox_background",
            "pref_oled_textbox_accent", "pref_oled_accent_general"
        };
        boolean isOledColorKey = false;
        for (String oledKey : oledColorKeys) {
            if (oledKey.equals(key)) { isOledColorKey = true; break; }
        }
        if (ThemeManager.PREF_KEY_DARK_MODE.equals(key)) {
            Log.d(TAG, "PhotosActivity: Main theme preference changed. Recreating.");
            recreate();
        } else if (isOledColorKey) {
            String currentTheme = this.sharedPreferences.getString(ThemeManager.PREF_KEY_DARK_MODE, ThemeManager.THEME_DEFAULT);
            if (ThemeManager.THEME_OLED.equals(currentTheme)) {
                Log.d(TAG, "PhotosActivity: OLED color preference changed: " + key + ". Recreating.");
                recreate();
            }
        }
    }

    private void updateActivePhotoPromptsDisplay() {
        if (promptManager == null) promptManager = new PromptManager(this);
        List<Prompt> activePrompts = promptManager.getPromptsForMode("photo_vision").stream()
                .filter(Prompt::isActive)
                .collect(Collectors.toList());
        if (textActivePhotoPromptsDisplay != null) {
            if (activePrompts.isEmpty()) {
                textActivePhotoPromptsDisplay.setText(getString(R.string.photos_text_no_active_prompts));
            } else {
                String displayText = activePrompts.stream()
                        .map(Prompt::getLabel)
                        .collect(Collectors.joining("\n"));
                textActivePhotoPromptsDisplay.setText(displayText);
            }
            textActivePhotoPromptsDisplay.setVisibility(View.VISIBLE);
        }
    }

    private void refreshPhotoProcessingStatus(boolean userInitiated) {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            if (userInitiated) Toast.makeText(this, "No active photo to check status for.", Toast.LENGTH_SHORT).show();
            if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
            if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
            if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
            isNewPhotoTaskJustQueued = false;
            return;
        }
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<UploadTask> tasks = database.uploadTaskDao().getTasksByFilePath(currentPhotoPath);
            UploadTask latestTaskForFile = null;
            if (tasks != null && !tasks.isEmpty()) {
                for (UploadTask task : tasks) {
                    if (task.filePath.equals(currentPhotoPath) && UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                        if (latestTaskForFile == null || task.creationTimestamp > latestTaskForFile.creationTimestamp) {
                            latestTaskForFile = task;
                        }
                    }
                }
            }
            final UploadTask finalLatestTaskForFile = latestTaskForFile;
            mainHandler.post(() -> {
                if (finalLatestTaskForFile != null) {
                    String status = finalLatestTaskForFile.status != null ? finalLatestTaskForFile.status : UploadTask.STATUS_PENDING;
                    if (UploadTask.STATUS_PENDING.equals(status) || UploadTask.STATUS_UPLOADING.equals(status) || UploadTask.STATUS_PROCESSING.equals(status)) {
                        isNewPhotoTaskJustQueued = false;
                        if (textViewPhotoStatus != null) {
                            if(UploadTask.STATUS_PENDING.equals(status)) textViewPhotoStatus.setText("Photo processing is queued.");
                            else if(UploadTask.STATUS_UPLOADING.equals(status)) textViewPhotoStatus.setText("Uploading photo...");
                            else textViewPhotoStatus.setText("Processing photo...");
                            textViewPhotoStatus.setVisibility(View.VISIBLE);
                        }
                        if (progressBarPhotoProcessing != null) { progressBarPhotoProcessing.setIndeterminate(true); progressBarPhotoProcessing.setVisibility(View.VISIBLE); }
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
                        if (chkAutoSendInputStickPhoto != null && chkAutoSendInputStickPhoto.isChecked()) sendTextToInputStick();
                    } else if (UploadTask.STATUS_FAILED.equals(status)) {
                        isNewPhotoTaskJustQueued = false;
                        String errorMsg = "Photo processing failed: " + finalLatestTaskForFile.errorMessage;
                        if (textViewPhotoStatus != null) { textViewPhotoStatus.setText(errorMsg); textViewPhotoStatus.setVisibility(View.VISIBLE); }
                        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if (userInitiated) Toast.makeText(PhotosActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    } else {
                         isNewPhotoTaskJustQueued = false;
                         if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                         if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                         if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                         if (userInitiated) Toast.makeText(PhotosActivity.this, "Unknown task status: " + status, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (isNewPhotoTaskJustQueued) {
                        if (textViewPhotoStatus != null) { textViewPhotoStatus.setText("Queued for processing..."); textViewPhotoStatus.setVisibility(View.VISIBLE); }
                        if (progressBarPhotoProcessing != null) { progressBarPhotoProcessing.setIndeterminate(true); progressBarPhotoProcessing.setVisibility(View.VISIBLE); }
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                    } else {
                        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if (userInitiated) Toast.makeText(PhotosActivity.this, "No active processing task found for this photo.", Toast.LENGTH_SHORT).show();
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
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
        List<Prompt> activePhotoPrompts = promptManager.getPromptsForMode("photo_vision").stream()
                .filter(Prompt::isActive).collect(Collectors.toList());
        String concatenatedPromptText = activePhotoPrompts.isEmpty() ? getString(R.string.photos_default_image_description_prompt) :
                                        activePhotoPrompts.stream().map(Prompt::getText).collect(Collectors.joining("\n\n"));
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
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Queuing photo processing task.", "File: " + currentPhotoPath);
        UploadTask uploadTask = UploadTask.createPhotoVisionTask(currentPhotoPath, concatenatedPromptText, selectedPhotoModel);
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        final String finalConcatenatedPromptText = concatenatedPromptText;
        Executors.newSingleThreadExecutor().execute(() -> {
            database.uploadTaskDao().insert(uploadTask);
            Log.d(TAG, "New Photo Vision UploadTask inserted with ID: " + uploadTask.id + " for file: " + currentPhotoPath);
            AppLogManager.getInstance().addEntry("INFO", TAG + ": Photo processing task queued in DB.", "File: " + currentPhotoPath + ", Prompt: " + finalConcatenatedPromptText.substring(0, Math.min(finalConcatenatedPromptText.length(), 50)) + "...");
            UploadService.startUploadService(PhotosActivity.this);
        });
    }

    private void showPhotoUploadProgressUI() {
        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.VISIBLE);
        if (textViewPhotoStatus != null) {
            textViewPhotoStatus.setVisibility(View.VISIBLE);
            textViewPhotoStatus.setText("Queued for processing...");
        }
        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(false);
        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
        isNewPhotoTaskJustQueued = true;
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
                Uri photoURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Log.w(TAG, "No activity found to handle ACTION_IMAGE_CAPTURE intent.");
            Toast.makeText(this, getString(R.string.photos_toast_no_camera_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            setPic();
            if (chkAutoSendChatGptPhoto != null && chkAutoSendChatGptPhoto.isChecked() && currentPhotoPath != null && !currentPhotoPath.isEmpty()) {
                showPhotoUploadProgressUI();
                mainHandler.post(this::sendPhotoAndPromptsToChatGpt);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode != RESULT_OK) {
            if (currentPhotoPath != null) {
                File photoFile = new File(currentPhotoPath);
                if (photoFile.exists() && photoFile.length() == 0) photoFile.delete();
            }
        } else if ((requestCode == REQUEST_ADD_PHOTO_PROMPT || requestCode == REQUEST_EDIT_PHOTO_PROMPT) && resultCode == Activity.RESULT_OK) {
             if (photoPromptsAdapter != null) updateActivePhotoPromptsDisplay();
        }
    }

    private void setPic() {
        if (currentPhotoPath == null || currentPhotoPath.isEmpty()) {
            Log.w(TAG, "currentPhotoPath is null or empty in setPic");
            clearPhoto();
            return;
        }
        final String path = currentPhotoPath;
        executorService.execute(() -> {
            File imgFile = new File(path);
            if (imgFile.exists()) {
                final Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                mainHandler.post(() -> {
                    if (myBitmap != null) {
                        if (imageViewPhoto != null) { imageViewPhoto.setImageBitmap(myBitmap); imageViewPhoto.setVisibility(View.VISIBLE); }
                        if (btnTakePhotoArea != null) btnTakePhotoArea.setVisibility(View.GONE);
                        if (btnClearPhoto != null) btnClearPhoto.setVisibility(View.VISIBLE);
                        if (editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                        if (chkAutoSendChatGptPhoto != null && !chkAutoSendChatGptPhoto.isChecked()) {
                            if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                            if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                            if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        }
                    } else {
                        Log.e(TAG, "Failed to decode bitmap from path: " + path);
                        Toast.makeText(PhotosActivity.this, getString(R.string.photos_toast_failed_load_image_text), Toast.LENGTH_SHORT).show();
                        clearPhoto();
                    }
                });
            } else {
                mainHandler.post(() -> {
                    Log.w(TAG, "Image file does not exist at path: " + path);
                    Toast.makeText(PhotosActivity.this, "Image file no longer exists.", Toast.LENGTH_SHORT).show();
                    clearPhoto();
                });
            }
        });
    }

    private void clearPhoto() {
        if (currentPhotoPath != null) {
            File photoFile = new File(currentPhotoPath);
            if (photoFile.exists()) photoFile.delete();
            currentPhotoPath = null;
        }
        if(imageViewPhoto != null) { imageViewPhoto.setImageBitmap(null); imageViewPhoto.setVisibility(View.GONE); }
        if(btnTakePhotoArea != null) btnTakePhotoArea.setVisibility(View.VISIBLE);
        if(btnClearPhoto != null) btnClearPhoto.setVisibility(View.GONE);
        if(editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
        if (progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
        if (textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
        if (btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
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
            onBackPressed();
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

    private void sendTextToInputStick() {
        if (sharedPreferences != null && !sharedPreferences.getBoolean("inputstick_enabled", true)) {
            Toast.makeText(this, getString(R.string.photos_toast_inputstick_disabled_text), Toast.LENGTH_SHORT).show();
            return;
        }
        String textToSend = editTextChatGptResponsePhoto != null ? editTextChatGptResponsePhoto.getText().toString() : "";
        if (textToSend.isEmpty()) {
            Toast.makeText(this, getString(R.string.photos_toast_no_text_to_send_inputstick_text), Toast.LENGTH_SHORT).show();
            return;
        }
        if (InputStickBroadcast.isSupported(this, true)) {
            if (inputStickManager != null) {
                inputStickManager.typeText(textToSend);
                Toast.makeText(this, getString(R.string.photos_toast_text_sent_to_inputstick_text), Toast.LENGTH_SHORT).show();
                AppLogManager.getInstance().addEntry("INFO", TAG + ": Text sent to InputStick", "Length: " + textToSend.length());
            } else {
                Log.e(TAG, "inputStickManager is null. Cannot send text.");
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": inputStickManager is null", null);
                Toast.makeText(this, getString(R.string.photos_toast_error_inputstick_manager_null_text), Toast.LENGTH_SHORT).show();
            }
        } else {
            AppLogManager.getInstance().addEntry("WARN", TAG + ": InputStick Utility not supported or user cancelled download.", null);
        }
    }

    // @Override // OnSaveListener REMOVED
    // public void onSave(String editedText) {
    //     if (editTextChatGptResponsePhoto != null) {
    //         editTextChatGptResponsePhoto.setText(editedText);
    //     }
    // }

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
                if (receivedFilePath != null && receivedFilePath.equals(PhotosActivity.this.currentPhotoPath)) {
                    if (visionResult != null) {
                        if(textViewPhotoStatus != null) textViewPhotoStatus.setVisibility(View.GONE);
                        if(progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if(btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        editTextChatGptResponsePhoto.setText(visionResult);
                        Log.i(TAG, "Photo vision result updated via broadcast for task ID: " + taskId);
                        if (chkAutoSendInputStickPhoto != null && chkAutoSendInputStickPhoto.isChecked()) {
                            Log.d(TAG, "Auto-sending photo vision result to InputStick from broadcast receiver.");
                            sendTextToInputStick();
                        }
                    } else {
                        String errorMessage = getString(R.string.photos_vision_failed_placeholder);
                        Log.w(TAG, "Received null vision result or error for matched file path: " + receivedFilePath + ". Error: " + errorMessage);
                        if(textViewPhotoStatus != null) { textViewPhotoStatus.setText(errorMessage); textViewPhotoStatus.setVisibility(View.VISIBLE); }
                        if(progressBarPhotoProcessing != null) progressBarPhotoProcessing.setVisibility(View.GONE);
                        if(btnSendToChatGptPhoto != null) btnSendToChatGptPhoto.setEnabled(true);
                        if(editTextChatGptResponsePhoto != null) editTextChatGptResponsePhoto.setText("");
                    }
                    isNewPhotoTaskJustQueued = false;
                } else {
                    Log.d(TAG, "Received vision result for a different/unknown file path. Current: " + PhotosActivity.this.currentPhotoPath + ", Received: " + receivedFilePath + ". No UI update for this activity instance.");
                }
            }
        }
    }
}
