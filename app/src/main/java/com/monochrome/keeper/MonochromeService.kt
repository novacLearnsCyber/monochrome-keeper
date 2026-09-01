package com.monochrome.keeper

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service that reliably checks monochrome status
 * at user-configured intervals. Much more reliable than WorkManager
 * because foreground services are not killed by battery optimization.
 */
class MonochromeService : Service() {

    companion object {
        const val TAG = "MonochromeService"
        const val PREFS_NAME = "monochrome_keeper_prefs"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
        const val KEY_LAST_CHECK = "last_check_time"
        const val KEY_LAST_ACTION = "last_action"
        const val KEY_REACTIVATION_COUNT = "reactivation_count"
        const val KEY_SERVICE_RUNNING = "service_running"
        const val DEFAULT_INTERVAL = 15
        const val ACTION_STOP = "com.monochrome.keeper.STOP"

        fun start(context: Context) {
            val intent = Intent(context, MonochromeService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonochromeService::class.java)
            context.stopService(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null

    private val checkRunnable = object : Runnable {
        override fun run() {
            performCheck()
            val intervalMs = getIntervalMs()
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val interval = prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL)

        // Start as foreground with persistent notification
        val notification = NotificationHelper.buildServiceNotification(this, interval)
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)

        // Mark service as running
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, true).apply()

        // Acquire partial wake lock to prevent deep sleep from stopping checks
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MonochromeKeeper::ServiceLock"
        ).apply {
            acquire()
        }

        // Start periodic checks
        handler.removeCallbacks(checkRunnable)
        // Run first check immediately
        handler.post(checkRunnable)

        Log.d(TAG, "Service started with interval: ${interval}min")

        return START_STICKY // Restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)

        wakeLock?.let {
            if (it.isHeld) it.release()
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SERVICE_RUNNING, false).apply()

        Log.d(TAG, "Service destroyed")
    }

    private fun getIntervalMs(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val minutes = prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL)
        return minutes * 60 * 1000L
    }

    private fun performCheck() {
        val timestamp = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
            .format(Date())

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString(KEY_LAST_CHECK, timestamp)

        if (MonochromeHelper.isMonochromeEnabled(this)) {
            // Already active — do nothing, no notification
            Log.d(TAG, "[$timestamp] Monochrome ACTIV — nicio acțiune")
            editor.putString(KEY_LAST_ACTION, "✅ Activ — nicio acțiune ($timestamp)")
        } else {
            // Disabled — re-enable it
            val success = MonochromeHelper.enableMonochrome(this)

            if (success) {
                Log.d(TAG, "[$timestamp] Monochrome REACTIVAT!")
                editor.putString(KEY_LAST_ACTION, "🔄 Reactivat automat ($timestamp)")
                val count = prefs.getInt(KEY_REACTIVATION_COUNT, 0) + 1
                editor.putInt(KEY_REACTIVATION_COUNT, count)

                // Send notification ONLY when re-activated
                NotificationHelper.sendReactivatedNotification(this)
            } else {
                Log.e(TAG, "[$timestamp] EROARE la reactivare!")
                editor.putString(KEY_LAST_ACTION, "❌ Eroare reactivare ($timestamp)")
            }
        }

        editor.apply()

        // Update the persistent notification with current status
        val interval = prefs.getInt(KEY_INTERVAL_MINUTES, DEFAULT_INTERVAL)
        val notification = NotificationHelper.buildServiceNotification(this, interval)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
    }
}
