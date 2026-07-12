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

        // 1. 獲取所有 Profile
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcherApps.profiles
        } else {
            userManager.userProfiles
        }

        profiles.forEach { userHandle ->
            val userId = userManager.getSerialNumberForUser(userHandle)
            var isPrivate = false
            
            // 偵測是否為私密空間
            if (userId > 0) {
                if (Build.VERSION.SDK_INT >= 30) {
                    try {
                        val getTypeMethod = UserManager::class.java.getMethod("getUserType", android.os.UserHandle::class.java)
                        val type = getTypeMethod.invoke(userManager, userHandle) as? String
                        
                        // 廣泛匹配私密空間類型名稱
                        if (type != null) {
                            val t = type.lowercase()
                            if (t.contains("private") || t.contains("stealth") || t.contains("hidden")) {
                                isPrivate = true
                            }
                        }
                    } catch (e: Exception) {}
                }
                
                // 輔助判斷：如果 ID 較大 (Android 15 私密空間通常從較大的 Serial 開始) 且不是 Work Profile
                if (!isPrivate) {
                    val handleStr = userHandle.toString().lowercase()
                    if (userId >= 10 && !handleStr.contains("work")) {
                        isPrivate = true
                    }
                }
            }

            val isLocked = if (isPrivate) {
                userManager.isQuietModeEnabled(userHandle)
            } else false

            // 強制塞入種子（僅用於驅動搜尋結果顯示鎖頭/管理員）
            if (isPrivate) {
                apps.add(AppModel(
                    label = "Private Space Entry",
                    packageName = "com.android.settings", 
                    userId = userId,
                    uniqueId = "private_seed_$userId",
                    isPrivate = true,
                    isLocked = isLocked,
                    isHidden = true 
                ))
            }

            // 抓取應用
            val activityList = try {
                launcherApps.getActivityList(null, userHandle)
            } catch (e: Exception) { emptyList() }
            
            val seenUniqueIds = mutableSetOf<String>()

            activityList.forEach { info ->
                if (info.applicationInfo.packageName != myPackageName) {
                    val pkgName = info.applicationInfo.packageName
                    val activityName = info.name
                    val idSuffix = if (userId > 0) "@$userId" else ""
                    val uniqueId = "$pkgName/$activityName$idSuffix"
                    
                    apps.add(AppModel(
                        label = info.label.toString(),
                        packageName = pkgName,
                        userId = userId,
                        isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        uniqueId = uniqueId,
                        category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) info.applicationInfo.category else -1,
                        isFrozen = false,
                        isPrivate = isPrivate,
                        isLocked = isLocked
                    ))
                    seenUniqueIds.add(uniqueId)
                }
            }

            // 處理凍結
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
                    val idSuffix = if (userId > 0) "@$userId" else ""
                    val uniqueId = "$pkgName/${resolveInfo.activityInfo.name}$idSuffix"

                    if (!seenUniqueIds.contains(uniqueId)) {
                        val appInfo = resolveInfo.activityInfo.applicationInfo
                        if (!appInfo.enabled || pm.getApplicationEnabledSetting(pkgName) >= 2) {
                            apps.add(AppModel(
                                label = resolveInfo.loadLabel(pm).toString(),
                                packageName = pkgName,
                                userId = userId,
                                isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                uniqueId = uniqueId,
                                category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appInfo.category else -1,
                                isFrozen = true,
                                isPrivate = isPrivate,
                                isLocked = isLocked
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
