package com.liferlighdow.iteration.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_edit_mode)) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        viewModel.setEditMode(true)
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_add_widget)) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onAddWidgetClick()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_add_shortcut)) },
                    leadingContent = { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onAddShortcutClick()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_new_folder)) },
                    leadingContent = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onCreateFolderClick()
                        onDismiss()
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_add_page)) },
                    leadingContent = { Icon(Icons.Default.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        viewModel.addEmptyPage()
                        onDismiss()
                    }
                )

                if (isMultiplePages) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_delete_page), color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            onDeletePageClick()
                            onDismiss()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_wallpaper)) },
                    leadingContent = { Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onWallpaperClick()
                        onDismiss()
                    }
                )

                if (!isDefaultLauncher) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.menu_set_default)) },
                        leadingContent = { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            onSetDefaultClick()
                            onDismiss()
                        }
                    )
                }

                ListItem(
                    headlineContent = { Text(stringResource(R.string.menu_launcher_settings)) },
                    leadingContent = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        onSettingsClick()
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
