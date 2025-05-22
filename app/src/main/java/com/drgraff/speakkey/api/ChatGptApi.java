package com.drgraff.speakkey.api;

/**
 * API client for OpenAI's ChatGPT API
 */
public class ChatGptApi {
    private final String apiKey;
    private final String model;
    
    /**
     * Constructor
     * 
     * @param apiKey OpenAI API key
     * @param model Model name (e.g. "gpt-3.5-turbo")
     */
    public ChatGptApi(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }
    
    /**
     * Gets a response from the ChatGPT API
     * 
     * @param prompt The prompt text to send to ChatGPT
     * @return Response from ChatGPT
     * @throws Exception if API call fails
     */
    public String getResponse(String prompt) throws Exception {
        // Placeholder implementation
        return "Placeholder response from ChatGPT";
    }
    
    /**
     * Gets a completion from the ChatGPT API
     * 
     * @param prompt The prompt text to send to ChatGPT
     * @return Completion from ChatGPT
     * @throws Exception if API call fails
     */
    public String getCompletion(String prompt) throws Exception {
        // Placeholder implementation
        return getResponse(prompt);
    }
}