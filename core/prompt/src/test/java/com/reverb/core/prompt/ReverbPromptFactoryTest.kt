package com.reverb.core.prompt

import com.reverb.core.model.ReverbTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReverbPromptFactoryTest {

    private val factory = ReverbPromptFactory()

    @Test
    fun `rewrite prompt includes the writing sample`() {
        val prompt = factory.buildRewritePrompt("hey", ReverbTone.FRIENDLY, customInstruction = "yo whats up")
        assertTrue(prompt.systemPrompt.contains("yo whats up"))
        assertEquals("hey", prompt.userPrompt)
    }

    @Test
    fun `reply prompt includes the writing sample`() {
        val prompt = factory.buildReplyPrompt("Can you join?", ReverbTone.PROFESSIONAL, customInstruction = "sure thing!")
        assertTrue(prompt.systemPrompt.contains("sure thing!"))
        assertEquals("Can you join?", prompt.userPrompt)
    }

    @Test
    fun `blank sample adds no style line`() {
        val prompt = factory.buildReplyPrompt("Can you join?", ReverbTone.EDGY)
        assertFalse(prompt.systemPrompt.contains("natural writing style"))
    }

    @Test
    fun `fact check skips the length and style rules`() {
        val prompt = factory.buildRewritePrompt("x", ReverbTone.FACT_CHECK)
        assertFalse(prompt.systemPrompt.contains(RewriteLength.SIMILAR.instruction))
    }

    @Test
    fun `promotion allows formatting`() {
        val prompt = factory.buildRewritePrompt("x", ReverbTone.PROMOTION)
        assertFalse(prompt.systemPrompt.contains("Never use markdown formatting"))
    }
}
