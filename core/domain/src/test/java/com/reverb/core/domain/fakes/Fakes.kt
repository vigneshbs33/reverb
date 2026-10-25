package com.reverb.core.domain.fakes

import com.reverb.core.domain.engine.ReplyRequest
import com.reverb.core.domain.engine.RewriteRequest
import com.reverb.core.domain.engine.RewriteResult
import com.reverb.core.domain.engine.WritingEngine
import com.reverb.core.domain.field.FieldSnapshot
import com.reverb.core.domain.field.TextFieldGateway

/**
 * Behaves like an Android text field driven through accessibility: a full text write
 * moves the cursor to the end and drops the selection, unless a selection is set after it.
 */
class FakeTextField(
    text: String = "",
    selectionStart: Int = FieldSnapshot.NO_SELECTION,
    selectionEnd: Int = FieldSnapshot.NO_SELECTION,
) : TextFieldGateway {
    var current = FieldSnapshot(text, selectionStart, selectionEnd)
        private set
    var focused = true
    var acceptsWrites = true
    override var isAvailable = true
    var writes = 0
        private set

    override fun read(): FieldSnapshot? = if (focused) current else null

    override fun write(snapshot: FieldSnapshot): Boolean {
        if (!focused || !acceptsWrites) return false
        writes++
        val end = snapshot.text.length
        current = if (snapshot.hasSelection) snapshot else FieldSnapshot(snapshot.text, end, end)
        return true
    }

    /** Simulates the user moving the cursor or selecting text. */
    fun select(start: Int, end: Int) {
        current = current.copy(selectionStart = start, selectionEnd = end)
    }
}

class FakeWritingEngine(
    var rewriteResult: (RewriteRequest) -> RewriteResult = { RewriteResult.Rewritten(it.text.uppercase()) },
    var replyText: (ReplyRequest) -> String = { "Sounds good" },
) : WritingEngine {
    override var isConfigured = true
    val rewriteRequests = mutableListOf<RewriteRequest>()
    val replyRequests = mutableListOf<ReplyRequest>()

    override suspend fun rewrite(request: RewriteRequest): RewriteResult {
        rewriteRequests += request
        return rewriteResult(request)
    }

    override suspend fun reply(request: ReplyRequest): String {
        replyRequests += request
        return replyText(request)
    }
}
