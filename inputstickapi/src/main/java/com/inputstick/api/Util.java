package com.inputstick.api;

// TODO: Fetch content from https://github.com/inputstick/InputStickAPI-Android/tree/master/InputStickAPI/src/com/inputstick/api/Util.java
public class Util {
    public static byte getLSB(int val) {
        return (byte)(val & 0xFF);
    }

    public static byte getMSB(int val) {
        return (byte)((val >> 8) & 0xFF);
    }
}
