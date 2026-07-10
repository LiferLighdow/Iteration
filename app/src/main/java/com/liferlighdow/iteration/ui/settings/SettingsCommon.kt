package com.liferlighdow.iteration.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY, ICON_THEME, DOCK, LIQUID_GLASS, GESTURES, SEARCH, PERMISSIONS, MANUALS, GLOBAL_SEARCH_MANUAL, ICON_ENGINE_MANUAL, LANGUAGE, ADVANCED, PWA_MAKER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        )
    )
}

@Composable
fun SettingsItem(
    headline: String,
    supporting: String? = null,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
) {
    ListItem(
        headlineContent = { Text(headline, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        },
        trailingContent = trailing,
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
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
        Slider(value = h, onValueChange = { h = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..360f)
        
        Text(stringResource(R.string.saturation, (s * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = s, onValueChange = { s = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
        
        Text(stringResource(R.string.brightness, (v * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = v, onValueChange = { v = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
        
        Text(stringResource(R.string.alpha, (a * 100).toInt()), style = MaterialTheme.typography.labelSmall)
        Slider(value = a, onValueChange = { a = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
    }
}
