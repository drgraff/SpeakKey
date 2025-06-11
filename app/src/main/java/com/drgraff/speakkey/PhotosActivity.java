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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.drgraff.speakkey.data.PhotoPrompt;
import com.drgraff.speakkey.data.PhotoPromptManager;
import com.drgraff.speakkey.data.UploadTask; // Added for UploadTask
import com.drgraff.speakkey.inputstick.InputStickBroadcast; // Added
import com.drgraff.speakkey.service.UploadService; // Added for UploadService
import com.drgraff.speakkey.inputstick.InputStickManager;
import com.drgraff.speakkey.settings.SettingsActivity; // Added import
import com.drgraff.speakkey.utils.AppLogManager;
import com.drgraff.speakkey.FullScreenEditTextDialogFragment; // Added

public class PhotosActivity extends AppCompatActivity implements FullScreenEditTextDialogFragment.OnSaveListener { // Added interface

    private static final String TAG = "PhotosActivity";
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
    private PhotoPromptManager photoPromptManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photos);

        Toolbar toolbar = findViewById(R.id.toolbar_photos);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.photos_title_toolbar));
        }

        imageViewPhoto = findViewById(R.id.image_view_photo);
        btnTakePhotoArea = findViewById(R.id.btn_take_photo_area); // Initial Take Photo Button
        btnClearPhoto = findViewById(R.id.btn_clear_photo);
        btnPhotoPrompts = findViewById(R.id.btn_photo_prompts);
        textActivePhotoPromptsDisplay = findViewById(R.id.text_active_photo_prompts_display);
        photoPromptManager = new PhotoPromptManager(this);

        btnSendToChatGptPhoto = findViewById(R.id.btn_send_to_chatgpt_photo);
        editTextChatGptResponsePhoto = findViewById(R.id.edittext_chatgpt_response_photo);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        chkAutoSendChatGptPhoto = findViewById(R.id.chk_auto_send_chatgpt_photo);
        btnClearChatGptResponsePhoto = findViewById(R.id.btn_clear_chatgpt_response_photo);
        btnShareChatGptResponsePhoto = findViewById(R.id.btn_share_chatgpt_response_photo); // Initialize share button
        btnSendToInputStickPhoto = findViewById(R.id.btn_send_to_inputstick_photo); // Added
        chkAutoSendInputStickPhoto = findViewById(R.id.chk_auto_send_inputstick_photo);
        inputStickManager = new InputStickManager(this); // Added
        photoVisionReceiver = new PhotoVisionBroadcastReceiver();

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

        chkAutoSendChatGptPhoto.setChecked(sharedPreferences.getBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, false));
        chkAutoSendChatGptPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_CHATGPT_PHOTO, isChecked).apply();
        });

        btnClearChatGptResponsePhoto.setOnClickListener(v -> {
            editTextChatGptResponsePhoto.setText("");
        });

        chkAutoSendInputStickPhoto.setChecked(sharedPreferences.getBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, false)); // Added
        chkAutoSendInputStickPhoto.setOnCheckedChangeListener((buttonView, isChecked) -> { // Added
            sharedPreferences.edit().putBoolean(PREF_AUTO_SEND_INPUTSTICK_PHOTO, isChecked).apply();
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
            Intent intent = new Intent(PhotosActivity.this, PhotoPromptsActivity.class);
            startActivity(intent);
        });

        textActivePhotoPromptsDisplay.setOnClickListener(v -> { // Added
            Intent intent = new Intent(PhotosActivity.this, PhotoPromptsActivity.class);
            startActivity(intent);
        });

        btnSendToChatGptPhoto.setOnClickListener(v -> sendPhotoAndPromptsToChatGpt());

        editTextChatGptResponsePhoto.setOnClickListener(v -> { // Added listener
            FullScreenEditTextDialogFragment dialogFragment = FullScreenEditTextDialogFragment.newInstance(editTextChatGptResponsePhoto.getText().toString());
            dialogFragment.show(getSupportFragmentManager(), "edit_chatgpt_response_photo_dialog");
        });

        // Retain the OnTouchListener for refresh that was added in the previous subtask
        editTextChatGptResponsePhoto.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                String text = editTextChatGptResponsePhoto.getText().toString();
                if (text.contains("queued") || text.contains(UploadTask.STATUS_PENDING) || text.contains(UploadTask.STATUS_UPLOADING) || text.equals(PHOTO_PROCESSING_QUEUED_PLACEHOLDER)) {
                    refreshPhotoProcessingStatus(true);
                    return true; // Consumed touch
                }
            }
            // Allow click to proceed to open full screen editor if not a refresh action
            // but return false so other listeners (like the OnClickListener above) can also fire.
            // Also, ensure this doesn't interfere with normal EditText touch actions like cursor placement
            // if the condition for refresh isn't met.
            return v.performClick(); // Propagate if not handled, or false if only for this listener
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
    }

    private void updateActivePhotoPromptsDisplay() { // Added method
        if (photoPromptManager == null) photoPromptManager = new PhotoPromptManager(this); // Defensive init
        List<PhotoPrompt> activePrompts = photoPromptManager.getPhotoPrompts().stream()
                .filter(PhotoPrompt::isActive)
                .collect(Collectors.toList());

        if (activePrompts.isEmpty()) {
            textActivePhotoPromptsDisplay.setText(getString(R.string.photos_text_no_active_prompts));
        } else {
            String displayText = activePrompts.stream()
                    .map(PhotoPrompt::getLabel)
                    .collect(Collectors.joining("\n")); // Using newline as separator
            textActivePhotoPromptsDisplay.setText(displayText);
        }
        textActivePhotoPromptsDisplay.setVisibility(View.VISIBLE); // Ensure it's visible
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(UploadService.ACTION_PHOTO_VISION_COMPLETE);
        LocalBroadcastManager.getInstance(this).registerReceiver(photoVisionReceiver, filter);
        Log.d(TAG, "PhotoVisionBroadcastReceiver registered.");
        updateActivePhotoPromptsDisplay();
        refreshPhotoProcessingStatus(false); // Add this call
    }

    @Override
    protected void onPause() {
        super.onPause();
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
            return;
        }

        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        // Assuming currentPhotoPath is the key to find the relevant task.
        // We might need a more robust way if multiple tasks can be associated with the same photo path over time.
        // For now, getting the latest task for this path.
        Executors.newSingleThreadExecutor().execute(() -> {
            List<UploadTask> tasks = database.uploadTaskDao().getTasksByFilePath(currentPhotoPath);

            mainHandler.post(() -> {
                if (tasks != null && !tasks.isEmpty()) {
                    UploadTask latestTaskForFile = null;
                    // Find the most recent task for this file path
                    for(UploadTask task : tasks) {
                        if (task.filePath.equals(currentPhotoPath) && UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                            if (latestTaskForFile == null || task.creationTimestamp > latestTaskForFile.creationTimestamp) {
                                latestTaskForFile = task;
                            }
                        }
                    }

                    if (latestTaskForFile != null) {
                        Log.d(TAG, "Refresh found photo task ID " + latestTaskForFile.id + " with status: " + latestTaskForFile.status + " for path: " + currentPhotoPath);
                        if (UploadTask.STATUS_SUCCESS.equals(latestTaskForFile.status)) {
                            editTextChatGptResponsePhoto.setText(latestTaskForFile.visionApiResponse);
                            Toast.makeText(PhotosActivity.this, "Photo processing complete.", Toast.LENGTH_SHORT).show();
                            if (chkAutoSendInputStickPhoto.isChecked()) { // Check if auto-send to InputStick is enabled
                                sendTextToInputStick();
                            }
                        } else if (UploadTask.STATUS_FAILED.equals(latestTaskForFile.status)) {
                            String errorMsg = "Photo processing failed: " + latestTaskForFile.errorMessage;
                            editTextChatGptResponsePhoto.setText(errorMsg);
                            Toast.makeText(PhotosActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        } else if (UploadTask.STATUS_PENDING.equals(latestTaskForFile.status) || UploadTask.STATUS_UPLOADING.equals(latestTaskForFile.status)) {
                            editTextChatGptResponsePhoto.setText("[" + latestTaskForFile.status + "... Tap to refresh]");
                            if (userInitiated) Toast.makeText(PhotosActivity.this, "Photo processing is " + latestTaskForFile.status.toLowerCase() + ".", Toast.LENGTH_SHORT).show();
                        } else {
                            if (userInitiated) Toast.makeText(PhotosActivity.this, "Task status: " + latestTaskForFile.status, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                         if (userInitiated && !editTextChatGptResponsePhoto.getText().toString().startsWith("[")) { // Only show if not already showing a status
                            Toast.makeText(PhotosActivity.this, "No processing task found for the current photo.", Toast.LENGTH_SHORT).show();
                         }
                    }
                } else {
                     if (userInitiated && !editTextChatGptResponsePhoto.getText().toString().startsWith("[")) {
                        Toast.makeText(PhotosActivity.this, "No processing tasks found in queue for this photo.", Toast.LENGTH_SHORT).show();
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

        List<PhotoPrompt> activePhotoPrompts = photoPromptManager.getPhotoPrompts().stream()
                .filter(PhotoPrompt::isActive)
                .collect(Collectors.toList());

        String concatenatedPromptText = "";
        if (!activePhotoPrompts.isEmpty()) {
            concatenatedPromptText = activePhotoPrompts.stream()
                                    .map(PhotoPrompt::getText)
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
        editTextChatGptResponsePhoto.setText(PHOTO_PROCESSING_QUEUED_PLACEHOLDER);
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Queuing photo processing task.", "File: " + currentPhotoPath);

        UploadTask uploadTask = new UploadTask(
                currentPhotoPath,
                UploadTask.TYPE_PHOTO_VISION,
                concatenatedPromptText, // Storing the full prompt text
                selectedPhotoModel      // Storing the model name
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
                if (chkAutoSendChatGptPhoto.isChecked() && currentPhotoPath != null && !currentPhotoPath.isEmpty()) { // Added
                    sendPhotoAndPromptsToChatGpt();
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

        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            if (myBitmap != null) {
                imageViewPhoto.setImageBitmap(myBitmap);
                imageViewPhoto.setVisibility(View.VISIBLE);
                btnTakePhotoArea.setVisibility(View.GONE);
                btnClearPhoto.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "Failed to decode bitmap from path: " + currentPhotoPath);
                Toast.makeText(this, getString(R.string.photos_toast_failed_load_image_text), Toast.LENGTH_SHORT).show();
                clearPhoto();
            }
        } else {
            Log.w(TAG, "Image file does not exist at path: " + currentPhotoPath);
            clearPhoto(); // Reset if file doesn't exist
        }
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
                    if (visionResult != null) {
                        editTextChatGptResponsePhoto.setText(visionResult);
                        Log.i(TAG, "Photo vision result updated via broadcast for task ID: " + taskId);

                        // Auto-send to InputStick if checked
                        if (chkAutoSendInputStickPhoto != null && chkAutoSendInputStickPhoto.isChecked()) {
                            Log.d(TAG, "Auto-sending photo vision result to InputStick from broadcast receiver.");
                            sendTextToInputStick();
                        }
                    } else {
                        Log.w(TAG, "Received null vision result for matched file path: " + receivedFilePath);
                        editTextChatGptResponsePhoto.setText(getString(R.string.photos_vision_failed_placeholder)); // Assumes such a string exists or will be added
                    }
                } else {
                    Log.d(TAG, "Received vision result for a different/unknown file path. Current: " + PhotosActivity.this.currentPhotoPath + ", Received: " + receivedFilePath + ". No UI update for editTextChatGptResponsePhoto.");
                }
            }
        }
    }
}
// Need to add getApiKey() to ChatGptApi or remove the check.
// import java.io.ByteArrayOutputStream;
// import android.util.Base64;
// import android.preference.PreferenceManager;
// import com.drgraff.speakkey.api.ChatGptApi;
// import com.drgraff.speakkey.api.ChatGptRequest;
// import java.util.ArrayList; // Already have List, but if new ArrayList<>() is used directly
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import android.os.Handler;
// import android.os.Looper;
// import android.app.ProgressDialog;
// import android.content.SharedPreferences;
// import android.widget.EditText;
