package com.reverb.core.network

import com.reverb.core.domain.engine.RewriteResult
import com.google.gson.JsonParser

/**
 * Reads the JSON answer of the Fact Check tone:
 * `{"all_good": bool, "corrected_text": "...", "reason": "..."}`.
 * Anything that is not that JSON is treated as plain corrected text.
 */
internal object FactCheckParser {

    fun parse(content: String): RewriteResult = try {
        val json = JsonParser.parseString(content).asJsonObject
        val allGood = json.get("all_good")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
        if (allGood) {
            RewriteResult.NoChangeNeeded
        } else {
            val corrected = json.stringOrBlank("corrected_text")
            val reason = json.stringOrBlank("reason")
            RewriteResult.Rewritten(
                text = corrected.ifBlank { content },
                reason = reason.ifBlank { null },
            )
        }
    } catch (e: RuntimeException) {
        RewriteResult.Rewritten(content)
    }

    private fun com.google.gson.JsonObject.stringOrBlank(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
}
