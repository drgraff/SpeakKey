package com.drgraff.speakkey.formattingtags;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FormattingTagManager {

    private SQLiteDatabase database;
    private FormattingTagDbHelper dbHelper;
    private static final String TAG = "FormattingTagManager";

    public FormattingTagManager(Context context) {
        dbHelper = new FormattingTagDbHelper(context.getApplicationContext());
    }

    public void open() throws SQLException {
        // Ensure any existing database instance is closed before opening a new one
        // or simply re-assign. SQLiteOpenHelper handles underlying DB management.
        database = dbHelper.getWritableDatabase();
        Log.d(TAG, "Database opened.");
    }

    public void close() {
        if (database != null && database.isOpen()) {
            // database.close(); // This is often handled by dbHelper.close()
        }
        if (dbHelper != null) {
            dbHelper.close(); // This will close the database if it's open.
            Log.d(TAG, "Database helper closed.");
        }
    }

    public boolean isOpen() {
        return database != null && database.isOpen();
    }

    public long addTag(FormattingTag tag) {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open. Call open() before addTag.");
            // Optionally, open it here if this behavior is desired:
            // open(); 
            return -1; // Or throw an exception
        }
        ContentValues values = new ContentValues();
        values.put(FormattingTagDbHelper.COLUMN_NAME, tag.getName());
        values.put(FormattingTagDbHelper.COLUMN_OPENING_TAG_TEXT, tag.getOpeningTagText());
        // values.put(FormattingTagDbHelper.COLUMN_CLOSING_TAG_TEXT, tag.getClosingTagText()); // Removed
        values.put(FormattingTagDbHelper.COLUMN_KEYSTROKE_SEQUENCE, tag.getKeystrokeSequence());
        values.put(FormattingTagDbHelper.COLUMN_IS_ACTIVE, tag.isActive() ? 1 : 0);
        values.put(FormattingTagDbHelper.COLUMN_DELAY_MS, tag.getDelayMs()); // Add delayMs

        try {
            long insertId = database.insert(FormattingTagDbHelper.TABLE_FORMATTING_TAGS, null, values);
            tag.setId(insertId); // Set the ID on the passed object
            return insertId;
        } catch (SQLException e) {
            Log.e(TAG, "Error inserting tag: " + e.getMessage());
            return -1;
        }
    }

    public FormattingTag getTag(long id) {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open. Call open() before getTag.");
            return null;
        }
        Cursor cursor = null;
        FormattingTag tag = null;
        try {
            cursor = database.query(FormattingTagDbHelper.TABLE_FORMATTING_TAGS,
                    null, // All columns
                    FormattingTagDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                tag = cursorToFormattingTag(cursor);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting tag with id " + id + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tag;
    }

    public List<FormattingTag> getAllTags() {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open. Call open() before getAllTags.");
            return new ArrayList<>(); // Return empty list if DB not open
        }
        List<FormattingTag> tags = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(FormattingTagDbHelper.TABLE_FORMATTING_TAGS,
                    null, // All columns
                    null, null, null, null,
                    FormattingTagDbHelper.COLUMN_NAME + " ASC");

            if (cursor != null && cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    FormattingTag tag = cursorToFormattingTag(cursor);
                    tags.add(tag);
                    cursor.moveToNext();
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting all tags: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return tags;
    }

    public int updateTag(FormattingTag tag) {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open. Call open() before updateTag.");
            return 0;
        }
        ContentValues values = new ContentValues();
        values.put(FormattingTagDbHelper.COLUMN_NAME, tag.getName());
        values.put(FormattingTagDbHelper.COLUMN_OPENING_TAG_TEXT, tag.getOpeningTagText());
        // values.put(FormattingTagDbHelper.COLUMN_CLOSING_TAG_TEXT, tag.getClosingTagText()); // Removed
        values.put(FormattingTagDbHelper.COLUMN_KEYSTROKE_SEQUENCE, tag.getKeystrokeSequence());
        values.put(FormattingTagDbHelper.COLUMN_IS_ACTIVE, tag.isActive() ? 1 : 0);
        values.put(FormattingTagDbHelper.COLUMN_DELAY_MS, tag.getDelayMs()); // Add delayMs

        try {
            return database.update(FormattingTagDbHelper.TABLE_FORMATTING_TAGS,
                    values,
                    FormattingTagDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(tag.getId())});
        } catch (SQLException e) {
            Log.e(TAG, "Error updating tag with id " + tag.getId() + ": " + e.getMessage());
            return 0;
        }
    }

    public boolean deleteTag(long id) {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open. Call open() before deleteTag.");
            return false;
        }
        try {
            int rowsAffected = database.delete(FormattingTagDbHelper.TABLE_FORMATTING_TAGS,
                    FormattingTagDbHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(id)});
            return rowsAffected > 0;
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting tag with id " + id + ": " + e.getMessage());
            return false;
        }
    }

    private FormattingTag cursorToFormattingTag(Cursor cursor) {
        FormattingTag tag = new FormattingTag();
        // Ensure to get column indices dynamically to avoid issues if column order changes
        int idIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_ID);
        int nameIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_NAME);
        int openingTagIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_OPENING_TAG_TEXT);
        // int closingTagIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_CLOSING_TAG_TEXT); // Removed
        int keystrokeIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_KEYSTROKE_SEQUENCE);
        int isActiveIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_IS_ACTIVE);
        int delayMsIndex = cursor.getColumnIndex(FormattingTagDbHelper.COLUMN_DELAY_MS);

        tag.setId(cursor.getLong(idIndex));
        tag.setName(cursor.getString(nameIndex));
        tag.setOpeningTagText(cursor.getString(openingTagIndex));
        // tag.setClosingTagText(cursor.getString(closingTagIndex)); // Removed
        tag.setKeystrokeSequence(cursor.getString(keystrokeIndex));
        tag.setActive(cursor.getInt(isActiveIndex) == 1);
        if (delayMsIndex != -1) { // Check if column exists
            tag.setDelayMs(cursor.getInt(delayMsIndex));
        } else {
            tag.setDelayMs(0); // Default to 0 if column not found (e.g. during migration)
        }
        
        return tag;
    }

    public List<FormattingTag> getActiveTags() {
        if (database == null || !database.isOpen()) {
            Log.w(TAG, "Database is not open in getActiveTags. Returning empty list.");
            // Consider throwing an exception or ensuring 'open()' is called if this state is unexpected.
            // For now, returning an empty list to prevent crashes if 'open()' was missed.
            return new ArrayList<>();
        }

        List<FormattingTag> activeTags = new ArrayList<>();
        Cursor cursor = null;
        try {
            String selection = FormattingTagDbHelper.COLUMN_IS_ACTIVE + " = ?";
            String[] selectionArgs = { "1" };
            // Optional: Add an order by clause, e.g., by name or by length of opening tag text
            // String orderBy = FormattingTagDbHelper.COLUMN_NAME + " ASC";
            String orderBy = "LENGTH(" + FormattingTagDbHelper.COLUMN_OPENING_TAG_TEXT + ") DESC, " + FormattingTagDbHelper.COLUMN_NAME + " ASC";


            cursor = database.query(
                    FormattingTagDbHelper.TABLE_FORMATTING_TAGS,
                    null, // All columns
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    orderBy // orderBy
            );

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    activeTags.add(cursorToFormattingTag(cursor));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting active tags", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return activeTags;
    }
}
