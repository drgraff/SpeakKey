package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.content.SharedPreferences; // Added
import android.util.Log;
import android.widget.Toast;
import androidx.preference.PreferenceManager; // Added

import com.drgraff.speakkey.R;
import com.inputstick.api.broadcast.InputStickBroadcast;

public class InputStickManager {
    private static final String TAG = "InputStickManager";
    private final Context context;

    public InputStickManager(Context context) {
        this.context = context;
    }

    /**
     * Connects to InputStick using the InputStickBroadcast API
     * @param callback Callback to be called when connection is complete
     */
    public void connect(ConnectionCallback callback) {
        // Check if InputStickUtility is installed
        if (!InputStickBroadcast.isSupported(context, true)) {
            Log.e(TAG, "InputStickUtility app is not installed or outdated");
            callback.onConnectionResult(false);
            return;
        }

        // Request connection before typing
        InputStickBroadcast.requestConnection(context);
        
        // Since we're using the broadcast API, we consider the connection attempt successful
        // The actual connection will be handled by InputStickUtility
        callback.onConnectionResult(true);
    }

    /**
     * Types text using the InputStickBroadcast API
     * @param text Text to be typed
     */
    public void typeText(String text) {
        if (text == null || text.isEmpty()) {
            Log.e(TAG, "Text is null or empty");
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean formatEnabled = sharedPreferences.getBoolean("pref_inputstick_format_tags_enabled", false);
        // String delayMsStr = sharedPreferences.getString("pref_inputstick_format_delay_ms", "100"); // Removed
        // int delayMs = 100; // Removed
        // try { // Removed
        //     delayMs = Integer.parseInt(delayMsStr); // Removed
        // } catch (NumberFormatException e) { // Removed
        //     Log.e(TAG, "Failed to parse formatting delay, using default: " + delayMsStr, e); // Removed
        // } // Removed

        if (formatEnabled) {
            Log.d(TAG, "InputStick text formatting is enabled.");
            TextTagFormatter formatter = new TextTagFormatter();
            List<InputAction> actions = formatter.parseTextToActions(context, text);

            for (InputAction action : actions) {
                if (action.getType() == ActionType.TYPE_TEXT) {
                    TypeTextAction typeTextAction = (TypeTextAction) action;
                    Log.d(TAG, "Executing TypeTextAction: " + typeTextAction.getText());
                    sendTextSegment(typeTextAction.getText()); // Call local method
                } else if (action.getType() == ActionType.SEND_KEYSTROKES) {
                    SendKeystrokesAction sendKeystrokesAction = (SendKeystrokesAction) action;
                    Log.d(TAG, "Executing SendKeystrokesAction: " + sendKeystrokesAction.getKeystrokeSequence() + " with delay: " + sendKeystrokesAction.getDelayMs() + "ms");
                    sendCustomKeystrokes(sendKeystrokesAction.getKeystrokeSequence()); // Call local method
                    if (sendKeystrokesAction.getDelayMs() > 0) {
                        try {
                            // Apply delay directly here as it's part of action processing
                            Thread.sleep(sendKeystrokesAction.getDelayMs());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            Log.e(TAG, "Delay interrupted", e);
                        }
                    }
                }
            }
        } else {
            Log.d(TAG, "InputStick text formatting is disabled. Sending raw text.");
            InputStickBroadcast.type(context, text, "en-US");
        }
    }

    /**
     * Interface for connection result callback
     */
    public interface ConnectionCallback {
        void onConnectionResult(boolean connected);
    }

    // --- Moved methods from TextTagFormatter ---

    private void sendTextSegment(String segment) {
        if (segment != null && !segment.isEmpty()) {
            Log.d(TAG, "Sending text segment: " + segment);
            InputStickBroadcast.type(this.context, segment, "en-US");
        }
    }

    private int getHidKeyCode(String keyName) {
        if (keyName == null) return 0; 
        keyName = keyName.trim().toUpperCase();
        // Re-import com.inputstick.api.hid.HIDKeycodes directly or copy values
        // For now, assuming HIDKeycodes will be imported in this file
        // Note: This requires adding 'import com.inputstick.api.hid.HIDKeycodes;'
        switch (keyName) {
            case "CTRL_LEFT": return com.inputstick.api.hid.HIDKeycodes.CTRL_LEFT;
            case "SHIFT_LEFT": return com.inputstick.api.hid.HIDKeycodes.SHIFT_LEFT;
            case "ALT_LEFT": return com.inputstick.api.hid.HIDKeycodes.ALT_LEFT;
            case "GUI_LEFT": return com.inputstick.api.hid.HIDKeycodes.GUI_LEFT;
            case "CTRL_RIGHT": return com.inputstick.api.hid.HIDKeycodes.CTRL_RIGHT;
            case "SHIFT_RIGHT": return com.inputstick.api.hid.HIDKeycodes.SHIFT_RIGHT;
            case "ALT_RIGHT": return com.inputstick.api.hid.HIDKeycodes.ALT_RIGHT;
            case "GUI_RIGHT": return com.inputstick.api.hid.HIDKeycodes.GUI_RIGHT;
            case "KEY_A": return com.inputstick.api.hid.HIDKeycodes.KEY_A;
            case "KEY_B": return com.inputstick.api.hid.HIDKeycodes.KEY_B;
            case "KEY_C": return com.inputstick.api.hid.HIDKeycodes.KEY_C;
            case "KEY_D": return com.inputstick.api.hid.HIDKeycodes.KEY_D;
            case "KEY_E": return com.inputstick.api.hid.HIDKeycodes.KEY_E;
            case "KEY_F": return com.inputstick.api.hid.HIDKeycodes.KEY_F;
            case "KEY_G": return com.inputstick.api.hid.HIDKeycodes.KEY_G;
            case "KEY_H": return com.inputstick.api.hid.HIDKeycodes.KEY_H;
            case "KEY_I": return com.inputstick.api.hid.HIDKeycodes.KEY_I;
            case "KEY_J": return com.inputstick.api.hid.HIDKeycodes.KEY_J;
            case "KEY_K": return com.inputstick.api.hid.HIDKeycodes.KEY_K;
            case "KEY_L": return com.inputstick.api.hid.HIDKeycodes.KEY_L;
            case "KEY_M": return com.inputstick.api.hid.HIDKeycodes.KEY_M;
            case "KEY_N": return com.inputstick.api.hid.HIDKeycodes.KEY_N;
            case "KEY_O": return com.inputstick.api.hid.HIDKeycodes.KEY_O;
            case "KEY_P": return com.inputstick.api.hid.HIDKeycodes.KEY_P;
            case "KEY_Q": return com.inputstick.api.hid.HIDKeycodes.KEY_Q;
            case "KEY_R": return com.inputstick.api.hid.HIDKeycodes.KEY_R;
            case "KEY_S": return com.inputstick.api.hid.HIDKeycodes.KEY_S;
            case "KEY_T": return com.inputstick.api.hid.HIDKeycodes.KEY_T;
            case "KEY_U": return com.inputstick.api.hid.HIDKeycodes.KEY_U;
            case "KEY_V": return com.inputstick.api.hid.HIDKeycodes.KEY_V;
            case "KEY_W": return com.inputstick.api.hid.HIDKeycodes.KEY_W;
            case "KEY_X": return com.inputstick.api.hid.HIDKeycodes.KEY_X;
            case "KEY_Y": return com.inputstick.api.hid.HIDKeycodes.KEY_Y;
            case "KEY_Z": return com.inputstick.api.hid.HIDKeycodes.KEY_Z;
            case "KEY_0": return com.inputstick.api.hid.HIDKeycodes.KEY_0;
            case "KEY_1": return com.inputstick.api.hid.HIDKeycodes.KEY_1;
            case "KEY_2": return com.inputstick.api.hid.HIDKeycodes.KEY_2;
            case "KEY_3": return com.inputstick.api.hid.HIDKeycodes.KEY_3;
            case "KEY_4": return com.inputstick.api.hid.HIDKeycodes.KEY_4;
            case "KEY_5": return com.inputstick.api.hid.HIDKeycodes.KEY_5;
            case "KEY_6": return com.inputstick.api.hid.HIDKeycodes.KEY_6;
            case "KEY_7": return com.inputstick.api.hid.HIDKeycodes.KEY_7;
            case "KEY_8": return com.inputstick.api.hid.HIDKeycodes.KEY_8;
            case "KEY_9": return com.inputstick.api.hid.HIDKeycodes.KEY_9;
            case "KEY_F1": return com.inputstick.api.hid.HIDKeycodes.KEY_F1;
            case "KEY_F2": return com.inputstick.api.hid.HIDKeycodes.KEY_F2;
            case "KEY_F3": return com.inputstick.api.hid.HIDKeycodes.KEY_F3;
            case "KEY_F4": return com.inputstick.api.hid.HIDKeycodes.KEY_F4;
            case "KEY_F5": return com.inputstick.api.hid.HIDKeycodes.KEY_F5;
            case "KEY_F6": return com.inputstick.api.hid.HIDKeycodes.KEY_F6;
            case "KEY_F7": return com.inputstick.api.hid.HIDKeycodes.KEY_F7;
            case "KEY_F8": return com.inputstick.api.hid.HIDKeycodes.KEY_F8;
            case "KEY_F9": return com.inputstick.api.hid.HIDKeycodes.KEY_F9;
            case "KEY_F10": return com.inputstick.api.hid.HIDKeycodes.KEY_F10;
            case "KEY_F11": return com.inputstick.api.hid.HIDKeycodes.KEY_F11;
            case "KEY_F12": return com.inputstick.api.hid.HIDKeycodes.KEY_F12;
            case "KEY_ENTER": return com.inputstick.api.hid.HIDKeycodes.KEY_ENTER;
            case "KEY_ESCAPE": return com.inputstick.api.hid.HIDKeycodes.KEY_ESCAPE;
            case "KEY_BACKSPACE": return com.inputstick.api.hid.HIDKeycodes.KEY_BACKSPACE;
            case "KEY_TAB": return com.inputstick.api.hid.HIDKeycodes.KEY_TAB;
            case "KEY_SPACEBAR": return com.inputstick.api.hid.HIDKeycodes.KEY_SPACEBAR;
            case "KEY_CAPS_LOCK": return com.inputstick.api.hid.HIDKeycodes.KEY_CAPS_LOCK;
            case "KEY_PRINT_SCREEN": return com.inputstick.api.hid.HIDKeycodes.KEY_PRINT_SCREEN;
            case "KEY_SCROLL_LOCK": return com.inputstick.api.hid.HIDKeycodes.KEY_SCROLL_LOCK;
            case "KEY_PASUE": return com.inputstick.api.hid.HIDKeycodes.KEY_PASUE;
            case "KEY_PAUSE": return com.inputstick.api.hid.HIDKeycodes.KEY_PASUE;
            case "KEY_INSERT": return com.inputstick.api.hid.HIDKeycodes.KEY_INSERT;
            case "KEY_HOME": return com.inputstick.api.hid.HIDKeycodes.KEY_HOME;
            case "KEY_PAGE_UP": return com.inputstick.api.hid.HIDKeycodes.KEY_PAGE_UP;
            case "KEY_DELETE": return com.inputstick.api.hid.HIDKeycodes.KEY_DELETE;
            case "KEY_END": return com.inputstick.api.hid.HIDKeycodes.KEY_END;
            case "KEY_PAGE_DOWN": return com.inputstick.api.hid.HIDKeycodes.KEY_PAGE_DOWN;
            case "KEY_ARROW_RIGHT": return com.inputstick.api.hid.HIDKeycodes.KEY_ARROW_RIGHT;
            case "KEY_ARROW_LEFT": return com.inputstick.api.hid.HIDKeycodes.KEY_ARROW_LEFT;
            case "KEY_ARROW_DOWN": return com.inputstick.api.hid.HIDKeycodes.KEY_ARROW_DOWN;
            case "KEY_ARROW_UP": return com.inputstick.api.hid.HIDKeycodes.KEY_ARROW_UP;
            case "KEY_NUM_LOCK": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_LOCK;
            case "KEY_NUM_SLASH": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_SLASH;
            case "KEY_NUM_STAR": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_STAR;
            case "KEY_NUM_MINUS": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_MINUS;
            case "KEY_NUM_PLUS": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_PLUS;
            case "KEY_NUM_ENTER": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_ENTER;
            case "KEY_NUM_1": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_1;
            case "KEY_NUM_2": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_2;
            case "KEY_NUM_3": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_3;
            case "KEY_NUM_4": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_4;
            case "KEY_NUM_5": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_5;
            case "KEY_NUM_6": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_6;
            case "KEY_NUM_7": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_7;
            case "KEY_NUM_8": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_8;
            case "KEY_NUM_9": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_9;
            case "KEY_NUM_0": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_0;
            case "KEY_NUM_DOT": return com.inputstick.api.hid.HIDKeycodes.KEY_NUM_DOT;
            case "KEY_MINUS": return com.inputstick.api.hid.HIDKeycodes.KEY_MINUS;
            case "KEY_EQUALS": return com.inputstick.api.hid.HIDKeycodes.KEY_EQUALS;
            case "KEY_LEFT_BRACKET": return com.inputstick.api.hid.HIDKeycodes.KEY_LEFT_BRACKET;
            case "KEY_RIGHT_BRACKET": return com.inputstick.api.hid.HIDKeycodes.KEY_RIGHT_BRACKET;
            case "KEY_BACKSLASH": return com.inputstick.api.hid.HIDKeycodes.KEY_BACKSLASH;
            case "KEY_SEMICOLON": return com.inputstick.api.hid.HIDKeycodes.KEY_SEMICOLON;
            case "KEY_APOSTROPHE": return com.inputstick.api.hid.HIDKeycodes.KEY_APOSTROPHE;
            case "KEY_GRAVE": return com.inputstick.api.hid.HIDKeycodes.KEY_GRAVE;
            case "KEY_COMA": return com.inputstick.api.hid.HIDKeycodes.KEY_COMA;
            case "KEY_DOT": return com.inputstick.api.hid.HIDKeycodes.KEY_DOT;
            case "KEY_SLASH": return com.inputstick.api.hid.HIDKeycodes.KEY_SLASH;
            case "KEY_APPLICATION": return com.inputstick.api.hid.HIDKeycodes.KEY_APPLICATION;
            default:
                Log.w(TAG, "Unknown HIDKeyCode string: " + keyName);
                return 0;
        }
    }

    private void sendCustomKeystrokes(String keystrokeString) {
        Log.d(TAG, "Sending custom keystrokes for: " + keystrokeString);
        String[] parts = keystrokeString.split("\\+");
        int modifier = 0;
        int key = 0;

        if (parts.length == 1) {
            key = getHidKeyCode(parts[0]);
        } else if (parts.length == 2) {
            modifier = getHidKeyCode(parts[0]);
            key = getHidKeyCode(parts[1]);
            if (key == 0 && modifier != 0) {
                Log.w(TAG, "Potential misinterpretation of modifier as key for: " + keystrokeString + ". Assuming " + parts[0] + " is the modifier and " + parts[1] + " is the key.");
            }
        } else if (parts.length > 2) {
            Log.e(TAG, "Unsupported keystroke format: " + keystrokeString + ". Too many parts. Expected 'MODIFIER+KEY' or 'KEY'.");
            return;
        } else {
            Log.e(TAG, "Unexpected parsing outcome for keystroke: " + keystrokeString);
            return;
        }
        
        if (key == 0) {
            Log.e(TAG, "Invalid or unknown main key in keystroke sequence: " + keystrokeString);
            return;
        }

        if (modifier != 0) {
            InputStickBroadcast.pressAndRelease(this.context, (byte)modifier, (byte)key);
            Log.d(TAG, "Sent custom: MOD=" + modifier + ", KEY=" + key);
        } else {
            Log.d(TAG, "Sending single key (modifier 0): KEY=" + key + " using pressAndRelease.");
            InputStickBroadcast.pressAndRelease(this.context, (byte)0, (byte)key);
        }
    }
}