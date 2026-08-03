package com.liferlighdow.iteration.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.ui.showNativeUninstallDialog
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.getIcon
import com.liferlighdow.iteration.viewmodel.deletePWA

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrozenAppsManagerDialog(
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onUnfreezeClick: (AppModel) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val iconShape by viewModel.iconShape.collectAsState()
    
    val frozenApps = remember(allApps) { 
        allApps.filter { it.isFrozen }
            .distinctBy { "${it.packageName}@${it.userId}" } 
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.frozen_apps_title)) },
        text = {
            if (frozenApps.isEmpty()) {
                Text(stringResource(R.string.no_frozen_apps))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(frozenApps, key = { it.uniqueId }) { app ->
                        var showMenu by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) },
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f)) },
                            leadingContent = {
                                val icon = viewModel.getIcon(app.uniqueId)
                                if (icon != null) {
                                    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(40.dp * 0.238f)
                                    val colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                                    Image(
                                        bitmap = icon, 
                                        contentDescription = null, 
                                        colorFilter = colorFilter,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(shape)
                                            .border(
                                                width = 0.5.dp,
                                                color = Color.White.copy(alpha = 0.3f),
                                                shape = shape
                                            )
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.combinedClickable(
                                onClick = { onUnfreezeClick(app) },
                                onLongClick = { showMenu = true }
                            )
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.unfreeze)) },
                                onClick = { onUnfreezeClick(app); showMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PrivateSpaceManagerDialog(
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onAppClick: (AppModel, Offset) -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val mContext = LocalContext.current
    val privateApps = remember(allApps) { allApps.filter { it.isPrivate && !it.uniqueId.startsWith("private_seed") } }
    val isLocked by viewModel.isPrivateSpaceLocked.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.private_space_title), modifier = Modifier.weight(1f))
                if (!isLocked) {
                    IconButton(onClick = { 
                        allApps.find { it.isPrivate }?.userId?.let { viewModel.lockPrivateSpace(it) }
                    }) {
                        Icon(Icons.Default.LockOpen, null)
                    }
                }
            }
        },
        text = {
            if (isLocked) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clickable {
                            allApps.find { it.isPrivate }?.userId?.let { viewModel.unlockPrivateSpace(it) }
                        },
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.private_space_locked_desc), textAlign = TextAlign.Center)
                }
            } else {
                if (privateApps.isEmpty()) {
                    Text("No apps in Private Space.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(privateApps, key = { it.uniqueId }) { app ->
                            var showMenu by remember { mutableStateOf(false) }
                            var itemPosition by remember { mutableStateOf(Offset.Zero) }
                            Box(modifier = Modifier.onGloballyPositioned { itemPosition = it.positionInRoot() }) {
                                ListItem(
                                    headlineContent = { Text(app.label) },
                                    leadingContent = {
                                        val icon = viewModel.getIcon(app.uniqueId)
                                        if (icon != null) {
                                            val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(40.dp * 0.238f)
                                            Image(
                                                bitmap = icon, 
                                                contentDescription = null, 
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(shape)
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = Color.White.copy(alpha = 0.3f),
                                                        shape = shape
                                                    )
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onAppClick(app, itemPosition) },
                                        onLongClick = { showMenu = true }
                                    )
                                )
                                
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_app_info)) },
                                        leadingIcon = { Icon(Icons.Default.Info, null) },
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.parse("package:${app.packageName}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            mContext.startActivity(intent)
                                            showMenu = false
                                        }
                                    )
                                    if (!app.isSystem) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.menu_uninstall)) },
                                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                if (app.isPWA) {
                                                    showNativeUninstallDialog(mContext, app.label) {
                                                        viewModel.deletePWA(app)
                                                    }
                                                    showMenu = false
                                                    return@DropdownMenuItem
                                                }
                                                try {
                                                    val intent = Intent(Intent.ACTION_DELETE).apply {
                                                        data = Uri.fromParts("package", app.packageName, null)
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    mContext.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Log.e("Iteration", "Uninstall failed", e)
                                                }
                                                showMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}
