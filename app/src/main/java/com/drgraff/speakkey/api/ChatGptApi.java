package com.drgraff.speakkey.api;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.File; // Already present
import java.io.IOException;
import java.nio.file.Files; // Added
import android.util.Base64; // Added
import java.util.Collections;
import java.util.List; // Added for List<ModelInfo>
import com.drgraff.speakkey.api.OpenAIModelData.*; // Added for model data classes
import okhttp3.MediaType; // Added
import okhttp3.MultipartBody; // Added
import okhttp3.RequestBody; // Already present
import org.json.JSONArray; // Added
import org.json.JSONObject; // Already present
import org.json.JSONException; // Already present
// okhttp3.Request and okhttp3.Response are used fully qualified, so direct imports not strictly needed but can be added for clarity if preferred.

/**
 * API client for OpenAI's ChatGPT API
 */
public class ChatGptApi {
    private static final String TAG = "ChatGptApi";
    private final String apiKey;
    private String model; // Made non-final to allow changing
    private final OkHttpClient client; // Added OkHttpClient as a member

    /**
     * Constructor
     *
     * @param apiKey OpenAI API key
     * @param model Model name (e.g. "gpt-3.5-turbo")
     */
    public ChatGptApi(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.d(TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        this.client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Sets the model to be used for API calls.
     * @param model The model name.
     */
    public void setModel(String model) {
        this.model = model;
        Log.d(TAG, "ChatGPT model updated to: " + model);
    }

    public String getModel() {
        return this.model;
    }

    /**
     * Gets a completion from the ChatGPT API
     *
     * @param prompt The prompt text to send to ChatGPT
     * @return Completion from ChatGPT
     * @throws Exception if API call fails
     */
    public String getCompletion(String prompt) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }

        // HttpLoggingInterceptor and OkHttpClient are now member variables, initialized in constructor

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(this.client) // Use member client
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ChatGptApiService apiService = retrofit.create(ChatGptApiService.class);

        ChatGptRequest.Message userMessage = new ChatGptRequest.Message("user", prompt);
        ChatGptRequest request = new ChatGptRequest(this.model, Collections.singletonList(userMessage));

        String authToken = "Bearer " + this.apiKey;

        Call<ChatGptResponse> call = apiService.getChatCompletion(authToken, request);

        try {
            Response<ChatGptResponse> response = call.execute();

            if (response.isSuccessful() && response.body() != null && response.body().getChoices() != null && !response.body().getChoices().isEmpty()) {
                return response.body().getChoices().get(0).getMessage().getContent();
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                throw new Exception("Error getting completion: " + response.code() + " " + response.message() + " - " + errorBody);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "getCompletion failed due to socket timeout: " + e.getMessage(), e);
            throw new SocketTimeoutException("getCompletion socket timeout: " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "getCompletion failed due to unknown host: " + e.getMessage(), e);
            throw new UnknownHostException("getCompletion unknown host: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "getCompletion failed due to network issue: " + e.getMessage(), e);
            throw new IOException("Error getting completion due to network issue: " + e.getMessage(), e);
        }
    }

    /**
     * Gets a completion from the ChatGPT API for vision models (text and image input).
     *
     * @param contentParts List of content parts (text or image URLs)
     * @param visionModelName The specific vision model to use
     * @param maxTokens Optional maximum number of tokens for the response
     * @return Completion from ChatGPT
     * @throws Exception if API call fails
     */
    public String getVisionCompletion(List<ChatGptRequest.ContentPart> contentParts, String visionModelName, Integer maxTokens) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        if (contentParts == null || contentParts.isEmpty()) {
            throw new IllegalArgumentException("Content parts cannot be empty");
        }
        if (visionModelName == null || visionModelName.isEmpty()) {
            throw new IllegalArgumentException("Vision model name is required");
        }

        // HttpLoggingInterceptor and OkHttpClient are now member variables, initialized in constructor

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(this.client) // Use member client
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ChatGptApiService apiService = retrofit.create(ChatGptApiService.class);

        ChatGptRequest.Message userMessage = new ChatGptRequest.Message("user", contentParts);
        // Use the constructor that accepts maxTokens
        ChatGptRequest request = new ChatGptRequest(visionModelName, Collections.singletonList(userMessage), maxTokens);


        String authToken = "Bearer " + this.apiKey;
        Call<ChatGptResponse> call = apiService.getChatCompletion(authToken, request);

        try {
            Response<ChatGptResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null && response.body().getChoices() != null && !response.body().getChoices().isEmpty()) {
                return response.body().getChoices().get(0).getMessage().getContent();
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                throw new Exception("Error getting vision completion: " + response.code() + " " + response.message() + " - " + errorBody);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "getVisionCompletion failed due to socket timeout: " + e.getMessage(), e);
            throw new SocketTimeoutException("getVisionCompletion socket timeout: " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "getVisionCompletion failed due to unknown host: " + e.getMessage(), e);
            throw new UnknownHostException("getVisionCompletion unknown host: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "getVisionCompletion failed due to network issue: " + e.getMessage(), e);
            throw new IOException("Error getting vision completion due to network issue: " + e.getMessage(), e);
        }
    }


    public List<ModelInfo> listModels() throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key is required for listing models");
        }

        // HttpLoggingInterceptor and OkHttpClient are now member variables, initialized in constructor

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/") // Ensure this base URL is appropriate
                .client(this.client) // Use member client
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ChatGptApiService apiService = retrofit.create(ChatGptApiService.class);
        String authToken = "Bearer " + this.apiKey;

        Call<OpenAIModelsResponse> call = apiService.listModels(authToken);

        try {
            Response<OpenAIModelsResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                return response.body().getData();
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                Log.e(TAG, "Error listing models: " + response.code() + " " + response.message() + " - " + errorBody);
                throw new Exception("Error listing models: " + response.code() + " " + response.message() + " - " + errorBody);
            }
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "listModels failed due to socket timeout: " + e.getMessage(), e);
            throw new SocketTimeoutException("listModels socket timeout: " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "listModels failed due to unknown host: " + e.getMessage(), e);
            throw new UnknownHostException("listModels unknown host: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "listModels failed due to network issue: " + e.getMessage(), e);
            throw new IOException("Error listing models due to network issue: " + e.getMessage(), e);
        }
    }

    public String getTranscriptionFromAudio(File audioFile, String userPrompt, String modelName) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("OpenAI API key is not set.");
        }
        if (audioFile == null || !audioFile.exists()) {
            throw new IOException("Audio file is missing or invalid.");
        }

        Log.d(TAG, "Sending audio transcription request. Model: " + modelName + ", Prompt: " + userPrompt);

        String whisperFileMimeType;
        String lowerName = audioFile.getName().toLowerCase();

        if (lowerName.equals("recording.m4a")) { // Prioritize this case
            whisperFileMimeType = "audio/m4a";
        } else if (lowerName.endsWith(".m4a")) { // General .m4a check
            whisperFileMimeType = "audio/m4a";
        } else if (lowerName.endsWith(".mp3")) {
            whisperFileMimeType = "audio/mpeg";
        } else if (lowerName.endsWith(".wav")) {
            whisperFileMimeType = "audio/wav";
        } else {
            whisperFileMimeType = "audio/m4a"; // Default if extension is unknown
            Log.w(TAG, "Unknown audio file extension for Whisper: " + lowerName + ". Defaulting to audio/m4a MIME type.");
        }
        Log.i(TAG, "WHISPER_MIME_CHECK: Using MIME type: " + whisperFileMimeType + " for file: " + lowerName);
        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse(whisperFileMimeType));

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(), fileBody)
                .addFormDataPart("model", modelName);

        if (userPrompt != null && !userPrompt.isEmpty()) {
            requestBodyBuilder.addFormDataPart("prompt", userPrompt);
        }
        // Add other parameters as needed, e.g., language, temperature
        // requestBodyBuilder.addFormDataPart("language", "en");
        // requestBodyBuilder.addFormDataPart("temperature", "0.2");

        RequestBody requestBody = requestBodyBuilder.build();

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/audio/transcriptions") // Standard Whisper API endpoint
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (okhttp3.Response response = this.client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                Log.e(TAG, "Audio transcription API request failed: " + response.code() + " " + errorBody);
                throw new IOException("Unexpected code " + response + "\n" + errorBody);
            }

            String responseBodyString = response.body().string();
            Log.d(TAG, "Audio transcription response: " + responseBodyString);

            try {
                JSONObject jsonResponse = new JSONObject(responseBodyString);
                if (jsonResponse.has("text")) {
                    return jsonResponse.getString("text");
                } else {
                    Log.e(TAG, "Audio transcription response does not contain 'text' field: " + responseBodyString);
                    throw new IOException("Invalid response format from transcription API: 'text' field missing.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse JSON from audio transcription response", e);
                throw new IOException("Failed to parse JSON response: " + e.getMessage(), e);
            }
        }
    }

    // }

    private String encodeAudioToBase64(File audioFile) throws IOException {
        // Ensure java.nio.file.Files and android.util.Base64 are imported
        byte[] audioBytes = java.nio.file.Files.readAllBytes(audioFile.toPath());
        return android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP);
    }

    public String getCompletionFromAudioAndPrompt(File audioFile, String userPrompt, String modelName) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("OpenAI API key is not set.");
        }
        if (audioFile == null || !audioFile.exists()) {
            throw new IOException("Audio file is missing or invalid.");
        }
        if (userPrompt == null || userPrompt.isEmpty()) {
            userPrompt = "What is in this recording?"; // Default prompt from JS example
        }

        Log.d(TAG, "Sending JSON chat completion request with audio to " + modelName +
                   ". Prompt: " + userPrompt + ", Audio: " + audioFile.getName());

        String base64Audio = encodeAudioToBase64(audioFile);

        String audioFileFormat;
        String fileNameLower = audioFile.getName().toLowerCase();

        if (fileNameLower.equals("recording.m4a")) { // Prioritize this case
            audioFileFormat = "m4a";
        } else if (fileNameLower.endsWith(".m4a")) { // General .m4a check
            audioFileFormat = "m4a";
        } else if (fileNameLower.endsWith(".mp3")) {
            audioFileFormat = "mp3";
        } else if (fileNameLower.endsWith(".wav")) {
            audioFileFormat = "wav";
        } else if (fileNameLower.endsWith(".ogg")) {
            audioFileFormat = "ogg";
        } else if (fileNameLower.endsWith(".flac")) {
            audioFileFormat = "flac";
        } else {
            audioFileFormat = "m4a"; // Default if extension is unknown or different
            Log.w(TAG, "Unknown audio file extension for getCompletionFromAudioAndPrompt: " + fileNameLower + ". Defaulting to m4a format for API.");
        }
        Log.i(TAG, "AUDIO_FORMAT_API_CHECK: Using audioFileFormat: " + audioFileFormat + " for file: " + fileNameLower);


        JSONObject payload = new JSONObject();
        try {
            payload.put("model", modelName); // e.g., "gpt-4o-audio-preview"

            JSONArray modalitiesArray = new JSONArray();
            modalitiesArray.put("text");
            modalitiesArray.put("audio");
            payload.put("modalities", modalitiesArray);

            JSONObject audioSettings = new JSONObject();
            // The JS example has 'audio: { voice: "alloy", format: "wav" }'
            // 'voice' is for TTS. For input, 'format' seems relevant as a hint.
            audioSettings.put("format", audioFileFormat); // Hinting the input format
            audioSettings.put("voice", "alloy");         // Add the required voice parameter
            payload.put("audio", audioSettings);

            JSONArray messagesArray = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            JSONArray contentArray = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", userPrompt);
            contentArray.put(textPart);

            JSONObject audioInputPart = new JSONObject();
            audioInputPart.put("type", "input_audio"); // Crucial: type "input_audio"
            JSONObject inputAudioData = new JSONObject();
            inputAudioData.put("data", base64Audio);   // Base64 encoded string
            inputAudioData.put("format", audioFileFormat); // Format of the encoded data
            audioInputPart.put("input_audio", inputAudioData);
            contentArray.put(audioInputPart);

            userMessage.put("content", contentArray);
            messagesArray.put(userMessage);
            payload.put("messages", messagesArray);

            payload.put("store", true); // As per JS example

        } catch (org.json.JSONException e) {
            throw new IOException("Error creating JSON payload for audio chat completion: " + e.getMessage(), e);
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json") // Explicitly set Content-Type for JSON body
                .post(body)
                .build();

        try (okhttp3.Response response = this.client.newCall(request).execute()) {
            String responseBodyString = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                Log.e(TAG, "API request failed (JSON audio chat): " + response.code() + " Body: " + responseBodyString);
                throw new IOException("Unexpected code " + response + (responseBodyString != null ? "\n" + responseBodyString : ""));
            }

            if (responseBodyString == null) {
                throw new IOException("Empty response body from API (JSON audio chat)");
            }
            Log.d(TAG, "API Response (JSON audio chat): " + responseBodyString);

            try {
                JSONObject jsonResponse = new JSONObject(responseBodyString);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message") && firstChoice.getJSONObject("message").has("content")) {
                        return firstChoice.getJSONObject("message").getString("content");
                    } else {
                        Log.w(TAG, "Unexpected message structure in choice (JSON audio chat): " + firstChoice.toString());
                        throw new IOException("No 'message.content' in API response choice (JSON audio chat).");
                    }
                } else {
                    throw new IOException("No choices returned in API response (JSON audio chat).");
                }
            } catch (org.json.JSONException e) {
                throw new IOException("Error parsing JSON from API response (JSON audio chat): " + e.getMessage(), e);
            }
        }
    }
}