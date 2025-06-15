package com.drgraff.speakkey.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {UploadTask.class}, version = 2, exportSchema = false) // Incremented version
public abstract class AppDatabase extends RoomDatabase {

    public abstract UploadTaskDao uploadTaskDao();

    private static volatile AppDatabase INSTANCE;

    // Migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new columns for UploadTask table.
            // The table name is "upload_tasks" as defined in UploadTask.java @Entity annotation
            database.execSQL("ALTER TABLE upload_tasks ADD COLUMN model_name_for_transcription TEXT");
            database.execSQL("ALTER TABLE upload_tasks ADD COLUMN transcription_hint TEXT");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "speakkey_database")
                            .addMigrations(MIGRATION_1_2) // Added migration
                            // .fallbackToDestructiveMigration() // Removed
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
