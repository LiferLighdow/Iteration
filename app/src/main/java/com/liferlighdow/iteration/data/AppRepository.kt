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
                val query = LauncherApps.ShortcutQuery().apply {
                    setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
                }
                val shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
                
                shortcuts.forEach { shortcut ->
                    // 避免重複加入已經在列表中的 App 內的普通 Shortcut (這裡只針對網頁捷徑等 Pinned 類型)
                    // PWA 捷徑通常有自己的 label 和 icon
                    apps.add(AppModel(
                        label = (shortcut.shortLabel ?: shortcut.longLabel ?: "Shortcut").toString(),
                        packageName = shortcut.`package`,
                        shortcutId = shortcut.id,
                        displayCategory = "Web Apps"
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return apps.sortedBy { it.label.lowercase() }
    }
}