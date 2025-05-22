package com.drgraff.speakkey.api;

import com.google.gson.annotations.SerializedName;

/**
 * Response model for OpenAI's Whisper API
 */
public class WhisperApiResponse {
    
    @SerializedName("text")
    private String text;
    
    /**
     * Gets the transcribed text
     * 
     * @return Transcribed text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the transcribed text
     * 
     * @param text Transcribed text
     */
    public void setText(String text) {
        this.text = text;
    }
}