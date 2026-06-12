package com.liferlighdow.iteration

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class IconPackManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private var iconPackPackageName: String? = null
    private var iconPackResources: Resources? = null
    private val iconMapping = mutableMapOf<String, String>()

    // Icon Pack 常用 Intent Action
    companion object {
        private val ICON_PACK_ACTIONS = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES.icons",
            "com.gau.go.launcherex.theme"
        )
    }

    /**
     * 獲取手機上安裝的所有 Icon Pack
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val iconPacks = mutableMapOf<String, IconPackInfo>()
        for (action in ICON_PACK_ACTIONS) {
            val intent = Intent(action)
            val list = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (info in list) {
                val pkgName = info.activityInfo.packageName
                if (!iconPacks.containsKey(pkgName)) {
                    iconPacks[pkgName] = IconPackInfo(
                        packageName = pkgName,
                        label = info.loadLabel(pm).toString(),
                        icon = info.loadIcon(pm)
                    )
                }
            }
        }
        return iconPacks.values.toList().sortedBy { it.label }
    }

    /**
     * 載入指定的 Icon Pack 映射表 (appfilter.xml)
     */
    fun loadIconPack(packageName: String?) {
        if (packageName == iconPackPackageName && iconMapping.isNotEmpty()) return
        
        iconPackPackageName = packageName
        iconMapping.clear()
        
        if (packageName == null || packageName.isEmpty()) {
            iconPackResources = null
            return
        }

        try {
            iconPackResources = pm.getResourcesForApplication(packageName)
            val resId = iconPackResources!!.getIdentifier("appfilter", "xml", packageName)
            if (resId != 0) {
                val xpp = iconPackResources!!.getXml(resId)
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && xpp.name == "item") {
                        val component = xpp.getAttributeValue(null, "component")
                        val drawableName = xpp.getAttributeValue(null, "drawable")
                        if (component != null && drawableName != null) {
                            // component 通常格式為 ComponentInfo{pkg/activity}
                            iconMapping[component] = drawableName
                        }
                    }
                    eventType = xpp.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 根據包名獲取對應的圖示，如果找不到則返回 null
     */
    fun getIcon(packageName: String): Drawable? {
        val res = iconPackResources ?: return null
        val iconPkg = iconPackPackageName ?: return null

        var drawableName: String? = null
        
        // 嘗試精確匹配包名或 ComponentInfo
        // 1. 嘗試直接從映射表中尋找包含該包名的 key
        // 因為我們只有包名，所以我們搜尋包含 "packageName/" 的 ComponentInfo 字串
        for ((key, value) in iconMapping) {
            if (key.contains("$packageName/") || key.contains("$packageName}")) {
                drawableName = value
                break
            }
        }

        if (drawableName != null) {
            val resId = res.getIdentifier(drawableName, "drawable", iconPkg)
            if (resId != 0) {
                return res.getDrawable(resId, null)
            }
        }

        return null
    }
}

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable
)
