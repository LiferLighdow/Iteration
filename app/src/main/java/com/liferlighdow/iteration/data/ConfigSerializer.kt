package com.liferlighdow.iteration.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ConfigSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true // 強制將不匹配的值轉換為預設值，增加容錯
        classDiscriminator = "type" // 對齊密封類別的判斷欄位
    }

    /**
     * 將所有配置導出為 JSON 字串
     */
    fun exportConfig(config: LauncherConfig): String {
        return json.encodeToString(config)
    }

    /**
     * 從 JSON 字串解析配置
     */
    fun deserializeConfig(jsonStr: String): LauncherConfig? {
        return try {
            json.decodeFromString<LauncherConfig>(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- Helper methods for single objects (Backward compatibility for internal storage) ---

    fun serializeAppModel(app: AppModel): String = json.encodeToString(app)
    fun deserializeAppModel(jsonStr: String): AppModel? = try { json.decodeFromString(jsonStr) } catch(e: Exception) { null }

    fun serializeWidgetModel(widget: WidgetModel): String = json.encodeToString(widget)

    fun deserializeWidgetModel(jsonStr: String): WidgetModel? {
        return try {
            // 嘗試使用新格式解析
            json.decodeFromString<WidgetModel>(jsonStr)
        } catch (e: Exception) {
            // 如果失敗，可能是舊格式，這裡可以根據需要手動映射
            // 或者使用更寬鬆的解析方式
            null
        }
    }

    // 針對舊有 JSONObject 結構的相容方法
    fun deserializeWidgetFromObject(obj: org.json.JSONObject): WidgetModel? {
        val jsonStr = obj.toString()
        // 舊版結構中，widget type 可能直接就在 type 欄位
        // 這裡我們嘗試將其包裝成符合 kotlinx-serialization 預期的結構
        return deserializeWidgetModel(jsonStr)
    }
}
