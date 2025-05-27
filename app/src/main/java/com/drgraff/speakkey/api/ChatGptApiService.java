package com.drgraff.speakkey.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET; // Added
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ChatGptApiService {
    @POST("/v1/chat/completions")
    Call<ChatGptResponse> getChatCompletion(
            @Header("Authorization") String authorization,
            @Body ChatGptRequest request
    );

    @GET("v1/models")
    Call<OpenAIModelData.OpenAIModelsResponse> listModels(@Header("Authorization") String authToken);
}
