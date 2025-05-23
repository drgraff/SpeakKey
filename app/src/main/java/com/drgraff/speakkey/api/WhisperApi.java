package com.drgraff.speakkey.api;

import android.util.Log;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * API client for OpenAI's Whisper API for speech-to-text
 */
public class WhisperApi {
    private static final String TAG = "WhisperApi";
    private final String apiKey;
    private final String endpoint;
    private final String language;
    
    /**
     * Constructor
     * 
     * @param apiKey OpenAI API key
     * @param endpoint Whisper API endpoint
     * @param language Language code (e.g. "en", "fr")
     */
    public WhisperApi(String apiKey, String endpoint, String language) {
        this.apiKey = apiKey;
        
        String correctedEndpoint = endpoint;
        if (correctedEndpoint != null && !correctedEndpoint.trim().isEmpty()) { // Check for non-empty after trimming whitespace
            // Remove all existing trailing slashes first to handle cases like "http://example.com///"
            while (correctedEndpoint.endsWith("/")) {
                correctedEndpoint = correctedEndpoint.substring(0, correctedEndpoint.length() - 1);
            }
            // Then add a single trailing slash
            correctedEndpoint += "/";
        } else {
            // If endpoint is null, empty, or whitespace-only, this is problematic.
            // Throw an error or use a hardcoded default known to be correct.
            // Given MainActivity provides a default, an exception for a bad configured state is reasonable.
            throw new IllegalArgumentException("Endpoint URL cannot be null or empty.");
        }
        this.endpoint = correctedEndpoint;
        
        this.language = language;
    }
    
    /**
     * Transcribes audio file to text using Whisper API
     * 
     * @param audioFile Audio file to transcribe
     * @return Transcribed text
     * @throws Exception if transcription fails
     */
    public String transcribe(File audioFile) throws Exception {
        // Perform input validation
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        
        if (!audioFile.exists() || audioFile.length() == 0) {
            throw new IllegalArgumentException("Audio file does not exist or is empty");
        }
        
        // Create HTTP client and make API request
        // Create logging interceptor for debugging
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                Log.d(TAG, message);
            }
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        // Build OkHttp client
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build();
        
        // Create Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(endpoint)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
            
        // Create API service
        WhisperApiService apiService = retrofit.create(WhisperApiService.class);

        // Create RequestBody for "model"
        RequestBody model = RequestBody.create(MediaType.parse("text/plain"), "whisper-1");

        // Create RequestBody for "language"
        RequestBody language = RequestBody.create(MediaType.parse("text/plain"), this.language);

        // Create MultipartBody.Part for the audio file
        RequestBody audioRequestBody = RequestBody.create(MediaType.parse("audio/*"), audioFile);
        MultipartBody.Part audioFilePart = MultipartBody.Part.createFormData("file", audioFile.getName(), audioRequestBody);

        // Construct the "Authorization" header
        String authToken = "Bearer " + this.apiKey;

        // Call apiService.transcribeAudio()
        Call<WhisperApiResponse> call = apiService.transcribeAudio(authToken, audioFilePart, model, language);

        try {
            // Execute the Retrofit call synchronously
            Response<WhisperApiResponse> response = call.execute();

            // Check if the call was successful
            if (response.isSuccessful() && response.body() != null) {
                return response.body().getText();
            } else {
                // Throw an exception if the call was not successful or the body is null
                String errorMessage = "Transcription failed: ";
                if (response.errorBody() != null) {
                    errorMessage += response.errorBody().string();
                } else {
                    errorMessage += "Response code " + response.code();
                }
                throw new Exception(errorMessage);
            }
        } catch (java.io.IOException e) {
            // Handle potential IOException
            throw new Exception("Transcription failed due to network error: " + e.getMessage(), e);
        }
    }
}