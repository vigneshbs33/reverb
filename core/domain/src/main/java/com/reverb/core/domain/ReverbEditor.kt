package com.reverb.core.domain

import com.reverb.core.domain.engine.ReplyRequest
import com.reverb.core.domain.engine.RewriteRequest
import com.reverb.core.domain.engine.RewriteResult
import com.reverb.core.domain.engine.WritingEngine
import com.reverb.core.domain.field.FieldSnapshot
import com.reverb.core.domain.field.TextFieldGateway
import com.reverb.core.domain.storage.SettingsStorage
import com.reverb.core.domain.storage.StyleSamples
import com.reverb.core.model.ReverbTone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs Reverb's three actions against a text field: Polish, Smart Reply and Undo.
 *
 * This class holds no Android code. Give it a [TextFieldGateway], a [WritingEngine] and a
 * [SettingsStorage] and it works in any host. Actions are serialised, so a second tap
 * waits for the first one to finish.
 *
 * Undo restores the whole field as it was before the last edit, including the text
 * around a selection and the selection itself.
 */
class ReverbEditor(
    private val field: TextFieldGateway,
    private val engine: WritingEngine,
    settings: SettingsStorage,
) {
    private val samples = StyleSamples(settings)
    private val mutex = Mutex()
    private var undoSnapshot: FieldSnapshot? = null

    private val _canUndo = MutableStateFlow(false)

    /** True while there is an edit that [undo] can revert. */
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    /** Rewrites the selection, or the whole field when nothing is selected, in [tone]. */
    suspend fun polish(tone: ReverbTone): EditResult = mutex.withLock {
        if (!engine.isConfigured) return@withLock EditResult.Failed(EditError.EngineNotConfigured)
        if (!field.isAvailable) return@withLock EditResult.Failed(EditError.FieldUnavailable)

        val before = field.read()
        val target = before?.target
        if (before == null || target.isNullOrBlank()) return@withLock EditResult.Failed(EditError.NoText)

        val result = try {
            engine.rewrite(RewriteRequest(target, tone, samples.forRewrite(tone)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock EditResult.Failed(EditError.EngineFailed(e))
        }

        when (result) {
            RewriteResult.NoChangeNeeded -> EditResult.NothingToChange
            is RewriteResult.Rewritten -> apply(before, result.text, result.reason)
        }
    }

    /**
     * Writes a reply to [incomingMessage] (usually the clipboard) with the given [intent]
     * and puts it in the focused field, in place of the selection when there is one.
     */
    suspend fun reply(intent: ReverbTone, incomingMessage: String): EditResult = mutex.withLock {
        if (incomingMessage.isBlank()) return@withLock EditResult.Failed(EditError.NoMessageToReplyTo)
        if (!engine.isConfigured) return@withLock EditResult.Failed(EditError.EngineNotConfigured)
        if (!field.isAvailable) return@withLock EditResult.Failed(EditError.FieldUnavailable)

        val before = field.read() ?: return@withLock EditResult.Failed(EditError.WriteRejected)

        val reply = try {
            engine.reply(ReplyRequest(incomingMessage, intent, samples.forReply(intent)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withLock EditResult.Failed(EditError.EngineFailed(e))
        }

        apply(before, reply, reason = null)
    }

    /** Restores the field to how it was before the last Polish or Smart Reply. */
    suspend fun undo(): EditResult = mutex.withLock {
        val snapshot = undoSnapshot ?: return@withLock EditResult.Failed(EditError.NothingToUndo)
        if (!field.isAvailable) return@withLock EditResult.Failed(EditError.FieldUnavailable)
        if (!field.write(snapshot)) return@withLock EditResult.Failed(EditError.WriteRejected)
        setUndo(null)
        EditResult.Undone
    }

    private fun apply(before: FieldSnapshot, newText: String, reason: String?): EditResult {
        if (!field.write(before.replaceTarget(newText))) {
            return EditResult.Failed(EditError.WriteRejected)
        }
        setUndo(before)
        return EditResult.Applied(preview = newText, reason = reason)
    }

    private fun setUndo(snapshot: FieldSnapshot?) {
        undoSnapshot = snapshot
        _canUndo.value = snapshot != null
    }
}

/** The outcome of an [ReverbEditor] action. */
sealed interface EditResult {
    /** New text is in the field. [preview] is what was inserted. */
    data class Applied(val preview: String, val reason: String? = null) : EditResult

    /** The engine found nothing to fix, so the field was left untouched. */
    data object NothingToChange : EditResult

    /** The field is back to how it was before the last edit. */
    data object Undone : EditResult

    data class Failed(val error: EditError) : EditResult
}

sealed interface EditError {
    /** The engine needs setup first, for example an API key. */
    data object EngineNotConfigured : EditError

    /** No text field can be reached (for example the accessibility service is off). */
    data object FieldUnavailable : EditError

    /** The focused field is empty, or there is no focused field. */
    data object NoText : EditError

    /** Smart Reply was asked to answer an empty message. */
    data object NoMessageToReplyTo : EditError

    /** The host app did not accept the new text. */
    data object WriteRejected : EditError

    /** There is no edit to revert. */
    data object NothingToUndo : EditError

    /** The writing engine threw. */
    data class EngineFailed(val cause: Throwable) : EditError
}
