package com.reverb.core.network

import com.google.gson.JsonParser
import com.reverb.core.domain.engine.ReplyRequest
import com.reverb.core.domain.engine.RewriteRequest
import com.reverb.core.domain.engine.RewriteResult
import com.reverb.core.domain.engine.WritingEngineException
import com.reverb.core.model.ReverbTone
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OpenAiWritingEngineTest {

    private val server = MockWebServer()
    private var apiKey: String? = "sk-test"
    private lateinit var engine: OpenAiWritingEngine

    @Before
    fun setUp() {
        server.start()
        engine = OpenAiWritingEngine(
            config = OpenAiConfig(baseUrl = server.url("/").toString(), model = "test-model"),
            apiKeyProvider = { apiKey },
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `rewrite posts the configured model, the key and the prompt`() = runTest {
        server.enqueue(completion("Polished text"))

        val result = engine.rewrite(RewriteRequest("polish me", ReverbTone.PROFESSIONAL, styleSample = "Cheers, V"))

        assertEquals(RewriteResult.Rewritten("Polished text"), result)
        val recorded = server.takeRequest()
        assertEquals("/v1/chat/completions", recorded.path)
        assertEquals("Bearer sk-test", recorded.getHeader("Authorization"))
        val body = JsonParser.parseString(recorded.body.readUtf8()).asJsonObject
        assertEquals("test-model", body["model"].asString)
        val messages = body["messages"].asJsonArray
        assertTrue(messages[0].asJsonObject["content"].asString.contains("Cheers, V"))
        assertEquals("polish me", messages[1].asJsonObject["content"].asString)
        assertFalse(body.has("response_format"))
    }

    @Test
    fun `reply sends the writing sample to the model`() = runTest {
        server.enqueue(completion("yep, lets do it"))

        val reply = engine.reply(ReplyRequest("Lunch?", ReverbTone.PROFESSIONAL, styleSample = "haha ok bet"))

        assertEquals("yep, lets do it", reply)
        val system = JsonParser.parseString(server.takeRequest().body.readUtf8())
            .asJsonObject["messages"].asJsonArray[0].asJsonObject["content"].asString
        assertTrue(system.contains("haha ok bet"))
    }

    @Test
    fun `fact check asks for JSON and understands all_good`() = runTest {
        server.enqueue(completion("""{"all_good": true, "corrected_text": "", "reason": ""}"""))

        val result = engine.rewrite(RewriteRequest("The Sun is a star.", ReverbTone.FACT_CHECK))

        assertEquals(RewriteResult.NoChangeNeeded, result)
        val body = JsonParser.parseString(server.takeRequest().body.readUtf8()).asJsonObject
        assertEquals("json_object", body["response_format"].asJsonObject["type"].asString)
    }

    @Test
    fun `http errors carry the status code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"bad key"}}"""))

        try {
            engine.rewrite(RewriteRequest("hi", ReverbTone.FRIENDLY))
            fail("expected an exception")
        } catch (e: WritingEngineException) {
            assertEquals(401, e.statusCode)
        }
    }

    @Test
    fun `empty choices are an error`() = runTest {
        server.enqueue(MockResponse().setBody("""{"choices": []}"""))

        try {
            engine.reply(ReplyRequest("hi", ReverbTone.FRIENDLY))
            fail("expected an exception")
        } catch (e: WritingEngineException) {
            assertEquals(null, e.statusCode)
        }
    }

    @Test
    fun `engine is configured only with a key`() {
        assertTrue(engine.isConfigured)
        apiKey = " "
        assertFalse(engine.isConfigured)
        apiKey = null
        assertFalse(engine.isConfigured)
    }

    private fun completion(content: String): MockResponse {
        val escaped = com.google.gson.Gson().toJson(content)
        return MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":$escaped}}]}""")
    }
}
