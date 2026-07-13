package com.liferlighdow.iteration.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.util.UUID

@Serializable
data class TodoTask(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("t") val text: String,
    @SerialName("d") val isDone: Boolean = false
)

@Serializable
sealed class WidgetType {
    @Serializable @SerialName("Battery") object Battery : WidgetType()
    @Serializable @SerialName("Clock") data class Clock(val isDigital: Boolean = false) : WidgetType()
    @Serializable @SerialName("Calendar") data class Calendar(val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Photo") data class Photo(val isWide: Boolean = false, val uri: String? = null) : WidgetType()
    @Serializable @SerialName("Music") data class Music(val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Note") data class Note(val text: String = "", val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Weather") data class Weather(val isWide: Boolean = true) : WidgetType()
    @Serializable @SerialName("ToDoList") data class ToDoList(val tasks: List<TodoTask> = emptyList(), val isWide: Boolean = true) : WidgetType()
    @Serializable @SerialName("Stack") data class Stack(val children: List<WidgetModel> = emptyList(), val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("RSS") data class RSS(val url: String = "", val isWide: Boolean = true, val isTall: Boolean = false) : WidgetType()
    @Serializable @SerialName("InfoHub") object InfoHub : WidgetType()
    @Serializable @SerialName("Custom") data class Custom(val size: String, val components: List<CustomComponent> = emptyList()) : WidgetType()
}

@Serializable
sealed class CustomComponent {
    abstract val id: String
    abstract val name: String
    abstract val x: Float
    abstract val y: Float
    abstract val isVisible: Boolean

    @Serializable @SerialName("Text")
    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Text",
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val isVisible: Boolean = true,
        val content: String = "Hello World",
        val fontSize: Int = 16,
        val color: Int = 0xFFFFFFFF.toInt()
    ) : CustomComponent()

    @Serializable @SerialName("Shape")
    data class Shape(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String = "Shape",
        override val x: Float = 0f,
        override val y: Float = 0f,
        override val isVisible: Boolean = true,
        val width: Float = 100f,
        val height: Float = 100f,
        val color: Int = 0x88FFFFFF.toInt(),
        val cornerRadius: Float = 12f
    ) : CustomComponent()
}


@Serializable
enum class WidgetDisplayMode { GLASS, COLOR }

@Serializable
enum class WeatherProvider { MET_NORWAY, OPEN_METEO }

@Serializable
data class WidgetModel(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("t") val widgetType: WidgetType,
    @SerialName("l") val label: String,
    @SerialName("m") val displayMode: WidgetDisplayMode = WidgetDisplayMode.GLASS
) {
    val type: WidgetType get() = widgetType
}

data class CalendarEvent(val title: String, val startTime: Long, val endTime: Long)

data class DailyWeather(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherCode: Int
)

data class WeatherInfo(
    val currentTemp: Double,
    val daily: List<DailyWeather>,
    val cityName: String = "Taipei"
)
