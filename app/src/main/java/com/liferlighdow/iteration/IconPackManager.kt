package com.liferlighdow.iteration

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

class IconPackManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private var iconPackPackageName: String? = null
    private var iconPackResources: Resources? = null
    private val iconMapping = mutableMapOf<String, String>()
    // 新增：packageName 到 component 的快速索引快取
    private val packageToComponentCache = mutableMapOf<String, String>()

    companion object {
        private const val TAG = "IconPackManager"
        private val ICON_PACK_ACTIONS = arrayOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES.icons",
            "com.gau.go.launcherex.theme",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME"
        )
    }

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

    fun loadIconPack(packageName: String?) {
        if (packageName == iconPackPackageName && iconMapping.isNotEmpty()) return

        iconPackPackageName = packageName
        iconMapping.clear()
        packageToComponentCache.clear()

        if (packageName.isNullOrEmpty()) {
            iconPackResources = null
            return
        }

        try {
            // 修正 1：改用 createPackageContext 並加入安全標記，確保能讀取 IPS 動態生成的 APK 資源
            val themeContext = context.createPackageContext(
                packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            val res = themeContext.resources
            iconPackResources = res

            // 修正 2：先嘗試傳統的 res/xml/appfilter.xml
            var resId = res.getIdentifier("appfilter", "xml", packageName)
            var isRaw = false

            // 如果找不到，代表是 IPS，改找 res/raw/appfilter.xml
            if (resId == 0) {
                resId = res.getIdentifier("appfilter", "raw", packageName)
                isRaw = true
            }

            if (resId != 0) {
                if (isRaw) {
                    // 修正 3：IPS 放在 raw 資料夾，必須以 binary stream 讀取，不能用 getXml()
                    val inputStream: InputStream = res.openRawResource(resId)
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val xpp = factory.newPullParser()
                    xpp.setInput(inputStream, "UTF-8")

                    parseAppFilter(xpp)
                    inputStream.close()
                } else {
                    // 傳統圖標包維持原樣解析
                    val xpp = res.getXml(resId)
                    parseAppFilter(xpp)
                }
                Log.d(TAG, "Successfully loaded mappings for $packageName. ${iconMapping.size} icons mapped.")
            } else {
                Log.e(TAG, "appfilter.xml could not be found in 'xml' or 'raw' folders for $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load icon pack: $packageName", e)
        }
    }

    private fun parseAppFilter(xpp: XmlPullParser) {
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xpp.name.equals("item", ignoreCase = true)) {
                var component: String? = null
                var drawable: String? = null

                // 修正 4：不給定固定命名空間(null)，直接遍歷屬性名稱，防止被 IPS 的特殊 Namespace 檔掉
                for (i in 0 until xpp.attributeCount) {
                    val attrName = xpp.getAttributeName(i)
                    if (attrName.equals("component", ignoreCase = true)) {
                        component = xpp.getAttributeValue(i)
                    } else if (attrName.equals("drawable", ignoreCase = true)) {
                        drawable = xpp.getAttributeValue(i)
                    }
                }

                if (drawable != null && component != null) {
                    val cleanComponent = component.substringAfter("{").substringBefore("}")
                    iconMapping[cleanComponent] = drawable
                }
            }
            eventType = xpp.next()
        }
    }

    fun getIcon(packageName: String): Drawable? {
        val res = iconPackResources ?: return null
        val iconPkg = iconPackPackageName ?: return null

        // 優先從快速索引中尋找
        var drawableName = packageToComponentCache[packageName]?.let { iconMapping[it] }
        
        // 如果沒有快取過，嘗試在 Mapping 中尋找
        if (drawableName == null) {
            for ((key, value) in iconMapping) {
                if (key.startsWith("$packageName/") || key == packageName) {
                    drawableName = value
                    packageToComponentCache[packageName] = key // 存入快速索引
                    break
                }
            }
        }

        if (drawableName != null) {
            val resId = res.getIdentifier(drawableName, "drawable", iconPkg)
            if (resId != 0) {
                return try {
                    res.getDrawable(resId, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to draw icon: $drawableName from pack $iconPkg", e)
                    null
                }
            }
        }
        return null
    }
}

data class IconPackInfo(val packageName: String, val label: String, val icon: Drawable)
