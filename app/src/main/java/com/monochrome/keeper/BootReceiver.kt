package com.monochrome.keeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * BroadcastReceiver that re-schedules the MonochromeWorker
 * after device reboot so the periodic check continues.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Re-scheduling MonochromeWorker...")
            scheduleMonochromeWorker(context)
        }
    }

    companion object {
        fun scheduleMonochromeWorker(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MonochromeWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MonochromeWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d("BootReceiver", "MonochromeWorker scheduled (every 15 minutes)")
        }
    }
}
