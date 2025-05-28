package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String customTagName = sharedPreferences.getString("pref_custom_tag_1_name", null);
        String customTagKeystrokes = sharedPreferences.getString("pref_custom_tag_1_keystrokes", null);
        boolean customTagEnabled = customTagName != null && !customTagName.isEmpty() && customTagKeystrokes != null && !customTagKeystrokes.isEmpty();

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
            } else if (customTagEnabled && text.startsWith("<" + customTagName + ">", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0);
                sendCustomKeystrokes(context, customTagKeystrokes);
                applyDelay(delayMs);
                i += ("<" + customTagName + ">").length();
            } else if (customTagEnabled && text.startsWith("</" + customTagName + ">", i)) {
                sendTextSegment(context, currentSegment.toString());
                currentSegment.setLength(0);
                sendCustomKeystrokes(context, customTagKeystrokes); // Assuming custom keys toggle formatting
                applyDelay(delayMs);
                i += ("</" + customTagName + ">").length();
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

    private int getHidKeyCode(String keyName) {
        if (keyName == null) return 0; // Or throw an exception
        keyName = keyName.trim().toUpperCase();
        switch (keyName) {
            // Modifiers
            case "CTRL_LEFT": return HIDKeycodes.CTRL_LEFT;
            case "SHIFT_LEFT": return HIDKeycodes.SHIFT_LEFT;
            case "ALT_LEFT": return HIDKeycodes.ALT_LEFT;
            case "GUI_LEFT": return HIDKeycodes.GUI_LEFT; // Windows/Command key
            case "CTRL_RIGHT": return HIDKeycodes.CTRL_RIGHT;
            case "SHIFT_RIGHT": return HIDKeycodes.SHIFT_RIGHT;
            case "ALT_RIGHT": return HIDKeycodes.ALT_RIGHT;
            case "GUI_RIGHT": return HIDKeycodes.GUI_RIGHT;

            // Common Keys (add more as needed)
            case "KEY_A": return HIDKeycodes.KEY_A;
            case "KEY_B": return HIDKeycodes.KEY_B;
            case "KEY_C": return HIDKeycodes.KEY_C;
            case "KEY_D": return HIDKeycodes.KEY_D;
            case "KEY_E": return HIDKeycodes.KEY_E;
            case "KEY_F": return HIDKeycodes.KEY_F;
            case "KEY_G": return HIDKeycodes.KEY_G;
            case "KEY_H": return HIDKeycodes.KEY_H;
            case "KEY_I": return HIDKeycodes.KEY_I;
            case "KEY_J": return HIDKeycodes.KEY_J;
            case "KEY_K": return HIDKeycodes.KEY_K;
            case "KEY_L": return HIDKeycodes.KEY_L;
            case "KEY_M": return HIDKeycodes.KEY_M;
            case "KEY_N": return HIDKeycodes.KEY_N;
            case "KEY_O": return HIDKeycodes.KEY_O;
            case "KEY_P": return HIDKeycodes.KEY_P;
            case "KEY_Q": return HIDKeycodes.KEY_Q;
            case "KEY_R": return HIDKeycodes.KEY_R;
            case "KEY_S": return HIDKeycodes.KEY_S;
            case "KEY_T": return HIDKeycodes.KEY_T;
            case "KEY_U": return HIDKeycodes.KEY_U;
            case "KEY_V": return HIDKeycodes.KEY_V;
            case "KEY_W": return HIDKeycodes.KEY_W;
            case "KEY_X": return HIDKeycodes.KEY_X;
            case "KEY_Y": return HIDKeycodes.KEY_Y;
            case "KEY_Z": return HIDKeycodes.KEY_Z;

            // Numbers
            case "KEY_0": return HIDKeycodes.KEY_0;
            case "KEY_1": return HIDKeycodes.KEY_1;
            case "KEY_2": return HIDKeycodes.KEY_2;
            case "KEY_3": return HIDKeycodes.KEY_3;
            case "KEY_4": return HIDKeycodes.KEY_4;
            case "KEY_5": return HIDKeycodes.KEY_5;
            case "KEY_6": return HIDKeycodes.KEY_6;
            case "KEY_7": return HIDKeycodes.KEY_7;
            case "KEY_8": return HIDKeycodes.KEY_8;
            case "KEY_9": return HIDKeycodes.KEY_9;

            // Function Keys
            case "KEY_F1": return HIDKeycodes.KEY_F1;
            case "KEY_F2": return HIDKeycodes.KEY_F2;
            case "KEY_F3": return HIDKeycodes.KEY_F3;
            case "KEY_F4": return HIDKeycodes.KEY_F4;
            case "KEY_F5": return HIDKeycodes.KEY_F5;
            case "KEY_F6": return HIDKeycodes.KEY_F6;
            case "KEY_F7": return HIDKeycodes.KEY_F7;
            case "KEY_F8": return HIDKeycodes.KEY_F8;
            case "KEY_F9": return HIDKeycodes.KEY_F9;
            case "KEY_F10": return HIDKeycodes.KEY_F10;
            case "KEY_F11": return HIDKeycodes.KEY_F11;
            case "KEY_F12": return HIDKeycodes.KEY_F12;

            // Special Control Keys
            case "KEY_ENTER": return HIDKeycodes.KEY_ENTER;
            case "KEY_ESCAPE": return HIDKeycodes.KEY_ESCAPE;
            case "KEY_BACKSPACE": return HIDKeycodes.KEY_BACKSPACE;
            case "KEY_TAB": return HIDKeycodes.KEY_TAB;
            case "KEY_SPACEBAR": return HIDKeycodes.KEY_SPACEBAR;
            case "KEY_CAPS_LOCK": return HIDKeycodes.KEY_CAPS_LOCK;
            case "KEY_PRINT_SCREEN": return HIDKeycodes.KEY_PRINT_SCREEN;
            case "KEY_SCROLL_LOCK": return HIDKeycodes.KEY_SCROLL_LOCK;
            case "KEY_PAUSE": return HIDKeycodes.KEY_PAUSE; // Assuming HIDKeycodes.java uses KEY_PAUSE
            case "KEY_INSERT": return HIDKeycodes.KEY_INSERT;
            case "KEY_HOME": return HIDKeycodes.KEY_HOME;
            case "KEY_PAGE_UP": return HIDKeycodes.KEY_PAGE_UP;
            case "KEY_DELETE": return HIDKeycodes.KEY_DELETE;
            case "KEY_END": return HIDKeycodes.KEY_END;
            case "KEY_PAGE_DOWN": return HIDKeycodes.KEY_PAGE_DOWN;

            // Arrow Keys
            case "KEY_ARROW_RIGHT": return HIDKeycodes.KEY_ARROW_RIGHT;
            case "KEY_ARROW_LEFT": return HIDKeycodes.KEY_ARROW_LEFT;
            case "KEY_ARROW_DOWN": return HIDKeycodes.KEY_ARROW_DOWN;
            case "KEY_ARROW_UP": return HIDKeycodes.KEY_ARROW_UP;

            // Numpad Keys
            case "KEY_NUM_LOCK": return HIDKeycodes.KEY_NUM_LOCK;
            case "KEY_NUM_SLASH": return HIDKeycodes.KEY_NUM_SLASH;
            case "KEY_NUM_STAR": return HIDKeycodes.KEY_NUM_STAR;
            case "KEY_NUM_MINUS": return HIDKeycodes.KEY_NUM_MINUS;
            case "KEY_NUM_PLUS": return HIDKeycodes.KEY_NUM_PLUS;
            case "KEY_NUM_ENTER": return HIDKeycodes.KEY_NUM_ENTER;
            case "KEY_NUM_1": return HIDKeycodes.KEY_NUM_1;
            case "KEY_NUM_2": return HIDKeycodes.KEY_NUM_2;
            case "KEY_NUM_3": return HIDKeycodes.KEY_NUM_3;
            case "KEY_NUM_4": return HIDKeycodes.KEY_NUM_4;
            case "KEY_NUM_5": return HIDKeycodes.KEY_NUM_5;
            case "KEY_NUM_6": return HIDKeycodes.KEY_NUM_6;
            case "KEY_NUM_7": return HIDKeycodes.KEY_NUM_7;
            case "KEY_NUM_8": return HIDKeycodes.KEY_NUM_8;
            case "KEY_NUM_9": return HIDKeycodes.KEY_NUM_9;
            case "KEY_NUM_0": return HIDKeycodes.KEY_NUM_0;
            case "KEY_NUM_DOT": return HIDKeycodes.KEY_NUM_DOT;

            // Symbol Keys
            case "KEY_MINUS": return HIDKeycodes.KEY_MINUS;
            case "KEY_EQUALS": return HIDKeycodes.KEY_EQUALS;
            case "KEY_LEFT_BRACKET": return HIDKeycodes.KEY_LEFT_BRACKET;
            case "KEY_RIGHT_BRACKET": return HIDKeycodes.KEY_RIGHT_BRACKET;
            case "KEY_BACKSLASH": return HIDKeycodes.KEY_BACKSLASH;
            case "KEY_SEMICOLON": return HIDKeycodes.KEY_SEMICOLON;
            case "KEY_APOSTROPHE": return HIDKeycodes.KEY_APOSTROPHE;
            case "KEY_GRAVE": return HIDKeycodes.KEY_GRAVE;
            case "KEY_COMA": return HIDKeycodes.KEY_COMA; // Assuming HIDKeycodes.java uses KEY_COMA
            case "KEY_DOT": return HIDKeycodes.KEY_DOT;
            case "KEY_SLASH": return HIDKeycodes.KEY_SLASH;

            // Other Keys
            case "KEY_APPLICATION": return HIDKeycodes.KEY_APPLICATION;

            default:
                Log.w(TAG, "Unknown HIDKeyCode string: " + keyName);
                return 0; // Or handle error appropriately
        }
    }

    private void sendCustomKeystrokes(Context context, String keystrokeString) {
        Log.d(TAG, "Sending custom keystrokes for: " + keystrokeString);
        String[] parts = keystrokeString.split("\\+"); // Ensure this regex correctly splits by '+'
        int modifier = 0;
        int key = 0;

        if (parts.length == 1) {
            key = getHidKeyCode(parts[0]);
        } else if (parts.length == 2) {
            modifier = getHidKeyCode(parts[0]);
            key = getHidKeyCode(parts[1]);
            if (key == 0 && modifier !=0) { // Check if the modifier was mistakenly parsed as a key
                Log.w(TAG, "Potential misinterpretation of modifier as key for: " + keystrokeString + ". Assuming " + parts[0] + " is the modifier and " + parts[1] + " is the key.");
            }
        } else if (parts.length > 2) {
            Log.e(TAG, "Unsupported keystroke format: " + keystrokeString + ". Too many parts. Expected 'MODIFIER+KEY' or 'KEY'.");
            return;
        }
        else { // Should not happen if split correctly
            Log.e(TAG, "Unexpected parsing outcome for keystroke: " + keystrokeString);
            return;
        }
        
        // Ensure the main key is valid before proceeding
        if (key == 0) {
            Log.e(TAG, "Invalid or unknown main key in keystroke sequence: " + keystrokeString);
            return;
        }

        if (modifier != 0) {
            InputStickBroadcast.pressAndRelease(context, (byte)modifier, (byte)key);
            Log.d(TAG, "Sent custom: MOD=" + modifier + ", KEY=" + key);
        } else {
            // For single keys (modifier is 0), pressAndRelease is still the correct method
            // to send a specific HID key event.
            Log.d(TAG, "Sending single key (modifier 0): KEY=" + key + " using pressAndRelease.");
            InputStickBroadcast.pressAndRelease(context, (byte)0, (byte)key);
        }
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
