package com.liferlighdow.iteration.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager

class AppRepository(private val context: Context) {

    fun getInstalledApps(frozenPackages: Set<String> = emptySet()): List<AppModel> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val pm = context.packageManager
        val myPackageName = context.packageName
        val apps = mutableListOf<AppModel>()

        // 1. 遍歷系統中所有的使用者設定檔 (Profiles)，支援 Work Profile 與分身
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcherApps.profiles
        } else {
            userManager.userProfiles
        }

        profiles.forEach { userHandle ->
            val userId = userManager.getSerialNumberForUser(userHandle)
            // 抓取該使用者下所有可啟動的 Activity (處理一鍵多入口)
            val activityList = launcherApps.getActivityList(null, userHandle)
            
            val seenUniqueIds = mutableSetOf<String>()

            activityList.forEach { info ->
                if (info.applicationInfo.packageName != myPackageName) {
                    val pkgName = info.applicationInfo.packageName
                    val activityName = info.name
                    val idSuffix = if (userId > 0) "@$userId" else ""
                    val uniqueId = "$pkgName/$activityName$idSuffix"
                    
                    val isSystem = (info.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                    
                    apps.add(AppModel(
                        label = info.label.toString(),
                        packageName = pkgName,
                        userId = userId,
                        isSystem = isSystem,
                        uniqueId = uniqueId,
                        category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            info.applicationInfo.category
                        } else {
                            -1
                        },
                        isFrozen = false
                    ))
                    seenUniqueIds.add(uniqueId)
                }
            }

            // 2. 額外處理已凍結 (已停用) 的應用程式
            // 只有當應用程式在我們的 frozenPackages 名單中，或者是被停用的 Launcher 應用時才顯示
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val disabledActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DISABLED_COMPONENTS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_DISABLED_COMPONENTS)
            }

            disabledActivities.forEach { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName != myPackageName) {
                    val activityName = resolveInfo.activityInfo.name
                    val idSuffix = if (userId > 0) "@$userId" else ""
                    val uniqueId = "$pkgName/$activityName$idSuffix"

                    // 如果這個 Activity 還沒被加進去 (代表它是被停用的)
                    if (!seenUniqueIds.contains(uniqueId)) {
                        val isFrozenByUs = frozenPackages.contains(pkgName)
                        // 我們只顯示「被停用」且「具有啟動界面」且「在我們凍結名單中」或「被停用但非系統預設停用」的應用
                        // 簡單起見，只要它是被停用的 Launcher App，我們就顯示它為凍結狀態
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        if (!appInfo.enabled || pm.getApplicationEnabledSetting(pkgName) >= PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER) {
                            val isSystem = (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
                            
                            apps.add(AppModel(
                                label = resolveInfo.loadLabel(pm).toString(),
                                packageName = pkgName,
                                userId = userId,
                                isSystem = isSystem,
                                uniqueId = uniqueId,
                                category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1,
                                isFrozen = true
                            ))
                            seenUniqueIds.add(uniqueId)
                        }
                    }
                }
            }
        }

        return apps.sortedBy { it.label.lowercase() }
    }
}

