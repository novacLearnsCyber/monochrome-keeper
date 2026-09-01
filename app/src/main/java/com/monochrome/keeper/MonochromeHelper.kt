package com.monochrome.keeper

import android.content.Context
import android.provider.Settings

/**
 * Utility class for checking and setting the monochrome (grayscale) display mode.
 *
 * Uses Settings.Secure:
 *   - accessibility_display_daltonizer_enabled: 1 = on, 0 = off
 *   - accessibility_display_daltonizer: 0 = monochromacy, -1 = disabled
 *
 * Requires WRITE_SECURE_SETTINGS permission (grant via ADB).
 */
object MonochromeHelper {

    private const val DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val DALTONIZER_MODE = "accessibility_display_daltonizer"

    private const val MODE_MONOCHROMACY = 0
    private const val ENABLED = "1"
    private const val DISABLED = "0"

    /**
     * Check if monochrome mode is currently active.
     * Returns true if daltonizer is enabled AND set to monochromacy (0).
     */
    fun isMonochromeEnabled(context: Context): Boolean {
        return try {
            val enabled = Settings.Secure.getString(context.contentResolver, DALTONIZER_ENABLED)
            val mode = Settings.Secure.getString(context.contentResolver, DALTONIZER_MODE)
            enabled == ENABLED && mode == MODE_MONOCHROMACY.toString()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Enable monochrome mode by setting daltonizer to monochromacy.
     * Returns true if the operation succeeded.
     */
    fun enableMonochrome(context: Context): Boolean {
        return try {
            Settings.Secure.putString(context.contentResolver, DALTONIZER_ENABLED, ENABLED)
            Settings.Secure.putString(
                context.contentResolver,
                DALTONIZER_MODE,
                MODE_MONOCHROMACY.toString()
            )
            true
        } catch (e: SecurityException) {
            // WRITE_SECURE_SETTINGS not granted
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Disable monochrome mode.
     * Returns true if the operation succeeded.
     */
    fun disableMonochrome(context: Context): Boolean {
        return try {
            Settings.Secure.putString(context.contentResolver, DALTONIZER_ENABLED, DISABLED)
            Settings.Secure.putString(context.contentResolver, DALTONIZER_MODE, "-1")
            true
        } catch (e: Exception) {
            false
        }
    }
}
