package com.drgraff.speakkey.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit interface for OpenAI's Whisper API
 */
public interface WhisperApiService {
    
    /**
     * Transcribes an audio file using the OpenAI Whisper API
     *
     * @param authorization Authorization header with API key
     * @param file Audio file to transcribe
     * @param model Model to use (default: "whisper-1")
     * @param language Language of the audio (optional)
     * @return Response containing the transcription
     */
    @Multipart
    @POST("v1/audio/transcriptions")
    Call<WhisperApiResponse> transcribeAudio(
        @Header("Authorization") String authorization,
        @Part MultipartBody.Part file,
        @Part("model") RequestBody model,
        @Part("language") RequestBody language
    );
}