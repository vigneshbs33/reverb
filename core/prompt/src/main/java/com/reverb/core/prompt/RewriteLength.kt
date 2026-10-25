package com.reverb.core.prompt

enum class RewriteLength(
    val instruction: String,
) {
    SIMILAR("Keep the rewritten text close to the original length."),
    SHORTER("Make the rewritten text shorter than the original while preserving intent."),
    LONGER("Expand the rewritten text slightly while preserving intent."),
}
