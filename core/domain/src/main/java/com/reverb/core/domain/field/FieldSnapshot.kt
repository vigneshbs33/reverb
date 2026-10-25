package com.reverb.core.domain.field

/**
 * An immutable copy of a text field: its full [text] and its selection.
 *
 * Positions follow Android's convention: `-1` means "no selection". A selection may be
 * reversed (start after end) when the user dragged backwards; [selectionRange] normalises it.
 */
data class FieldSnapshot(
    val text: String,
    val selectionStart: Int = NO_SELECTION,
    val selectionEnd: Int = NO_SELECTION,
) {
    /** The selected range in [text], or null when nothing (or nothing valid) is selected. */
    val selectionRange: IntRange?
        get() {
            val start = minOf(selectionStart, selectionEnd)
            val end = maxOf(selectionStart, selectionEnd)
            val valid = start >= 0 && end <= text.length && start != end
            return if (valid) start until end else null
        }

    val hasSelection: Boolean get() = selectionRange != null

    /** The text Reverb works on: the selection when there is one, otherwise the whole field. */
    val target: String
        get() = selectionRange?.let { text.substring(it.first, it.last + 1) } ?: text

    /**
     * Returns the field after replacing [target] with [replacement]. Text outside the
     * selection is kept as is. The result carries no selection, matching what the
     * platform does after a full `SET_TEXT`.
     */
    fun replaceTarget(replacement: String): FieldSnapshot {
        val range = selectionRange ?: return FieldSnapshot(replacement)
        val updated = text.substring(0, range.first) + replacement + text.substring(range.last + 1)
        return FieldSnapshot(updated)
    }

    companion object {
        const val NO_SELECTION = -1
    }
}
