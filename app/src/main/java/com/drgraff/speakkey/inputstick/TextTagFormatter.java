package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.content.SharedPreferences; // Added import
import androidx.preference.PreferenceManager; // Added import
import android.util.Log;
// import android.widget.Toast; // Removed Toast import
import com.drgraff.speakkey.utils.AppLogManager; // Added for AppLogManager
// Removed: com.inputstick.api.broadcast.InputStickBroadcast;
// Removed: com.inputstick.api.hid.HIDKeycodes;
import com.drgraff.speakkey.R; // Added import
import com.drgraff.speakkey.formattingtags.FormattingTag;
import com.drgraff.speakkey.formattingtags.FormattingTagManager;

import java.util.ArrayList;
import java.util.List;

public class TextTagFormatter {

    private static final String TAG = "TextTagFormatter";

    public TextTagFormatter() {
        // Constructor if needed, otherwise default is fine
    }

    /**
     * Parses text for formatting tags and generates a list of InputActions.
     * @param context Context for FormattingTagManager
     * @param text The text to parse
     * @return A list of InputActions representing the parsed text and formatting commands.
     */
    public List<InputAction> parseTextToActions(Context context, String text) {
        List<InputAction> actions = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return actions;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean formattingTagDelayEnabled = sharedPreferences.getBoolean(context.getString(R.string.pref_key_formatting_tag_delay_enabled), true);

        FormattingTagManager tagManager = new FormattingTagManager(context);
        List<FormattingTag> activeTags;
        try {
            tagManager.open();
            activeTags = tagManager.getActiveTags();
            
            // AppLogManager entries for active tags (Toast removed)
            int activeTagCount = (activeTags != null ? activeTags.size() : 0);
            // Toast.makeText(context, "Active Tags Found: " + activeTagCount, Toast.LENGTH_LONG).show(); // Removed
            AppLogManager.getInstance().addEntry("INFO", TAG + " - Active Tags", "Number of active tags found: " + activeTagCount);
            if (activeTags != null) {
                for (FormattingTag tagDetail : activeTags) {
                    AppLogManager.getInstance().addEntry("INFO", TAG + " - Tag Detail", "Name='" + tagDetail.getName() + "', OpeningText='" + tagDetail.getOpeningTagText() + "', Keystrokes='" + tagDetail.getKeystrokeSequence() + "'");
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error opening FormattingTagManager or getting active tags, creating TypeTextAction for whole text.", e);
            // AppLogManager entry for error (Toast removed)
            AppLogManager.getInstance().addEntry("ERROR", TAG + " - Tag Loading Error", "Error loading formatting tags: " + e.getMessage() + (e.getCause() != null ? " - Cause: " + e.getCause().toString() : ""));
            // Toast.makeText(context, "Error loading formatting tags. Check App Log.", Toast.LENGTH_LONG).show(); // Removed
            // If DB ops fail, treat the whole text as a single segment to type
            if (text != null && !text.isEmpty()) {
                actions.add(new TypeTextAction(text));
            }
            return actions;
        } finally {
            if (tagManager.isOpen()) {
                tagManager.close();
            }
        }

        // Existing Logcat logging for active tags (can remain)
        Log.d(TAG, "Number of active tags found: " + (activeTags != null ? activeTags.size() : 0));
        if (activeTags != null && !activeTags.isEmpty()) {
            for (FormattingTag tag : activeTags) {
                Log.d(TAG, "Active Tag: Name='" + tag.getName() + "', OpeningText='" + tag.getOpeningTagText() + "', Keystrokes='" + tag.getKeystrokeSequence() + "'");
            }
        }

        StringBuilder currentSegment = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            Log.d(TAG, "Processing text at index " + i + ": '" + text.substring(i, Math.min(i + 10, text.length())) + "...'");
            boolean tagFound = false;
            if (activeTags != null && !activeTags.isEmpty()) {
                for (FormattingTag tag : activeTags) {
                    Log.d(TAG, "  Attempting to match tag: '" + tag.getOpeningTagText() + "'");
                    if (tag.getOpeningTagText() != null && !tag.getOpeningTagText().isEmpty() &&
                            text.startsWith(tag.getOpeningTagText(), i)) {
                        Log.d(TAG, "    MATCHED tag: '" + tag.getOpeningTagText() + "'");
                        // Add current text segment if not empty
                        if (currentSegment.length() > 0) {
                            actions.add(new TypeTextAction(currentSegment.toString()));
                            currentSegment.setLength(0);
                        }
                        // Add keystroke action for the matched tag
                        actions.add(new SendKeystrokesAction(tag.getKeystrokeSequence(), formattingTagDelayEnabled ? tag.getDelayMs() : 0));
                        i += tag.getOpeningTagText().length();
                        tagFound = true;
                        break;
                    }
                }
            }

            if (!tagFound) {
                currentSegment.append(text.charAt(i));
                i++;
            }
        }
        // Add any remaining text segment
        if (currentSegment.length() > 0) {
            actions.add(new TypeTextAction(currentSegment.toString()));
        }

        Log.d(TAG, "Generated " + actions.size() + " actions:");
        for (int j = 0; j < actions.size(); j++) {
            InputAction currentAction = actions.get(j);
            Log.d(TAG, "  Action " + j + ": " + currentAction.getType() +
                       (currentAction.getType() == ActionType.TYPE_TEXT ? " - Text: '" + ((TypeTextAction)currentAction).getText() + "'" : "") +
                       (currentAction.getType() == ActionType.SEND_KEYSTROKES ? " - Keystrokes: '" + ((SendKeystrokesAction)currentAction).getKeystrokeSequence() + "'" : ""));
        }
        return actions;
    }

    // Helper methods sendTextSegment, getHidKeyCode, sendCustomKeystrokes, and applyDelay
    // have been removed from this class and moved to InputStickManager.
}
