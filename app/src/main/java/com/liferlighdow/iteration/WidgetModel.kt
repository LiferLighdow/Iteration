package com.liferlighdow.iteration

import java.util.UUID

sealed class WidgetType {
    object Battery : WidgetType()
    object Clock : WidgetType()
    data class Calendar(val isWide: Boolean = false) : WidgetType()
    data class Photo(val isWide: Boolean = false) : WidgetType()
    data class Music(val isWide: Boolean = false) : WidgetType()
    data class Stack(val children: List<WidgetModel> = emptyList()) : WidgetType()
}

enum class WidgetDisplayMode {
    GLASS, COLOR
}

data class WidgetModel(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType,
    val label: String,
    val displayMode: WidgetDisplayMode = WidgetDisplayMode.GLASS
)
