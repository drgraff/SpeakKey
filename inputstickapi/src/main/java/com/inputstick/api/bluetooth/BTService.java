package com.inputstick.api.bluetooth;

import android.app.Application;
import android.os.Handler;

public class BTService {
    public static final int EVENT_DATA = 1;
    public static final int EVENT_CONNECTED = 2;
    public static final int EVENT_CANCELLED = 3;
    public static final int EVENT_ERROR = 4;
    public static final int DEFAULT_CONNECT_TIMEOUT = 10000; // Example value

    public BTService(Application app, Handler handler) {
        // Placeholder constructor
    }
    public void setConnectTimeout(int timeout) { }
    public void enableReflection(boolean enable) { }
    public void connect(String mac, boolean doNotAsk, boolean isBT40) { }
    public void disconnect() { }
}
