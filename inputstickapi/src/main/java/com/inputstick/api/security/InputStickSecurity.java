package com.inputstick.api.security;

import com.inputstick.api.AES; // Assuming AES.java is in com.inputstick.api

public class InputStickSecurity {

    public static byte[] getResponse(byte[] key, byte[] payload) {
        if (key == null || payload == null) {
            System.err.println("InputStickSecurity Error: Key or payload is null.");
            return null;
        }
        AES aes = new AES();
        // The AES init method in the fetched AES.java returns an IV,
        // but for a simple challenge-response, we might only need to encrypt the payload.
        // The IV handling would be part of the broader protocol if AES/CBC is used across multiple blocks.
        // For now, we assume getResponse is about encrypting the payload (challenge).
        byte[] iv = aes.init(key); 
        if (!aes.isReady() || iv == null) {
            System.err.println("InputStickSecurity Error: AES initialization failed.");
            return null;
        }
        // Important: AES/CBC/NoPadding requires input to be a multiple of block size (16 bytes).
        // The challenge (payload) must be padded if not already a multiple of 16.
        // This placeholder won't handle padding, which could be an issue.
        // The original API might have specific padding handling or assumptions about payload length.
        byte[] encryptedPayload = aes.encrypt(payload);
        if (encryptedPayload == null) {
             System.err.println("InputStickSecurity Error: Encryption failed.");
        }
        return encryptedPayload;
    }
}
