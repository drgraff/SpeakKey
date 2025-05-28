package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.util.Log;
import com.inputstick.api.broadcast.InputStickBroadcast;
import com.inputstick.api.hid.HIDKeycodes; // Assuming this class exists and provides keycodes

public class TextTagFormatter {

    private static final String TAG = "TextTagFormatter";

    public TextTagFormatter() {
        // Constructor if needed, otherwise default is fine
    }

    /**
     * Parses text for <b> and <i> tags and sends appropriate keystrokes via InputStick.
     * @param context Context for InputStickBroadcast
     * @param text The text to format and send
     * @param delayMs Delay in milliseconds after sending a formatting keystroke sequence
     */
    public void formatAndSend(Context context, String text, int delayMs) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // This is a simplified parser. A more robust solution might use regex or a proper parser.
        // This version focuses on <b> and <i> tags and assumes they are not nested incorrectly.
        // It toggles formatting on/off.

        StringBuilder currentSegment = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith("<b>", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0); // Clear segment
                sendCtrlB(context);
                applyDelay(delayMs);
                i += 3; // Length of "<b>"
            } else if (text.startsWith("</b>", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0);
                sendCtrlB(context); // Toggle off
                applyDelay(delayMs);
                i += 4; // Length of "</b>"
            } else if (text.startsWith("<i>", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0);
                sendCtrlI(context);
                applyDelay(delayMs);
                i += 3; // Length of "<i>"
            } else if (text.startsWith("</i>", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0);
                sendCtrlI(context); // Toggle off
                applyDelay(delayMs);
                i += 4; // Length of "</i>"
            } else {
                currentSegment.append(text.charAt(i));
                i++;
            }
        }

        // Send any remaining text
        sendTextSegment(context, currentSegment.toString());
    }

    private void sendTextSegment(Context context, String segment) {
        if (segment != null && !segment.isEmpty()) {
            Log.d(TAG, "Sending text segment: " + segment);
            InputStickBroadcast.type(context, segment, "en-US");
        }
    }

    private void sendCtrlB(Context context) {
        Log.d(TAG, "Sending Ctrl+B");
        // Assumes InputStickBroadcast can handle raw keyboard reports or similar
        // The actual implementation depends on InputStickBroadcast API capabilities
        // This is a placeholder for what might be required:
        // InputStickBroadcast.press(context, HIDKeycodes.MOD_CTRL_LEFT);
        // InputStickBroadcast.press(context, HIDKeycodes.KEY_B);
        // InputStickBroadcast.release(context, HIDKeycodes.KEY_B);
        // InputStickBroadcast.release(context, HIDKeycodes.MOD_CTRL_LEFT);
        
        Log.d(TAG, "Sending Ctrl+B using pressAndRelease.");
        InputStickBroadcast.pressAndRelease(context, HIDKeycodes.CTRL_LEFT, HIDKeycodes.KEY_B);
    }

    private void sendCtrlI(Context context) {
        Log.d(TAG, "Sending Ctrl+I using pressAndRelease.");
        InputStickBroadcast.pressAndRelease(context, HIDKeycodes.CTRL_LEFT, HIDKeycodes.KEY_I);
    }

    private void applyDelay(int delayMs) {
        if (delayMs > 0) {
            try {
                Log.d(TAG, "Applying delay: " + delayMs + "ms");
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Delay interrupted", e);
            }
        }
    }
}
