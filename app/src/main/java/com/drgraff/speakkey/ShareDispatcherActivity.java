package com.drgraff.speakkey;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider; // If needed for granting permissions later, not for initial copy

import com.drgraff.speakkey.data.AppDatabase;
import com.drgraff.speakkey.data.UploadTask;
import com.drgraff.speakkey.service.UploadService;
import com.drgraff.speakkey.utils.AppLogManager;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ShareDispatcherActivity extends AppCompatActivity {

    private static final String TAG = "ShareDispatcherActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (streamUri != null) {
                Log.d(TAG, "Received ACTION_SEND intent with URI: " + streamUri.toString() + " and type: " + type);
                if (type.startsWith("audio/")) {
                    handleSharedAudio(streamUri);
                } else if (type.startsWith("image/")) {
                    handleSharedImage(streamUri);
                } else {
                    Log.w(TAG, "Unsupported shared content type: " + type);
                    Toast.makeText(this, "Unsupported content type: " + type, Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.w(TAG, "ACTION_SEND intent received without EXTRA_STREAM.");
                Toast.makeText(this, "Error: No content found in share intent.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Log.w(TAG, "Intent not supported or no type: Action=" + action + ", Type=" + type);
            // Optional: Show a message if the activity is launched directly by mistake
            Toast.makeText(this, "This activity is for sharing content to SpeakKey.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleSharedAudio(Uri audioUri) {
        Log.d(TAG, "Handling shared audio: " + audioUri.toString());
        String fileExtension = getFileExtensionFromUri(audioUri);
        if (fileExtension == null || fileExtension.isEmpty()) {
            // Try to get from MIME type
            String mimeType = getContentResolver().getType(audioUri);
            if (mimeType != null) {
                fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }
            if (fileExtension == null || fileExtension.isEmpty()) {
                fileExtension = "audio"; // Fallback extension
            }
        }

        File localAudioFile = copyContentUriToLocalFile(audioUri, "shared_audio", fileExtension);

        if (localAudioFile != null && localAudioFile.exists()) {
            AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared audio file copied to " + localAudioFile.getAbsolutePath(), null);

            // Create UploadTask - simulating the "auto-send Whisper" path
            // This assumes default Whisper processing. If different models/prompts are needed for shared audio,
            // this part would need more complex logic or user preferences.
            // For now, using default transcription like a new recording.
            UploadTask uploadTask = UploadTask.createAudioTranscriptionTask(
                    localAudioFile.getAbsolutePath(),
                    "whisper-1", // Default model for transcription
                    "" // Default empty prompt
            );

            AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
            Executors.newSingleThreadExecutor().execute(() -> {
                database.uploadTaskDao().insert(uploadTask);
                Log.d(TAG, "Shared audio UploadTask inserted with ID: " + uploadTask.id);
                AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared audio transcription task queued in DB.", "File: " + localAudioFile.getAbsolutePath());
                UploadService.startUploadService(ShareDispatcherActivity.this);
            });

            Toast.makeText(this, "Audio shared for transcription. Opening app...", Toast.LENGTH_LONG).show();

            // Navigate to MainActivity
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP); // Or FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(mainActivityIntent);

        } else {
            Log.e(TAG, "Failed to copy shared audio file locally.");
            Toast.makeText(this, "Failed to process shared audio.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private String getFileExtensionFromUri(Uri uri) {
        String extension = null;
        String path = uri.getPath();
        if (path != null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot >= 0) {
                extension = path.substring(lastDot + 1);
            }
        }
        if (extension == null || extension.isEmpty()) {
            // Try to get from ContentResolver if it's a content URI
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                String mimeType = getContentResolver().getType(uri);
                if (mimeType != null) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                }
            }
        }
        return extension;
    }

    private void handleSharedImage(Uri imageUri) {
        Log.d(TAG, "Handling shared image: " + imageUri.toString());
        String fileExtension = getFileExtensionFromUri(imageUri);
        if (fileExtension == null || fileExtension.isEmpty()) {
            // Try to get from MIME type
            String mimeType = getContentResolver().getType(imageUri);
            if (mimeType != null) {
                fileExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            }
            if (fileExtension == null || fileExtension.isEmpty()) {
                fileExtension = "jpg"; // Fallback to jpg for images
            }
        }

        File localImageFile = copyContentUriToLocalFile(imageUri, "shared_image", fileExtension);

        if (localImageFile != null && localImageFile.exists()) {
            AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared image file copied to " + localImageFile.getAbsolutePath(), null);

            Intent photosIntent = new Intent(this, PhotosActivity.class);
            photosIntent.putExtra("SHARED_IMAGE_PATH", localImageFile.getAbsolutePath());
            photosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear task to make PhotosActivity the new root for this flow
            startActivity(photosIntent);
            Toast.makeText(this, "Image sent to Photos activity.", Toast.LENGTH_LONG).show();
        } else {
            Log.e(TAG, "Failed to copy shared image file locally.");
            Toast.makeText(this, "Failed to process shared image.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private File copyContentUriToLocalFile(Uri uri, String filePrefix, String fileExtension) {
        ContentResolver contentResolver = getContentResolver();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        File outputFile = null;

        try {
            inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + uri);
                return null;
            }

            // Create a destination file in the app's cache directory
            File cacheDir = getCacheDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = filePrefix + "_" + timeStamp + "." + fileExtension;
            outputFile = new File(cacheDir, fileName);

            outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024 * 4]; // 4KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            Log.d(TAG, "File copied successfully to: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file from URI: " + uri, e);
            if (outputFile != null && outputFile.exists()) {
                outputFile.delete(); // Clean up partially written file
            }
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }
}
