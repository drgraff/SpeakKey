package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.util.Log;
// Removed: com.inputstick.api.broadcast.InputStickBroadcast;
// Removed: com.inputstick.api.hid.HIDKeycodes;
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

        FormattingTagManager tagManager = new FormattingTagManager(context);
        List<FormattingTag> activeTags;
        try {
            tagManager.open();
            activeTags = tagManager.getActiveTags();
        } catch (Exception e) {
            Log.e(TAG, "Error opening FormattingTagManager or getting active tags, creating TypeTextAction for whole text.", e);
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

        StringBuilder currentSegment = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            boolean tagFound = false;
            if (activeTags != null && !activeTags.isEmpty()) {
                for (FormattingTag tag : activeTags) {
                    if (tag.getOpeningTagText() != null && !tag.getOpeningTagText().isEmpty() &&
                            text.startsWith(tag.getOpeningTagText(), i)) {
                        // Add current text segment if not empty
                        if (currentSegment.length() > 0) {
                            actions.add(new TypeTextAction(currentSegment.toString()));
                            currentSegment.setLength(0);
                        }
                        // Add keystroke action for the matched tag
                        actions.add(new SendKeystrokesAction(tag.getKeystrokeSequence(), tag.getDelayMs()));
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
        return actions;
    }

    // Helper methods sendTextSegment, getHidKeyCode, sendCustomKeystrokes, and applyDelay
    // have been removed from this class and moved to InputStickManager.
}
