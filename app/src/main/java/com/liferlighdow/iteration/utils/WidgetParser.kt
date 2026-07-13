package com.liferlighdow.iteration.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.*

object WidgetParser {
    
    fun parseText(formula: String, context: Context): String {
        var result = formula
        
        // 1. Battery [BATT]
        if (result.contains("[BATT]")) {
            val batteryLevel = getBatteryLevel(context)
            result = result.replace("[BATT]", batteryLevel.toString())
        }
        
        // 2. Time [TIME]
        if (result.contains("[TIME]")) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            result = result.replace("[TIME]", time)
        }
        
        // 3. Date [DATE]
        if (result.contains("[DATE]")) {
            val date = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date())
            result = result.replace("[DATE]", date)
        }
        
        // 4. Greeting [GREET]
        if (result.contains("[GREET]")) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 0..11 -> "Good Morning"
                in 12..17 -> "Good Afternoon"
                else -> "Good Evening"
            }
            result = result.replace("[GREET]", greeting)
        }
        
        return result
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
