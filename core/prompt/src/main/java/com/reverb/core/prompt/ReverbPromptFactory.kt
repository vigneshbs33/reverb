package com.reverb.core.prompt

import com.reverb.core.model.ReverbTone

/**
 * Builds the system and user prompts for rewrites and replies from the tone, the
 * requested length, an optional app context and the user's own writing sample for that tone.
 */
class ReverbPromptFactory {

    fun buildRewritePrompt(
        input: String,
        tone: ReverbTone,
        appContext: String? = null,
        length: RewriteLength = RewriteLength.SIMILAR,
        customInstruction: String = "",
    ): PromptBundle {
        val contextLine = appContext
            ?.takeIf { it.isNotBlank() }
            ?.let { "Context: The user is currently typing in $it." }
            .orEmpty()

        return PromptBundle(
            systemPrompt = buildString {
                appendLine("You are Reverb, a smart writing assistant that makes people's writing look effortless.")
                appendLine(toneInstruction(tone))
                if (tone != ReverbTone.FACT_CHECK) {
                    if (tone == ReverbTone.PROMOTION) {
                        appendLine(
                            globalConstraints.replace(
                                "Never use markdown formatting (no **, no #, no bullet points unless the original had them).",
                                "",
                            ),
                        )
                    } else {
                        appendLine(globalConstraints)
                    }
                    appendLine(length.instruction)
                }
                if (contextLine.isNotBlank()) appendLine(contextLine)
                if (customInstruction.isNotBlank()) {
                    appendLine(
                        "Here is an example of the user's natural writing style in this tone: '$customInstruction'. Analyze their vocabulary, pacing, and sentence structure, and strictly match this vibe.",
                    )
                }
            }.trim(),
            userPrompt = input.trim(),
        )
    }

    fun buildReplyPrompt(
        contextText: String,
        replyIntent: ReverbTone,
        customInstruction: String = "",
    ): PromptBundle {
        val intentInstruction = when (replyIntent) {
            ReverbTone.PROFESSIONAL ->
                "Write a positive, agreeable response to the following message. Sound like a real person, not a bot."
            ReverbTone.EDGY ->
                "Write a polite but firm declining response to the following message. Keep it real and direct."
            ReverbTone.FRIENDLY ->
                "Write a curious, warm response asking for more detail about the following message."
            ReverbTone.FACT_CHECK ->
                "Write a concise, factually accurate response to the following message."
            ReverbTone.PROMOTION ->
                "Write an engaging, positive marketing-focused response to the following message."
            ReverbTone.CLEAN_SPEECH ->
                "Write a clear, concise response to the following message. Sound natural."
        }
        val customLine = if (customInstruction.isNotBlank()) {
            "\nHere is an example of the user's natural writing style: '$customInstruction'. " +
                "Analyze their vocabulary, pacing, and sentence structure, and strictly match this vibe."
        } else {
            ""
        }
        return PromptBundle(
            systemPrompt = """
                You are Reverb, a personal writing assistant.
                $intentInstruction
                Keep the reply to 1-3 sentences max. Sound natural and human, like a real person texting back.
                Never use em dashes (—). Never use markdown. No "Here is your reply:" intros. Just the reply text.
                Output ONLY the reply text. Nothing else.$customLine
            """.trimIndent(),
            userPrompt = contextText.trim(),
        )
    }

    private fun toneInstruction(tone: ReverbTone): String = when (tone) {
        ReverbTone.PROFESSIONAL ->
            """Rewrite the input to sound polished, clear, and professional.
               It should sound like a confident, intelligent human wrote it — not a corporate robot.
               Remove slang. Fix grammar. Keep it tight and purposeful."""

        ReverbTone.FRIENDLY ->
            """Rewrite the input to sound warm, genuine, and conversational.
               It should sound like a real friend texting — approachable, easy to read, never stiff.
               Use natural contractions (I'm, we'll, it's). Avoid sounding overly eager or fake-cheerful."""

        ReverbTone.EDGY ->
            """Rewrite the input to sound bold, punchy, and full of personality.
               High energy. Modern. Direct. The kind of message that makes people stop scrolling.
               Don't be edgy for shock value — be compelling and real."""

        ReverbTone.FACT_CHECK ->
            """Ensure all facts are 100% accurate. Correct any spelling or grammar mistakes.
               DO NOT change the user's personal tone or style. If they made a factual error (e.g. 'Sun is a planet'), correct it (e.g. 'Sun is a star').
               If the text is already 100% factually correct and has no spelling or grammar errors, output: {"all_good": true, "corrected_text": "", "reason": ""}.
               Otherwise, output in valid JSON with exactly three keys:
               "all_good": false,
               "corrected_text": the exact corrected text,
               "reason": a short concise explanation of what you fixed and why."""

        ReverbTone.CLEAN_SPEECH ->
            """The input is a voice transcript from dictation. Rewrite it as clean written text.
               Remove filler words (um, uh, like, you know), false starts and accidental repeats.
               Add correct punctuation, capitalization and paragraph breaks.
               Keep every idea and fact the speaker gave, in their own voice. Do not add anything new."""

        ReverbTone.PROMOTION ->
            """Transform the following text into a clear, engaging marketing message.
               Fix grammar and spelling. Make it sound professional but friendly.
               Keep it simple and easy to understand.
               Add attractive formatting (short lines, spacing) and use bullet points when appropriate.
               Use relevant emojis (not too many).
               Highlight key offers (like discounts, bonuses, free items).
               Make it motivating and action-oriented.
               Output format: Title line (optional but catchy), short paragraphs or bullet points."""
    }

    private companion object {
        private val globalConstraints = """
            Always preserve the user's intent and key facts exactly.
            Fix spelling, grammar, and punctuation errors naturally — don't over-correct.
            Sound like a real, intelligent human wrote this — not an AI.
            Never add introductions like "Here is your rewrite." or "Sure! Here's..." or any sign-offs.
            Never use em dashes (—). Use commas, periods, or rephrase instead.
            Never use markdown formatting (no **, no #, no bullet points unless the original had them).
            Do not add extra sentences or ideas that weren't in the original.
            Output ONLY the rewritten text. Nothing else at all.
        """.trimIndent()
    }
}
