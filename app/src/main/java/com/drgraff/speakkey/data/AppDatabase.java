package com.drgraff.speakkey.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {UploadTask.class}, version = 1, exportSchema = false) // Set exportSchema to true if you plan to export schemas
public abstract class AppDatabase extends RoomDatabase {

    public abstract UploadTaskDao uploadTaskDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "speakkey_database")
                            // Consider adding migration strategies for production apps
                            // .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .fallbackToDestructiveMigration() // Use this only during development
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
