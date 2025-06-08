package com.drgraff.speakkey;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class MainApplication extends Application {

    public static final String UPLOAD_PROGRESS_CHANNEL_ID = "UploadProgressChannel";
    public static final String UPLOAD_COMPLETE_CHANNEL_ID = "UploadCompleteChannel";
    public static final String UPLOAD_FAILED_CHANNEL_ID = "UploadFailedChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel for Ongoing Uploads (Foreground Service)
            NotificationChannel progressChannel = new NotificationChannel(
                    UPLOAD_PROGRESS_CHANNEL_ID,
                    "Upload Progress",
                    NotificationManager.IMPORTANCE_LOW // Low importance for ongoing, less intrusive
            );
            progressChannel.setDescription("Shows the progress of ongoing uploads.");

            // Channel for Successful Uploads
            NotificationChannel completeChannel = new NotificationChannel(
                    UPLOAD_COMPLETE_CHANNEL_ID,
                    "Uploads Successful",
                    NotificationManager.IMPORTANCE_DEFAULT // Default importance for completion
            );
            completeChannel.setDescription("Notifies when an upload is successfully completed.");

            // Channel for Failed Uploads
            NotificationChannel failedChannel = new NotificationChannel(
                    UPLOAD_FAILED_CHANNEL_ID,
                    "Upload Failures",
                    NotificationManager.IMPORTANCE_HIGH // High importance for failures
            );
            failedChannel.setDescription("Notifies about failed uploads that require attention.");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(progressChannel);
                manager.createNotificationChannel(completeChannel);
                manager.createNotificationChannel(failedChannel);
            }
        }
    }
}
