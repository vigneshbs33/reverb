package com.reverb.core.domain.engine

import com.reverb.core.model.ReverbTone

/**
 * The language model behind Reverb. Swap this to use your own model, gateway or on-device engine.
 *
 * Implementations should throw [WritingEngineException] for provider errors so the UI can show
 * a precise message. Calls are made off the main thread.
 */
interface WritingEngine {
    /** False when the engine is missing something it needs, such as an API key. */
    val isConfigured: Boolean get() = true

    /** Rewrites [RewriteRequest.text] in the requested tone. */
    suspend fun rewrite(request: RewriteRequest): RewriteResult

    /** Writes a short reply to [ReplyRequest.message]. Returns only the reply text. */
    suspend fun reply(request: ReplyRequest): String
}

/**
 * @property styleSample the user's own writing in this tone, used to match their voice. May be blank.
 */
data class RewriteRequest(
    val text: String,
    val tone: ReverbTone,
    val styleSample: String = "",
)

/**
 * @property intent the kind of reply: in the bubble, Accept maps to [ReverbTone.PROFESSIONAL],
 * Decline to [ReverbTone.EDGY] and Clarify to [ReverbTone.FRIENDLY].
 * @property styleSample the user's own writing, used to match their voice. May be blank.
 */
data class ReplyRequest(
    val message: String,
    val intent: ReverbTone,
    val styleSample: String = "",
)

sealed interface RewriteResult {
    /** The engine produced new text, with an optional short explanation of what changed. */
    data class Rewritten(val text: String, val reason: String? = null) : RewriteResult

    /** The text is already correct (Fact Check) and should be left untouched. */
    data object NoChangeNeeded : RewriteResult
}

/** A provider error. [statusCode] is the HTTP status when there was one. */
class WritingEngineException(
    message: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause)
