package com.reverb.app.service

import com.reverb.core.domain.EditError
import com.reverb.core.domain.engine.WritingEngineException
import java.io.IOException

/** The bubble action a message is for, so each failure reads naturally. */
enum class BubbleAction { POLISH, SMART_REPLY, UNDO }

/** Turns an [EditError] into the short line shown in the bubble, or null to stay silent. */
object EditMessages {

    const val ALL_GOOD = "You're all good! No corrections needed."

    fun forError(action: BubbleAction, error: EditError): String? = when (error) {
        EditError.EngineNotConfigured -> "No API key saved. Open Reverb to set your key."
        EditError.FieldUnavailable -> "Accessibility service not active. Enable it in Settings."
        EditError.NoText -> "No text found. Tap inside a text field first."
        EditError.NoMessageToReplyTo -> "Nothing on clipboard. Copy a message first, then try Smart Reply."
        EditError.NothingToUndo -> null
        EditError.WriteRejected -> when (action) {
            BubbleAction.POLISH -> "Could not replace text in this app."
            BubbleAction.SMART_REPLY -> "Could not insert reply into this app."
            BubbleAction.UNDO -> "Could not undo in this app."
        }
        is EditError.EngineFailed -> forEngineFailure(error.cause)
    }

    private fun forEngineFailure(cause: Throwable): String {
        val status = (cause as? WritingEngineException)?.statusCode
        val message = cause.message.orEmpty()
        return when {
            status == 401 || message.contains("401") -> "Invalid API key. Check your key in Reverb settings."
            status == 429 || message.contains("429") -> "Rate limit reached. Wait a moment and try again."
            cause is IOException ||
                cause.cause is IOException ||
                message.contains("timeout", ignoreCase = true) ||
                message.contains("connect", ignoreCase = true) -> "Network error. Check your internet connection."
            message.isBlank() -> "Unknown error"
            else -> message.take(120)
        }
    }
}
