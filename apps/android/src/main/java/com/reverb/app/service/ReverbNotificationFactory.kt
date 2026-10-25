package com.reverb.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Builds the persistent notification required by the foreground service, including the
 * Pause / Resume action that toggles text capture on and off.
 */
object ReverbNotificationFactory {
    const val CHANNEL_ID = "reverb_background_service"
    const val NOTIFICATION_ID = 7

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(com.reverb.app.R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps Reverb available to polish your text via the bubble overlay."
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, isIncognito: Boolean = false): Notification {
        // Incognito toggle action
        val incognitoIntent = Intent(context, ReverbForegroundService::class.java).apply {
            action = ReverbForegroundService.ACTION_TOGGLE_INCOGNITO
        }
        val incognitoPi = PendingIntent.getService(
            context,
            0,
            incognitoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Show Bubble action — tapping this re-shows the floating bubble
        val showBubbleIntent = Intent(context, ReverbForegroundService::class.java).apply {
            action = ReverbForegroundService.ACTION_SHOW_BUBBLE
        }
        val showBubblePi = PendingIntent.getService(
            context,
            1,
            showBubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val incognitoLabel = if (isIncognito) "Resume" else "Pause"
        val contentText = if (isIncognito) {
            "Paused — tap Resume to bring Reverb back."
        } else {
            context.getString(com.reverb.app.R.string.notification_text)
        }

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.reverb.app.R.drawable.ic_reverb_logo)
            .setContentTitle(context.getString(com.reverb.app.R.string.notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_add, "Show Bubble", showBubblePi)
            .addAction(android.R.drawable.ic_media_pause, incognitoLabel, incognitoPi)
            .build()
    }
}
