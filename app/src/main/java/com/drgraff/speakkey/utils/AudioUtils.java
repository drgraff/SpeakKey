package com.drgraff.speakkey.utils;

import android.content.Context;
import android.util.Log;

import com.hualee.lame.LameControl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioUtils {

    private static final String TAG = "AudioUtils";

    // Default LAME parameters - consider making these configurable if needed
    private static final int DEFAULT_SAMPLE_RATE = 16000; // Hz
    private static final int DEFAULT_CHANNEL_CONFIG = 1; // Mono
    private static final int DEFAULT_BITRATE = 96; // kbps
    private static final int DEFAULT_LAME_QUALITY = 7; // VBR quality, 0=best, 9=worst (7 is a good compromise)

    /**
     * Converts a PCM audio file to MP3 format using LAME.
     *
     * @param inputPcmFile  The input PCM audio file.
     * @param outputMp3File The desired output MP3 file.
     * @return The output MP3 file if conversion is successful, null otherwise.
     */
    public static File convertToMp3(File inputPcmFile, File outputMp3File) {
        if (inputPcmFile == null || !inputPcmFile.exists()) {
            Log.e(TAG, "Input PCM file is null or does not exist: " + inputPcmFile);
            return null;
        }
        if (outputMp3File == null) {
            Log.e(TAG, "Output MP3 file is null.");
            return null;
        }

        // Ensure output directory exists
        File outputDir = outputMp3File.getParentFile();
        if (outputDir != null && !outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                Log.e(TAG, "Failed to create output directory: " + outputDir.getAbsolutePath());
                return null;
            }
        }

        Log.d(TAG, "Starting MP3 conversion: " + inputPcmFile.getAbsolutePath() + " -> " + outputMp3File.getAbsolutePath());

        byte[] buffer = new byte[1024 * 4]; // 4KB buffer for reading PCM
        byte[] mp3buffer = new byte[1024 * 4]; // Buffer for LAME output

        try (FileInputStream fis = new FileInputStream(inputPcmFile);
             FileOutputStream fos = new FileOutputStream(outputMp3File)) {

            LameControl.init(DEFAULT_SAMPLE_RATE, DEFAULT_CHANNEL_CONFIG, DEFAULT_SAMPLE_RATE, DEFAULT_BITRATE, DEFAULT_LAME_QUALITY);
            Log.d(TAG, "LameControl initialized with SampleRate=" + DEFAULT_SAMPLE_RATE +
                    ", Channels=" + DEFAULT_CHANNEL_CONFIG + ", Bitrate=" + DEFAULT_BITRATE);

            int bytesRead;
            short[] shortBuffer = new short[buffer.length / 2]; // LAME expects short PCM samples

            while ((bytesRead = fis.read(buffer)) > 0) {
                // Convert byte[] to short[] (assuming 16-bit PCM, little-endian)
                for (int i = 0; i < bytesRead / 2; i++) {
                    shortBuffer[i] = (short) ((buffer[2 * i] & 0xff) | (buffer[2 * i + 1] << 8));
                }
                // Number of samples is bytesRead / 2 (since each sample is 2 bytes)
                int samplesRead = bytesRead / 2;
                int encodedBytes = LameControl.encode(shortBuffer, shortBuffer, samplesRead, mp3buffer);

                if (encodedBytes > 0) {
                    fos.write(mp3buffer, 0, encodedBytes);
                } else if (encodedBytes < 0) {
                    Log.e(TAG, "LameControl.encode error: " + encodedBytes);
                    return null; // Encoding error
                }
            }

            int flushResult = LameControl.flush(mp3buffer);
            if (flushResult > 0) {
                fos.write(mp3buffer, 0, flushResult);
            } else if (flushResult < 0) {
                 Log.e(TAG, "LameControl.flush error: " + flushResult);
                // continue, try to close, but log it. The file might still be usable.
            }

            Log.i(TAG, "MP3 conversion successful: " + outputMp3File.getAbsolutePath() + ", Size: " + outputMp3File.length() + " bytes");
            return outputMp3File;

        } catch (IOException e) {
            Log.e(TAG, "MP3 conversion failed due to IOException", e);
            // Clean up partially written file if error occurs
            if (outputMp3File.exists()) {
                outputMp3File.delete();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "MP3 conversion failed due to general Exception", e);
             if (outputMp3File.exists()) {
                outputMp3File.delete();
            }
            return null;
        } finally {
            LameControl.close();
            Log.d(TAG, "LameControl closed.");
        }
    }

    /**
     * Checks if the given MIME type is for an MP3 file.
     * @param mimeType The MIME type string (e.g., "audio/mpeg", "audio/mp3").
     * @return true if it's an MP3 MIME type, false otherwise.
     */
    public static boolean isMimeTypeMp3(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return "audio/mpeg".equalsIgnoreCase(mimeType) ||
               "audio/mp3".equalsIgnoreCase(mimeType);
        // Add other common MP3 MIME types if necessary, though "audio/mpeg" is standard.
    }
}
