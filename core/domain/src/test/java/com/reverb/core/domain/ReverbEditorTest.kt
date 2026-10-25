package com.reverb.core.domain

import com.reverb.core.domain.engine.RewriteResult
import com.reverb.core.domain.engine.WritingEngineException
import com.reverb.core.domain.fakes.FakeTextField
import com.reverb.core.domain.fakes.FakeWritingEngine
import com.reverb.core.domain.field.FieldSnapshot
import com.reverb.core.domain.storage.InMemorySettingsStorage
import com.reverb.core.domain.storage.StyleSamples
import com.reverb.core.model.ReverbTone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverbEditorTest {

    private val field = FakeTextField()
    private val engine = FakeWritingEngine()
    private val settings = InMemorySettingsStorage()
    private val editor = ReverbEditor(field, engine, settings)

    // Polish

    @Test
    fun `polish rewrites the whole field when nothing is selected`() = runTest {
        field.write(FieldSnapshot("make this better"))

        val result = editor.polish(ReverbTone.PROFESSIONAL)

        assertEquals(EditResult.Applied("MAKE THIS BETTER"), result)
        assertEquals("MAKE THIS BETTER", field.current.text)
        assertTrue(editor.canUndo.value)
    }

    @Test
    fun `polish rewrites only the selection and keeps the rest of the field`() = runTest {
        field.write(FieldSnapshot("Hi Sam, can u send it by fri? Thanks, Vig", 8, 29))

        editor.polish(ReverbTone.PROFESSIONAL)

        assertEquals("can u send it by fri?", engine.rewriteRequests.single().text)
        assertEquals("Hi Sam, CAN U SEND IT BY FRI? Thanks, Vig", field.current.text)
    }

    @Test
    fun `polish sends the tone's writing sample to the engine`() = runTest {
        settings.saveCustomInstruction("FRIENDLY", "omg yes!! see u soon")
        field.write(FieldSnapshot("i will be late"))

        editor.polish(ReverbTone.FRIENDLY)

        assertEquals("omg yes!! see u soon", engine.rewriteRequests.single().styleSample)
    }

    @Test
    fun `fact check with nothing to fix leaves the field and undo untouched`() = runTest {
        field.write(FieldSnapshot("The Sun is a star."))
        engine.rewriteResult = { RewriteResult.NoChangeNeeded }

        val result = editor.polish(ReverbTone.FACT_CHECK)

        assertEquals(EditResult.NothingToChange, result)
        assertEquals("The Sun is a star.", field.current.text)
        assertFalse(editor.canUndo.value)
    }

    @Test
    fun `polish reports a reason from the engine`() = runTest {
        field.write(FieldSnapshot("The Sun is a planet."))
        engine.rewriteResult = { RewriteResult.Rewritten("The Sun is a star.", "The Sun is a star.") }

        val result = editor.polish(ReverbTone.FACT_CHECK)

        assertEquals(EditResult.Applied("The Sun is a star.", "The Sun is a star."), result)
    }

    @Test
    fun `polish fails cleanly on an empty field, a blank selection or no focus`() = runTest {
        assertEquals(EditResult.Failed(EditError.NoText), editor.polish(ReverbTone.EDGY))

        field.write(FieldSnapshot("word   word", 4, 7))
        assertEquals(EditResult.Failed(EditError.NoText), editor.polish(ReverbTone.EDGY))

        field.focused = false
        assertEquals(EditResult.Failed(EditError.NoText), editor.polish(ReverbTone.EDGY))
        assertTrue(engine.rewriteRequests.isEmpty())
    }

    @Test
    fun `polish checks engine setup before touching the field`() = runTest {
        engine.isConfigured = false
        field.isAvailable = false

        assertEquals(EditResult.Failed(EditError.EngineNotConfigured), editor.polish(ReverbTone.EDGY))
    }

    @Test
    fun `polish reports an unavailable field`() = runTest {
        field.isAvailable = false

        assertEquals(EditResult.Failed(EditError.FieldUnavailable), editor.polish(ReverbTone.EDGY))
    }

    @Test
    fun `engine errors are returned, not thrown, and the field is untouched`() = runTest {
        field.write(FieldSnapshot("hello"))
        val error = WritingEngineException("Unauthorized", statusCode = 401)
        engine.rewriteResult = { throw error }

        val result = editor.polish(ReverbTone.PROFESSIONAL)

        assertEquals(EditResult.Failed(EditError.EngineFailed(error)), result)
        assertEquals("hello", field.current.text)
        assertFalse(editor.canUndo.value)
    }

    @Test
    fun `a rejected write does not offer undo`() = runTest {
        field.write(FieldSnapshot("hello"))
        field.acceptsWrites = false

        assertEquals(EditResult.Failed(EditError.WriteRejected), editor.polish(ReverbTone.PROFESSIONAL))
        assertFalse(editor.canUndo.value)
    }

    // Undo (regression: undo after polishing a selection used to wipe the rest of the field)

    @Test
    fun `undo after polishing a selection restores the whole field and the selection`() = runTest {
        val original = FieldSnapshot("Dear Ana, thx for the help!! Best, Raj", 10, 28)
        field.write(original)
        editor.polish(ReverbTone.PROFESSIONAL)
        assertEquals("Dear Ana, THX FOR THE HELP!! Best, Raj", field.current.text)

        val result = editor.undo()

        assertEquals(EditResult.Undone, result)
        assertEquals(original, field.current)
        assertFalse(editor.canUndo.value)
    }

    @Test
    fun `undo after polishing the whole field restores it`() = runTest {
        field.write(FieldSnapshot("first draft"))
        editor.polish(ReverbTone.EDGY)

        editor.undo()

        assertEquals("first draft", field.current.text)
    }

    @Test
    fun `undo restores the last edit only`() = runTest {
        field.write(FieldSnapshot("one"))
        engine.rewriteResult = { RewriteResult.Rewritten(it.text + " two") }
        editor.polish(ReverbTone.FRIENDLY)
        editor.polish(ReverbTone.FRIENDLY)
        assertEquals("one two two", field.current.text)

        editor.undo()

        assertEquals("one two", field.current.text)
        assertEquals(EditResult.Failed(EditError.NothingToUndo), editor.undo())
    }

    @Test
    fun `a failed undo keeps the snapshot so the user can retry`() = runTest {
        field.write(FieldSnapshot("before"))
        editor.polish(ReverbTone.EDGY)
        field.acceptsWrites = false

        assertEquals(EditResult.Failed(EditError.WriteRejected), editor.undo())
        assertTrue(editor.canUndo.value)

        field.acceptsWrites = true
        assertEquals(EditResult.Undone, editor.undo())
        assertEquals("before", field.current.text)
    }

    // Smart Reply (regression: replies used to ignore the user's writing samples)

    @Test
    fun `smart reply uses the Smart Reply writing sample`() = runTest {
        settings.saveCustomInstruction(StyleSamples.REPLY_KEY, "love this, lets talk tmrw!")
        settings.saveCustomInstruction("PROFESSIONAL", "Dear all, kindly note")

        editor.reply(ReverbTone.PROFESSIONAL, "Can we ship on Friday?")

        val request = engine.replyRequests.single()
        assertEquals("love this, lets talk tmrw!", request.styleSample)
        assertEquals("Can we ship on Friday?", request.message)
        assertEquals(ReverbTone.PROFESSIONAL, request.intent)
    }

    @Test
    fun `smart reply falls back to the tone sample`() = runTest {
        settings.saveCustomInstruction("EDGY", "nope. not happening.")

        editor.reply(ReverbTone.EDGY, "Want to buy a timeshare?")

        assertEquals("nope. not happening.", engine.replyRequests.single().styleSample)
    }

    @Test
    fun `smart reply goes into an empty field`() = runTest {
        val result = editor.reply(ReverbTone.FRIENDLY, "Dinner tonight?")

        assertEquals(EditResult.Applied("Sounds good"), result)
        assertEquals("Sounds good", field.current.text)
    }

    @Test
    fun `smart reply replaces only the selection and undo brings the field back`() = runTest {
        val original = FieldSnapshot("Re: dinner\n[reply here]\n-- sent from my phone", 11, 23)
        field.write(original)

        editor.reply(ReverbTone.FRIENDLY, "Dinner tonight?")
        assertEquals("Re: dinner\nSounds good\n-- sent from my phone", field.current.text)

        editor.undo()
        assertEquals(original, field.current)
    }

    @Test
    fun `smart reply with an empty clipboard does not call the engine`() = runTest {
        assertEquals(EditResult.Failed(EditError.NoMessageToReplyTo), editor.reply(ReverbTone.FRIENDLY, "  "))
        assertTrue(engine.replyRequests.isEmpty())
    }

    @Test
    fun `smart reply without a focused field does not call the engine`() = runTest {
        field.focused = false

        assertEquals(EditResult.Failed(EditError.WriteRejected), editor.reply(ReverbTone.FRIENDLY, "hi"))
        assertTrue(engine.replyRequests.isEmpty())
    }
}
