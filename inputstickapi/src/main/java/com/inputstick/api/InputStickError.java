package com.inputstick.api;

// TODO: Fetch content from https://github.com/inputstick/InputStickAPI-Android/tree/master/InputStickAPI/src/com/inputstick/api/InputStickError.java
public class InputStickError {
    public static final int ERROR_NONE = 0;
    public static final int ERROR_UNKNOWN = 1;
    public static final int ERROR_BT_NOT_SUPPORTED = 2;
    public static final int ERROR_BT_NOT_ENABLED = 3;
    public static final int ERROR_BT_NO_PAIRED_DEVICES = 4; // Not used
    public static final int ERROR_BT_DEVICE_NOT_PAIRED = 5; // Not used
    public static final int ERROR_BT_SERVICE_NOT_RUNNING = 6; // Not used
    public static final int ERROR_BT_SERVICE_CONNECTION_FAILED = 7; // Not used
    public static final int ERROR_BT_DEVICE_NOT_FOUND = 8;
    public static final int ERROR_BT_CONNECTION_FAILED = 9;
    public static final int ERROR_BT_CONNECTION_LOST = 10;
    public static final int ERROR_BT_ADDRESS_INVALID = 11;
    public static final int ERROR_BT_UNABLE_TO_OPEN_SOCKET = 12;
    public static final int ERROR_BT_UNABLE_TO_CLOSE_SOCKET = 13;
    public static final int ERROR_BT_UNABLE_TO_SEND_DATA = 14;
    public static final int ERROR_BT_UNABLE_TO_RECEIVE_DATA = 15;
    public static final int ERROR_BT_CONNECTION_TIMEOUT = 16;
    public static final int ERROR_BT_ALREADY_CONNECTING = 17;
    public static final int ERROR_BT_ALREADY_CONNECTED = 18;
    public static final int ERROR_BT_NOT_CONNECTED = 19;
    public static final int ERROR_BT_SECURITY_ERROR = 20;

    public static final int ERROR_USB_NOT_SUPPORTED = 21;
    public static final int ERROR_USB_NOT_ENABLED = 22;
    public static final int ERROR_USB_NO_PERMISSIONS = 23;
    public static final int ERROR_USB_DEVICE_NOT_FOUND = 24;
    public static final int ERROR_USB_CONNECTION_FAILED = 25;
    public static final int ERROR_USB_CONNECTION_LOST = 26;
    public static final int ERROR_USB_UNABLE_TO_SEND_DATA = 27;
    public static final int ERROR_USB_UNABLE_TO_RECEIVE_DATA = 28;

    public static final int ERROR_CRYPTO_ERROR = 40;
    public static final int ERROR_DEVICE_NOT_RESPONDING = 41;
    public static final int ERROR_FW_TOO_OLD = 42;
    public static final int ERROR_FW_INVALID_RESPONSE = 43;
    public static final int ERROR_FW_UNKNOWN_CMD_RESPONSE = 44;
    public static final int ERROR_FW_BUFFER_FULL = 45;
    public static final int ERROR_FW_NOT_READY = 46;
    public static final int ERROR_FW_CHECKSUM = 47;
    public static final int ERROR_FW_DECRYPTION = 48;
    public static final int ERROR_FW_INTERNAL = 49;

    public static final int ERROR_INIT_NO_RESPONSE = 50;
    public static final int ERROR_INIT_DECRYPTION = 51;
    public static final int ERROR_INIT_AUTH = 52;
    public static final int ERROR_INIT_CHALLENGE = 53;

    public static final int ERROR_ANDROID_NO_UTILITY_APP = 60;
    public static final int ERROR_ANDROID_UTILITY_APP_TOO_OLD = 61;
    public static final int ERROR_ANDROID_UTILITY_APP_BIND_FAILED = 62;
    public static final int ERROR_ANDROID_UTILITY_APP_CONNECTION_TIMEOUT = 63;
}
