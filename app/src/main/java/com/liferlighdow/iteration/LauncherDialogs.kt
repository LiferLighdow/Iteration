package com.liferlighdow.iteration

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap

@Composable
fun MultiAppPickerDialog(
    allApps: List<AppModel>,
    iconShape: IconShape = IconShape.DEFAULT,
    initialSelectedPackages: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onAppsSelected: (List<String>) -> Unit
) {
    val visibleApps = remember(allApps) { allApps.filter { !it.isHidden } }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(initialSelectedPackages) } }
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.select_apps), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onAppsSelected(selectedPackages.toList()) }) { Text(stringResource(R.string.done)) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(visibleApps, key = { it.uniqueId }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap()
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
                                    )
                                }
                            },
                            trailingContent = {
                                Checkbox(
                                    checked = selectedPackages.contains(app.packageName),
                                    onCheckedChange = { if (it) selectedPackages.add(app.packageName) else selectedPackages.remove(app.packageName) }
                                )
                            },
                            modifier = Modifier.clickable {
                                if (selectedPackages.contains(app.packageName)) selectedPackages.remove(app.packageName)
                                else selectedPackages.add(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickEditDialog(
    app: AppModel,
    viewModel: MainViewModel,
    iconShape: IconShape = IconShape.DEFAULT,
    onDismiss: () -> Unit
) {
    var labelText by remember { mutableStateOf(app.label) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(18.dp)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${app.label}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(shape)
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (app.processedIcon != null) {
                        Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Label") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.setCustomLabel(app.packageName, labelText)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.setCustomLabel(app.packageName, "")
                viewModel.resetCustomIcon(app.packageName)
                onDismiss()
            }) { Text("Reset") }
        }
    )

    if (pickedImageUri != null) {
        IconCropperDialog(
            uri = pickedImageUri!!,
            onDismiss = { pickedImageUri = null },
            onConfirm = { croppedBitmap ->
                viewModel.setCustomIcon(app.packageName, croppedBitmap)
                pickedImageUri = null
            }
        )
    }
}

@Composable
fun AppPickerDialog(
    allApps: List<AppModel>,
    iconShape: IconShape = IconShape.DEFAULT,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit
) {
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.select_app), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(allApps, key = { it.uniqueId }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val icon = app.processedIcon ?: app.icon?.toBitmap()?.asImageBitmap()
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onAppSelected(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}
