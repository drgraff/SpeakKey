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

        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("audio/mpeg")); // Adjust MediaType if necessary

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

    private String encodeAudioToBase64(File audioFile) throws IOException {
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        return Base64.encodeToString(audioBytes, Base64.NO_WRAP);
    }

    public String getCompletionFromAudioAndPrompt(File audioFile, String userPrompt, String modelName) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("OpenAI API key is not set.");
        }
        if (audioFile == null || !audioFile.exists()) {
            throw new IOException("Audio file is missing or invalid.");
        }
        if (userPrompt == null || userPrompt.isEmpty()) {
            userPrompt = "Transcribe the audio and follow any instructions if present. If no specific instructions, just transcribe."; // Default if empty
        }

        Log.d("ChatGptApi", "Sending multimodal request to " + modelName + ". Prompt: " + userPrompt);

        String base64Audio = encodeAudioToBase64(audioFile);
        String audioMimeType = "audio/mpeg"; // Assuming MP3, adjust if other formats are possible
        String audioDataUrl = "data:" + audioMimeType + ";base64," + base64Audio;

        JSONObject payload = new JSONObject();
        try {
            payload.put("model", modelName); // e.g., "gpt-4o"
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");

            JSONArray contentArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", userPrompt);
            contentArray.put(textPart);

            JSONObject audioContentPart = new JSONObject();
            audioContentPart.put("type", "audio_url");
            JSONObject audioUrlObject = new JSONObject();
            audioUrlObject.put("url", audioDataUrl);
            audioContentPart.put("audio_url", audioUrlObject);
            contentArray.put(audioContentPart);

            userMessage.put("content", contentArray);
            messages.put(userMessage);
            payload.put("messages", messages);

            // payload.put("max_tokens", 1000); // Optional

        } catch (org.json.JSONException e) {
            throw new IOException("Error creating JSON payload for multimodal request: " + e.getMessage(), e);
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json; charset=utf-8"));
        okhttp3.Request request = new okhttp3.Request.Builder() // Explicitly using okhttp3.Request
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) { // Explicitly using okhttp3.Response
            String responseBodyString = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                Log.e("ChatGptApi", "Multimodal API request failed: " + response.code() + " Body: " + responseBodyString);
                throw new IOException("Unexpected code " + response + (responseBodyString != null ? "\n" + responseBodyString : ""));
            }

            if (responseBodyString == null) {
                throw new IOException("Empty response body from multimodal API");
            }
            Log.d("ChatGptApi", "Multimodal response: " + responseBodyString);

            try {
                JSONObject jsonResponse = new JSONObject(responseBodyString);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.has("message") && firstChoice.getJSONObject("message").has("content")) {
                        return firstChoice.getJSONObject("message").getString("content");
                    } else {
                        Log.w("ChatGptApi", "Unexpected message structure in choice: " + firstChoice.toString());
                        if (firstChoice.has("delta") && firstChoice.getJSONObject("delta").has("content")) {
                             return firstChoice.getJSONObject("delta").getString("content");
                        }
                        throw new IOException("No 'message.content' in API response choice.");
                    }
                } else {
                    throw new IOException("No choices returned in multimodal API response.");
                }
            } catch (org.json.JSONException e) {
                throw new IOException("Error parsing JSON from multimodal response: " + e.getMessage(), e);
            }
        }
    }
}