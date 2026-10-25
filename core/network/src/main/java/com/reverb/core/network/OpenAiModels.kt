package com.reverb.core.network

import com.google.gson.annotations.SerializedName

internal data class OpenAiRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<OpenAiMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("temperature") val temperature: Double = 0.7,
    @SerializedName("response_format") val responseFormat: ResponseFormat? = null,
)

internal data class ResponseFormat(
    @SerializedName("type") val type: String = "json_object",
)

internal data class OpenAiMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
)

internal data class OpenAiResponse(
    @SerializedName("choices") val choices: List<Choice>?,
    @SerializedName("error") val error: OpenAiError?,
) {
    data class Choice(
        @SerializedName("message") val message: OpenAiMessage,
    )
}

internal data class OpenAiError(
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String?,
)
