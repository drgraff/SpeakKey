package com.drgraff.speakkey.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.drgraff.speakkey.data.AppDatabase;
import com.drgraff.speakkey.data.UploadTask;
import com.drgraff.speakkey.data.UploadTaskDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UploadServiceTest {

    private AppDatabase database;
    private UploadTaskDao uploadTaskDao;
    private Context context;
    private File dummyAudioFile;
    private File dummyPhotoFile;

    @Before
    public void setUp() throws IOException {
        context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        uploadTaskDao = database.uploadTaskDao();

        // Setup SharedPreferences for API keys (UploadService reads these)
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("openai_api_key", "test_api_key"); // Dummy key
        editor.putString("whisper_endpoint", "https://api.openai.com");
        editor.putString("language", "en");
        editor.apply();

        // Create dummy files
        File cacheDir = context.getCacheDir();
        dummyAudioFile = new File(cacheDir, "test_audio.mp3");
        dummyPhotoFile = new File(cacheDir, "test_photo.jpg");
        if (!dummyAudioFile.exists()) dummyAudioFile.createNewFile();
        if (!dummyPhotoFile.exists()) dummyPhotoFile.createNewFile();
    }

    @After
    public void tearDown() throws IOException {
        database.close();
        if (dummyAudioFile.exists()) dummyAudioFile.delete();
        if (dummyPhotoFile.exists()) dummyPhotoFile.delete();
    }

    @Test
    public void testServiceProcessesTasks() throws Exception {
        // Task 1: Audio transcription - simulated to succeed by UploadService's current logic
        UploadTask audioTask = new UploadTask(dummyAudioFile.getAbsolutePath(), UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        audioTask.id = 1; // Explicitly set for predictability in test, though not best practice for real ID generation
        uploadTaskDao.insert(audioTask);

        // Task 2: Photo vision - simulated to fail by UploadService's current logic (e.g. if Random makes it fail)
        // To make it more deterministic for testing failure, we could rely on the service's current random failure,
        // or ideally, inject a mock API that we control. For now, we'll assume the service's simulation can lead to failure.
        // Let's assume the default random in UploadService makes photo vision fail more often.
        UploadTask photoTask = new UploadTask(dummyPhotoFile.getAbsolutePath(), UploadTask.TYPE_PHOTO_VISION, "describe this", "gpt-4-vision-preview");
        photoTask.id = 2;
        uploadTaskDao.insert(photoTask);

        Intent serviceIntent = new Intent(context, UploadService.class);
        serviceIntent.setAction(UploadService.ACTION_UPLOAD);

        // Start the service. Using InstrumentationRegistry for this.
        // Note: Testing IntentService directly can be tricky.
        // Consider using ServiceTestRule if more complex lifecycle management is needed.
        // For now, a direct start and sleep should work for this simple case.
        context.startService(serviceIntent);

        // Wait for the service to process.
        // The service has a Thread.sleep(1000-3000) per task in its simulation part of performAudio/Photo.
        // It also does DB operations. Give it enough time.
        // This is a fragile way to test. A CountDownLatch or IdlingResource would be better.
        Thread.sleep(TimeUnit.SECONDS.toMillis(8)); // Wait for 2 tasks (3-4s each + overhead)

        // Verify audio task (simulated success by service, then deleted)
        UploadTask processedAudioTask = uploadTaskDao.getTaskById(audioTask.id);
        assertNull("Audio task should be deleted after successful processing", processedAudioTask);
        assertFalse("Dummy audio file should be deleted after successful processing", dummyAudioFile.exists());


        // Verify photo task (simulated failure by service)
        UploadTask processedPhotoTask = uploadTaskDao.getTaskById(photoTask.id);
        assertNotNull("Photo task should still exist if it failed", processedPhotoTask);
        assertEquals("Photo task status should be FAILED", UploadTask.STATUS_FAILED, processedPhotoTask.status);
        assertTrue("Photo task retry count should be incremented", processedPhotoTask.retryCount > 0);
        assertNotNull("Photo task error message should be set", processedPhotoTask.errorMessage);
        // The dummy photo file for a failed task should NOT be deleted by the service
        assertTrue("Dummy photo file for failed task should still exist", dummyPhotoFile.exists());
    }
}
