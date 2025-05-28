package com.drgraff.speakkey.formattingtags;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FormattingTagDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "formatting_tags.db";
    public static final int DATABASE_VERSION = 2; // Incremented version

    public static final String TABLE_FORMATTING_TAGS = "formatting_tags";
    public static final String COLUMN_ID = "_id"; // Standard convention for primary key
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_OPENING_TAG_TEXT = "opening_tag_text";
    // public static final String COLUMN_CLOSING_TAG_TEXT = "closing_tag_text"; // Removed
    public static final String COLUMN_KEYSTROKE_SEQUENCE = "keystroke_sequence";
    public static final String COLUMN_IS_ACTIVE = "is_active";

    // Updated table creation string
    private static final String TABLE_CREATE_V2 =
            "CREATE TABLE " + TABLE_FORMATTING_TAGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_OPENING_TAG_TEXT + " TEXT NOT NULL UNIQUE, " +
                    // COLUMN_CLOSING_TAG_TEXT + " TEXT NOT NULL, " + // Removed
                    COLUMN_KEYSTROKE_SEQUENCE + " TEXT NOT NULL, " +
                    COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1" +
                    ");";

    public FormattingTagDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_V2); // Use new creation string
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // For simplicity in this development stage, we drop and recreate.
            // A real-world app might use ALTER TABLE to preserve data.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORMATTING_TAGS);
            onCreate(db); // Recreate with the new schema (V2)
        }
        // Add further migration steps for future versions if (oldVersion < 3), etc.
    }
}
