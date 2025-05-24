package com.inputstick.api;

// TODO: Fetch content from https://github.com/inputstick/InputStickAPI-Android/tree/master/InputStickAPI/src/com/inputstick/api/Packet.java
public class Packet {

    public static final byte CMD_NOP = (byte)0x00;
    public static final byte CMD_GET_FW_INFO = (byte)0x01;
    public static final byte CMD_FW_INFO = (byte)0x02;
    public static final byte CMD_GET_STATUS = (byte)0x03;
    public static final byte CMD_HID_STATUS = (byte)0x04;
    public static final byte CMD_HID_DATA = (byte)0x05;
    public static final byte CMD_SET_CONFIG = (byte)0x06;
    public static final byte CMD_GET_CONFIG = (byte)0x07;
    public static final byte CMD_CONFIG_DATA = (byte)0x08;
    public static final byte CMD_SET_AUTH_KEY = (byte)0x09;
    public static final byte CMD_SET_PASSWORD = (byte)0x0A;
    public static final byte CMD_SET_PASSWORD_NEW = (byte)0x0B;
    public static final byte CMD_SET_PASSWORD_DONE = (byte)0x0C;
    public static final byte CMD_SAVE_CONFIG = (byte)0x0D;
    public static final byte CMD_LOAD_DEFAULT_CONFIG = (byte)0x0E;
    public static final byte CMD_REBOOT = (byte)0x0F;
    public static final byte CMD_ERROR = (byte)0x10;
    public static final byte CMD_GET_CHALLENGE = (byte)0x11;
    public static final byte CMD_CHALLENGE = (byte)0x12;
    public static final byte CMD_AUTH_RESPONSE = (byte)0x13;
    public static final byte CMD_SET_INIT_VECTOR = (byte)0x14;
    public static final byte CMD_SET_AUTH_KEY_NEW = (byte)0x15;
    public static final byte CMD_SET_AUTH_KEY_DONE = (byte)0x16;
    public static final byte CMD_SET_INIT_VECTOR_NEW = (byte)0x17;
    public static final byte CMD_SET_INIT_VECTOR_DONE = (byte)0x18;
    public static final byte CMD_HID_DATA_RAW = (byte)0x19;
    public static final byte CMD_USB_RESUME = (byte)0x1A;
    public static final byte CMD_SET_TOUCHSCREEN_CONFIG = (byte)0x1B;
    public static final byte CMD_SET_GAMEPAD_CONFIG = (byte)0x1C;

    public Packet(boolean requiresResponse, byte command) {}
    public Packet(boolean requiresResponse, byte command, byte[] payload) {}
    public Packet(byte[] data) {}

    public byte getCommand() { return CMD_NOP; }
    public byte[] getPayload() { return null; }
    public byte[] getBytes() { return null; }
    public boolean requiresResponse() { return false; }
    public boolean isEncrypted() { return false; }
    public void setEncrypted(boolean value) {}
}
