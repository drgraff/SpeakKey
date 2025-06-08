package com.drgraff.speakkey.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UploadTaskDao {

    @Insert
    void insert(UploadTask task);

    @Update
    void update(UploadTask task);

    @Delete
    void delete(UploadTask task);

    @Query("SELECT * FROM upload_tasks WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY creationTimestamp ASC")
    List<UploadTask> getPendingUploads();

    @Query("SELECT * FROM upload_tasks WHERE id = :id")
    UploadTask getTaskById(long id);

    @Query("DELETE FROM upload_tasks WHERE status = 'SUCCESS'")
    void clearSuccessfulTasks();

    @Query("SELECT * FROM upload_tasks WHERE filePath = :filePath ORDER BY creationTimestamp DESC")
    List<UploadTask> getTasksByFilePath(String filePath);
}
