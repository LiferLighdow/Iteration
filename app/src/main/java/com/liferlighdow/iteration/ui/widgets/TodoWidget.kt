package com.liferlighdow.iteration.ui.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel

@Composable
fun TodoWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false,
    drawFrame: Boolean = true
) {
    val todoType = widget.type as? WidgetType.ToDoList ?: return
    val isWide = todoType.isWide
    val tasks = todoType.tasks

    val isGlass = displayMode == WidgetDisplayMode.GLASS
    val containerColor = if (isGlass) null else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    WidgetContainer(
        displayMode = displayMode,
        modifier = modifier,
        aspectRatio = if (isWide) 2f else 1f,
        containerColor = containerColor,
        backdrop = backdrop,
        isMinusOnePage = isMinusOnePage,
        drawFrame = drawFrame
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "ToDo List",
                style = MaterialTheme.typography.titleMedium.withGlassShadow(isGlass),
                color = contentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No tasks. Tap to edit.",
                        style = MaterialTheme.typography.bodyMedium.withGlassShadow(isGlass),
                        color = contentColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tasks) { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = contentColor.copy(alpha = if (task.isDone) 0.5f else 1.0f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodySmall.withGlassShadow(isGlass).copy(
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                                ),
                                color = contentColor.copy(alpha = if (task.isDone) 0.5f else 1.0f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
