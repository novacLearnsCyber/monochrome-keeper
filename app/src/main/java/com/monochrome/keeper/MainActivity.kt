package com.monochrome.keeper

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusIcon: ImageView
    private lateinit var statusTitle: TextView
    private lateinit var statusSubtitle: TextView
    private lateinit var statusCard: CardView
    private lateinit var lastCheckValue: TextView
    private lateinit var lastActionValue: TextView
    private lateinit var reactivationCountValue: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var intervalValue: TextView
    private lateinit var intervalSeekBar: SeekBar
    private lateinit var btnToggleService: Button
    private lateinit var btnForceCheck: Button
    private lateinit var btnBatteryOptimization: Button
    private lateinit var serviceStatusText: TextView

    private lateinit var prefs: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 2000L

    // Available intervals in minutes
    private val intervals = intArrayOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)

    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshUI()
            handler.postDelayed(this, refreshInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(MonochromeService.PREFS_NAME, Context.MODE_PRIVATE)

        // Create notification channels
        NotificationHelper.createChannels(this)

        // Request notification permission on Android 13+
        requestNotificationPermission()

        // Bind views
        statusIcon = findViewById(R.id.statusIcon)
        statusTitle = findViewById(R.id.statusTitle)
        statusSubtitle = findViewById(R.id.statusSubtitle)
        statusCard = findViewById(R.id.statusCard)
        lastCheckValue = findViewById(R.id.lastCheckValue)
        lastActionValue = findViewById(R.id.lastActionValue)
        reactivationCountValue = findViewById(R.id.reactivationCountValue)
        permissionStatus = findViewById(R.id.permissionStatus)
        intervalValue = findViewById(R.id.intervalValue)
        intervalSeekBar = findViewById(R.id.intervalSeekBar)
        btnToggleService = findViewById(R.id.btnToggleService)
        btnForceCheck = findViewById(R.id.btnForceCheck)
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization)
        serviceStatusText = findViewById(R.id.serviceStatusText)

        setupIntervalSeekBar()
        setupButtons()

        // Auto-start service on first launch
        if (!prefs.contains(MonochromeService.KEY_SERVICE_RUNNING)) {
            startMonochromeService()
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun setupIntervalSeekBar() {
        intervalSeekBar.max = intervals.size - 1

        // Restore saved interval
        val savedInterval = prefs.getInt(MonochromeService.KEY_INTERVAL_MINUTES, MonochromeService.DEFAULT_INTERVAL)
        val savedIndex = intervals.indexOf(savedInterval).let { if (it < 0) 5 else it } // default to 15min
        intervalSeekBar.progress = savedIndex
        updateIntervalText(savedIndex)

        intervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateIntervalText(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val newInterval = intervals[seekBar?.progress ?: 5]
                prefs.edit().putInt(MonochromeService.KEY_INTERVAL_MINUTES, newInterval).apply()

                // Restart service with new interval
                if (prefs.getBoolean(MonochromeService.KEY_SERVICE_RUNNING, false)) {
                    restartMonochromeService()
                }
            }
        })
    }

    private fun updateIntervalText(index: Int) {
        val minutes = intervals[index]
        intervalValue.text = if (minutes == 1) {
            "1 minut"
        } else if (minutes < 60) {
            "$minutes minute"
        } else {
            "1 oră"
        }
    }

    private fun setupButtons() {
        btnForceCheck.setOnClickListener {
            forceCheck()
        }

        btnToggleService.setOnClickListener {
            val isRunning = prefs.getBoolean(MonochromeService.KEY_SERVICE_RUNNING, false)
            if (isRunning) {
                stopMonochromeService()
            } else {
                startMonochromeService()
            }
            refreshUI()
        }

        btnBatteryOptimization.setOnClickListener {
            openBatteryOptimizationSettings()
        }
    }

    private fun startMonochromeService() {
        MonochromeService.start(this)
        prefs.edit().putBoolean(MonochromeService.KEY_SERVICE_RUNNING, true).apply()
    }

    private fun stopMonochromeService() {
        MonochromeService.stop(this)
        prefs.edit().putBoolean(MonochromeService.KEY_SERVICE_RUNNING, false).apply()
    }

    private fun restartMonochromeService() {
        MonochromeService.stop(this)
        handler.postDelayed({
            MonochromeService.start(this)
        }, 500)
    }

    private fun forceCheck() {
        val isMonochrome = MonochromeHelper.isMonochromeEnabled(this)
        val timestamp = java.text.SimpleDateFormat(
            "HH:mm:ss dd/MM", java.util.Locale.getDefault()
        ).format(java.util.Date())

        if (!isMonochrome) {
            val success = MonochromeHelper.enableMonochrome(this)
            prefs.edit().apply {
                putString(MonochromeService.KEY_LAST_CHECK, timestamp)
                if (success) {
                    putString(MonochromeService.KEY_LAST_ACTION, "🔄 Reactivat manual ($timestamp)")
                    val count = prefs.getInt(MonochromeService.KEY_REACTIVATION_COUNT, 0) + 1
                    putInt(MonochromeService.KEY_REACTIVATION_COUNT, count)
                    // Send notification
                    NotificationHelper.sendReactivatedNotification(this@MainActivity)
                } else {
                    putString(MonochromeService.KEY_LAST_ACTION, "❌ Eroare reactivare ($timestamp)")
                }
                apply()
            }
        } else {
            prefs.edit().apply {
                putString(MonochromeService.KEY_LAST_CHECK, timestamp)
                putString(MonochromeService.KEY_LAST_ACTION, "✅ Deja activ ($timestamp)")
                apply()
            }
        }
        refreshUI()
    }

    private fun openBatteryOptimizationSettings() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general battery settings
            try {
                startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
            } catch (e2: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    private fun refreshUI() {
        val isMonochrome = MonochromeHelper.isMonochromeEnabled(this)
        val isServiceRunning = prefs.getBoolean(MonochromeService.KEY_SERVICE_RUNNING, false)

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

        // Service status
        if (isServiceRunning) {
            serviceStatusText.text = "🟢 Serviciu activ"
            serviceStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
            btnToggleService.text = "⏹ Oprește Serviciul"
            btnToggleService.setBackgroundResource(R.drawable.bg_button_stop)
        } else {
            serviceStatusText.text = "🔴 Serviciu oprit"
            serviceStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
            btnToggleService.text = "▶ Pornește Serviciul"
            btnToggleService.setBackgroundResource(R.drawable.bg_button_start)
        }

        // Info values
        lastCheckValue.text = prefs.getString(MonochromeService.KEY_LAST_CHECK, "Niciodată")
        lastActionValue.text = prefs.getString(MonochromeService.KEY_LAST_ACTION, "Nicio acțiune")
        reactivationCountValue.text = prefs.getInt(MonochromeService.KEY_REACTIVATION_COUNT, 0).toString()

        // Permission check
        val canWriteSettings = try {
            MonochromeHelper.enableMonochrome(this)
            MonochromeHelper.isMonochromeEnabled(this)
            true
        } catch (e: SecurityException) {
            false
        }

        if (canWriteSettings) {
            permissionStatus.text = "✅ Permisiune acordată"
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active))
        } else {
            permissionStatus.text = "❌ Lipsă permisiune! Rulează comanda ADB."
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
        }

        // Battery optimization check
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            btnBatteryOptimization.text = "✅ Baterie — Nerestricționat"
            btnBatteryOptimization.setBackgroundResource(R.drawable.bg_button_battery_ok)
        } else {
            btnBatteryOptimization.text = "⚠️ Dezactivează Optimizarea Bateriei"
            btnBatteryOptimization.setBackgroundResource(R.drawable.bg_button_battery_warn)
        }
    }
}
