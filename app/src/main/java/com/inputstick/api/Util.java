package com.inputstick.api;

/**
 * Utility class for InputStick API
 */
public class Util {
    
    /**
     * Gets the least significant byte of an integer value
     * @param value integer value
     * @return least significant byte
     */
    public static byte getLSB(int value) {
        return (byte)(value & 0xFF);
    }
    
    /**
     * Gets the most significant byte of an integer value
     * @param value integer value
     * @return most significant byte
     */
    public static byte getMSB(int value) {
        return (byte)((value >> 8) & 0xFF);
    }
}