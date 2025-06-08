package com.drgraff.speakkey.api;

import android.util.Log;

import java.io.File;
// Unused imports for URL and MalformedURLException will be removed by not re-adding them.

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.io.IOException;
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
        this.language = language;

        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint URL cannot be null, empty, or whitespace-only.");
        }
        this.endpoint = endpoint.trim(); // Assign trimmed endpoint directly
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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Transcription failed due to socket timeout: " + e.getMessage(), e);
            throw new SocketTimeoutException("Transcription socket timeout: " + e.getMessage());
        } catch (UnknownHostException e) {
            Log.e(TAG, "Transcription failed due to unknown host: " + e.getMessage(), e);
            throw new UnknownHostException("Transcription unknown host: " + e.getMessage());
        } catch (IOException e) { // Catching a broader IOException for other network issues
            Log.e(TAG, "Transcription failed due to network error: " + e.getMessage(), e);
            // It's better to throw a more specific custom exception or re-throw e if it's already specific enough
            throw new IOException("Transcription failed due to network error: " + e.getMessage(), e);
        }
    }
}