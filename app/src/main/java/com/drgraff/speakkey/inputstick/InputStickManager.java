package com.drgraff.speakkey.inputstick;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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

        // Use the US English layout by default
        InputStickBroadcast.type(context, text, "en-US");
    }

    /**
     * Interface for connection result callback
     */
    public interface ConnectionCallback {
        void onConnectionResult(boolean connected);
    }
}