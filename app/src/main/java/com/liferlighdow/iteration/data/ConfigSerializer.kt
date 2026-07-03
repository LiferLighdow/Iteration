package com.liferlighdow.iteration.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ConfigSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true
        classDiscriminator = "type"
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
            // 1. 嘗試直接解析 (v3 格式)
            json.decodeFromString<LauncherConfig>(jsonStr)
        } catch (e: Exception) {
            try {
                // 2. 如果失敗，嘗試將舊格式字串 (v2) 預處理為 v3
                val migratedJson = migrateJsonV2ToV3(jsonStr)
                json.decodeFromString<LauncherConfig>(migratedJson)
            } catch (e2: Exception) {
                e2.printStackTrace()
                null
            }
        }
    }

    /**
     * 針對舊有的 JSON 備份進行欄位對應轉換 (label -> l, pkg -> p 等)
     */
    private fun migrateJsonV2ToV3(jsonStr: String): String {
        // 使用簡單的字串替換來處理鍵名，這比手動操作 JSONObject 快且乾淨
        return jsonStr
            .replace("\"label\":", "\"l\":")
            .replace("\"pkg\":", "\"p\":")
            .replace("\"isHidden\":", "\"h\":")
            .replace("\"category\":", "\"c\":")
            .replace("\"displayCategory\":", "\"dc\":")
            .replace("\"isFolder\":", "\"f\":")
            .replace("\"children\":", "\"ch\":")
            .replace("\"widget\":", "\"w\":")
            .replace("\"userId\":", "\"u\":")
            .replace("\"type\":", "\"t\":") // WidgetType 的 type
            .replace("\"displayMode\":", "\"m\":") // WidgetDisplayMode
    }

    // --- Helper methods for single objects ---

    fun serializeAppModel(app: AppModel): String = json.encodeToString(app)
    fun deserializeAppModel(jsonStr: String): AppModel? = try { json.decodeFromString(jsonStr) } catch(_: Exception) { null }

    fun serializeWidgetModel(widget: WidgetModel): String = json.encodeToString(widget)
    fun deserializeWidgetModel(jsonStr: String): WidgetModel? = try { json.decodeFromString(jsonStr) } catch(_: Exception) { null }
}
