package com.drgraff.speakkey.data;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UploadTaskDaoTest {

    private AppDatabase database;
    private UploadTaskDao uploadTaskDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                // Allowing main thread queries, just for testing.
                .allowMainThreadQueries()
                .build();
        uploadTaskDao = database.uploadTaskDao();
    }

    @After
    public void closeDb() throws IOException {
        database.close();
    }

    @Test
    public void insertAndGetTaskById() throws Exception {
        UploadTask task = UploadTask.createAudioTranscriptionTask("path/to/file1", "whisper-1", "");
        uploadTaskDao.insert(task);
        // The ID is auto-generated, so we need to fetch all tasks or query by known fields if ID is not easily predictable.
        // For simplicity, let's assume it's the first task and ID is 1, or fetch it differently.
        List<UploadTask> allTasks = uploadTaskDao.getTasksByFilePath("path/to/file1");
        assertFalse(allTasks.isEmpty());
        UploadTask retrievedTask = uploadTaskDao.getTaskById(allTasks.get(0).id);

        assertNotNull(retrievedTask);
        assertEquals(task.filePath, retrievedTask.filePath);
        assertEquals(task.uploadType, retrievedTask.uploadType);
    }

    @Test
    public void insertAndGetTasksByFilePath() throws Exception {
        UploadTask task1 = UploadTask.createAudioTranscriptionTask("path/file.txt", "whisper-1", "");
        UploadTask task2 = UploadTask.createPhotoVisionTask("path/file.txt", "prompt", "model");
        uploadTaskDao.insert(task1);
        uploadTaskDao.insert(task2);

        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("path/file.txt");
        assertEquals(2, tasks.size());
        // Tasks should be ordered by creationTimestamp DESC by default in getTasksByFilePath
        assertTrue(tasks.get(0).creationTimestamp >= tasks.get(1).creationTimestamp);
    }

    @Test
    public void updateTaskAndVerify() throws Exception {
        UploadTask task = UploadTask.createAudioTranscriptionTask("path/update_me.txt", "whisper-1", "");
        uploadTaskDao.insert(task);
        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("path/update_me.txt");
        UploadTask insertedTask = tasks.get(0); // Get the inserted task with its ID

        insertedTask.status = UploadTask.STATUS_UPLOADING;
        insertedTask.retryCount = 1;
        uploadTaskDao.update(insertedTask);

        UploadTask updatedTask = uploadTaskDao.getTaskById(insertedTask.id);
        assertNotNull(updatedTask);
        assertEquals(UploadTask.STATUS_UPLOADING, updatedTask.status);
        assertEquals(1, updatedTask.retryCount);
    }

    @Test
    public void deleteTaskAndVerify() throws Exception {
        UploadTask task = UploadTask.createAudioTranscriptionTask("path/delete_me.txt", "whisper-1", "");
        uploadTaskDao.insert(task);
        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("path/delete_me.txt");
        UploadTask insertedTask = tasks.get(0);

        assertNotNull(uploadTaskDao.getTaskById(insertedTask.id));
        uploadTaskDao.delete(insertedTask);
        assertNull(uploadTaskDao.getTaskById(insertedTask.id));
    }

    @Test
    public void getPendingUploadsFiltersAndOrders() throws Exception {
        UploadTask pendingAudio = UploadTask.createAudioTranscriptionTask("pending/audio.mp3", "whisper-1", "");
        pendingAudio.status = UploadTask.STATUS_PENDING;
        pendingAudio.creationTimestamp = System.currentTimeMillis();
        uploadTaskDao.insert(pendingAudio);

        Thread.sleep(10); // Ensure different timestamp

        UploadTask failedPhoto = UploadTask.createPhotoVisionTask("failed/photo.jpg", "prompt", "model");
        failedPhoto.status = UploadTask.STATUS_FAILED;
        failedPhoto.creationTimestamp = System.currentTimeMillis();
        uploadTaskDao.insert(failedPhoto);

        Thread.sleep(10);

        UploadTask successAudio = UploadTask.createAudioTranscriptionTask("success/audio.mp3", "whisper-1", "");
        successAudio.status = UploadTask.STATUS_SUCCESS;
        successAudio.creationTimestamp = System.currentTimeMillis();
        uploadTaskDao.insert(successAudio);

        List<UploadTask> pending = uploadTaskDao.getPendingUploads();
        assertEquals(2, pending.size()); // Should get PENDING and FAILED
        assertEquals(pendingAudio.filePath, pending.get(0).filePath); // Ordered by creationTimestamp ASC
        assertEquals(failedPhoto.filePath, pending.get(1).filePath);
        assertEquals(UploadTask.STATUS_PENDING, pending.get(0).status);
        assertEquals(UploadTask.STATUS_FAILED, pending.get(1).status);
    }

    @Test
    public void clearSuccessfulTasksActuallyClears() throws Exception {
        UploadTask success1 = UploadTask.createAudioTranscriptionTask("s1.txt", "whisper-1", "");
        success1.status = UploadTask.STATUS_SUCCESS;
        uploadTaskDao.insert(success1);

        UploadTask pending1 = UploadTask.createAudioTranscriptionTask("p1.txt", "whisper-1", "");
        pending1.status = UploadTask.STATUS_PENDING;
        uploadTaskDao.insert(pending1);

        uploadTaskDao.clearSuccessfulTasks();

        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("s1.txt");
        assertTrue(tasks.isEmpty());

        List<UploadTask> pendingTasks = uploadTaskDao.getTasksByFilePath("p1.txt");
        assertFalse(pendingTasks.isEmpty());
    }
}
