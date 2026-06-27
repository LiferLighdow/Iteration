package com.liferlighdow.iteration.utils

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService

fun performGestureAction(
    action: GestureAction,
    pkg: String,
    context: Context,
    onSettingsClick: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onOpenDesktopMenu: () -> Unit
) {
    when (action) {
        GestureAction.LOCK_SCREEN -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val service = IterationAccessibilityService.instance
                if (service != null) {
                    service.lockScreen()
                } else {
                    Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
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
            val service = IterationAccessibilityService.instance
            if (service != null) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            } else {
                Toast.makeText(context, context.getString(R.string.need_accessibility), Toast.LENGTH_SHORT).show()
            }
        }
        GestureAction.NONE -> {}
    }
}
