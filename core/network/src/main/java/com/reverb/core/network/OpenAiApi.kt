package com.reverb.core.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Body body: OpenAiRequest,
    ): Response<OpenAiResponse>
}
