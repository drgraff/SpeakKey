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
        UploadTask task = new UploadTask("path/to/file1", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
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
        UploadTask task1 = new UploadTask("path/file.txt", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        UploadTask task2 = new UploadTask("path/file.txt", UploadTask.TYPE_PHOTO_VISION, "prompt", "model");
        uploadTaskDao.insert(task1);
        uploadTaskDao.insert(task2);

        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("path/file.txt");
        assertEquals(2, tasks.size());
        // Tasks should be ordered by creationTimestamp DESC by default in getTasksByFilePath
        assertTrue(tasks.get(0).creationTimestamp >= tasks.get(1).creationTimestamp);
    }

    @Test
    public void updateTaskAndVerify() throws Exception {
        UploadTask task = new UploadTask("path/update_me.txt", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
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
        UploadTask task = new UploadTask("path/delete_me.txt", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        uploadTaskDao.insert(task);
        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("path/delete_me.txt");
        UploadTask insertedTask = tasks.get(0);

        assertNotNull(uploadTaskDao.getTaskById(insertedTask.id));
        uploadTaskDao.delete(insertedTask);
        assertNull(uploadTaskDao.getTaskById(insertedTask.id));
    }

    @Test
    public void getPendingUploadsFiltersAndOrders() throws Exception {
        UploadTask pendingAudio = new UploadTask("pending/audio.mp3", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        pendingAudio.status = UploadTask.STATUS_PENDING;
        pendingAudio.creationTimestamp = System.currentTimeMillis();
        uploadTaskDao.insert(pendingAudio);

        Thread.sleep(10); // Ensure different timestamp

        UploadTask failedPhoto = new UploadTask("failed/photo.jpg", UploadTask.TYPE_PHOTO_VISION, "prompt", "model");
        failedPhoto.status = UploadTask.STATUS_FAILED;
        failedPhoto.creationTimestamp = System.currentTimeMillis();
        uploadTaskDao.insert(failedPhoto);

        Thread.sleep(10);

        UploadTask successAudio = new UploadTask("success/audio.mp3", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
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
        UploadTask success1 = new UploadTask("s1.txt", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        success1.status = UploadTask.STATUS_SUCCESS;
        uploadTaskDao.insert(success1);

        UploadTask pending1 = new UploadTask("p1.txt", UploadTask.TYPE_AUDIO_TRANSCRIPTION, true);
        pending1.status = UploadTask.STATUS_PENDING;
        uploadTaskDao.insert(pending1);

        uploadTaskDao.clearSuccessfulTasks();

        List<UploadTask> tasks = uploadTaskDao.getTasksByFilePath("s1.txt");
        assertTrue(tasks.isEmpty());

        List<UploadTask> pendingTasks = uploadTaskDao.getTasksByFilePath("p1.txt");
        assertFalse(pendingTasks.isEmpty());
    }
}
