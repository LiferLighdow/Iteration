package com.liferlighdow.iteration.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.addEmptyPage
import com.liferlighdow.iteration.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherMenu(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    isMultiplePages: Boolean,
    isDefaultLauncher: Boolean,
    onAddWidgetClick: () -> Unit,
    onAddShortcutClick: () -> Unit,
    onCreateFolderClick: () -> Unit,
    onWallpaperClick: () -> Unit,
    onSetDefaultClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeletePageClick: () -> Unit
) {
    if (isVisible) {
        val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
        val desktopMenuOptions by viewModel.desktopMenuOptions.collectAsState()
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                if (!isDesktopLocked) {
                    if (desktopMenuOptions.contains("add_widget")) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_add_widget)) },
                            leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                onAddWidgetClick()
                                onDismiss()
                            }
                        )
                    }
                    if (desktopMenuOptions.contains("add_shortcut")) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_add_shortcut)) },
                            leadingContent = { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                onAddShortcutClick()
                                onDismiss()
                            }
                        )
                    }
                    if (desktopMenuOptions.contains("new_folder")) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_new_folder)) },
                            leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.clickable {
                                onCreateFolderClick()
                                onDismiss()
                            }
                        )
                    }

                    if (isMultiplePages && desktopMenuOptions.contains("delete_page")) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.menu_delete_page), color = MaterialTheme.colorScheme.error) },
                            leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.clickable {
                                onDeletePageClick()
                                onDismiss()
                            }
                        )
                    }
                }

                if (desktopMenuOptions.contains("wallpaper")) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_wallpaper)) },
                        leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            onWallpaperClick()
                            onDismiss()
                        }
                    )
                }

                if (!isDefaultLauncher && !isDesktopLocked && desktopMenuOptions.contains("set_default")) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_set_default)) },
                        leadingContent = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            onSetDefaultClick()
                            onDismiss()
                        }
                    )
                }

                if (desktopMenuOptions.contains("settings")) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_launcher_settings)) },
                        leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            onSettingsClick()
                            onDismiss()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
