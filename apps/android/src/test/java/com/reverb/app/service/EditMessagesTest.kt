package com.reverb.app.service

import com.reverb.core.domain.EditError
import com.reverb.core.domain.engine.WritingEngineException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.SocketTimeoutException

class EditMessagesTest {

    @Test
    fun `nothing to undo stays silent`() {
        assertNull(EditMessages.forError(BubbleAction.UNDO, EditError.NothingToUndo))
    }

    @Test
    fun `write failures name the action`() {
        assertEquals(
            "Could not insert reply into this app.",
            EditMessages.forError(BubbleAction.SMART_REPLY, EditError.WriteRejected),
        )
        assertEquals("Could not undo in this app.", EditMessages.forError(BubbleAction.UNDO, EditError.WriteRejected))
    }

    @Test
    fun `engine status codes map to friendly messages`() {
        val unauthorized = EditError.EngineFailed(WritingEngineException("OpenAI error 401: bad key", statusCode = 401))
        val limited = EditError.EngineFailed(WritingEngineException("slow down", statusCode = 429))
        assertEquals(
            "Invalid API key. Check your key in Reverb settings.",
            EditMessages.forError(BubbleAction.POLISH, unauthorized),
        )
        assertEquals(
            "Rate limit reached. Wait a moment and try again.",
            EditMessages.forError(BubbleAction.POLISH, limited),
        )
    }

    @Test
    fun `network failures map to a network message`() {
        val error = EditError.EngineFailed(WritingEngineException("Network error", cause = SocketTimeoutException()))
        assertEquals("Network error. Check your internet connection.", EditMessages.forError(BubbleAction.POLISH, error))
    }

    @Test
    fun `other engine errors show a trimmed message`() {
        val error = EditError.EngineFailed(IllegalStateException("x".repeat(300)))
        assertEquals(120, EditMessages.forError(BubbleAction.POLISH, error)!!.length)
    }
}
