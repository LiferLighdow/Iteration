package com.liferlighdow.iteration.ui.dialogs

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.*

@Composable
fun ShortcutPickerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    val allApps by viewModel.allApps.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()

    // 篩選出含有快捷方式的 App 列表
    val appsWithShortcuts by produceState<List<AppModel>>(initialValue = emptyList(), allApps) {
        value = withContext(Dispatchers.IO) {
            allApps.filter { app ->
                !app.isFolder && viewModel.getAppShortcuts(app.packageName, app.userId).isNotEmpty()
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedApp != null) {
                        IconButton(onClick = { selectedApp = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    }
                    Text(
                        text = if (selectedApp == null) stringResource(R.string.select_app) else selectedApp!!.label,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                if (selectedApp == null) {
                    // 第一步：選擇 App
                    var searchQuery by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val filteredApps = remember(appsWithShortcuts, searchQuery) {
                        appsWithShortcuts.filter { it.label.contains(searchQuery, ignoreCase = true) }
                    }

                    LazyColumn {
                        items(filteredApps, key = { it.uniqueId }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label) },
                                leadingContent = {
                                    val icon = viewModel.getIcon(app.uniqueId)
                                    if (icon != null) {
                                        Image(
                                            bitmap = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp).clip(if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)).background(Color.White)
                                        )
                                    }
                                },
                                modifier = Modifier.clickable { selectedApp = app }
                            )
                        }
                    }
                } else {
                    // 第二步：選擇該 App 的 Shortcuts
                    val shortcuts = remember(selectedApp) {
                        viewModel.getAppShortcuts(selectedApp!!.packageName, selectedApp!!.userId)
                    }

                    if (shortcuts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_shortcuts_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn {
                            items(shortcuts) { shortcut ->
                                ListItem(
                                    headlineContent = { 
                                        @Suppress("NewApi")
                                        Text(shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut") 
                                    },
                                    leadingContent = {
                                        val icon = viewModel.getShortcutIcon(shortcut)
                                        if (icon != null) {
                                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(32.dp))
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        @Suppress("NewApi")
                                        val shortcutId = shortcut.id
                                        @Suppress("NewApi")
                                        val label = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "Shortcut"
                                        
                                        // 保存圖標並加入桌面
                                        viewModel.viewModelScope.launch {
                                            val icon = viewModel.getShortcutIcon(shortcut)
                                            if (icon != null) {
                                                val fileSafeId = "shortcut_${selectedApp!!.packageName}_${shortcutId}"
                                                    .replace("/", "_").replace(":", "_").replace("@", "_")
                                                viewModel.saveIconToDisk(icon, java.io.File(viewModel.processedIconCacheDir, "${fileSafeId}_${viewModel.currentStyleSuffix}.png"))
                                            }
                                            viewModel.addShortcutToHome(selectedApp!!.packageName, shortcutId, label)
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
