package com.inputstick.api;

import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {

        private Cipher mCipherEncr;
        private Cipher mCipherDecr;
        private SecretKeySpec mKey;
        private boolean ready;

        public AES() {
                ready = false;
        }

        public static byte[] getMD5(String s) {
                try {
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        return md.digest(s.getBytes("UTF-8"));
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return null;
        }

        public byte[] init(byte[] key) {
                byte[] iv = null;
                try {
                        mKey = new SecretKeySpec(key, "AES");
                        mCipherEncr = Cipher.getInstance("AES/CBC/NoPadding");
                        mCipherEncr.init(Cipher.ENCRYPT_MODE, mKey);
                        iv = mCipherEncr.getIV();
                        // Util.printHex(iv, "AES IV: "); // Assuming Util.printHex might not be available/working yet or causes issues.
                        mCipherDecr = Cipher.getInstance("AES/CBC/NoPadding");
                        mCipherDecr.init(Cipher.DECRYPT_MODE, mKey, new IvParameterSpec(iv));
                        ready = true;
                } catch (Exception e) {
                        e.printStackTrace();
                        ready = false; // Ensure ready is false on exception
                }
                return iv;
        }

        public byte[] encrypt(byte[] data) {
            if (!ready) return null;
            try {
                return mCipherEncr.doFinal(data); // Use doFinal for encryption if it's a single block
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public byte[] decrypt(byte[] data) {
            if (!ready) return null;
            try {
                return mCipherDecr.doFinal(data); // Use doFinal for decryption
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean isReady() {
                return ready;
        }
}
