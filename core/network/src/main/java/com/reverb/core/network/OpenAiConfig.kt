package com.reverb.core.network

/**
 * Settings for [OpenAiWritingEngine]. Any server that speaks the OpenAI Chat Completions
 * protocol works (OpenAI, Azure OpenAI behind a gateway, a self hosted proxy).
 *
 * In the app these come from Gradle properties through BuildConfig, see `gradle.properties`.
 */
data class OpenAiConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 60,
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }
}
