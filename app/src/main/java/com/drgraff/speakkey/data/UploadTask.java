package com.drgraff.speakkey.data;

import androidx.room.Entity;
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
    public String transcriptionResult; // For storing the result of audio transcription
    public String promptText; // For TYPE_PHOTO_VISION
    public String modelName; // For TYPE_PHOTO_VISION
    public String visionApiResponse; // For TYPE_PHOTO_VISION result

    // Status constants
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    // UploadType constants
    public static final String TYPE_AUDIO_TRANSCRIPTION = "AUDIO_TRANSCRIPTION";
    public static final String TYPE_PHOTO_VISION = "PHOTO_VISION";


    public UploadTask(String filePath, String uploadType) {
        this.filePath = filePath;
        this.uploadType = uploadType;
        this.status = STATUS_PENDING; // Use constant
        this.retryCount = 0;
        this.creationTimestamp = System.currentTimeMillis();
        this.lastAttemptTimestamp = 0; // Or System.currentTimeMillis() if attempting immediately
        this.transcriptionResult = null;
        this.promptText = null;
        this.modelName = null;
        this.visionApiResponse = null;
    }

    // Constructor for Audio Transcription
    public UploadTask(String filePath, String uploadType, boolean isAudio) {
        this(filePath, uploadType); // Calls the main constructor
        if (!isAudio) {
            // This is a simple way to differentiate, could be more robust
            throw new IllegalArgumentException("This constructor is for audio tasks.");
        }
    }

    // Constructor for Photo Vision
    public UploadTask(String filePath, String uploadType, String promptText, String modelName) {
        this(filePath, uploadType); // Calls the main constructor
        this.promptText = promptText;
        this.modelName = modelName;
    }


    // It's good practice to have a no-arg constructor for Room, though not strictly required if all fields are public
    // Or if you provide a constructor that Room can use (like the one above if all params match fields)
    // However, for complex objects or if you want to ensure Room uses specific constructor, you might need @Ignore annotations for others
}
