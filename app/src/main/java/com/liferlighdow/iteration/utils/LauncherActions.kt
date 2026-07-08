package com.liferlighdow.iteration.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService
import rikka.shizuku.Shizuku
import kotlin.concurrent.thread

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
                    executeShizukuCommand(arrayOf("input", "keyevent", "26"), context)
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
                    executeShizukuCommand(arrayOf("cmd", "statusbar", "expand-notifications"), context)
                }
            }
        }
        GestureAction.NONE -> {}
    }
}

private fun executeShizukuCommand(command: Array<String>, context: Context) {
    if (!Shizuku.pingBinder()) {
        Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_SHORT).show()
        return
    }

    val handler = Handler(Looper.getMainLooper())
    thread {
        try {
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                var success = false
                try {
                    // Method 1: Robust Reflection (Iterate through methods to find matching signature)
                    val method = Shizuku::class.java.declaredMethods.find { 
                        it.name == "newProcess" && it.parameterTypes.size == 3 && it.parameterTypes[0].isArray
                    }
                    
                    if (method != null) {
                        method.isAccessible = true
                        val process = method.invoke(null, command, null, null) as rikka.shizuku.ShizukuRemoteProcess
                        val exitCode = process.waitFor()
                        success = (exitCode == 0)
                    } else {
                        throw NoSuchMethodException("Shizuku.newProcess method not found via reflection")
                    }
                } catch (e: Exception) {
                    Log.e("Iteration", "Shizuku Reflection failed", e)
                    handler.post { Toast.makeText(context, "Shizuku Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
                }
            } else {
                handler.post { Toast.makeText(context, context.getString(R.string.shizuku_permission_denied), Toast.LENGTH_SHORT).show() }
            }
        } catch (e: Exception) {
            Log.e("Iteration", "Shizuku error", e)
            handler.post { Toast.makeText(context, "Shizuku Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show() }
        }
    }
}

private fun executeCommand(command: Array<String>, context: Context) {
    thread {
        try {
            Runtime.getRuntime().exec(command).waitFor()
        } catch (e: Exception) {
            Log.e("Iteration", "Command failed: ${command.joinToString(" ")}", e)
        }
    }
}
