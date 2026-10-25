package com.reverb.core.network

import com.reverb.core.domain.engine.ReplyRequest
import com.reverb.core.domain.engine.RewriteRequest
import com.reverb.core.domain.engine.RewriteResult
import com.reverb.core.domain.engine.WritingEngine
import com.reverb.core.domain.engine.WritingEngineException
import com.reverb.core.model.ReverbTone
import com.reverb.core.prompt.PromptBundle
import com.reverb.core.prompt.ReverbPromptFactory
import java.io.IOException

/**
 * The default [WritingEngine]: OpenAI Chat Completions with the user's own API key.
 *
 * @param apiKeyProvider returns the current key, or null when none is saved.
 */
class OpenAiWritingEngine(
    private val config: OpenAiConfig = OpenAiConfig(),
    private val apiKeyProvider: () -> String?,
    private val promptFactory: ReverbPromptFactory = ReverbPromptFactory(),
) : WritingEngine {

    private val api: OpenAiApi by lazy { OpenAiClientFactory.create(config, apiKeyProvider) }

    override val isConfigured: Boolean
        get() = !apiKeyProvider().isNullOrBlank()

    override suspend fun rewrite(request: RewriteRequest): RewriteResult {
        val prompt = promptFactory.buildRewritePrompt(
            input = request.text,
            tone = request.tone,
            customInstruction = request.styleSample,
        )
        val isFactCheck = request.tone == ReverbTone.FACT_CHECK
        val content = complete(prompt, jsonOutput = isFactCheck)
        return if (isFactCheck) FactCheckParser.parse(content) else RewriteResult.Rewritten(content)
    }

    override suspend fun reply(request: ReplyRequest): String {
        val prompt = promptFactory.buildReplyPrompt(
            contextText = request.message,
            replyIntent = request.intent,
            customInstruction = request.styleSample,
        )
        return complete(prompt, jsonOutput = false)
    }

    private suspend fun complete(prompt: PromptBundle, jsonOutput: Boolean): String {
        val body = OpenAiRequest(
            model = config.model,
            messages = listOf(
                OpenAiMessage(role = "system", content = prompt.systemPrompt),
                OpenAiMessage(role = "user", content = prompt.userPrompt),
            ),
            responseFormat = if (jsonOutput) ResponseFormat("json_object") else null,
        )

        val response = try {
            api.createChatCompletion(body)
        } catch (e: IOException) {
            throw WritingEngineException("Network error: ${e.message}", cause = e)
        }

        if (!response.isSuccessful) {
            val detail = response.errorBody()?.string().orEmpty().ifBlank { "HTTP ${response.code()}" }
            throw WritingEngineException("OpenAI error ${response.code()}: $detail", statusCode = response.code())
        }

        val result = response.body()
        result?.error?.let { throw WritingEngineException("OpenAI API error: ${it.message}") }

        return result?.choices?.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw WritingEngineException("Empty response from OpenAI")
    }
}
