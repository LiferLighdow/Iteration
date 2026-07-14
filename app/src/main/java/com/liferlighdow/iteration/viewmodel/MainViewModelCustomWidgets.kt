package com.liferlighdow.iteration.viewmodel

import com.liferlighdow.iteration.data.ConfigSerializer
import com.liferlighdow.iteration.data.CustomComponent
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import org.json.JSONArray
import java.util.UUID

fun MainViewModel.loadCustomWidgets() {
    val saved = prefs.getString("custom_widgets_v1", null) ?: return
    val list = mutableListOf<WidgetModel>()
    try {
        val array = JSONArray(saved)
        for (i in 0 until array.length()) {
            ConfigSerializer.deserializeWidgetModel(array.getString(i))?.let { list.add(it) }
        }
    } catch (e: Exception) { e.printStackTrace() }
    _customWidgets.value = list
}

fun MainViewModel.createCustomWidget(size: String) {
    val newWidget = WidgetModel(
        widgetType = WidgetType.Custom(size = size),
        label = "Custom Widget $size"
    )
    val newList = _customWidgets.value + newWidget
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

fun MainViewModel.saveCustomWidgets(list: List<WidgetModel>) {
    val array = JSONArray()
    list.forEach { widget ->
        array.put(ConfigSerializer.serializeWidgetModel(widget))
    }
    prefs.edit().putString("custom_widgets_v1", array.toString()).apply()
}

fun MainViewModel.deleteCustomWidget(widgetId: String) {
    val newList = _customWidgets.value.filter { it.id != widgetId }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

fun MainViewModel.updateCustomWidgetLabel(widgetId: String, newLabel: String) {
    val newList = _customWidgets.value.map {
        if (it.id == widgetId) it.copy(label = newLabel) else it
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}


fun MainViewModel.addComponentToCustomWidget(widgetId: String, type: String) {
    val newList = _customWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Custom) {
            val newComponent = when (type) {
                "TEXT" -> CustomComponent.Text(name = "Text ${widget.widgetType.components.size + 1}")
                "SHAPE" -> CustomComponent.Shape(name = "Shape ${widget.widgetType.components.size + 1}")
                "PROGRESS" -> CustomComponent.Progress(name = "Progress ${widget.widgetType.components.size + 1}")
                "IMAGE" -> CustomComponent.Image(name = "Image ${widget.widgetType.components.size + 1}")
                else -> null
            }
            if (newComponent != null) {
                widget.copy(widgetType = widget.widgetType.copy(components = widget.widgetType.components + newComponent))
            } else widget
        } else widget
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

fun MainViewModel.updateCustomWidgetGlobal(widgetId: String, scale: Float? = null, x: Float? = null, y: Float? = null) {
    val newList = _customWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Custom) {
            val newType = widget.widgetType.copy(
                globalScale = scale ?: widget.widgetType.globalScale,
                globalX = x ?: widget.widgetType.globalX,
                globalY = y ?: widget.widgetType.globalY
            )
            widget.copy(widgetType = newType)
        } else widget
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}



fun MainViewModel.updateComponentInCustomWidget(widgetId: String, updated: CustomComponent) {
    val newList = _customWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Custom) {
            val updatedComponents = widget.widgetType.components.map {
                if (it.id == updated.id) updated else it
            }
            widget.copy(widgetType = widget.widgetType.copy(components = updatedComponents))
        } else widget
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

fun MainViewModel.removeComponentFromCustomWidget(widgetId: String, componentId: String) {
    val newList = _customWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Custom) {
            val updatedComponents = widget.widgetType.components.filter { it.id != componentId }
            widget.copy(widgetType = widget.widgetType.copy(components = updatedComponents))
        } else widget
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

fun MainViewModel.moveComponentInCustomWidget(widgetId: String, componentId: String, up: Boolean) {
    val newList = _customWidgets.value.map { widget ->
        if (widget.id == widgetId && widget.widgetType is WidgetType.Custom) {
            val components = widget.widgetType.components.toMutableList()
            val index = components.indexOfFirst { it.id == componentId }
            if (index != -1) {
                val targetIndex = if (up) index - 1 else index + 1
                if (targetIndex in components.indices) {
                    val item = components.removeAt(index)
                    components.add(targetIndex, item)
                    widget.copy(widgetType = widget.widgetType.copy(components = components))
                } else widget
            } else widget
        } else widget
    }
    _customWidgets.value = newList
    saveCustomWidgets(newList)
}

