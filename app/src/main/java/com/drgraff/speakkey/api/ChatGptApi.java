package com.drgraff.speakkey.api;

import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import java.util.Collections;

/**
 * API client for OpenAI's ChatGPT API
 */
public class ChatGptApi {
    private static final String TAG = "ChatGptApi";
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

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d(TAG, message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(client)
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
        } catch (IOException e) {
            throw new Exception("Error getting completion due to network issue: " + e.getMessage(), e);
        }
    }
}