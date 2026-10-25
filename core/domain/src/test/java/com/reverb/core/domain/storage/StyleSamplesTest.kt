package com.reverb.core.domain.storage

import com.reverb.core.model.ReverbTone
import org.junit.Assert.assertEquals
import org.junit.Test

class StyleSamplesTest {

    @Test
    fun `rewrite uses the sample of its tone`() {
        val samples = StyleSamples(InMemorySettingsStorage(mapOf("FRIENDLY" to "hey!! running late lol")))
        assertEquals("hey!! running late lol", samples.forRewrite(ReverbTone.FRIENDLY))
        assertEquals("", samples.forRewrite(ReverbTone.PROFESSIONAL))
    }

    @Test
    fun `reply prefers the Smart Reply sample`() {
        val storage = InMemorySettingsStorage(
            mapOf(StyleSamples.REPLY_KEY to "love it, lets chat tmrw", "PROFESSIONAL" to "Dear team"),
        )
        assertEquals("love it, lets chat tmrw", StyleSamples(storage).forReply(ReverbTone.PROFESSIONAL))
    }

    @Test
    fun `reply falls back to the tone sample when the Smart Reply sample is blank`() {
        val storage = InMemorySettingsStorage(mapOf(StyleSamples.REPLY_KEY to "  ", "EDGY" to "nah, not for me"))
        assertEquals("nah, not for me", StyleSamples(storage).forReply(ReverbTone.EDGY))
    }
}
