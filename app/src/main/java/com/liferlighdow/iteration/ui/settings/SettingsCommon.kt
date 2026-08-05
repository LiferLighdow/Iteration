package com.liferlighdow.iteration.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liferlighdow.iteration.R

data class SettingsMetadata(
    val label: String,
    val supporting: String,
    val icon: ImageVector,
    val iconColor: Color,
    val action: () -> Unit,
    val isLiquidGlass: Boolean = false,
    val isHideApps: Boolean = false,
    val isExport: Boolean = false,
    val isImport: Boolean = false,
    val isRestart: Boolean = false
)

enum class SettingsPage {
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY, ICON_THEME, DOCK, LIQUID_GLASS, GESTURES, SEARCH, SEARCH_ENGINE, PERMISSIONS, MANUALS, GLOBAL_SEARCH_MANUAL, ICON_ENGINE_MANUAL, DOCK_MANUAL, LANGUAGE, ADVANCED, PWA_MAKER, PWA_MANAGE, WIDGET_MAKER, WIDGET_WORKSHOP, GREENIFY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IterationSearchBar(
    query: String,
    placeholder: String = stringResource(R.string.search_hint),
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGlass: Boolean = false,
    backdrop: com.kyant.backdrop.Backdrop? = null,
    focusRequester: FocusRequester? = null
) {
    val content = @Composable {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            placeholder = { Text(placeholder, color = (if (isGlass) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = if (isGlass) Color.White else MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )
    }

    if (isGlass) {
        com.liferlighdow.iteration.ui.components.GlassBox(
            modifier = modifier.height(56.dp),
            backdrop = backdrop,
            cornerRadius = 28.dp,
            fallbackAlpha = 0.2f
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            content()
        }
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    supportingText: String? = null,
    icon: ImageVector? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingContent ?: icon?.let { { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease))
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase))
            }
        }
    }
}

@Composable
fun SettingCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    headline: String,
    supporting: String? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
) {
    ListItem(
        headlineContent = { Text(headline, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = icon?.let {
            {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(it, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
            }
        },
        trailingContent = trailing,
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(content = content)
    }
}

@Composable
fun PaddingRemaining(padding: androidx.compose.ui.unit.Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(padding)) {
        content()
    }
}

@Composable
fun ColorPicker(
    initialColor: Int,
    onColorChanged: (Int) -> Unit
) {
    var hexText by remember(initialColor) { mutableStateOf(String.format("%08X", initialColor)) }
    val hsv = remember(initialColor) {
        val res = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, res)
        res
    }
    var h by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var s by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var v by remember(initialColor) { mutableFloatStateOf(hsv[2]) }
    var a by remember(initialColor) { mutableFloatStateOf(android.graphics.Color.alpha(initialColor) / 255f) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(initialColor), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = hexText,
            onValueChange = {
                hexText = it
                if (it.length == 8) {
                    try {
                        val color = android.graphics.Color.parseColor("#$it")
                        onColorChanged(color)
                    } catch (e: Exception) {}
                }
            },
            label = { Text(stringResource(R.string.hex_color_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.hue, h.toInt()), style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { h = (h - 1f).coerceAtLeast(0f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
            }
            Slider(value = h, onValueChange = { h = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..360f, modifier = Modifier.weight(1f))
            IconButton(onClick = { h = (h + 1f).coerceAtMost(360f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(stringResource(R.string.saturation, (s * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { s = (s - 0.01f).coerceAtLeast(0f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
            }
            Slider(value = s, onValueChange = { s = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            IconButton(onClick = { s = (s + 0.01f).coerceAtMost(1f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(stringResource(R.string.brightness, (v * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { v = (v - 0.01f).coerceAtLeast(0f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
            }
            Slider(value = v, onValueChange = { v = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            IconButton(onClick = { v = (v + 0.01f).coerceAtMost(1f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Text(stringResource(R.string.alpha, (a * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { a = (a - 0.01f).coerceAtLeast(0f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary)
            }
            Slider(value = a, onValueChange = { a = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
            IconButton(onClick = { a = (a + 0.01f).coerceAtMost(1f); onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
