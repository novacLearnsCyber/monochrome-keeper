package com.monochrome.keeper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that re-starts the MonochromeService
 * after device reboot so the periodic check continues.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted. Checking if service should restart...")

            val prefs = context.getSharedPreferences(
                MonochromeService.PREFS_NAME, Context.MODE_PRIVATE
            )

            // Only restart if the service was running before reboot
            val wasRunning = prefs.getBoolean(MonochromeService.KEY_SERVICE_RUNNING, true)

            if (wasRunning) {
                Log.d("BootReceiver", "Re-starting MonochromeService...")
                MonochromeService.start(context)
            } else {
                Log.d("BootReceiver", "Service was stopped. Not restarting.")
            }
        }
    }
}
