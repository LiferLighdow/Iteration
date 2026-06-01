package com.liferlighdow.iteration

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsNavigation()
            }
        }
    }
}

enum class SettingsPage {
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON
}

@Composable
fun SettingsNavigation() {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    val context = LocalContext.current
    
    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        currentPage = SettingsPage.MAIN
    }

    when (currentPage) {
        SettingsPage.MAIN -> SettingsMainScreen(
            onBack = { (context as? ComponentActivity)?.finish() },
            onNavigateToHideApps = { currentPage = SettingsPage.HIDE_APPS },
            onNavigateToRenameApps = { currentPage = SettingsPage.RENAME_APPS },
            onNavigateToChangeIcon = { currentPage = SettingsPage.CHANGE_ICON }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.RENAME_APPS -> RenameAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CHANGE_ICON -> ChangeIconScreen(onBack = { currentPage = SettingsPage.MAIN })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onBack: () -> Unit, 
    onNavigateToHideApps: () -> Unit, 
    onNavigateToRenameApps: () -> Unit,
    onNavigateToChangeIcon: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("Hide Apps") },
                    supportingContent = { Text("Manage hidden applications and security") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToHideApps() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Rename Apps") },
                    supportingContent = { Text("Customize app labels on home screen") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToRenameApps() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Change Apps Icon") },
                    supportingContent = { Text("Customize app icons with your images") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToChangeIcon() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    
    var selectedApp by remember { mutableStateOf<AppModel?>(null) }
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Apps Icon") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            items(allApps) { app ->
                ListItem(
                    headlineContent = { Text(app.label) },
                    leadingContent = {
                        if (app.processedIcon != null) {
                            Image(
                                bitmap = app.processedIcon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                            )
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.resetCustomIcon(app.packageName) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restore Default")
                            }
                            IconButton(onClick = { 
                                selectedApp = app
                                launcher.launch("image/*")
                            }) {
                                Icon(Icons.Default.Image, contentDescription = "Change Icon")
                            }
                        }
                    }
                )
            }
        }

        if (pickedImageUri != null && selectedApp != null) {
            IconCropperDialog(
                uri = pickedImageUri!!,
                onDismiss = { pickedImageUri = null },
                onConfirm = { croppedBitmap ->
                    viewModel.setCustomIcon(selectedApp!!.packageName, croppedBitmap)
                    pickedImageUri = null
                    selectedApp = null
                }
            )
        }
    }
}

@Composable
fun IconCropperDialog(uri: Uri, onDismiss: () -> Unit, onConfirm: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val originalBitmap = remember(uri) {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } ?: return

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crop Icon") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Drag to move, pinch to scale", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(44.dp))
                        .background(Color.LightGray)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale *= zoom
                                offset += pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = originalBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val size = 256
                val cropped = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(cropped)
                val matrix = Matrix()
                val viewSize = 200f
                val scaleFactor = (size / viewSize)
                val centerX = originalBitmap.width / 2f
                val centerY = originalBitmap.height / 2f
                matrix.postTranslate(-centerX, -centerY)
                matrix.postScale(scale * (size.toFloat() / originalBitmap.width), scale * (size.toFloat() / originalBitmap.width))
                matrix.postTranslate(size / 2f + offset.x * scaleFactor, size / 2f + offset.y * scaleFactor)
                canvas.drawBitmap(originalBitmap, matrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
                onConfirm(cropped)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameAppsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    var editingApp by remember { mutableStateOf<AppModel?>(null) }
    var newLabel by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rename Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            items(allApps) { app ->
                ListItem(
                    headlineContent = { Text(app.label) },
                    leadingContent = {
                        if (app.processedIcon != null) {
                            Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.setCustomLabel(app.packageName, "") }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Restore Default")
                            }
                            IconButton(onClick = { editingApp = app; newLabel = app.label }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                        }
                    }
                )
            }
        }
        if (editingApp != null) {
            AlertDialog(
                onDismissRequest = { editingApp = null },
                title = { Text("Rename ${editingApp?.label}") },
                text = { OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text("New Label") }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        editingApp?.let { viewModel.setCustomLabel(it.packageName, newLabel) }
                        editingApp = null
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { editingApp = null }) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    var currentPassword by remember { mutableStateOf(viewModel.getPassword()) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hide Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Security", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { 
                        currentPassword = it
                        viewModel.setPassword(it)
                    },
                    label = { Text("Hidden Apps Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    }
                )
            }
            item {
                HorizontalDivider()
                Text(text = "Select apps to hide", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            items(allApps) { app ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.toggleHiddenApp(app.packageName) }, verticalAlignment = Alignment.CenterVertically) {
                    if (app.processedIcon != null) {
                        Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
                    }
                    Text(text = app.label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Checkbox(checked = app.isHidden, onCheckedChange = { viewModel.toggleHiddenApp(app.packageName) })
                }
            }
        }
    }
}
