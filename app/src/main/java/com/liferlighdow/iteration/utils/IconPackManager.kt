package com.liferlighdow.iteration.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import kotlin.collections.iterator

class IconPackManager(private val context: Context) {
    private val pm: PackageManager = context.packageManager
    private var iconPackPackageName: String? = null
    private var iconPackResources: Resources? = null
    private val iconMapping = mutableMapOf<String, String>()
    // 新增：packageName 到 component 的快速索引快取
    private val packageToComponentCache = mutableMapOf<String, String>()

    companion object {
        private const val TAG = "IconPackManager"
        const val BUILTIN_PACKAGE_NAME = "com.liferlighdow.iteration.builtin"
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
        
        // Add built-in icon pack
        iconPacks[BUILTIN_PACKAGE_NAME] = IconPackInfo(
            packageName = BUILTIN_PACKAGE_NAME,
            label = "Built-in Icons",
            icon = context.packageManager.getApplicationIcon(context.packageName)
        )

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
            val res = if (packageName == BUILTIN_PACKAGE_NAME) {
                context.resources
            } else {
                // 修正 1：改用 createPackageContext 並加入安全標記，確保能讀取 IPS 動態生成的 APK 資源
                val themeContext = context.createPackageContext(
                    packageName,
                    Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                )
                themeContext.resources
            }
            iconPackResources = res

            val targetPkg = if (packageName == BUILTIN_PACKAGE_NAME) context.packageName else packageName
            // 修正 2：先嘗試傳統的 res/xml/appfilter.xml
            var resId = res.getIdentifier("appfilter", "xml", targetPkg)
            var isRaw = false

            // 如果找不到，代表是 IPS，改找 res/raw/appfilter.xml
            if (resId == 0) {
                resId = res.getIdentifier("appfilter", "raw", targetPkg)
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

                for (i in 0 until xpp.attributeCount) {
                    val attrName = xpp.getAttributeName(i)
                    if (attrName.equals("component", ignoreCase = true)) {
                        component = xpp.getAttributeValue(i)
                    } else if (attrName.equals("drawable", ignoreCase = true)) {
                        drawable = xpp.getAttributeValue(i)
                    }
                }

                if (drawable != null && component != null) {
                    // 解析 ComponentInfo{package/activity}
                    var cleanComponent = component
                    if (cleanComponent.startsWith("ComponentInfo{") && cleanComponent.endsWith("}")) {
                        cleanComponent = cleanComponent.substring(14, cleanComponent.length - 1)
                    }
                    
                    // 儲存完整組件名稱
                    iconMapping[cleanComponent] = drawable
                    
                    // 如果包含 /，也額外儲存一份純包名的對應，增加匹配成功率
                    if (cleanComponent.contains("/")) {
                        val pkgOnly = cleanComponent.substringBefore("/")
                        if (!iconMapping.containsKey(pkgOnly)) {
                            iconMapping[pkgOnly] = drawable
                        }
                    }
                }
            }
            eventType = xpp.next()
        }
    }

    private fun isDefaultBrowser(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
            val info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName == packageName
        } catch (e: Exception) {
            false
        }
    }

    fun getIcon(packageName: String, uniqueId: String? = null): Drawable? {
        val res = iconPackResources ?: return null
        val iconPkg = iconPackPackageName ?: return null

        // 1. 處理 uniqueId，移除 ComponentInfo 標籤與時間戳記
        var cleanId = uniqueId ?: packageName
        if (cleanId.startsWith("ComponentInfo{") && cleanId.endsWith("}")) {
            cleanId = cleanId.substring(14, cleanId.length - 1)
        }
        if (cleanId.contains("@")) {
            val parts = cleanId.split("@")
            if (parts.last().length >= 10 && parts.last().toLongOrNull() != null) {
                cleanId = cleanId.substringBeforeLast("@")
            }
        }

        // 2. 優先精準匹配 (完整組件名稱)
        var drawableName = iconMapping[cleanId]
        
        // 3. 次之匹配 (純包名)
        if (drawableName == null) {
            val pkgFromId = if (cleanId.contains("/")) cleanId.substringBefore("/") else cleanId
            drawableName = iconMapping[pkgFromId] ?: iconMapping[packageName]
        }

        // 4. 針對內建圖示包的關鍵字模糊匹配 (最後防線)
        if (drawableName == null && iconPkg == BUILTIN_PACKAGE_NAME) {
            val lowerPkg = packageName.lowercase()
            if (lowerPkg.contains("messaging") || lowerPkg.contains("message") || lowerPkg.contains("sms") || lowerPkg.contains("mms") || lowerPkg.contains("chat")) {
                drawableName = "ic_builtin_messages"
            } else if (isDefaultBrowser(packageName)) {
                // 僅針對預設瀏覽器套用內建圖標
                drawableName = "ic_builtin_browser"
            } else if (lowerPkg.contains("camera") || lowerPkg.contains("gallery") || lowerPkg.contains("photo")) {
                drawableName = "ic_builtin_camera"
            }
        }

        if (drawableName != null) {
            val targetPkg = if (iconPkg == BUILTIN_PACKAGE_NAME) context.packageName else iconPkg
            val resId = res.getIdentifier(drawableName, "drawable", targetPkg)
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
