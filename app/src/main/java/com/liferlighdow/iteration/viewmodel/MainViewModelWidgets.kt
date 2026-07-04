package com.liferlighdow.iteration.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.data.ConfigSerializer
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

fun MainViewModel.loadWidgets() {
    val saved = prefs.getString("minus_one_widgets_v3", null) ?: return
    val list = mutableListOf<WidgetModel>()
    try {
        val array = JSONArray(saved)
        for (i in 0 until array.length()) {
            ConfigSerializer.deserializeWidgetModel(array.getString(i))?.let { list.add(it) }
        }
    } catch (e: Exception) { e.printStackTrace() }
    _minusOneWidgets.value = list
}

fun MainViewModel.addWidget(type: WidgetType, pageIndex: Int = -1) {
    val label = when (type) {
        is WidgetType.Battery -> "Battery"
        is WidgetType.Clock -> "Clock"
        is WidgetType.Calendar -> "Calendar"
        is WidgetType.Photo -> "Photo"
        is WidgetType.Music -> "Music"
        is WidgetType.Note -> "Note"
        is WidgetType.Weather -> "Weather"
        is WidgetType.Stack -> if (type.isWide) "Wide Widget Stacker" else "Stack"
    }

    if (pageIndex == -1) {
        // 加入到負一屏
        val newList = _minusOneWidgets.value + WidgetModel(widgetType = type, label = label)
        _minusOneWidgets.value = newList
        saveWidgets(newList)
    } else {
        // 加入到指定桌面分頁
        val widget = WidgetModel(widgetType = type, label = label)
        val appModel = AppModel(
            label = label,
            uniqueId = "widget_${widget.id}",
            widget = widget
        )
        val currentPages = _pages.value.map { it.toMutableList() }.toMutableList()
        insertAtPage(currentPages, pageIndex, appModel)
        reorganizeAllPages(currentPages)
    }
}

fun MainViewModel.removeWidget(id: String) {
    val newList = _minusOneWidgets.value.filter { it.id != id }
    _minusOneWidgets.value = newList
    saveWidgets(newList)
}

fun MainViewModel.updateWidgetDisplayMode(id: String, mode: WidgetDisplayMode) {
    // 1. 更新負一屏的小工具
    val newList = _minusOneWidgets.value.map {
        if (it.id == id) it.copy(displayMode = mode) else it
    }
    _minusOneWidgets.value = newList
    saveWidgets(newList)

    // 2. 更新桌面分頁上的小工具
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            if (item.widget?.id == id) {
                item.copy(widget = item.widget.copy(displayMode = mode))
            } else {
                item
            }
        }
    }
    _pages.value = currentPages
}

fun MainViewModel.saveWidgets(list: List<WidgetModel>) {
    val array = JSONArray()
    list.forEach { widget ->
        array.put(ConfigSerializer.serializeWidgetModel(widget))
    }
    prefs.edit().putString("minus_one_widgets_v3", array.toString()).apply()
}

fun MainViewModel.updateStackChildren(stackId: String, children: List<WidgetModel>) {
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            val w = item.widget
            if (w?.id == stackId && w.widgetType is WidgetType.Stack) {
                val isWide = w.widgetType.isWide
                item.copy(widget = w.copy(widgetType = WidgetType.Stack(children, isWide)))
            } else item
        }
    }
    _pages.value = currentPages

    val newMinusOne = _minusOneWidgets.value.map { widget ->
        if (widget.id == stackId && widget.widgetType is WidgetType.Stack) {
            val isWide = widget.widgetType.isWide
            widget.copy(widgetType = WidgetType.Stack(children, isWide))
        } else widget
    }
    _minusOneWidgets.value = newMinusOne

    saveLayout()
    saveWidgets(newMinusOne)
}

fun MainViewModel.updateNoteText(widgetId: String, text: String) {
    val currentPages = _pages.value.map { page ->
        page.map { item ->
            val w = item.widget
            if (w?.id == widgetId && w.widgetType is WidgetType.Note) {
                item.copy(widget = w.copy(widgetType = w.widgetType.copy(text = text)))
            } else item
        }
    }
    _pages.value = currentPages

    val newMinusOne = _minusOneWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Note) {
            widget.copy(widgetType = widget.widgetType.copy(text = text))
        } else widget
    }
    _minusOneWidgets.value = newMinusOne

    saveLayout()
    saveWidgets(newMinusOne)
}

fun MainViewModel.saveWidgetPhoto(widgetId: String, bitmap: Bitmap) {
    val file = File(widgetPhotoDir, "$widgetId.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    _minusOneWidgets.value = _minusOneWidgets.value.toList()
}

fun MainViewModel.getWidgetPhoto(widgetId: String): Bitmap? {
    val file = File(widgetPhotoDir, "$widgetId.png")
    return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
}
