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
    @Serializable @SerialName("Clock") object Clock : WidgetType()
    @Serializable @SerialName("Calendar") data class Calendar(val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Photo") data class Photo(val isWide: Boolean = false, val uri: String? = null) : WidgetType()
    @Serializable @SerialName("Music") data class Music(val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Note") data class Note(val text: String = "", val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("Weather") data class Weather(val isWide: Boolean = true) : WidgetType()
    @Serializable @SerialName("ToDoList") data class ToDoList(val tasks: List<TodoTask> = emptyList(), val isWide: Boolean = true) : WidgetType()
    @Serializable @SerialName("Stack") data class Stack(val children: List<WidgetModel> = emptyList(), val isWide: Boolean = false) : WidgetType()
    @Serializable @SerialName("RSS") data class RSS(val url: String = "", val isWide: Boolean = true, val isTall: Boolean = false) : WidgetType()
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
