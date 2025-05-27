package com.speakkey.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.drgraff.speakkey.R; // For string resources if needed later
import com.drgraff.speakkey.utils.AppLogManager; // Added for logging
import com.drgraff.speakkey.inputstick.HIDKeycodes;
import com.drgraff.speakkey.inputstick.InputStickBroadcast;
import com.speakkey.data.ActionType;
import com.speakkey.data.Macro;
import com.speakkey.data.MacroAction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MacroExecutor {
    private static final String TAG = "MacroExecutor";
    private final Context applicationContext;
    private final Handler mainThreadHandler;

    // Map to convert string representations of special keys to HIDKeycodes
    private static final Map<String, Byte> specialKeyMap = new HashMap<>();
    static {
        // Modifiers
        specialKeyMap.put("CTRL", HIDKeycodes.CTRL_LEFT);
        specialKeyMap.put("LCTRL", HIDKeycodes.CTRL_LEFT);
        specialKeyMap.put("RCTRL", HIDKeycodes.CTRL_RIGHT);
        specialKeyMap.put("SHIFT", HIDKeycodes.SHIFT_LEFT);
        specialKeyMap.put("LSHIFT", HIDKeycodes.SHIFT_LEFT);
        specialKeyMap.put("RSHIFT", HIDKeycodes.SHIFT_RIGHT);
        specialKeyMap.put("ALT", HIDKeycodes.ALT_LEFT);
        specialKeyMap.put("LALT", HIDKeycodes.ALT_LEFT);
        specialKeyMap.put("RALT", HIDKeycodes.ALT_RIGHT);
        specialKeyMap.put("GUI", HIDKeycodes.GUI_LEFT); // Windows/Command Key
        specialKeyMap.put("LGUI", HIDKeycodes.GUI_LEFT);
        specialKeyMap.put("RGUI", HIDKeycodes.GUI_RIGHT);
        specialKeyMap.put("WIN", HIDKeycodes.GUI_LEFT); // Alias for GUI

        // Function Keys
        for (int i = 1; i <= 12; i++) {
            specialKeyMap.put("F" + i, (byte)(HIDKeycodes.KEY_F1 + (i - 1)));
        }

        // Other common keys
        specialKeyMap.put("ENTER", HIDKeycodes.KEY_ENTER);
        specialKeyMap.put("ESC", HIDKeycodes.KEY_ESCAPE);
        specialKeyMap.put("ESCAPE", HIDKeycodes.KEY_ESCAPE);
        specialKeyMap.put("BACKSPACE", HIDKeycodes.KEY_BACKSPACE);
        specialKeyMap.put("TAB", HIDKeycodes.KEY_TAB);
        specialKeyMap.put("SPACE", HIDKeycodes.KEY_SPACEBAR);
        specialKeyMap.put("CAPSLOCK", HIDKeycodes.KEY_CAPS_LOCK);
        specialKeyMap.put("PRINTSCREEN", HIDKeycodes.KEY_PRINT_SCREEN);
        specialKeyMap.put("SCROLLLOCK", HIDKeycodes.KEY_SCROLL_LOCK);
        specialKeyMap.put("PAUSE", HIDKeycodes.KEY_PASUE); // Note: HIDKeycodes uses "PASUE"
        specialKeyMap.put("INSERT", HIDKeycodes.KEY_INSERT);
        specialKeyMap.put("HOME", HIDKeycodes.KEY_HOME);
        specialKeyMap.put("PAGEUP", HIDKeycodes.KEY_PAGE_UP);
        specialKeyMap.put("PAGEDOWN", HIDKeycodes.KEY_PAGE_DOWN);
        specialKeyMap.put("DELETE", HIDKeycodes.KEY_DELETE);
        specialKeyMap.put("END", HIDKeycodes.KEY_END);
        specialKeyMap.put("RIGHT", HIDKeycodes.KEY_ARROW_RIGHT);
        specialKeyMap.put("LEFT", HIDKeycodes.KEY_ARROW_LEFT);
        specialKeyMap.put("DOWN", HIDKeycodes.KEY_ARROW_DOWN);
        specialKeyMap.put("UP", HIDKeycodes.KEY_ARROW_UP);
        specialKeyMap.put("NUMLOCK", HIDKeycodes.KEY_NUM_LOCK);
        specialKeyMap.put("APPLICATION", HIDKeycodes.KEY_APPLICATION);

        // Alphanumeric keys (A-Z) - primarily for combinations like CTRL+C
        for (char c = 'A'; c <= 'Z'; c++) {
            specialKeyMap.put(String.valueOf(c), (byte) (HIDKeycodes.KEY_A + (c - 'A')));
        }
    }


    public MacroExecutor(Context context) {
        this.applicationContext = context.getApplicationContext(); // Use application context
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Executes a macro. This method should be called on a background thread.
     * @param macro The macro to execute.
     * @param activityContext The current activity context, needed for UI interactions like PAUSE_CONFIRMATION.
     * @return true if the macro completed, false if it was cancelled.
     */
    public boolean executeMacro(Macro macro, Activity activityContext) {
        if (!InputStickBroadcast.isSupported(applicationContext, false)) { // false: don't show dialog from here, MainActivity does that.
            String errorMsg = "InputStick Utility not installed/updated or not supported.";
            Log.w(TAG, errorMsg);
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": " + errorMsg, "Macro: " + macro.getName());
            mainThreadHandler.post(() -> Toast.makeText(applicationContext, "InputStick not ready. Check InputStickUtility.", Toast.LENGTH_LONG).show());
            return false;
        }
        // Request connection at the beginning of macro execution.
        // InputStickUtility will handle if already connected.
        InputStickBroadcast.requestConnection(applicationContext);


        for (MacroAction action : macro.getActions()) {
            Log.d(TAG, "Executing action: " + action.getDisplayName());
            switch (action.getType()) {
                case TEXT:
                    if (action.getValue() != null && !action.getValue().isEmpty()) {
                        InputStickBroadcast.type(applicationContext, action.getValue(), "en-US", 1); // Assuming en-US layout
                    } else {
                        Log.w(TAG, "TEXT action has null or empty value. Skipping. Macro: " + macro.getName() + ", Action: " + action.getDisplayName());
                        AppLogManager.getInstance().addEntry("WARN", TAG + ": TEXT action has no value. Skipped.", "Macro: " + macro.getName());
                    }
                    break;
                case SPECIAL_KEY:
                    if (action.getValue() != null && !action.getValue().isEmpty()) {
                        if (!handleSpecialKey(action.getValue(), macro.getName())) {
                            // Error already logged and toasted in handleSpecialKey
                            return false; // Optionally stop macro if a special key fails critically
                        }
                    } else {
                        Log.w(TAG, "SPECIAL_KEY action has null or empty value. Skipping. Macro: " + macro.getName() + ", Action: " + action.getDisplayName());
                        AppLogManager.getInstance().addEntry("WARN", TAG + ": SPECIAL_KEY action has no value. Skipped.", "Macro: " + macro.getName());
                    }
                    break;
                case TAB:
                    InputStickBroadcast.pressAndRelease(applicationContext, HIDKeycodes.NONE, HIDKeycodes.KEY_TAB, 1);
                    break;
                case ENTER:
                    InputStickBroadcast.pressAndRelease(applicationContext, HIDKeycodes.NONE, HIDKeycodes.KEY_ENTER, 1);
                    break;
                case DELAY:
                    if (action.getDelayMillis() != null && action.getDelayMillis() > 0) {
                        try {
                            Thread.sleep(action.getDelayMillis());
                        } catch (InterruptedException e) {
                            Log.w(TAG, "Delay interrupted, macro '" + macro.getName() + "' execution stopped.", e);
                            AppLogManager.getInstance().addEntry("WARN", TAG + ": Delay interrupted.", "Macro: " + macro.getName() + ", Details: " + e.getMessage());
                            Thread.currentThread().interrupt(); // Preserve interrupt status
                            return false; // Stop macro execution if delay is interrupted
                        }
                    } else {
                        Log.w(TAG, "DELAY action has invalid or zero delay. Skipping. Macro: " + macro.getName() + ", Action: " + action.getDisplayName());
                        // AppLogManager.getInstance().addEntry("WARN", TAG + ": DELAY action invalid. Skipped.", "Macro: " + macro.getName()); // Optional, might be too noisy
                    }
                    break;
                case PAUSE_CONFIRMATION:
                    final CountDownLatch latch = new CountDownLatch(1);
                    final boolean[] cancelled = {false}; // Array to allow modification from inner class

                    mainThreadHandler.post(() -> new AlertDialog.Builder(activityContext) // Use activityContext here
                            .setTitle("Macro Paused")
                            .setMessage("Press Continue to proceed with the macro, or Cancel to stop.")
                            .setPositiveButton("Continue", (dialog, which) -> {
                                dialog.dismiss();
                                latch.countDown();
                            })
                            .setNegativeButton("Cancel Macro", (dialog, which) -> {
                                cancelled[0] = true;
                                dialog.dismiss();
                                latch.countDown();
                            })
                            .setCancelable(false)
                            .show());

                    try {
                        latch.await(); // Wait for dialog interaction
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Pause confirmation interrupted for macro '" + macro.getName() + "'.", e);
                        AppLogManager.getInstance().addEntry("WARN", TAG + ": Pause confirmation interrupted.", "Macro: " + macro.getName() + ", Details: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        return false; // Stop macro execution
                    }

                    if (cancelled[0]) {
                        Log.i(TAG, "Macro '" + macro.getName() + "' execution cancelled by user at pause point.");
                        AppLogManager.getInstance().addEntry("INFO", TAG + ": Macro execution cancelled by user.", "Macro: " + macro.getName());
                        mainThreadHandler.post(() -> Toast.makeText(applicationContext, "Macro cancelled.", Toast.LENGTH_SHORT).show());
                        return false; // Stop macro execution
                    }
                    break;
                default:
                    String unknownActionMsg = "Unknown action type: " + action.getType() + " in macro '" + macro.getName() + "'. Skipping.";
                    Log.w(TAG, unknownActionMsg);
                    AppLogManager.getInstance().addEntry("WARN", TAG + ": " + unknownActionMsg, null);
                    mainThreadHandler.post(() -> Toast.makeText(applicationContext, "Skipped unknown action: " + action.getType(), Toast.LENGTH_SHORT).show());
            }
            // Small delay between actions to ensure commands are processed.
            // This might be crucial for some target applications/OS.
            try {
                Thread.sleep(action.getType() == ActionType.DELAY ? 0 : 50); // No extra delay if it was already a DELAY action
            } catch (InterruptedException e) {
                Log.w(TAG, "Inter-action delay interrupted, macro '" + macro.getName() + "' execution stopped.", e);
                AppLogManager.getInstance().addEntry("WARN", TAG + ": Inter-action delay interrupted.", "Macro: " + macro.getName() + ", Details: " + e.getMessage());
                Thread.currentThread().interrupt();
                return false;
            }
        }
        Log.d(TAG, "Macro '" + macro.getName() + "' execution finished successfully.");
        AppLogManager.getInstance().addEntry("INFO", TAG + ": Macro execution finished.", "Macro: " + macro.getName());
        return true;
    }

    private boolean handleSpecialKey(String value, String macroName) {
        String[] parts = value.toUpperCase(Locale.ROOT).split("\\+");
        byte modifier = HIDKeycodes.NONE;
        Byte key = null;
        boolean success = true;

        for (String part : parts) {
            part = part.trim();
            if (specialKeyMap.containsKey(part)) {
                byte code = specialKeyMap.get(part);
                // Check if it's one of the 8 defined modifier bitmasks
                boolean isModifierBitmask = false;
                for(int i=0; i<8; i++) {
                    if (code == (byte)(HIDKeycodes.CTRL_LEFT << i)) {
                        isModifierBitmask = true;
                        break;
                    }
                }

                if (isModifierBitmask) {
                     modifier |= code;
                } else if (key == null) { // If not a modifier bitmask, it could be the primary key
                    key = code;
                } else {
                    // This case means multiple non-modifier keys were found, e.g., "CTRL+C+V"
                    // Current simple model sends CTRL+(first non-modifier). Log a warning.
                    Log.w(TAG, "Multiple non-modifier keys in special key action: '" + value + "' for macro '" + macroName + "'. Using first one: " + HIDKeycodes.keyToString(key));
                    AppLogManager.getInstance().addEntry("WARN", TAG + ": Multiple non-modifier keys in special key.", "Value: " + value + ", Macro: " + macroName);
                    // Optionally, could decide to fail here: success = false; break;
                }
            } else {
                Log.w(TAG, "Unknown special key part: '" + part + "' in '" + value + "' for macro '" + macroName + "'.");
                AppLogManager.getInstance().addEntry("ERROR", TAG + ": Unknown special key part.", "Part: " + part + ", Value: " + value + ", Macro: " + macroName);
                // Fallback for single characters not in map but potentially in ASCIItoHID
                if (part.length() == 1 && key == null) {
                    byte asciiKeyCode = (byte) HIDKeycodes.getKeyCode(part.charAt(0));
                    if (asciiKeyCode != 0) { // Check if it's a valid ASCII key
                         if ((asciiKeyCode & 0x80) != 0) { // Check if it's a shifted character (ASCIItoHID convention)
                            modifier |= HIDKeycodes.SHIFT_LEFT;
                            key = (byte)(asciiKeyCode & 0x7F); // Remove shifted flag
                        } else {
                            key = asciiKeyCode;
                        }
                        Log.d(TAG, "Fallback: Parsed '" + part + "' as key " + HIDKeycodes.keyToString(key) + ( (modifier & HIDKeycodes.SHIFT_LEFT) != 0 ? " with SHIFT" : ""));
                    } else {
                        success = false; // Unknown part and not a simple character
                        break;
                    }
                } else {
                     success = false; // Unknown part and not a situation for simple character fallback
                     break;
                }
            }
        }

        if (!success) {
            String errorMsg = "Failed to parse special key: '" + value + "' in macro '" + macroName + "'. Action skipped.";
            Log.e(TAG, errorMsg);
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": " + errorMsg, null);
            mainThreadHandler.post(() -> Toast.makeText(applicationContext, "Error in special key: " + value, Toast.LENGTH_LONG).show());
            return false; // Indicate failure to parse/execute this action
        }

        if (key != null) {
            Log.d(TAG, "Sending special key for macro '" + macroName + "'. Modifier: " + HIDKeycodes.modifiersToString(modifier) + ", Key: " + HIDKeycodes.keyToString(key));
            InputStickBroadcast.pressAndRelease(applicationContext, modifier, key, 1);
        } else if (modifier != HIDKeycodes.NONE) {
            // This case means only modifiers were specified (e.g., "CTRL").
            // This is unusual as a standalone action. Log warning.
            String msg = "Special key action for macro '" + macroName + "' only contains modifiers: " + HIDKeycodes.modifiersToString(modifier) + ". Sending as modifier press/release with NO key.";
            Log.w(TAG, msg);
            AppLogManager.getInstance().addEntry("WARN", TAG + ": " + msg, null);
            InputStickBroadcast.pressAndRelease(applicationContext, modifier, HIDKeycodes.NONE, 1);
        } else {
            // This case should ideally be caught by !success, but as a fallback.
            String errorMsg = "No valid key or modifier found for special key: '" + value + "' in macro '" + macroName + "'. Action skipped.";
            Log.e(TAG, errorMsg);
            AppLogManager.getInstance().addEntry("ERROR", TAG + ": " + errorMsg, null);
            mainThreadHandler.post(() -> Toast.makeText(applicationContext, "Invalid special key: " + value, Toast.LENGTH_LONG).show());
            return false; // Indicate failure
        }
        return true; // Indicate success for this action
    }
}
