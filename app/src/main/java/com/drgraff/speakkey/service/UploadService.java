package com.drgraff.speakkey.service;

import android.app.IntentService;
import android.content.Context;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.drgraff.speakkey.MainActivity; // For PendingIntent
import com.drgraff.speakkey.MainApplication; // For Channel IDs
import com.drgraff.speakkey.PhotosActivity; // For PendingIntent
import com.drgraff.speakkey.R; // For icons (assuming you have some)
import com.drgraff.speakkey.api.ChatGptApi;
import com.drgraff.speakkey.api.ChatGptRequest;
import com.drgraff.speakkey.api.WhisperApi;
import com.drgraff.speakkey.data.AppDatabase;
import com.drgraff.speakkey.data.UploadTask;
import com.drgraff.speakkey.data.UploadTaskDao;

import java.io.ByteArrayOutputStream;
import java.io.File; // Added
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList; // Added
import java.util.List;
import java.util.Random; // For simulating success/failure
import android.graphics.Bitmap; // Added
import android.graphics.BitmapFactory; // Added
import android.util.Base64; // Added

public class UploadService extends IntentService {
    private static final String TAG = "UploadService";
    public static final String ACTION_UPLOAD = "com.drgraff.speakkey.service.action.UPLOAD";

    public static final String ACTION_TRANSCRIPTION_COMPLETE = "com.drgraff.speakkey.service.action.TRANSCRIPTION_COMPLETE";
    public static final String EXTRA_FILE_PATH = "com.drgraff.speakkey.service.extra.FILE_PATH";
    public static final String EXTRA_TRANSCRIPTION_RESULT = "com.drgraff.speakkey.service.extra.TRANSCRIPTION_RESULT";
    // Using task.id (long) directly is fine, but if a String ID was ever needed:
    // public static final String EXTRA_TASK_ID_STRING = "com.drgraff.speakkey.service.extra.TASK_ID_STRING";
    public static final String EXTRA_TASK_ID_LONG = "com.drgraff.speakkey.service.extra.TASK_ID_LONG";

    public static final String ACTION_PHOTO_VISION_COMPLETE = "com.drgraff.speakkey.service.action.PHOTO_VISION_COMPLETE";
    public static final String EXTRA_PHOTO_FILE_PATH = "com.drgraff.speakkey.service.extra.PHOTO_FILE_PATH";
    public static final String EXTRA_VISION_RESULT = "com.drgraff.speakkey.service.extra.VISION_RESULT";
    public static final String EXTRA_PHOTO_TASK_ID_LONG = "com.drgraff.speakkey.service.extra.PHOTO_TASK_ID_LONG";
    public static final String ACTION_PHOTO_TASK_QUEUED = "com.drgraff.speakkey.service.action.PHOTO_TASK_QUEUED"; // Added

    private static final int ONGOING_NOTIFICATION_ID = 1001;
    private static final int SUCCESS_NOTIFICATION_ID_OFFSET = 2000; // So each success can have a unique ID
    private static final int FAILED_NOTIFICATION_ID_OFFSET = 3000; // So each failure can have a unique ID
    private static final int MAX_RETRIES = 5; // Added

    private UploadTaskDao uploadTaskDao;
    private NotificationManagerCompat notificationManager;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public UploadService() {
        super("UploadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "UploadService created.");
        AppDatabase database = AppDatabase.getDatabase(getApplicationContext());
        uploadTaskDao = database.uploadTaskDao();
        notificationManager = NotificationManagerCompat.from(this);
    }

    private void showOngoingNotification(String text, int currentProgress, int maxProgress, boolean isStarting) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainApplication.UPLOAD_PROGRESS_CHANNEL_ID)
                .setContentTitle("Upload in Progress")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_upload) // Placeholder icon
                .setOngoing(true)
                .setOnlyAlertOnce(true);

        if (maxProgress > 0) {
            builder.setProgress(maxProgress, currentProgress, false);
        } else {
            builder.setProgress(0, 0, true); // Indeterminate progress
        }

        Notification notification = builder.build();

        if (isStarting) {
            startForeground(ONGOING_NOTIFICATION_ID, notification);
        } else {
            notificationManager.notify(ONGOING_NOTIFICATION_ID, notification);
        }
    }

    private void showSuccessNotification(UploadTask task) {
        String title = "Upload Successful";
        String content = "File processed: " + new File(task.filePath).getName();
        Intent resultIntent;

        if (UploadTask.TYPE_AUDIO_TRANSCRIPTION.equals(task.uploadType)) {
            title = "Audio Transcribed";
            resultIntent = new Intent(this, MainActivity.class);
            // Optionally, add extras to MainActivity to highlight the specific task or audio file.
            // resultIntent.putExtra("processed_task_id", task.id);
        } else if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
            title = "Photo Processed";
            resultIntent = new Intent(this, PhotosActivity.class);
            // resultIntent.putExtra("processed_task_id", task.id);
        } else {
            Log.w(TAG, "Unknown task type for success notification: " + task.uploadType);
            return; // Don't show notification for unknown type
        }

        // Cast task.id to int for requestCode
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) task.id /* requestCode */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainApplication.UPLOAD_COMPLETE_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done) // Placeholder icon
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Cast task.id to int for notificationId
        notificationManager.notify(SUCCESS_NOTIFICATION_ID_OFFSET + (int) task.id, builder.build());
    }

    private void showFailedNotification(UploadTask task) {
        String title = "Upload Failed";
        String content = "File failed: " + new File(task.filePath).getName();
        Intent resultIntent;

        // For now, all failures point to MainActivity. This could be more specific.
        if (UploadTask.TYPE_AUDIO_TRANSCRIPTION.equals(task.uploadType)) {
             resultIntent = new Intent(this, MainActivity.class);
        } else if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
             resultIntent = new Intent(this, PhotosActivity.class);
        } else {
            resultIntent = new Intent(this, MainActivity.class); // Default
        }
        // resultIntent.putExtra("failed_task_id", task.id); // To highlight in UI

        // Cast task.id to int for requestCode
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int) task.id /* requestCode */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainApplication.UPLOAD_FAILED_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content + "\n" + task.errorMessage)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content + "\n" + task.errorMessage))
                .setSmallIcon(android.R.drawable.stat_notify_error) // Placeholder icon
                .setContentIntent(pendingIntent)
                .setAutoCancel(false); // Keep it until user dismisses or acts

        // Cast task.id to int for notificationId
        notificationManager.notify(FAILED_NOTIFICATION_ID_OFFSET + (int) task.id, builder.build());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || !ACTION_UPLOAD.equals(intent.getAction())) {
            Log.w(TAG, "Intent is null or action is not UPLOAD. Exiting.");
            return;
        }

        Log.d(TAG, "UploadService started, handling intent.");
        showOngoingNotification("Preparing to upload...", 0, 0, true);

        List<UploadTask> pendingTasks = uploadTaskDao.getPendingUploads();

        if (pendingTasks == null || pendingTasks.isEmpty()) {
            Log.d(TAG, "No pending uploads found from DAO."); // Modified log message
            stopForeground(true); // Remove ongoing notification
            return;
        }
        Log.d(TAG, "Fetched " + pendingTasks.size() + " tasks from DAO. Filtering by retryCount < " + MAX_RETRIES);

        List<UploadTask> processableTasks = new ArrayList<>();
        for (UploadTask task : pendingTasks) {
            if (task.retryCount < MAX_RETRIES) {
                processableTasks.add(task);
            } else {
                Log.w(TAG, "Skipping task ID: " + task.id + " (" + task.uploadType + ") as it has reached max retries (" + task.retryCount + "). File: " + task.filePath);
                if (!UploadTask.STATUS_FAILED.equals(task.status)) {
                    task.status = UploadTask.STATUS_FAILED;
                    if (task.errorMessage == null) {
                        task.errorMessage = "Task exceeded max retries.";
                    }
                    uploadTaskDao.update(task);
                }
            }
        }

        if (processableTasks.isEmpty()) {
            Log.d(TAG, "No processable tasks after filtering by retryCount.");
            stopForeground(true);
            return;
        }

        Log.d(TAG, "Processing " + processableTasks.size() + " tasks after filtering.");
        int totalTasks = processableTasks.size(); // Use processableTasks
        int tasksProcessed = 0;

        // Log.d(TAG, "Found " + totalTasks + " pending tasks."); // Original log, use new totalTasks

        for (UploadTask task : processableTasks) { // Use processableTasks
            tasksProcessed++;
            String fileName = new File(task.filePath).getName();
            showOngoingNotification("Processing: " + fileName + " (" + tasksProcessed + "/" + totalTasks + ")", tasksProcessed, totalTasks, false);

            // Check if it's a photo task and its status is PENDING (meaning it's just been picked up)
            // The status check here is a bit tricky because getPendingUploads() fetches based on STATUS_PENDING.
            // However, if the service was stopped and restarted, a task might be re-fetched from DB
            // while it was already STATUS_UPLOADING or STATUS_PROCESSING.
            // For this broadcast, we're interested in the *first time* it's picked from PENDING.
            // The current logic in onHandleIntent already filters for retryCount < MAX_RETRIES.
            // If a task is picked up here, its status *should* be PENDING if it's truly new.
            // To be more precise, we might need to check the status *before* it's set to UPLOADING below.
            // However, the DAO call `getPendingUploads()` should ideally only return tasks that are indeed PENDING.
            // Let's assume `task.status` reflects its state from the DB at this point (before being set to UPLOADING).
            if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType) && UploadTask.STATUS_PENDING.equals(task.status)) {
                Intent queuedBroadcastIntent = new Intent(ACTION_PHOTO_TASK_QUEUED);
                queuedBroadcastIntent.putExtra(EXTRA_PHOTO_FILE_PATH, task.filePath);
                queuedBroadcastIntent.putExtra(EXTRA_PHOTO_TASK_ID_LONG, (long) task.id); // Ensure task.id is cast to long
                LocalBroadcastManager.getInstance(this).sendBroadcast(queuedBroadcastIntent);
                Log.d(TAG, "Sent ACTION_PHOTO_TASK_QUEUED broadcast for photo task ID: " + task.id + ", File: " + task.filePath);
            }

            Log.d(TAG, "Processing task ID: " + task.id + ", Type: " + task.uploadType + ", File: " + task.filePath);
            task.status = UploadTask.STATUS_UPLOADING;
            task.lastAttemptTimestamp = System.currentTimeMillis();
            task.errorMessage = null;
            task.transcriptionResult = null; // Assuming visionApiResponse is also handled if it's a separate field
            if (task.uploadType.equals(UploadTask.TYPE_PHOTO_VISION)) task.visionApiResponse = null;
            uploadTaskDao.update(task);

            boolean success = false;
            if (UploadTask.TYPE_AUDIO_TRANSCRIPTION.equals(task.uploadType)) {
                success = performAudioTranscription(task);
            } else if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                success = performPhotoVisionProcessing(task);
            } else {
                Log.w(TAG, "Unknown upload type: " + task.uploadType + " for task ID: " + task.id);
                task.errorMessage = "Unknown upload type";
                success = false;
            }

            if (success) {
                task.status = UploadTask.STATUS_SUCCESS;
                Log.d(TAG, "Task ID: " + task.id + " processed successfully.");
                showSuccessNotification(task);

                // Cleanup for successful task - conditionally delete
                if (UploadTask.TYPE_AUDIO_TRANSCRIPTION.equals(task.uploadType)) {
                    File localFile = new File(task.filePath);
                    if (localFile.exists()) {
                        if (localFile.delete()) {
                            Log.i(TAG, "Successfully deleted local audio file: " + task.filePath + " for task ID: " + task.id);
                        } else {
                            Log.w(TAG, "Failed to delete local audio file: " + task.filePath + " for task ID: " + task.id);
                        }
                    } else {
                        Log.w(TAG, "Local audio file not found for deletion: " + task.filePath + " for task ID: " + task.id);
                    }
                } else if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                    Log.i(TAG, "Skipping file deletion for photo task ID: " + task.id + ", File: " + task.filePath);
                    // Photo files are not deleted by the service to allow reprocessing from PhotosActivity.
                }
                // Delete task from DB (Option A)
                // Broadcast success before deleting
                if (UploadTask.TYPE_AUDIO_TRANSCRIPTION.equals(task.uploadType)) {
                    Intent broadcastIntent = new Intent(ACTION_TRANSCRIPTION_COMPLETE);
                    broadcastIntent.putExtra(EXTRA_FILE_PATH, task.filePath);
                    broadcastIntent.putExtra(EXTRA_TRANSCRIPTION_RESULT, task.transcriptionResult);
                    broadcastIntent.putExtra(EXTRA_TASK_ID_LONG, (long) task.id); // Cast task.id to long
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                    Log.d(TAG, "Sent ACTION_TRANSCRIPTION_COMPLETE broadcast for task ID: " + task.id);
                } else if (UploadTask.TYPE_PHOTO_VISION.equals(task.uploadType)) {
                    Intent broadcastIntent = new Intent(ACTION_PHOTO_VISION_COMPLETE);
                    broadcastIntent.putExtra(EXTRA_PHOTO_FILE_PATH, task.filePath);
                    broadcastIntent.putExtra(EXTRA_VISION_RESULT, task.visionApiResponse); // Assuming vision result is in task.visionApiResponse
                    broadcastIntent.putExtra(EXTRA_PHOTO_TASK_ID_LONG, (long) task.id); // Cast task.id to long
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                    Log.d(TAG, "Sent ACTION_PHOTO_VISION_COMPLETE broadcast for task ID: " + task.id);
                }
                uploadTaskDao.delete(task);
                Log.i(TAG, "Task ID: " + task.id + " deleted from database after successful processing.");

            } else {
                task.status = UploadTask.STATUS_FAILED;
                task.retryCount += 1;
                if (task.errorMessage == null) {
                    task.errorMessage = "Processing failed due to an unknown error.";
                }
                Log.w(TAG, "Task ID: " + task.id + " (" + task.uploadType + ") processing failed. Retry count: " + task.retryCount + ". Error: " + task.errorMessage);
                if (task.retryCount >= 5) { // Max 5 retries
                    Log.e(TAG, "Task ID: " + task.id + " has reached max retries. Marking as permanently failed (conceptually). Not attempting further retries in this run.");
                    // In a fuller implementation, you might set a specific status like STATUS_PERMANENTLY_FAILED
                    // and ensure getPendingUploads() excludes these. For now, it will just be updated.
                    // The showFailedNotification logic can remain as is (e.g., notify after 3).
                }
                // For now, show failure immediately. Retry logic might change this.
                 if (task.retryCount >= 3) { // Example: Notify failure after 3 retries
                    showFailedNotification(task);
                }
                // Update task in DB if it failed but is not yet deleted (e.g. needs more retries)
                uploadTaskDao.update(task);
            }
            // If successful, task is deleted, so no update needed here.
        }
        Log.d(TAG, "Finished processing tasks.");
        stopForeground(true); // Remove ongoing notification when all tasks are done
    }

    // performAudioTranscription and performPhotoVisionProcessing remain the same
    // encodeImageToBase64 remains the same

    private boolean performAudioTranscription(UploadTask task) {
        File audioFileCheck = new File(task.filePath);
        if (!audioFileCheck.exists() || audioFileCheck.length() == 0) {
            Log.e(TAG, "Audio file does not exist or is empty for task ID: " + task.id + " Path: " + task.filePath);
            task.errorMessage = "Audio file missing or empty at path: " + task.filePath;
            task.retryCount = MAX_RETRIES;
            return false;
        }

        Log.d(TAG, "Performing actual audio transcription for: " + task.filePath);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String apiKey = sharedPreferences.getString("openai_api_key", "");
        String whisperEndpoint = sharedPreferences.getString("whisper_endpoint", "https://api.openai.com");
        String language = sharedPreferences.getString("language", "en");

        if (apiKey.isEmpty()) {
            Log.e(TAG, "API key is missing. Cannot perform transcription for task ID: " + task.id);
            task.errorMessage = "API key is missing.";
            return false;
        }

        File audioFile = new File(task.filePath);
        if (!audioFile.exists() || audioFile.length() == 0) {
            Log.e(TAG, "Audio file does not exist or is empty for task ID: " + task.id + " Path: " + task.filePath);
            task.errorMessage = "Audio file missing or empty.";
            return false;
        }

        // WhisperApi whisperApi = new WhisperApi(apiKey, whisperEndpoint, language); // Old direct WhisperApi call
        ChatGptApi chatGptApi = new ChatGptApi(apiKey, ""); // Initialize ChatGptApi; model param not directly used by getTranscriptionFromAudio

        String modelToUse = (task.modelNameForTranscription != null && !task.modelNameForTranscription.isEmpty())
                            ? task.modelNameForTranscription
                            : "whisper-1"; // Fallback if somehow still null/empty
        String hintToUse = (task.transcriptionHint != null)
                            ? task.transcriptionHint
                            : ""; // Fallback if somehow null

        Log.d(TAG, "UploadService: Transcribing audio task ID: " + task.id +
                   " with model: " + modelToUse + " and hint: '" + hintToUse + "'");

        try {
            Log.d(TAG, "Task ID: " + task.id + " - Setting status to PROCESSING before API call (audio).");
            task.status = UploadTask.STATUS_PROCESSING;
            uploadTaskDao.update(task);

            // final String transcription = chatGptApi.getTranscriptionFromAudio(audioFile, hintToUse, modelToUse);
            String transcriptionResultText = chatGptApi.getTranscriptionFromAudio(audioFile, hintToUse, modelToUse); // Corrected variable name
            task.transcriptionResult = transcriptionResultText;
            Log.d(TAG, "Actual audio transcription successful for task ID: " + task.id + ". Result length: " + (transcriptionResultText != null ? transcriptionResultText.length() : "null"));
            return true;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SocketTimeoutException during actual audio transcription for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Network timeout: " + e.getMessage();
            return false;
        } catch (UnknownHostException e) {
            Log.e(TAG, "UnknownHostException during actual audio transcription for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Unknown host: " + e.getMessage();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException during actual audio transcription for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Network IO error: " + e.getMessage();
            return false;
        } catch (Exception e) { // Catch any other unexpected errors
            Log.e(TAG, "Unexpected exception during actual audio transcription for task ID: " + task.id + " - " + e.getMessage(), e);
            task.errorMessage = "Transcription failed: " + e.getMessage();
            return false;
        }
    }

    private boolean performPhotoVisionProcessing(UploadTask task) {
        if (task.filePath == null || task.filePath.isEmpty()) {
            Log.e(TAG, "File path is missing for photo vision task ID: " + task.id);
            task.errorMessage = "File path is missing for photo vision task.";
            task.retryCount = MAX_RETRIES;
            return false;
        }
        File photoFileCheck = new File(task.filePath);
        if (!photoFileCheck.exists() || photoFileCheck.length() == 0) {
            Log.e(TAG, "Photo file does not exist or is empty for task ID: " + task.id + " Path: " + task.filePath);
            task.errorMessage = "Photo file missing or empty at path: " + task.filePath;
            task.retryCount = MAX_RETRIES;
            return false;
        }

        Log.d(TAG, "Performing actual photo vision processing for: " + task.filePath);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String apiKey = sharedPreferences.getString("openai_api_key", "");

        if (apiKey.isEmpty()) {
            Log.e(TAG, "API key is missing. Cannot perform photo vision for task ID: " + task.id);
            task.errorMessage = "API key is missing.";
            return false;
        }
        if (task.filePath == null || task.filePath.isEmpty()) {
            Log.e(TAG, "File path is missing for task ID: " + task.id);
            task.errorMessage = "File path is missing.";
            return false;
        }
        if (task.promptText == null || task.promptText.isEmpty()) {
            Log.e(TAG, "Prompt text is missing for task ID: " + task.id);
            task.errorMessage = "Prompt text is missing.";
            return false;
        }
        if (task.modelName == null || task.modelName.isEmpty()) {
            Log.e(TAG, "Model name is missing for task ID: " + task.id);
            task.errorMessage = "Model name is missing.";
            return false;
        }

        String base64Image = encodeImageToBase64(task.filePath);
        if (base64Image == null) {
            Log.e(TAG, "Failed to encode image to Base64 for task ID: " + task.id + ", Path: " + task.filePath);
            task.errorMessage = "Failed to encode image.";
            return false;
        }
        String dataUri = "data:image/jpeg;base64," + base64Image;

        List<ChatGptRequest.ContentPart> contentParts = new ArrayList<>();
        contentParts.add(new ChatGptRequest.TextContentPart(task.promptText));
        contentParts.add(new ChatGptRequest.ImageContentPart(dataUri));

        // The ChatGptApi constructor's model parameter is not used by getVisionCompletion,
        // as the model is passed directly to that method. So, an empty string or any model is fine here.
        ChatGptApi chatGptApi = new ChatGptApi(apiKey, "");

        try {
            Log.d(TAG, "Task ID: " + task.id + " - Setting status to PROCESSING before API call (photo).");
            task.status = UploadTask.STATUS_PROCESSING;
            uploadTaskDao.update(task);

            // Using a fixed maxTokens value as it was in PhotosActivity. This could also be stored in UploadTask if needed.
            String visionResponse = chatGptApi.getVisionCompletion(contentParts, task.modelName, 1024);
            task.visionApiResponse = visionResponse;
            Log.d(TAG, "Actual photo vision processing successful for task ID: " + task.id + ". Result length: " + (visionResponse != null ? visionResponse.length() : "null"));
            return true;
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "SocketTimeoutException during actual photo vision for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Network timeout: " + e.getMessage();
            return false;
        } catch (UnknownHostException e) {
            Log.e(TAG, "UnknownHostException during actual photo vision for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Unknown host: " + e.getMessage();
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IOException during actual photo vision for task ID: " + task.id + " - " + e.getMessage());
            task.errorMessage = "Network IO error: " + e.getMessage();
            return false;
        } catch (Exception e) { // Catch any other unexpected errors
            Log.e(TAG, "Unexpected exception during actual photo vision for task ID: " + task.id + " - " + e.getMessage(), e);
            task.errorMessage = "Photo vision processing failed: " + e.getMessage();
            return false;
        }
    }

    private String encodeImageToBase64(String imagePath) {
        if (imagePath == null) return null;
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            Log.e(TAG, "encodeImageToBase64: File not found at " + imagePath);
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Or RGB_565 for smaller size if alpha not needed
        // Consider adding inSampleSize if images are very large to avoid OutOfMemoryError
        // options.inSampleSize = 2; // Example: scales down by factor of 2

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
        if (bitmap == null) {
            Log.e(TAG, "encodeImageToBase64: BitmapFactory.decodeFile returned null for " + imagePath);
            return null;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use JPEG for photos, adjust quality as needed.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        bitmap.recycle(); // Important to free up memory
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }


    /**
     * Starts this service to perform upload action. If the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startUploadService(Context context) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_UPLOAD);
        context.startService(intent);
        Log.d(TAG, "startUploadService called.");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "UploadService destroyed.");
        super.onDestroy();
    }
}
