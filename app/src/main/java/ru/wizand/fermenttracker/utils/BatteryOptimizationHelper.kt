package ru.wizand.fermenttracker.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            true
        }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                Log.w("BatteryOptimizationHelper", "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS not supported, trying fallback...")
                openBatterySettingsFallback(context)
            }
        }
    }

    private fun openBatterySettingsFallback(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Log.w("BatteryOptimizationHelper", "ACTION_IGNORE_BATTERY_OPTIMIZATIONS_SETTINGS not supported, opening app details...")
            openAppDetails(context)
        }
    }

    private fun openAppDetails(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e("BatteryOptimizationHelper", "Could not open app settings", e)
        }
    }
}