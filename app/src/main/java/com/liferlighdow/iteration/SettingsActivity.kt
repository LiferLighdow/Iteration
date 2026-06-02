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
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope

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
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY
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
            onNavigateToChangeIcon = { currentPage = SettingsPage.CHANGE_ICON },
            onNavigateToAppLibrary = { currentPage = SettingsPage.APP_LIBRARY }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.RENAME_APPS -> RenameAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CHANGE_ICON -> ChangeIconScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.APP_LIBRARY -> AppLibrarySettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search apps...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    onBack: () -> Unit, 
    onNavigateToHideApps: () -> Unit, 
    onNavigateToRenameApps: () -> Unit,
    onNavigateToChangeIcon: () -> Unit,
    onNavigateToAppLibrary: () -> Unit
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
            item {
                ListItem(
                    headlineContent = { Text("App Library Settings") },
                    supportingContent = { Text("Manage app categories and sections") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToAppLibrary() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLibrarySettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    var selectingAppForCategory by remember { mutableStateOf<AppModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Library Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 分類管理區
            Text(
                "Custom Categories", 
                style = MaterialTheme.typography.titleSmall, 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(userCategories) { category ->
                    ListItem(
                        headlineContent = { Text(category) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteUserCategory(category) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    )
                }
            }
            
            HorizontalDivider()
            
            Text(
                "Assign Apps to Categories", 
                style = MaterialTheme.typography.titleSmall, 
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )

            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.packageName }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = { Text("Category: ${app.displayCategory}") },
                        leadingContent = {
                            if (app.processedIcon != null) {
                                Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
                            }
                        },
                        modifier = Modifier.clickable { selectingAppForCategory = app }
                    )
                }
            }
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add New Category") },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addUserCategory(newCategoryName)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
            )
        }

        if (selectingAppForCategory != null) {
            AlertDialog(
                onDismissRequest = { selectingAppForCategory = null },
                title = { Text("Select Category for ${selectingAppForCategory?.label}") },
                text = {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text("Default (System)") },
                                modifier = Modifier.clickable {
                                    viewModel.setAppCategory(selectingAppForCategory!!.packageName, "")
                                    selectingAppForCategory = null
                                }
                            )
                        }
                        items(userCategories) { category ->
                            ListItem(
                                headlineContent = { Text(category) },
                                modifier = Modifier.clickable {
                                    viewModel.setAppCategory(selectingAppForCategory!!.packageName, category)
                                    selectingAppForCategory = null
                                }
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { selectingAppForCategory = null }) { Text("Close") } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeIconScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    
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
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.packageName }) { app ->
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
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var containerWidthPx by remember { mutableStateOf(0f) }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            try {
                // 優化：inMutable 確保圖片可被 Matrix 處理，減少黑邊可能
                val options = BitmapFactory.Options().apply { inMutable = true }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                    BitmapFactory.decodeStream(it, null, options) 
                }
                originalBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Icon Position") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (originalBitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ensure important parts are within the circle", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .onGloballyPositioned { containerWidthPx = it.size.width.toFloat() }
                                .clip(RoundedCornerShape(44.dp))
                                .background(Color.Gray.copy(alpha = 0.1f))
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                        offset += pan
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = originalBitmap!!.asImageBitmap(),
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
                            
                            // 遮罩邊框提示
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    color = Color.Black.copy(alpha = 0.1f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                } else {
                    Text("Failed to load image")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading && originalBitmap != null,
                onClick = {
                    originalBitmap?.let { bitmap ->
                        // 提高解析度至 512，讓 Launcher 縮放時更清晰
                        val outputSize = 512
                        val cropped = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(cropped)
                        
                        // 高品質繪圖參數
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            isFilterBitmap = true
                            isDither = true
                        }

                        val matrix = Matrix()
                        
                        // 計算基準比例 (圖片適應 200dp 容器後的比例)
                        val baseScale = containerWidthPx / Math.max(bitmap.width.toFloat(), bitmap.height.toFloat())
                        // 計算最終映射到 512px 畫布的總比例
                        val totalScale = scale * baseScale * (outputSize / containerWidthPx)
                        
                        // 1. 先平移到圖片中心
                        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                        // 2. 進行縮放
                        matrix.postScale(totalScale, totalScale)
                        // 3. 套用使用者在 UI 上的位移 (需換算座標系)
                        val userOffsetX = offset.x * (outputSize / containerWidthPx)
                        val userOffsetY = offset.y * (outputSize / containerWidthPx)
                        // 4. 平移回輸出畫布中心並加上使用者位移
                        matrix.postTranslate(outputSize / 2f + userOffsetX, outputSize / 2f + userOffsetY)
                        
                        canvas.drawBitmap(bitmap, matrix, paint)
                        onConfirm(cropped)
                    }
                }
            ) {
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
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }
    
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
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.packageName }) { app ->
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
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

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
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
            
            item {
                SearchBar(searchQuery) { searchQuery = it }
            }

            items(filteredApps, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleHiddenApp(app.packageName) }
                        .padding(vertical = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
