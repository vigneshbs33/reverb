package com.reverb.core.network

import com.reverb.core.domain.engine.RewriteResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FactCheckParserTest {

    @Test
    fun `all good means no change`() {
        assertEquals(RewriteResult.NoChangeNeeded, FactCheckParser.parse("""{"all_good": true}"""))
    }

    @Test
    fun `correction and reason are read`() {
        val result = FactCheckParser.parse(
            """{"all_good": false, "corrected_text": "The Sun is a star.", "reason": "The Sun is a star, not a planet."}""",
        )
        assertEquals(RewriteResult.Rewritten("The Sun is a star.", "The Sun is a star, not a planet."), result)
    }

    @Test
    fun `blank reason becomes null`() {
        val result = FactCheckParser.parse("""{"all_good": false, "corrected_text": "Fixed.", "reason": ""}""")
        assertEquals(RewriteResult.Rewritten("Fixed.", null), result)
    }

    @Test
    fun `plain text is used as is`() {
        assertEquals(RewriteResult.Rewritten("Just text"), FactCheckParser.parse("Just text"))
    }
}
