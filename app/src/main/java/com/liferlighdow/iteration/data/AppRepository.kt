package com.liferlighdow.iteration.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppModel> {
        val pm = context.packageManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        // 1. 抓取所有具有 LAUNCHER 類別的 Activity
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val myPackageName = context.packageName

        val apps = resolveInfos.filter { it.activityInfo.packageName != myPackageName }.map { info ->
            val pkgName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            AppModel(
                label = info.loadLabel(pm).toString(),
                packageName = pkgName,
                uniqueId = "$pkgName/$activityName",
                category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    info.activityInfo.applicationInfo.category
                } else {
                    -1
                }
            )
        }.toMutableList()

        // 2. 抓取 Pinned Shortcuts (PWA / 網頁捷徑)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                // 檢查是否具有存取捷徑的權限 (通常需要是預設啟動器)
                val query = LauncherApps.ShortcutQuery().apply {
                    setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                }

                val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { launcherApps.profiles } catch (e: Exception) { listOf(android.os.Process.myUserHandle()) }
                } else {
                    listOf(android.os.Process.myUserHandle())
                }

                profiles.forEach { userHandle ->
                    try {
                        val shortcuts = launcherApps.getShortcuts(query, userHandle) ?: emptyList()
                        shortcuts.forEach { shortcut ->
                            val exists = apps.any { it.packageName == shortcut.`package` && it.shortcutId == shortcut.id }
                            if (!exists) {
                                apps.add(AppModel(
                                    label = (shortcut.shortLabel ?: shortcut.longLabel ?: "Shortcut").toString(),
                                    packageName = shortcut.`package`,
                                    shortcutId = shortcut.id,
                                    displayCategory = "Web Apps"
                                ))
                            }
                        }
                    } catch (e: SecurityException) {
                        // 如果不是預設啟動器，會噴 SecurityException，這裡忽略即可
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return apps.sortedBy { it.label.lowercase() }
    }
}