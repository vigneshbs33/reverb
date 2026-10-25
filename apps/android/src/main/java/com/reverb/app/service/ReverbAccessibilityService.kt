package com.reverb.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.reverb.core.domain.field.FieldSnapshot

/**
 * Accessibility Service that gives Reverb read and write access to the focused text field.
 *
 * It does no work on accessibility events. Text is only read when the user taps a tone
 * in the bubble, through [readFocusedField], and written back through [writeFocusedField]
 * using `ACTION_SET_TEXT`. Password fields and non editable nodes are ignored, and nothing is
 * read while Reverb is paused from its notification.
 *
 * The running instance is exposed through [instance] as a weak reference so
 * [com.reverb.app.field.AccessibilityTextFieldGateway] can reach it.
 */
class ReverbAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _instance = java.lang.ref.WeakReference(this)
        Log.i(TAG, "Reverb Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Capture is done on-demand, not on every event, to save battery.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (_instance?.get() === this) _instance?.clear()
        super.onDestroy()
    }

    /**
     * Returns the focused editable field with its selection, or null when no suitable
     * field is focused (or Reverb is paused).
     */
    fun readFocusedField(): FieldSnapshot? {
        val node = focusedEditableNode() ?: return null
        return FieldSnapshot(
            text = node.text?.toString().orEmpty(),
            selectionStart = node.textSelectionStart,
            selectionEnd = node.textSelectionEnd,
        )
    }

    /**
     * Replaces the focused field's whole text with [snapshot] and restores its selection
     * when it has one. Returns true when the app accepted the new text.
     */
    fun writeFocusedField(snapshot: FieldSnapshot): Boolean {
        val node = focusedEditableNode() ?: return false
        val textArgs = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, snapshot.text)
        }
        val written = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textArgs)
        val range = snapshot.selectionRange
        if (written && range != null) {
            val selectionArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, range.first)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, range.last + 1)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selectionArgs)
        }
        return written
    }

    private fun focusedEditableNode(): AccessibilityNodeInfo? {
        if (ReverbForegroundService.isIncognito) return null
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (!focused.isEditable || focused.isPassword) return null
        return focused
    }

    companion object {
        private const val TAG = "ReverbAccessibility"

        @Volatile
        private var _instance: java.lang.ref.WeakReference<ReverbAccessibilityService>? = null
        val instance: ReverbAccessibilityService? get() = _instance?.get()
    }
}
