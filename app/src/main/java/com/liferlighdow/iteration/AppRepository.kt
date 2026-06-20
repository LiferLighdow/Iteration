package com.liferlighdow.iteration

import android.content.Context
import android.content.Intent

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppModel> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // 抓取所有具有 LAUNCHER 類別的 Activity
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val myPackageName = context.packageName

        return resolveInfos.filter { it.activityInfo.packageName != myPackageName }.map { info ->
            val pkgName = info.activityInfo.packageName
            val activityName = info.activityInfo.name
            AppModel(
                label = info.loadLabel(pm).toString(),
                packageName = pkgName,
                uniqueId = "$pkgName/$activityName",
                category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    info.activityInfo.applicationInfo.category
                } else {
                    -1
                }
            )
        }.sortedBy { it.label.lowercase() } // 依名稱排序，讓列表整齊
    }
}