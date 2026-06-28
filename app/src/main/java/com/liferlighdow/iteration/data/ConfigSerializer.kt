package com.liferlighdow.iteration.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 負責處理 Launcher 配置與 App 模型之間的 JSON 序列化與反序列化
 */
object ConfigSerializer {

    fun serializeAppModel(item: AppModel): JSONObject {
        val obj = JSONObject()
        obj.put("type", when {
            item.isFolder -> "folder"
            item.isWidget -> "widget"
            item.isShortcut -> "shortcut"
            else -> "app"
        })
        obj.put("id", item.uniqueId)
        obj.put("label", item.label)
        obj.put("pkg", item.packageName)
        if (item.isShortcut) {
            obj.put("shortcutId", item.shortcutId)
            obj.put("intentUri", item.intentUri)
        }

        if (item.isFolder) {
            val children = JSONArray()
            item.folderItems.forEach { children.put(serializeAppModel(it)) }
            obj.put("children", children)
        }

        item.widget?.let { w ->
            obj.put("widget", serializeWidgetModel(w))
        }
        return obj
    }

    fun serializeWidgetModel(w: WidgetModel): JSONObject {
        val obj = JSONObject()
        obj.put("id", w.id)
        obj.put("label", w.label)
        obj.put("displayMode", w.displayMode.name)
        obj.put("type", when (w.type) {
            is WidgetType.Battery -> "Battery"
            is WidgetType.Clock -> "Clock"
            is WidgetType.Calendar -> "Calendar"
            is WidgetType.Photo -> "Photo"
            is WidgetType.Music -> "Music"
            is WidgetType.Note -> "Note"
            is WidgetType.Weather -> "Weather"
            is WidgetType.Stack -> "Stack"
        })
        if (w.type is WidgetType.Calendar) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Photo) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Music) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Weather) obj.put("is_wide", w.type.isWide)
        if (w.type is WidgetType.Note) {
            obj.put("is_wide", w.type.isWide)
            obj.put("note_text", w.type.text)
        }
        if (w.type is WidgetType.Stack) {
            obj.put("is_wide", w.type.isWide)
            val children = JSONArray()
            w.type.children.forEach { children.put(serializeWidgetModel(it)) }
            obj.put("children", children)
        }
        return obj
    }

    fun deserializeWidgetModel(obj: JSONObject): WidgetModel? {
        val id = obj.optString("id")
        val label = obj.optString("label")
        val mode = try {
            WidgetDisplayMode.valueOf(obj.optString("displayMode", "GLASS"))
        } catch (e: Exception) {
            WidgetDisplayMode.GLASS
        }
        val typeStr = obj.optString("type")
        val isWide = obj.optBoolean("is_wide", false)

        val type: WidgetType = when (typeStr) {
            "Battery" -> WidgetType.Battery
            "Clock" -> WidgetType.Clock
            "Calendar" -> WidgetType.Calendar(isWide)
            "Photo" -> WidgetType.Photo(isWide)
            "Music" -> WidgetType.Music(isWide)
            "Note" -> WidgetType.Note(text = obj.optString("note_text", ""), isWide = isWide)
            "Weather" -> WidgetType.Weather(isWide = obj.optBoolean("is_wide", true))
            "Stack" -> {
                val children = mutableListOf<WidgetModel>()
                val childrenArr = obj.optJSONArray("children")
                if (childrenArr != null) {
                    for (i in 0 until childrenArr.length()) {
                        deserializeWidgetModel(childrenArr.getJSONObject(i))?.let { children.add(it) }
                    }
                }
                WidgetType.Stack(children, isWide)
            }
            else -> return null
        }
        return WidgetModel(id = id, type = type, label = label, displayMode = mode)
    }

    fun deserializeAppModel(
        obj: JSONObject,
        allInstalled: List<AppModel>,
        customLabels: Map<String, String>,
        hiddenPackages: Set<String>
    ): AppModel? {
        val type = obj.optString("type", "app")
        val pkg = obj.optString("pkg", "")

        return when (type) {
            "folder" -> {
                val children = mutableListOf<AppModel>()
                val childrenArray = obj.optJSONArray("children")
                if (childrenArray != null) {
                    for (i in 0 until childrenArray.length()) {
                        deserializeAppModel(childrenArray.getJSONObject(i), allInstalled, customLabels, hiddenPackages)?.let {
                            children.add(it)
                        }
                    }
                }
                AppModel(
                    label = obj.optString("label", "Folder"),
                    isFolder = true,
                    uniqueId = obj.optString("id", "folder_${System.currentTimeMillis()}"),
                    folderItems = children
                )
            }
            "widget" -> {
                val widgetObj = obj.optJSONObject("widget")
                val widget = if (widgetObj != null) {
                    deserializeWidgetModel(widgetObj)
                } else {
                    // 相容舊格式
                    val wId = obj.optString("widget_id")
                    val wTypeStr = obj.optString("widget_type")
                    val wMode = try {
                        WidgetDisplayMode.valueOf(obj.optString("widget_mode", "GLASS"))
                    } catch(e: Exception) {
                        WidgetDisplayMode.GLASS
                    }
                    val isWide = obj.optBoolean("is_wide", false)
                    val wType: WidgetType? = when(wTypeStr) {
                        "Battery" -> WidgetType.Battery
                        "Clock" -> WidgetType.Clock
                        "Calendar" -> WidgetType.Calendar(isWide)
                        "Photo" -> WidgetType.Photo(isWide)
                        "Music" -> WidgetType.Music(isWide)
                        "Note" -> WidgetType.Note(text = obj.optString("note_text", ""), isWide = isWide)
                        else -> null
                    }
                    if (wType == null) null
                    else WidgetModel(
                        id = wId,
                        type = wType,
                        label = obj.optString("label"),
                        displayMode = wMode
                    )
                }

                if (widget == null) return null

                AppModel(
                    label = obj.optString("label"),
                    uniqueId = obj.optString("id"),
                    widget = widget
                )
            }
            "shortcut" -> {
                val shortcutId = obj.optString("shortcutId")
                val intentUri = if (obj.has("intentUri")) obj.getString("intentUri") else null
                
                if (intentUri != null) {
                    AppModel(
                        label = obj.optString("label"),
                        packageName = pkg,
                        intentUri = intentUri
                    )
                } else {
                    val baseApp = allInstalled.find { it.packageName == pkg && it.shortcutId == shortcutId } ?: return null
                    baseApp.copy(
                        label = obj.optString("label", baseApp.label),
                        isHidden = hiddenPackages.contains(baseApp.uniqueId) || hiddenPackages.contains(pkg)
                    )
                }
            }
            else -> {
                val baseApp = allInstalled.find { it.packageName == pkg && !it.isShortcut } ?: return null
                baseApp.copy(
                    label = customLabels[baseApp.uniqueId] ?: customLabels[pkg] ?: baseApp.label,
                    isHidden = hiddenPackages.contains(pkg)
                )
            }
        }
    }

    /**
     * 將所有配置導出為 JSON 字串
     */
    fun exportConfig(
        themedIcons: Boolean,
        liquidGlassDock: Boolean,
        liquidGlassHomeFolder: Boolean,
        liquidGlassAppLibraryFolder: Boolean,
        liquidGlassGlobalSearch: Boolean,
        liquidGlassAppLibrarySearch: Boolean,
        liquidGlassWidgets: Boolean,
        networkAccessEnabled: Boolean,
        liquidGlassEnabled: Boolean,
        showMinusOne: Boolean,
        showAppLibrary: Boolean,
        iconStyle: String,
        iconShape: String,
        libraryShape: String,
        searchEngineUrl: String,
        autoAddAppsToHome: Boolean,
        showStatusBar: Boolean,
        showNavigationBar: Boolean,
        pageSize: Int,
        // 新增項目
        glassParams: Map<String, Any>,
        gestures: Map<String, String>,
        appearance: Map<String, Any>,
        favorites: Set<String>,
        themeMode: String,
        appLanguage: String,
        isAmoledBlack: Boolean,
        excludedThemed: Set<String>,
        homeMenuOptions: Set<String>,
        customIconSettings: Map<String, Any>,
        password: String,
        hiddenPackages: Set<String>,
        customLabels: Map<String, String>,
        customCategories: Map<String, String>,
        userCategories: List<String>,
        categoryRenames: Map<String, String>,
        pages: List<List<AppModel>>,
        minusOneWidgets: List<WidgetModel>,
        dockPackageNames: List<String>,
        launchCounts: String?
    ): String {
        val root = JSONObject()
        val settings = JSONObject()

        // 1. 基礎設定
        settings.put("themed_icons", themedIcons)
        settings.put("liquid_glass_dock", liquidGlassDock)
        settings.put("liquid_glass_home_folder", liquidGlassHomeFolder)
        settings.put("liquid_glass_app_library_folder", liquidGlassAppLibraryFolder)
        settings.put("liquid_glass_global_search", liquidGlassGlobalSearch)
        settings.put("liquid_glass_app_library_search", liquidGlassAppLibrarySearch)
        settings.put("liquid_glass_widgets", liquidGlassWidgets)
        settings.put("network_access_enabled", networkAccessEnabled)
        settings.put("liquid_glass_enabled", liquidGlassEnabled)
        settings.put("show_minus_one", showMinusOne)
        settings.put("show_app_library", showAppLibrary)
        settings.put("icon_style", iconStyle)
        settings.put("icon_shape", iconShape)
        settings.put("library_shape", libraryShape)
        settings.put("search_engine_url", searchEngineUrl)
        settings.put("auto_add_apps_to_home", autoAddAppsToHome)
        settings.put("show_status_bar", showStatusBar)
        settings.put("show_navigation_bar", showNavigationBar)
        settings.put("theme_mode", themeMode)
        settings.put("app_language", appLanguage)
        settings.put("amoled_black", isAmoledBlack)
        settings.put("page_size", pageSize)

        // 寫入新增的詳細設定
        val glassObj = JSONObject()
        glassParams.forEach { (k, v) -> glassObj.put(k, v) }
        root.put("glass_params", glassObj)

        val gestureObj = JSONObject()
        gestures.forEach { (k, v) -> gestureObj.put(k, v) }
        root.put("gestures", gestureObj)

        val appearanceObj = JSONObject()
        appearance.forEach { (k, v) -> appearanceObj.put(k, v) }
        root.put("appearance", appearanceObj)

        val favArray = JSONArray()
        favorites.forEach { favArray.put(it) }
        root.put("favorites", favArray)

        val excludedArray = JSONArray()
        excludedThemed.forEach { excludedArray.put(it) }
        root.put("excluded_themed", excludedArray)

        val menuArray = JSONArray()
        homeMenuOptions.forEach { menuArray.put(it) }
        root.put("home_menu_options", menuArray)

        val customIconObj = JSONObject()
        customIconSettings.forEach { (k, v) -> customIconObj.put(k, v) }
        root.put("custom_icon_settings", customIconObj)
        settings.put("hidden_password", password)

        // 2. App 相關
        val hidden = JSONArray()
        hiddenPackages.forEach { hidden.put(it) }
        root.put("hidden_apps", hidden)

        val labels = JSONObject()
        customLabels.forEach { (k, v) -> labels.put(k, v) }
        root.put("custom_labels", labels)

        val categories = JSONObject()
        customCategories.forEach { (k, v) -> categories.put(k, v) }
        root.put("custom_categories", categories)

        val userCats = JSONArray()
        userCategories.forEach { userCats.put(it) }
        root.put("user_categories", userCats)

        val renames = JSONObject()
        categoryRenames.forEach { (k, v) -> renames.put(k, v) }
        root.put("category_renames", renames)

        // 3. 佈局
        val pagesArray = JSONArray()
        pages.forEach { page ->
            val pageArray = JSONArray()
            page.forEach { pageArray.put(serializeAppModel(it)) }
            pagesArray.put(pageArray)
        }
        root.put("layout", pagesArray)

        // 4. 負一屏小工具
        val minusOne = JSONArray()
        minusOneWidgets.forEach { widget ->
            minusOne.put(serializeWidgetModel(widget))
        }
        root.put("minus_one_widgets", minusOne)

        // 5. Dock & Stats
        root.put("dock", JSONArray(dockPackageNames))
        root.put("launch_counts", launchCounts ?: "")

        root.put("settings", settings)
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        return root.toString(4)
    }
}