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
import androidx.preference.PreferenceManager; // Added
// import androidx.core.content.FileProvider; // Not strictly needed for initial copy if target is cache

import android.content.SharedPreferences; // Added
import com.drgraff.speakkey.data.AppDatabase;
import com.drgraff.speakkey.data.UploadTask;
import com.drgraff.speakkey.service.UploadService;
import com.drgraff.speakkey.settings.SettingsActivity; // Added
import com.drgraff.speakkey.utils.AppLogManager;
import com.drgraff.speakkey.utils.AudioUtils; // Added for MP3 conversion


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
        String originalMimeType = getContentResolver().getType(audioUri);
        String fileExtension = getFileExtensionFromUri(audioUri, originalMimeType);

        File copiedAudioFile = copyContentUriToLocalFile(audioUri, "shared_audio_temp", fileExtension);

        if (copiedAudioFile == null || !copiedAudioFile.exists()) {
            Log.e(TAG, "Failed to copy shared audio file locally.");
            Toast.makeText(this, "Failed to process shared audio (copy phase).", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared audio file copied to " + copiedAudioFile.getAbsolutePath(), null);

        File fileToProcess = copiedAudioFile;

        // Transcode if not MP3
        if (!AudioUtils.isMimeTypeMp3(originalMimeType) && !"mp3".equalsIgnoreCase(fileExtension)) {
            Log.d(TAG, "Shared audio is not MP3 (MIME: " + originalMimeType + ", Ext: " + fileExtension + "). Attempting transcoding.");
            AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared audio is not MP3. Transcoding...", "Original: " + copiedAudioFile.getName());

            File cacheDir = getCacheDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String mp3FileName = "shared_audio_transcoded_" + timeStamp + ".mp3";
            File mp3Output = new File(cacheDir, mp3FileName);

            // Note: AudioUtils.convertToMp3 expects PCM input.
            // If the input 'copiedAudioFile' is not PCM (e.g., m4a, ogg, wav),
            // a more general transcoding solution (like using FFmpeg or MediaCodec) would be needed.
            // For this task, we assume the shared audio might be in a format that LAME can process
            // or that the primary concern is non-MP3 formats that are PCM-like or can be handled.
            // If it's a compressed format like M4A, LAME directly on it won't work as LAME expects PCM.
            // This is a limitation of the current AudioUtils.convertToMp3 if source is not PCM.
            // For the scope of this fix, we'll proceed assuming AudioUtils.convertToMp3
            // is intended to convert *some* non-MP3 to MP3, typically from PCM.
            // If the source is e.g. a WAV file (which is often PCM), it should work.
            // If it's M4A, this step will likely fail or produce an invalid MP3.
            // A robust solution would involve a full-fledged audio library.

            // For now, let's assume we're trying to convert. If it fails, we'll log and use original.
            File transcodedMp3 = AudioUtils.convertToMp3(copiedAudioFile, mp3Output);

            if (transcodedMp3 != null && transcodedMp3.exists()) {
                Log.d(TAG, "Transcoding successful: " + transcodedMp3.getAbsolutePath());
                AppLogManager.getInstance().addEntry("INFO", TAG + ": Transcoding to MP3 successful.", "New file: " + transcodedMp3.getName());
                fileToProcess = transcodedMp3;
                // Delete the original non-MP3 temp file
                if (!copiedAudioFile.delete()) {
                    Log.w(TAG, "Failed to delete original non-MP3 temp file: " + copiedAudioFile.getAbsolutePath());
                }
            } else {
                Log.w(TAG, "Transcoding to MP3 failed or source was not suitable for LAME. Using original copied file: " + copiedAudioFile.getName());
                AppLogManager.getInstance().addEntry("WARN", TAG + ": Transcoding to MP3 failed. Using original copied file.", "File: " + copiedAudioFile.getName());
                // fileToProcess remains copiedAudioFile
            }
        } else {
            Log.d(TAG, "Shared audio is already MP3 or transcoding is not attempted based on MIME/extension.");
        }

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    String transcriptionMode = sharedPreferences.getString(SettingsActivity.PREF_KEY_TRANSCRIPTION_MODE, "two_step_transcription");
    File fileToSendToMain = null;

    // Prioritize file extension for MP3 check. Mime type can be misleading.
    boolean isMp3Extension = "mp3".equalsIgnoreCase(fileExtension);

    if ("one_step_transcription".equals(transcriptionMode)) {
        if (isMp3Extension) {
            Log.d(TAG, "One-Step: Shared audio has .mp3 extension. Using original copied file: " + copiedAudioFile.getName());
            fileToSendToMain = copiedAudioFile;
        } else {
            Log.d(TAG, "One-Step: Shared audio is not MP3 (Ext: " + fileExtension + ", MIME: " + originalMimeType + "). Attempting transcoding to MP3.");
            File cacheDir = getCacheDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String mp3FileName = "shared_audio_transcoded_" + timeStamp + ".mp3";
            File mp3Output = new File(cacheDir, mp3FileName);
            File transcodedMp3File = AudioUtils.convertToMp3(copiedAudioFile, mp3Output);

            if (transcodedMp3File != null && transcodedMp3File.exists() && transcodedMp3File.getName().toLowerCase().endsWith(".mp3")) {
                Log.d(TAG, "One-Step: Transcoding to MP3 successful: " + transcodedMp3File.getAbsolutePath());
                AppLogManager.getInstance().addEntry("INFO", TAG + ": One-Step: Transcoding to MP3 successful.", "New file: " + transcodedMp3File.getName());
                fileToSendToMain = transcodedMp3File;
                if (!copiedAudioFile.delete()) {
                    Log.w(TAG, "Failed to delete original non-MP3 temp file: " + copiedAudioFile.getAbsolutePath());
                }
            } else {
                Log.e(TAG, "One-Step: Failed to convert '" + copiedAudioFile.getName() + "' to MP3. AudioUtils.convertToMp3 might not support this format or failed.");
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": One-Step: MP3 transcoding failed for " + fileExtension, "Original File: " + copiedAudioFile.getName());
                final String originalFormatForToast = (fileExtension != null && !fileExtension.isEmpty()) ? fileExtension.toUpperCase() : "audio";
                runOnUiThread(() -> Toast.makeText(ShareDispatcherActivity.this, "Failed to convert " + originalFormatForToast + " to MP3 for One-Step. Please use an MP3 file or select Two-Step mode in settings.", Toast.LENGTH_LONG).show());
                finish();
                return; // Abort
            }
        }

        // If we reach here for One-Step, fileToSendToMain should be a valid MP3
        Log.d(TAG, "One-Step: Proceeding to MainActivity with MP3: " + fileToSendToMain.getAbsolutePath());
        final File finalPathForMain = fileToSendToMain; // Effectively final for lambda
        runOnUiThread(() -> {
            Toast.makeText(ShareDispatcherActivity.this, "Audio prepared for One-Step processing...", Toast.LENGTH_LONG).show();
            Intent mainActivityIntent = new Intent(ShareDispatcherActivity.this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainActivityIntent.putExtra(MainActivity.EXTRA_SHARED_AUDIO_FILE_PATH, finalPathForMain.getAbsolutePath());
            startActivity(mainActivityIntent);
            finish();
        });

    } else { // Two-Step Transcription mode (or default)
        File fileForTwoStep = copiedAudioFile; // Start with the original copied file
        if (!isMp3Extension) { // If not an MP3 by extension, try to convert for Whisper robustness
            Log.d(TAG, "Two-Step: Shared audio is not MP3 (Ext: " + fileExtension + "). Attempting transcoding for robust Whisper processing.");
            File cacheDir = getCacheDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String mp3FileName = "shared_audio_transcoded_for_whisper_" + timeStamp + ".mp3";
            File mp3Output = new File(cacheDir, mp3FileName);
            File transcodedMp3File = AudioUtils.convertToMp3(copiedAudioFile, mp3Output);

            if (transcodedMp3File != null && transcodedMp3File.exists() && transcodedMp3File.getName().toLowerCase().endsWith(".mp3")) {
                Log.d(TAG, "Two-Step: Transcoding to MP3 successful: " + transcodedMp3File.getAbsolutePath());
                fileForTwoStep = transcodedMp3File;
                if (!copiedAudioFile.delete()) {
                    Log.w(TAG, "Failed to delete original non-MP3 temp file (two-step path): " + copiedAudioFile.getAbsolutePath());
                }
            } else {
                Log.w(TAG, "Two-Step: Transcoding to MP3 failed. Using original copied file for Whisper: " + copiedAudioFile.getName() + " (AudioUtils may not support this format for MP3 conversion).");
                // fileForTwoStep remains copiedAudioFile, Whisper endpoint might handle it
            }
        } else {
            Log.d(TAG, "Two-Step: Shared audio has .mp3 extension. Using original copied file: " + copiedAudioFile.getName());
        }
        processAsTwoStep(fileForTwoStep);
    }
}

private void processAsTwoStep(File audioFileToProcess) {
    final UploadTask uploadTask = UploadTask.createAudioTranscriptionTask(
            audioFileToProcess.getAbsolutePath(),
            "whisper-1", // Default model for initial Whisper transcription
            ""           // Default empty prompt
    );

    AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
    Executors.newSingleThreadExecutor().execute(() -> {
        long taskId = database.uploadTaskDao().insert(uploadTask);
        uploadTask.id = taskId;

        Log.d(TAG, "Shared audio UploadTask (Two-Step flow) inserted with ID: " + uploadTask.id + " for file " + audioFileToProcess.getName());
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Shared audio (Two-Step) transcription task queued.", "File: " + audioFileToProcess.getAbsolutePath() + ", TaskID: " + uploadTask.id);
        UploadService.startUploadService(ShareDispatcherActivity.this);

        runOnUiThread(() -> {
            // Toast message might vary if this is a fallback
            Toast.makeText(ShareDispatcherActivity.this, "Audio sent for standard transcription.", Toast.LENGTH_LONG).show();
            Intent mainActivityIntent = new Intent(ShareDispatcherActivity.this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mainActivityIntent.putExtra(MainActivity.EXTRA_SHARED_AUDIO_TASK_ID, uploadTask.id);
            mainActivityIntent.putExtra(MainActivity.EXTRA_SHARED_AUDIO_FILE_PATH, audioFileToProcess.getAbsolutePath());
            startActivity(mainActivityIntent);
            finish();
        });
    });
    }

    private String getFileExtensionFromUri(Uri uri, String mimeTypeHint) {
        String extension = null;
        // Try to get extension from file name in URI path first
        String path = uri.getPath();
        if (path != null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < path.length() - 1) {
                extension = path.substring(lastDot + 1);
            }
        }

        // If not found from path, try from MIME type
        if (extension == null || extension.isEmpty()) {
            if (mimeTypeHint != null) {
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeTypeHint);
            }
        }

        // If still not found, and it's a content URI, try querying display name
        if ((extension == null || extension.isEmpty()) && ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    if (displayName != null) {
                        int lastDot = displayName.lastIndexOf('.');
                        if (lastDot >= 0 && lastDot < displayName.length() - 1) {
                            extension = displayName.substring(lastDot + 1);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not determine extension from display name for URI: " + uri, e);
            }
        }

        // Fallback if all else fails
        if (extension == null || extension.isEmpty()) {
            if (mimeTypeHint != null && mimeTypeHint.startsWith("audio/")) {
                extension = "audio"; // Generic audio fallback
            } else if (mimeTypeHint != null && mimeTypeHint.startsWith("image/")) {
                extension = "img"; // Generic image fallback
            } else {
                extension = "tmp"; // Generic fallback
            }
        }
        return extension.toLowerCase(Locale.US);
    }


    private void handleSharedImage(Uri imageUri) {
        Log.d(TAG, "Handling shared image: " + imageUri.toString());
        String originalMimeType = getContentResolver().getType(imageUri);
        String fileExtension = getFileExtensionFromUri(imageUri, originalMimeType);

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
