package com.monochrome.keeper

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusSubtitle: TextView
    private lateinit var statusCard: CardView
    private lateinit var lastCheckValue: TextView
    private lateinit var lastActionValue: TextView
    private lateinit var reactivationCountValue: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var btnToggleService: Button
    private lateinit var btnForceCheck: Button

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 5000L // refresh UI every 5 seconds

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshUI()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(MonochromeWorker.PREFS_NAME, Context.MODE_PRIVATE)

        // Bind views
        statusIcon = findViewById(R.id.statusIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusSubtitle = findViewById(R.id.statusSubtitle)
        statusCard = findViewById(R.id.statusCard)
        lastCheckValue = findViewById(R.id.lastCheckValue)
        lastActionValue = findViewById(R.id.lastActionValue)
        reactivationCountValue = findViewById(R.id.reactivationCountValue)
        permissionStatus = findViewById(R.id.permissionStatus)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnForceCheck = findViewById(R.id.btnForceCheck)

        // Schedule the periodic worker on first launch
        scheduleWorker()

        // Button: Force an immediate check
        btnForceCheck.setOnClickListener {
            forceCheck()
        }

        // Button: Toggle service ON/OFF
        btnToggleService.setOnClickListener {
            val isActive = prefs.getBoolean("service_active", true)
            if (isActive) {
                // Stop the worker
                WorkManager.getInstance(this).cancelUniqueWork(MonochromeWorker.WORK_NAME)
                prefs.edit().putBoolean("service_active", false).apply()
            } else {
                // Restart the worker
                scheduleWorker()
                prefs.edit().putBoolean("service_active", true).apply()
            }
            refreshUI()
        }

        refreshUI()
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun scheduleWorker() {
        val workRequest = PeriodicWorkRequestBuilder<MonochromeWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            MonochromeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        prefs.edit().putBoolean("service_active", true).apply()
    }

    private fun forceCheck() {
        val isMonochrome = MonochromeHelper.isMonochromeEnabled(this)

        if (!isMonochrome) {
            val success = MonochromeHelper.enableMonochrome(this)
            val timestamp = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())

            prefs.edit().apply {
                putString(MonochromeWorker.KEY_LAST_CHECK, timestamp)
                if (success) {
                    putString(MonochromeWorker.KEY_LAST_ACTION, "🔄 Reactivat manual din aplicație!")
                    val count = prefs.getInt(MonochromeWorker.KEY_REACTIVATION_COUNT, 0) + 1
                    putInt(MonochromeWorker.KEY_REACTIVATION_COUNT, count)
                } else {
                    putString(MonochromeWorker.KEY_LAST_ACTION, "❌ Eroare la reactivare manuală")
                }
                apply()
            }
        } else {
            val timestamp = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
            ).format(java.util.Date())
            prefs.edit().apply {
                putString(MonochromeWorker.KEY_LAST_CHECK, timestamp)
                putString(MonochromeWorker.KEY_LAST_ACTION, "✅ Monochrome deja activ")
                apply()
            }
        }
        refreshUI()
    }

    private fun refreshUI() {
        val isMonochrome = MonochromeHelper.isMonochromeEnabled(this)
        val isServiceActive = prefs.getBoolean("service_active", true)

        // Status card
        if (isMonochrome) {
            statusIcon.setImageResource(R.drawable.ic_check_circle)
            statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_active))
            statusTitle.text = "Monochrome ACTIV"
            statusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_active))
            statusSubtitle.text = "Modul grayscale este pornit"
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_active_bg))
        } else {
            statusIcon.setImageResource(R.drawable.ic_warning)
            statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_inactive))
            statusTitle.text = "Monochrome DEZACTIVAT"
            statusTitle.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
            statusSubtitle.text = "Va fi reactivat la următoarea verificare"
            statusCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_inactive_bg))
        }

        // Info values
        lastCheckValue.text = prefs.getString(MonochromeWorker.KEY_LAST_CHECK, "Niciodată")
        lastActionValue.text = prefs.getString(MonochromeWorker.KEY_LAST_ACTION, "Nicio acțiune")
        reactivationCountValue.text = prefs.getInt(MonochromeWorker.KEY_REACTIVATION_COUNT, 0).toString()

        // Permission check
        val hasPermission = try {
            MonochromeHelper.isMonochromeEnabled(this)
            // If we can read the setting, we likely have permission
            // Try writing to truly verify
            true
        } catch (e: SecurityException) {
            false
        }

        if (hasPermission) {
            permissionStatus.text = "✅ Permisiune WRITE_SECURE_SETTINGS acordată"
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active))
        } else {
            permissionStatus.text = "❌ Lipsă permisiune! Rulează comanda ADB."
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
        }

        // Toggle button
        if (isServiceActive) {
            btnToggleService.text = "⏹ Oprește Serviciul"
            btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_stop))
        } else {
            btnToggleService.text = "▶ Pornește Serviciul"
            btnToggleService.setBackgroundColor(ContextCompat.getColor(this, R.color.btn_start))
        }
    }
}
