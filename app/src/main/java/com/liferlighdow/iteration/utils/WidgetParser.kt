package com.liferlighdow.iteration.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.app.ActivityManager
import android.net.wifi.WifiManager
import java.text.SimpleDateFormat
import java.util.*

object WidgetParser {
    
    fun parseText(formula: String, context: Context): String {
        var result = formula
        
        // --- 1. Variables ---
        // Battery [BATT]
        if (result.contains("[BATT]")) {
            result = result.replace("[BATT]", getBatteryLevel(context).toString())
        }
        
        // Time [TIME]
        if (result.contains("[TIME]")) {
            result = result.replace("[TIME]", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()))
        }
        
        // Date [DATE]
        if (result.contains("[DATE]")) {
            result = result.replace("[DATE]", SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date()))
        }
        
        // Greeting [GREET]
        if (result.contains("[GREET]")) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 0..11 -> "Good Morning"
                in 12..17 -> "Good Afternoon"
                else -> "Good Evening"
            }
            result = result.replace("[GREET]", greeting)
        }

        // RAM [RAM]
        if (result.contains("[RAM]")) {
            result = result.replace("[RAM]", getAvailableRam(context))
        }

        // WIFI [WIFI]
        if (result.contains("[WIFI]")) {
            result = result.replace("[WIFI]", getWifiSsid(context))
        }

        // --- 2. Conditional Logic: [IF:condition, true, false] ---
        // Simple regex-based IF parser for [IF:BATT<20, Low, Full]
        if (result.contains("[IF:")) {
            val pattern = Regex("\\[IF:(.*?), (.*?), (.*?)\\]")
            result = pattern.replace(result) { match ->
                val condition = match.groups[1]?.value ?: ""
                val trueVal = match.groups[2]?.value ?: ""
                val falseVal = match.groups[3]?.value ?: ""
                
                if (evaluateCondition(condition, context)) trueVal else falseVal
            }
        }
        
        return result
    }

    private fun evaluateCondition(condition: String, context: Context): Boolean {
        return when {
            condition.startsWith("BATT<") -> {
                val threshold = condition.substring(5).toIntOrNull() ?: 0
                getBatteryLevel(context) < threshold
            }
            condition.startsWith("BATT>") -> {
                val threshold = condition.substring(5).toIntOrNull() ?: 0
                getBatteryLevel(context) > threshold
            }
            else -> false
        }
    }

    private fun getAvailableRam(context: Context): String {
        val mi = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(mi)
        val availableMegs = mi.availMem / 0x100000L
        return "${availableMegs}MB"
    }

    private fun getWifiSsid(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        return info.ssid.replace("\"", "").let { if (it == "<unknown ssid>") "Disconnected" else it }
    }

    
    fun parseValue(formula: String, context: Context): Float {
        // Try to parse as pure number first
        formula.toFloatOrNull()?.let { return it / 100f }
        
        // Handle special variables
        return when {
            formula.contains("[BATT]") -> getBatteryLevel(context) / 100f
            else -> 0f
        }
    }

    fun parseColor(formula: String?, defaultColor: Int, context: Context): Int {
        if (formula.isNullOrBlank()) return defaultColor
        
        // Example logic: "[IF:BATT<20,#FF0000,#00FF00]" (This is complex to parse properly)
        // For now, let's support simple color hex or basic logic
        
        if (formula.startsWith("#")) {
            return try { android.graphics.Color.parseColor(formula) } catch (e: Exception) { defaultColor }
        }

        // Simple Battery Logic: "BATT_COLOR"
        if (formula == "BATT_COLOR") {
            val level = getBatteryLevel(context)
            return if (level < 20) android.graphics.Color.RED else android.graphics.Color.GREEN
        }

        return defaultColor
    }

    fun parseVisibility(formula: String?, defaultVisible: Boolean, context: Context): Boolean {
        if (formula.isNullOrBlank()) return defaultVisible
        
        // Simple logic: "HIDE_LOW_BATT"
        if (formula == "HIDE_LOW_BATT") {
            return getBatteryLevel(context) >= 20
        }

        return defaultVisible
    }

    
    private fun getBatteryLevel(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (scale != 0) (level * 100 / scale.toFloat()).toInt() else 0
    }
}
