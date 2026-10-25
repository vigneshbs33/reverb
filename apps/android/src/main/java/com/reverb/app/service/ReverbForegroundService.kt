package com.reverb.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.reverb.app.ReverbApplication
import com.reverb.app.clipboard.GhostClipboardActivity
import com.reverb.app.overlay.ReverbBubbleController
import com.reverb.core.domain.EditError
import com.reverb.core.domain.EditResult
import com.reverb.core.domain.ReverbEditor
import com.reverb.core.model.ReverbTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground service that owns the floating bubble and runs every Reverb action.
 *
 * The service is a thin Android shell: the work itself lives in [ReverbEditor] (from
 * `:core:domain`), which it gets from the app's [com.reverb.app.di.AppContainer].
 *
 * - Polish: rewrite the selection, or the whole field, in the chosen tone.
 * - Smart Reply: read the clipboard through [GhostClipboardActivity], write a reply in the
 *   user's voice and insert it into the focused field.
 * - Undo: restore the whole field as it was before the last Polish or Smart Reply.
 * - Pause: [ACTION_TOGGLE_INCOGNITO] hides the bubble and blocks all text capture.
 */
class ReverbForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var bubbleController: ReverbBubbleController
    private lateinit var editor: ReverbEditor

    override fun onCreate() {
        super.onCreate()

        ReverbNotificationFactory.ensureChannel(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                ReverbNotificationFactory.NOTIFICATION_ID,
                ReverbNotificationFactory.build(this),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                ReverbNotificationFactory.NOTIFICATION_ID,
                ReverbNotificationFactory.build(this),
            )
        }

        editor = (application as ReverbApplication).container.editor
        bubbleController = ReverbBubbleController(
            context = this,
            onToneSelected = { tone -> handlePolish(tone) },
            onReplySelected = { tone -> handleSmartReplyFromClipboard(tone) },
            onUndoSelected = { handleUndo() },
        )
        scope.launch {
            editor.canUndo.collect { bubbleController.setCanUndo(it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        // Handle explicit actions — do NOT show the bubble for these.
        when (intent?.action) {
            ACTION_TOGGLE_INCOGNITO -> {
                isIncognito = !isIncognito
                val notif = ReverbNotificationFactory.build(this, isIncognito)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    startForeground(
                        ReverbNotificationFactory.NOTIFICATION_ID,
                        notif,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                    )
                } else {
                    startForeground(ReverbNotificationFactory.NOTIFICATION_ID, notif)
                }
                if (isIncognito) bubbleController.hide() else bubbleController.show()
                return START_STICKY
            }

            ACTION_SHOW_BUBBLE -> {
                // Force-show the bubble. Hides first in case a stale view is attached.
                isIncognito = false
                bubbleController.hide()
                bubbleController.show()
                return START_STICKY
            }

            ACTION_SMART_REPLY_FROM_GHOST -> {
                val text = intent.getStringExtra(EXTRA_CLIPBOARD_TEXT).orEmpty()
                val toneName = intent.getStringExtra(EXTRA_TONE_NAME).orEmpty()
                val tone = runCatching { ReverbTone.valueOf(toneName) }.getOrDefault(ReverbTone.PROFESSIONAL)
                handleSmartReply(tone, text)
                return START_STICKY
            }
        }

        // Default: show the bubble (first launch or START_STICKY restart).
        if (!isIncognito) {
            val shown = bubbleController.show()
            if (!shown) {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        bubbleController.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Each bubble action runs on the editor off the main thread, then the result is shown.

    private fun handlePolish(tone: ReverbTone) {
        Log.d(TAG, "handlePolish: tone=$tone")
        runAction(BubbleAction.POLISH) { editor.polish(tone) }
    }

    /** Reads the clipboard through [GhostClipboardActivity], which is allowed to on Android 10+. */
    private fun handleSmartReplyFromClipboard(tone: ReverbTone) {
        val intent = Intent(this, GhostClipboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            putExtra(EXTRA_TONE_NAME, tone.name)
        }
        startActivity(intent)
    }

    private fun handleSmartReply(tone: ReverbTone, clipboardText: String) {
        Log.d(TAG, "handleSmartReply: tone=$tone textLen=${clipboardText.length}")
        runAction(BubbleAction.SMART_REPLY) { editor.reply(tone, clipboardText) }
    }

    private fun handleUndo() {
        Log.d(TAG, "handleUndo")
        runAction(BubbleAction.UNDO) { editor.undo() }
    }

    private fun runAction(action: BubbleAction, block: suspend () -> EditResult) {
        scope.launch {
            val result = withContext(Dispatchers.IO) { block() }
            showResult(action, result)
        }
    }

    private fun showResult(action: BubbleAction, result: EditResult) {
        when (result) {
            is EditResult.Applied -> bubbleController.setDone(result.preview, result.reason)
            EditResult.NothingToChange -> bubbleController.setDone(EditMessages.ALL_GOOD, null)
            EditResult.Undone -> bubbleController.resetToIdle()
            is EditResult.Failed -> {
                (result.error as? EditError.EngineFailed)?.let { Log.e(TAG, "$action failed", it.cause) }
                EditMessages.forError(action, result.error)?.let(bubbleController::setError)
            }
        }
    }

    companion object {
        const val TAG = "ReverbForegroundService"
        const val ACTION_TOGGLE_INCOGNITO = "TOGGLE_INCOGNITO"
        const val ACTION_SHOW_BUBBLE = "SHOW_BUBBLE"
        const val ACTION_SMART_REPLY_FROM_GHOST = "PROCESS_SMART_REPLY_FROM_GHOST"
        const val EXTRA_CLIPBOARD_TEXT = "CLIPBOARD_TEXT"
        const val EXTRA_TONE_NAME = "TONE_NAME"

        @Volatile
        var isIncognito: Boolean = false
    }
}
