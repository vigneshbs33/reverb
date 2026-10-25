package com.reverb.core.model

/**
 * The writing tones offered in the bubble. [title] is the label shown in the UI and
 * [summary] is a one line description of the tone.
 */
enum class ReverbTone(
    val title: String,
    val summary: String,
) {
    PROFESSIONAL(
        title = "Professional",
        summary = "Polished, concise, formal, and respectful.",
    ),
    FRIENDLY(
        title = "Friendly",
        summary = "Warm, approachable, kind, and conversational.",
    ),
    EDGY(
        title = "Edgy",
        summary = "Bold, punchy, creative, and energetic.",
    ),
    FACT_CHECK(
        title = "Fact Check",
        summary = "Ensures facts, spelling, and grammar are 100% correct.",
    ),
    PROMOTION(
        title = "Promotion",
        summary = "Clear, engaging marketing message.",
    ),
}
