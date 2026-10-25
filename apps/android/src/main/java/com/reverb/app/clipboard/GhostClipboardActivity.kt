package com.reverb.app.clipboard

import android.app.Activity
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.reverb.app.service.ReverbForegroundService

/**
 * An invisible Activity that briefly gains window focus so Android grants
 * clipboard read access (Android 10+ restriction).
 *
 * IMPORTANT: The clipboard MUST be read in onWindowFocusChanged(hasFocus=true),
 * NOT in onCreate. Reading it in onCreate happens before the window has focus,
 * causing Android to return an empty or null clipboard on API 29+.
 */
class GhostClipboardActivity : Activity() {

    private var toneName: String = ""
    private var dispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toneName = intent.getStringExtra(ReverbForegroundService.EXTRA_TONE_NAME).orEmpty()
        // Do NOT read clipboard here. Wait for window focus.
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || dispatched) return
        dispatched = true // prevent double-dispatch on focus flickers

        val clip = try {
            getSystemService(ClipboardManager::class.java).primaryClip
        } catch (e: Exception) {
            Log.e("ReverbGhost", "Failed to read clipboard", e)
            null
        }

        val contextText = if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(this)?.toString().orEmpty()
        } else {
            ""
        }

        Log.d("ReverbGhost", "Clipboard read on focus: len=${contextText.length}, tone=$toneName")

        val serviceIntent = Intent(this, ReverbForegroundService::class.java).apply {
            action = ReverbForegroundService.ACTION_SMART_REPLY_FROM_GHOST
            putExtra(ReverbForegroundService.EXTRA_CLIPBOARD_TEXT, contextText)
            putExtra(ReverbForegroundService.EXTRA_TONE_NAME, toneName)
        }
        startService(serviceIntent)

        finish()
        overridePendingTransition(0, 0)
    }
}
