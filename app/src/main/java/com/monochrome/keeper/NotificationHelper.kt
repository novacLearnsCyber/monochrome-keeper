package com.monochrome.keeper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Handles all notification logic:
 * - Persistent notification for the foreground service
 * - Alert notification when monochrome is re-activated
 */
object NotificationHelper {

    const val SERVICE_CHANNEL_ID = "monochrome_service_channel"
    const val ALERT_CHANNEL_ID = "monochrome_alert_channel"
    const val SERVICE_NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    /**
     * Create both notification channels (required for Android 8.0+).
     */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Service channel — low importance, silent, persistent
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Monochrome Keeper Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificare persistentă — serviciul rulează în background"
            setShowBadge(false)
        }

        // Alert channel — high importance, with sound
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "Alerte Monochrome",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificări când modul monochrome este reactivat"
            enableVibration(true)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }

    /**
     * Build the persistent notification for the foreground service.
     */
    fun buildServiceNotification(context: Context, intervalMinutes: Int): android.app.Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("Monochrome Keeper")
            .setContentText("Verificare la fiecare $intervalMinutes minute")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Send an alert notification when monochrome has been re-activated.
     */
    fun sendReactivatedNotification(context: Context) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("🔄 Monochrome Reactivat!")
            .setContentText("Modul monochrome a fost dezactivat și a fost reactivat automat.")
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }
}
