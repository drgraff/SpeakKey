package com.drgraff.speakkey.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "upload_tasks")
public class UploadTask {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String filePath;
    public String uploadType; // e.g., "AUDIO_TRANSCRIPTION", "PHOTO_VISION"
    public String status;     // e.g., "PENDING", "UPLOADING", "FAILED", "SUCCESS"
    public int retryCount;
    public long lastAttemptTimestamp;
    public long creationTimestamp;
    public String errorMessage; // Nullable

    // Fields for AUDIO_TRANSCRIPTION
    public String transcriptionResult;
    @ColumnInfo(name = "model_name_for_transcription")
    public String modelNameForTranscription;
    @ColumnInfo(name = "transcription_hint")
    public String transcriptionHint;

    // Fields for PHOTO_VISION
    public String promptText;
    public String modelName;  // This is for the vision model used in PHOTO_VISION tasks
    public String visionApiResponse;

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // UploadType constants
    public static final String TYPE_AUDIO_TRANSCRIPTION = "AUDIO_TRANSCRIPTION";
    public static final String TYPE_PHOTO_VISION = "PHOTO_VISION";

    // No-arg constructor for Room and factory methods
    public UploadTask() {
        this.status = STATUS_PENDING;
        this.retryCount = 0;
        this.creationTimestamp = System.currentTimeMillis();
        // Ensure all fields have a default state if not set by factory
        this.filePath = null;
        this.uploadType = null;
        this.lastAttemptTimestamp = 0;
        this.errorMessage = null;
        this.transcriptionResult = null;
        this.modelNameForTranscription = null;
        this.transcriptionHint = null;
        this.promptText = null;
        this.modelName = null;
        this.visionApiResponse = null;
    }

    // Factory method for Photo Vision Tasks
    public static UploadTask createPhotoVisionTask(String filePath, String visionPrompt, String visionModelName) {
        UploadTask task = new UploadTask();
        task.filePath = filePath;
        task.uploadType = TYPE_PHOTO_VISION;
        task.promptText = visionPrompt;
        task.modelName = visionModelName; // This is for the vision model
        // other fields (like transcription ones) remain default null/empty
        return task;
    }

    // Factory method for Audio Transcription Tasks
    public static UploadTask createAudioTranscriptionTask(String filePath, String modelName, String hint) {
        UploadTask task = new UploadTask();
        task.filePath = filePath;
        task.uploadType = TYPE_AUDIO_TRANSCRIPTION;
        task.modelNameForTranscription = (modelName != null && !modelName.isEmpty()) ? modelName : "whisper-1";
        task.transcriptionHint = hint != null ? hint : "";
        // other fields (like photo vision ones) remain default null/empty
        return task;
    }
}
