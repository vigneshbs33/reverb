package com.reverb.core.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object OpenAiClientFactory {

    /**
     * Builds one client for the life of the engine. The key is read on every request
     * through [apiKeyProvider], so a key changed in settings applies to the next call.
     */
    fun create(config: OpenAiConfig, apiKeyProvider: () -> String?): OpenAiApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("Authorization", "Bearer ${apiKeyProvider().orEmpty()}")
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()

        val baseUrl = if (config.baseUrl.endsWith("/")) config.baseUrl else config.baseUrl + "/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApi::class.java)
    }
}
