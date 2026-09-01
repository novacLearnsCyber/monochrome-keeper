package com.monochrome.keeper

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WorkManager Worker that runs every 15 minutes.
 * Checks if monochrome mode is enabled:
 *   - If disabled → re-enables it automatically
 *   - If already enabled → does nothing
 */
class MonochromeWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val TAG = "MonochromeWorker"
        const val WORK_NAME = "monochrome_keeper_periodic"
        const val PREFS_NAME = "monochrome_keeper_prefs"
        const val KEY_LAST_CHECK = "last_check_time"
        const val KEY_LAST_ACTION = "last_action"
        const val KEY_REACTIVATION_COUNT = "reactivation_count"
    }

    override fun doWork(): Result {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        Log.d(TAG, "[$timestamp] Running monochrome check...")

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // Save the check timestamp
        editor.putString(KEY_LAST_CHECK, timestamp)

        if (MonochromeHelper.isMonochromeEnabled(applicationContext)) {
            // Monochrome is ON — do nothing
            Log.d(TAG, "[$timestamp] Monochrome is ACTIVE. No action needed.")
            editor.putString(KEY_LAST_ACTION, "✅ Monochrome activ — nicio acțiune necesară")
        } else {
            // Monochrome is OFF — re-enable it
            val success = MonochromeHelper.enableMonochrome(applicationContext)

            if (success) {
                Log.d(TAG, "[$timestamp] Monochrome was OFF. Re-enabled successfully!")
                editor.putString(KEY_LAST_ACTION, "🔄 Monochrome reactivat cu succes!")
                // Increment reactivation counter
                val count = prefs.getInt(KEY_REACTIVATION_COUNT, 0) + 1
                editor.putInt(KEY_REACTIVATION_COUNT, count)
            } else {
                Log.e(TAG, "[$timestamp] FAILED to re-enable monochrome! Check WRITE_SECURE_SETTINGS permission.")
                editor.putString(KEY_LAST_ACTION, "❌ Eroare: Nu s-a putut reactiva. Verifică permisiunea ADB.")
            }
        }

        editor.apply()
        return Result.success()
    }
}
