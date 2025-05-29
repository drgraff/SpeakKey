package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.content.SharedPreferences; // Added
import android.util.Log;
import android.widget.Toast;
import androidx.preference.PreferenceManager; // Added

import com.drgraff.speakkey.R;
import com.inputstick.api.broadcast.InputStickBroadcast;
import com.inputstick.api.hid.HIDKeycodes; // Added for getHidKeyCode
import com.drgraff.speakkey.utils.AppLogManager; // Added for AppLogManager
import java.util.List; // Added for List<InputAction>
import java.util.concurrent.ExecutorService; // Added for ExecutorService
import java.util.concurrent.Executors; // Added for Executors
// Toast is already imported via android.widget.Toast

public class InputStickManager {
    private static final String TAG = "InputStickManager";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Added ExecutorService

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
        // Initial checks and logging on the calling thread
        AppLogManager.getInstance().addEntry("INFO", TAG, "typeText() entered. Text snippet: " + (text != null && text.length() > 20 ? text.substring(0, 20) : text));
        Toast.makeText(context, "IM.typeText() called", Toast.LENGTH_SHORT).show(); // This Toast remains on UI thread

        if (text == null || text.isEmpty()) {
            Log.e(TAG, "Text is null or empty");
            AppLogManager.getInstance().addEntry("WARN", TAG, "typeText() called with null or empty text.");
            return;
        }

        final Context runnableContext = this.context; // Use final context for runnable

        executor.execute(() -> {
            // All logic from here onwards goes into the background thread
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(runnableContext);
            boolean formatEnabled = sharedPreferences.getBoolean("pref_inputstick_format_tags_enabled", false);

            // Toast for preference removed, AppLogManager entry updated for background context
            AppLogManager.getInstance().addEntry("INFO", TAG, "Background: Preference pref_inputstick_format_tags_enabled: " + formatEnabled);
            // Toast.makeText(context, "Format Enabled Pref: " + formatEnabled, Toast.LENGTH_SHORT).show(); // Removed

            if (formatEnabled) {
                // Toast for formatting ON removed, AppLogManager entry updated for background context
                AppLogManager.getInstance().addEntry("INFO", TAG, "Background: formatEnabled is true. Attempting to parse actions...");
                // Toast.makeText(context, "Formatting: ON - Calling parser", Toast.LENGTH_SHORT).show(); // Removed

                Log.d(TAG, "Background: InputStick text formatting is enabled."); // Existing Logcat, updated for context
                TextTagFormatter formatter = new TextTagFormatter();
                // Pass runnableContext to parseTextToActions as it might do context-based operations (though it doesn't currently for Toasts)
                List<InputAction> actions = formatter.parseTextToActions(runnableContext, text);

                for (InputAction action : actions) {
                    if (action.getType() == ActionType.TYPE_TEXT) {
                        TypeTextAction typeTextAction = (TypeTextAction) action;
                        AppLogManager.getInstance().addEntry("INFO", TAG, "Background: Executing TypeTextAction: " + typeTextAction.getText());
                        sendTextSegment(typeTextAction.getText()); 
                    } else if (action.getType() == ActionType.SEND_KEYSTROKES) {
                        SendKeystrokesAction sendKeystrokesAction = (SendKeystrokesAction) action;
                        AppLogManager.getInstance().addEntry("INFO", TAG, "Background: Executing SendKeystrokesAction: " + sendKeystrokesAction.getKeystrokeSequence() + " with delay: " + sendKeystrokesAction.getDelayMs() + "ms");
                        sendCustomKeystrokes(sendKeystrokesAction.getKeystrokeSequence()); 
                        if (sendKeystrokesAction.getDelayMs() > 0) {
                            try {
                                Thread.sleep(sendKeystrokesAction.getDelayMs());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                Log.e(TAG, "Background: Delay interrupted", e);
                                AppLogManager.getInstance().addEntry("WARN", TAG, "Background: Delay interrupted for keystroke: " + sendKeystrokesAction.getKeystrokeSequence());
                            }
                        }
                    }
                }
            } else {
                // Toast for formatting OFF removed, AppLogManager entry updated for background context
                AppLogManager.getInstance().addEntry("INFO", TAG, "Background: formatEnabled is false. Sending raw text.");
                // Toast.makeText(context, "Formatting: OFF - Raw text", Toast.LENGTH_SHORT).show(); // Removed

                Log.d(TAG, "Background: InputStick text formatting is disabled. Sending raw text."); // Existing Logcat, updated for context
                // sendTextSegment is fine as it uses this.context, which is final runnableContext effectively
                sendTextSegment(text); 
            }
        });
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
        // HIDKeycodes is now imported
        switch (keyName) {
            case "CTRL_LEFT": return HIDKeycodes.CTRL_LEFT;
            case "SHIFT_LEFT": return HIDKeycodes.SHIFT_LEFT;
            case "ALT_LEFT": return HIDKeycodes.ALT_LEFT;
            case "GUI_LEFT": return HIDKeycodes.GUI_LEFT;
            case "CTRL_RIGHT": return HIDKeycodes.CTRL_RIGHT;
            case "SHIFT_RIGHT": return HIDKeycodes.SHIFT_RIGHT;
            case "ALT_RIGHT": return HIDKeycodes.ALT_RIGHT;
            case "GUI_RIGHT": return HIDKeycodes.GUI_RIGHT;
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
            case "KEY_ENTER": return HIDKeycodes.KEY_ENTER;
            case "KEY_ESCAPE": return HIDKeycodes.KEY_ESCAPE;
            case "KEY_BACKSPACE": return HIDKeycodes.KEY_BACKSPACE;
            case "KEY_TAB": return HIDKeycodes.KEY_TAB;
            case "KEY_SPACEBAR": return HIDKeycodes.KEY_SPACEBAR;
            case "KEY_CAPS_LOCK": return HIDKeycodes.KEY_CAPS_LOCK;
            case "KEY_PRINT_SCREEN": return HIDKeycodes.KEY_PRINT_SCREEN;
            case "KEY_SCROLL_LOCK": return HIDKeycodes.KEY_SCROLL_LOCK;
            case "KEY_PASUE": return HIDKeycodes.KEY_PASUE;
            case "KEY_PAUSE": return HIDKeycodes.KEY_PASUE;
            case "KEY_INSERT": return HIDKeycodes.KEY_INSERT;
            case "KEY_HOME": return HIDKeycodes.KEY_HOME;
            case "KEY_PAGE_UP": return HIDKeycodes.KEY_PAGE_UP;
            case "KEY_DELETE": return HIDKeycodes.KEY_DELETE;
            case "KEY_END": return HIDKeycodes.KEY_END;
            case "KEY_PAGE_DOWN": return HIDKeycodes.KEY_PAGE_DOWN;
            case "KEY_ARROW_RIGHT": return HIDKeycodes.KEY_ARROW_RIGHT;
            case "KEY_ARROW_LEFT": return HIDKeycodes.KEY_ARROW_LEFT;
            case "KEY_ARROW_DOWN": return HIDKeycodes.KEY_ARROW_DOWN;
            case "KEY_ARROW_UP": return HIDKeycodes.KEY_ARROW_UP;
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
            case "KEY_MINUS": return HIDKeycodes.KEY_MINUS;
            case "KEY_EQUALS": return HIDKeycodes.KEY_EQUALS;
            case "KEY_LEFT_BRACKET": return HIDKeycodes.KEY_LEFT_BRACKET;
            case "KEY_RIGHT_BRACKET": return HIDKeycodes.KEY_RIGHT_BRACKET;
            case "KEY_BACKSLASH": return HIDKeycodes.KEY_BACKSLASH;
            case "KEY_SEMICOLON": return HIDKeycodes.KEY_SEMICOLON;
            case "KEY_APOSTROPHE": return HIDKeycodes.KEY_APOSTROPHE;
            case "KEY_GRAVE": return HIDKeycodes.KEY_GRAVE;
            case "KEY_COMA": return HIDKeycodes.KEY_COMA;
            case "KEY_DOT": return HIDKeycodes.KEY_DOT;
            case "KEY_SLASH": return HIDKeycodes.KEY_SLASH;
            case "KEY_APPLICATION": return HIDKeycodes.KEY_APPLICATION;
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