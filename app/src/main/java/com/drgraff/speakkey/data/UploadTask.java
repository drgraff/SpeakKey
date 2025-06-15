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
    public String modelName;  // This is for the vision model
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

    // No-arg constructor for Room
    public UploadTask() {
        this.status = STATUS_PENDING;
        this.retryCount = 0;
        this.creationTimestamp = System.currentTimeMillis();
    }

    // General purpose constructor for manual instantiation, especially for older audio tasks or basic setup
    // This will be called by old code that only knows about filePath and uploadType for audio.
    // New audio tasks should use the more specific constructor.
    @Ignore
    public UploadTask(String filePath, String uploadType) {
        this(); // Calls no-arg constructor for defaults
        this.filePath = filePath;
        this.uploadType = uploadType;
        // Default transcription model and hint if not specified (e.g. for older Whisper via UploadService path)
        if (TYPE_AUDIO_TRANSCRIPTION.equals(uploadType)) {
            this.modelNameForTranscription = "whisper-1"; // Default model
            this.transcriptionHint = ""; // Default empty hint
        }
    }

    // Constructor specifically for new Audio Transcription tasks including model and hint
    @Ignore
    public UploadTask(String filePath, String uploadType, String modelNameForTranscription, String transcriptionHint) {
        this(); // Calls no-arg constructor for defaults
        if (!TYPE_AUDIO_TRANSCRIPTION.equals(uploadType)) {
            throw new IllegalArgumentException("This constructor is for AUDIO_TRANSCRIPTION tasks. Type was: " + uploadType);
        }
        this.filePath = filePath;
        this.uploadType = uploadType;
        this.modelNameForTranscription = modelNameForTranscription;
        this.transcriptionHint = transcriptionHint;
    }

    // Constructor for Photo Vision tasks
    @Ignore
    public UploadTask(String filePath, String uploadType, String visionPrompt, String visionModel) {
        this(); // Calls no-arg constructor for defaults
        if (!TYPE_PHOTO_VISION.equals(uploadType)) {
            throw new IllegalArgumentException("This constructor is for PHOTO_VISION tasks. Type was: " + uploadType);
        }
        this.filePath = filePath;
        this.uploadType = uploadType;
        this.promptText = visionPrompt;
        this.modelName = visionModel; // This is for the vision model name
    }
}
