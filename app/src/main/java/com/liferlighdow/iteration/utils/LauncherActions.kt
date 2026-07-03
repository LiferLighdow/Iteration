package com.liferlighdow.iteration.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService
import rikka.shizuku.Shizuku

fun performGestureAction(
    action: GestureAction,
    pkg: String,
    context: Context,
    actionMode: ActionMode = ActionMode.ACCESSIBILITY,
    onSettingsClick: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onOpenDesktopMenu: () -> Unit
) {
    when (action) {
        GestureAction.LOCK_SCREEN -> {
            when (actionMode) {
                ActionMode.ACCESSIBILITY -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val service = IterationAccessibilityService.instance
                        if (service != null) {
                            service.lockScreen()
                        } else {
                            Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                ActionMode.ROOT -> {
                    executeCommand(arrayOf("su", "-c", "input keyevent 26"), context)
                }
                ActionMode.SHIZUKU -> {
                    if (Shizuku.pingBinder()) {
                        // Pending proper Shizuku command execution implementation
                        Toast.makeText(context, "Shizuku mode active", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        GestureAction.LAUNCHER_SETTINGS -> onSettingsClick()
        GestureAction.OPEN_SYSTEM_SETTINGS -> {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.failed_to_open_settings), Toast.LENGTH_SHORT).show()
            }
        }
        GestureAction.LAUNCH_APP -> {
            if (pkg.isNotEmpty()) {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }
        }
        GestureAction.OPEN_GLOBAL_SEARCH -> onOpenGlobalSearch()
        GestureAction.OPEN_DESKTOP_MENU -> onOpenDesktopMenu()
        GestureAction.OPEN_NOTIFICATIONS -> {
            when (actionMode) {
                ActionMode.ACCESSIBILITY -> {
                    val service = IterationAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                    } else {
                        Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
                    }
                }
                ActionMode.ROOT -> {
                    executeCommand(arrayOf("su", "-c", "cmd statusbar expand-notifications"), context)
                }
                ActionMode.SHIZUKU -> {
                    if (Shizuku.pingBinder()) {
                        Toast.makeText(context, "Shizuku mode active", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        GestureAction.NONE -> {}
    }
}

private fun executeCommand(command: Array<String>, context: Context) {
    try {
        Runtime.getRuntime().exec(command)
    } catch (e: Exception) {
        Log.e("Iteration", "Command failed: ${command.joinToString(" ")}", e)
        Toast.makeText(context, "Execution failed", Toast.LENGTH_SHORT).show()
    }
}
