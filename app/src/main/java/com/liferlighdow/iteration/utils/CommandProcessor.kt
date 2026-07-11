package com.liferlighdow.iteration.utils

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.liferlighdow.iteration.R

data class CommandResult(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val action: (Context) -> Unit
)

object CommandProcessor {
    fun process(query: String, context: Context): List<CommandResult> {
        val q = query.lowercase().trim()
        val results = mutableListOf<CommandResult>()

        // WiFi
        if ("wifi".contains(q) || q == "wi-fi" || q == "無線網路" || q == "網路") {
            results.add(CommandResult(
                label = "Wi-Fi Settings",
                description = "Open system Wi-Fi settings",
                icon = Icons.Default.Wifi,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open Wi-Fi settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Bluetooth
        if ("bluetooth".contains(q) || q == "bt" || q == "藍牙") {
            results.add(CommandResult(
                label = "Bluetooth Settings",
                description = "Open system Bluetooth settings",
                icon = Icons.Default.Bluetooth,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open Bluetooth settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Alarm
        if (q.startsWith("alarm") || q.startsWith("鬧鐘")) {
            val timePart = q.removePrefix("alarm").removePrefix("鬧鐘").trim()
            val timeMatch = Regex("""(\d{1,2})[:：\s]*(\d{2})""").find(timePart)
            if (timeMatch != null) {
                val hour = timeMatch.groupValues[1].toInt()
                val minute = timeMatch.groupValues[2].toInt()
                if (hour in 0..23 && minute in 0..59) {
                    results.add(CommandResult(
                        label = "Set Alarm at ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}",
                        description = "Create a new alarm for this time",
                        icon = Icons.Default.Alarm,
                        action = {
                            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                                putExtra(AlarmClock.EXTRA_HOUR, hour)
                                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                            }
                            try {
                                it.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (e: Exception) {
                                Toast.makeText(it, "No Alarm app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ))
                }
            } else {
                results.add(CommandResult(
                    label = "Alarms",
                    description = "Open Alarms app",
                    icon = Icons.Default.Alarm,
                    action = { 
                    try {
                        it.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "No Alarm app found", Toast.LENGTH_SHORT).show()
                    }
                }
                ))
            }
        }

        // Timer
        if (q.startsWith("timer") || q.startsWith("計時器") || q.startsWith("倒數")) {
            val durationPart = q.removePrefix("timer").removePrefix("計時器").removePrefix("倒數").trim()
            val seconds = durationPart.toIntOrNull()
            if (seconds != null && seconds > 0) {
                results.add(CommandResult(
                    label = "Set Timer for $seconds seconds",
                    description = "Start a countdown timer",
                    icon = Icons.Default.Timer,
                    action = {
                        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        }
                        try {
                            it.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            Toast.makeText(it, "No Timer app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                ))
            } else {
                results.add(CommandResult(
                    label = "Timers",
                    description = "Open Timers",
                    icon = Icons.Default.Timer,
                    action = { 
                        try {
                            it.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e: Exception) {
                            Toast.makeText(it, "No Timer app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                ))
            }
        }
        
        // Torch
        if ("torch".contains(q) || "flashlight".contains(q) || q == "手電筒") {
             results.add(CommandResult(
                label = "Flashlight",
                description = "Manage display and lighting",
                icon = Icons.Default.FlashlightOn,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open display settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Display/Brightness
        if ("brightness".contains(q) || "display".contains(q) || q == "亮度" || q == "螢幕") {
            results.add(CommandResult(
                label = "Display Settings",
                description = "Adjust brightness and screen timeout",
                icon = Icons.Default.Brightness6,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open display settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Battery
        if ("battery".contains(q) || q == "電池" || q == "電量") {
            results.add(CommandResult(
                label = "Battery Settings",
                description = "Check battery usage and saver",
                icon = Icons.Default.BatteryChargingFull,
                action = { 
                    val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        it.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            // Fallback to battery saver settings if summary is not available
                            it.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (e2: Exception) {
                            // Last resort: open generic settings
                            it.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
            ))
        }

        // Airplane Mode
        if ("airplane".contains(q) || q == "飛航模式" || q == "飛行模式") {
            results.add(CommandResult(
                label = "Airplane Mode",
                description = "Toggle airplane mode in settings",
                icon = Icons.Default.AirplaneTicket,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open airplane settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // System Settings
        if (q == "settings" || q == "系統設定" || q == "設定") {
            results.add(CommandResult(
                label = "System Settings",
                description = "Open Android settings",
                icon = Icons.Default.Settings,
                action = { 
                    try {
                        it.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "Unable to open system settings", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Calendar
        if (q == "calendar" || q == "日曆" || q == "行事曆") {
            results.add(CommandResult(
                label = "Calendar",
                description = "Open system calendar",
                icon = Icons.Default.Event,
                action = {
                    val intent = Intent(Intent.ACTION_VIEW).setData("content://com.android.calendar/time/".toUri())
                    try {
                        it.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "No Calendar app found", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        // Files
        if (q == "files" || q == "檔案" || q == "文件") {
            results.add(CommandResult(
                label = "Files",
                description = "Open file manager",
                icon = Icons.Default.Folder,
                action = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                    try {
                        it.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        Toast.makeText(it, "No File Manager found", Toast.LENGTH_SHORT).show()
                    }
                }
            ))
        }

        return results
    }
}
