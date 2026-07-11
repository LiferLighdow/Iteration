package com.liferlighdow.iteration.data

import android.graphics.Bitmap

data class CalendarEventModel(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val calendarName: String?
)

data class FileModel(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long
)
