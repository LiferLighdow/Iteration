package com.liferlighdow.iteration.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserManager

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppModel> {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
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
            
            activityList.forEach { info ->
                if (info.applicationInfo.packageName != myPackageName) {
                    val pkgName = info.applicationInfo.packageName
                    val activityName = info.name
                    val idSuffix = if (userId > 0) "@$userId" else ""
                    
                    apps.add(AppModel(
                        label = info.label.toString(),
                        packageName = pkgName,
                        userId = userId,
                        // 關鍵：將 Activity 名稱包含在 ID 中，區分同包名的不同分身入口
                        uniqueId = "$pkgName/$activityName$idSuffix",
                        category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            info.applicationInfo.category
                        } else {
                            -1
                        }
                    ))
                }
            }
        }

        return apps.sortedBy { it.label.lowercase() }
    }
}
