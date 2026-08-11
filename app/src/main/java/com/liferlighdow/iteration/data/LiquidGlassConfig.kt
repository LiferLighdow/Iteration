package com.liferlighdow.iteration.data

import kotlinx.serialization.Serializable

@Serializable
enum class LiquidGlassComponent(val key: String) {
    GLOBAL("global"),
    DOCK("dock"),
    FOLDER("folder"),
    SEARCH("search"),
    WIDGET("widget"),
    BUTTON("button"),
    APP_ICON("icon")
}

@Serializable
data class LiquidGlassColorConfig(
    val enabled: Boolean = false,
    val blur: Float? = null,
    val refractionHeight: Float? = null,
    val refractionAmount: Float? = null,
    val chromaticAberration: Boolean? = null,
    val hue: Float = 0f,
    val saturation: Float = 1f,
    val brightness: Float = 1f,
    val alpha: Float = 0.3f
)
