package com.drgraff.speakkey.formattingtags;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FormattingTagDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "formatting_tags.db";
    public static final int DATABASE_VERSION = 3; // Incremented version

    public static final String TABLE_FORMATTING_TAGS = "formatting_tags";
    public static final String COLUMN_ID = "_id"; // Standard convention for primary key
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_OPENING_TAG_TEXT = "opening_tag_text";
    // public static final String COLUMN_CLOSING_TAG_TEXT = "closing_tag_text"; // Removed
    public static final String COLUMN_KEYSTROKE_SEQUENCE = "keystroke_sequence";
    public static final String COLUMN_IS_ACTIVE = "is_active";
    public static final String COLUMN_DELAY_MS = "delay_ms"; // New column for delay

    // Updated table creation string
    private static final String TABLE_CREATE_V3 =
            "CREATE TABLE " + TABLE_FORMATTING_TAGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_OPENING_TAG_TEXT + " TEXT NOT NULL UNIQUE, " +
                    // COLUMN_CLOSING_TAG_TEXT + " TEXT NOT NULL, " + // Removed
                    COLUMN_KEYSTROKE_SEQUENCE + " TEXT NOT NULL, " +
                    COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
                    COLUMN_DELAY_MS + " INTEGER NOT NULL DEFAULT 0" + // Added delay_ms column
                    ");";

    public FormattingTagDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_V3); // Use new creation string
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // If upgrading from a version before V2, drop and recreate (as per original logic)
            // This handles the case where the table might not even have the V2 schema.
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_FORMATTING_TAGS);
            onCreate(db); // Recreate with the new schema (V3)
        }
        if (oldVersion < 3) {
            // If upgrading from V2 to V3, add the new column
            // Note: If oldVersion < 2, onCreate would have already created the table with V3 schema.
            // This block specifically targets upgrades from V2.
            if (oldVersion == 2) { // Only run ALTER TABLE if upgrading from exactly V2
                 db.execSQL("ALTER TABLE " + TABLE_FORMATTING_TAGS + " ADD COLUMN " + COLUMN_DELAY_MS + " INTEGER NOT NULL DEFAULT 0;");
            }
        }
        // Add further migration steps for future versions if (oldVersion < 4), etc.
    }
}
