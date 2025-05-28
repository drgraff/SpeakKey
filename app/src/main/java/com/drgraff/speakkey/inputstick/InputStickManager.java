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
            Log.d(TAG, "InputStick text formatting is enabled."); // Removed delay info from log
            TextTagFormatter formatter = new TextTagFormatter();
            // Assuming TextTagFormatter and its methods are in the same package or imported.
            // The formatAndSend method should ideally run on a background thread.
            // For now, direct call. If InputStickManager itself is not on a BG thread, this needs review.
            formatter.formatAndSend(context, text); // Removed delayMs argument
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
}