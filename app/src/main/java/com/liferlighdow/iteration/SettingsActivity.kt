package com.liferlighdow.iteration

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.app.NotificationManagerCompat
import androidx.activity.enableEdgeToEdge

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IterationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsNavigation()
                }
            }
        }
    }
}

enum class SettingsPage {
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY, ICON_THEME, DOCK, LIQUID_GLASS
}

@Composable
fun SettingsNavigation() {
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }
    val context = LocalContext.current
    
    BackHandler(enabled = currentPage != SettingsPage.MAIN) {
        when (currentPage) {
            SettingsPage.CHANGE_ICON -> currentPage = SettingsPage.ICON_THEME
            else -> currentPage = SettingsPage.MAIN
        }
    }

    when (currentPage) {
        SettingsPage.MAIN -> SettingsMainScreen(
            onBack = { (context as? ComponentActivity)?.finish() },
            onNavigateToHideApps = { currentPage = SettingsPage.HIDE_APPS },
            onNavigateToRenameApps = { currentPage = SettingsPage.RENAME_APPS },
            onNavigateToAppLibrary = { currentPage = SettingsPage.APP_LIBRARY },
            onNavigateToIconTheme = { currentPage = SettingsPage.ICON_THEME },
            onNavigateToDock = { currentPage = SettingsPage.DOCK },
            onNavigateToLiquidGlass = { currentPage = SettingsPage.LIQUID_GLASS }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.RENAME_APPS -> RenameAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CHANGE_ICON -> ChangeIconScreen(onBack = { currentPage = SettingsPage.ICON_THEME })
        SettingsPage.APP_LIBRARY -> AppLibrarySettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.ICON_THEME -> IconThemeScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToChangeIcon = { currentPage = SettingsPage.CHANGE_ICON }
        )
        SettingsPage.DOCK -> DockSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.LIQUID_GLASS -> LiquidGlassSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
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
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear))
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
    onNavigateToAppLibrary: () -> Unit,
    onNavigateToIconTheme: () -> Unit,
    onNavigateToDock: () -> Unit,
    onNavigateToLiquidGlass: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { out ->
                    out.write(viewModel.exportConfig().toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    val content = input.bufferedReader().readText()
                    if (viewModel.importConfig(content)) {
                        Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val isNotificationEnabled = remember {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    var showRestartDialog by remember { mutableStateOf(false) }
    var showPasswordGate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("Icon Theme") },
                    supportingContent = { Text("Change the style and color of your app icons") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToIconTheme() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Liquid Glass") },
                    supportingContent = { Text("Real-time glassmorphism effects") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToLiquidGlass() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Dock Settings") },
                    supportingContent = { Text("Manage apps in your home screen dock") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToDock() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                    supportingContent = { 
                        Text(if (isNotificationEnabled) "Enabled" else stringResource(R.string.settings_notifications_desc)) 
                    },
                    trailingContent = { 
                        if (!isNotificationEnabled) {
                            TextButton(onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable { 
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_hide_apps)) },
                    supportingContent = { Text(stringResource(R.string.settings_hide_apps_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { 
                        if (viewModel.getPassword().isEmpty()) {
                            onNavigateToHideApps()
                        } else {
                            showPasswordGate = true
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_rename_apps)) },
                    supportingContent = { Text(stringResource(R.string.settings_rename_apps_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToRenameApps() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_library)) },
                    supportingContent = { Text(stringResource(R.string.settings_library_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToAppLibrary() }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_backup_restore),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.backup_image_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_export)) },
                    supportingContent = { Text(stringResource(R.string.settings_backup_restore_desc)) },
                    leadingContent = { Icon(Icons.Default.Backup, contentDescription = null) },
                    modifier = Modifier.clickable { exportLauncher.launch("iteration_backup.json") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_import)) },
                    supportingContent = { Text("Restore layout and settings from a backup file") },
                    leadingContent = { Icon(Icons.Default.Restore, contentDescription = null) },
                    modifier = Modifier.clickable { importLauncher.launch("application/json") }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_restart_launcher), color = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text(stringResource(R.string.settings_restart_desc)) },
                    leadingContent = { Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showRestartDialog = true }
                )
            }
        }
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(R.string.restart_confirm_title)) },
            text = { Text(stringResource(R.string.restart_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        val componentName = intent?.component
                        val mainIntent = Intent.makeRestartActivityTask(componentName)
                        context.startActivity(mainIntent)
                        Runtime.getRuntime().exit(0)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showPasswordGate) {
        var input by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordGate = false },
            title = { Text(stringResource(R.string.security_section)) },
            text = {
                Column {
                    Text(stringResource(R.string.password_label))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { 
                            input = it
                            isError = false
                        },
                        isError = isError,
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = "Incorrect password",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (input == viewModel.getPassword()) {
                        showPasswordGate = false
                        onNavigateToHideApps()
                    } else {
                        isError = true
                    }
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordGate = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconThemeScreen(onBack: () -> Unit, onNavigateToChangeIcon: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val isThemedIconsEnabled by viewModel.isThemedIconsEnabled.collectAsState()
    val currentStyle by viewModel.iconStyle.collectAsState()
    val currentIconPack by viewModel.iconPackPackage.collectAsState()
    
    var showIconPackPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Icon Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                Text(
                    text = "Customization",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_change_icon)) },
                    supportingContent = { Text(stringResource(R.string.settings_change_icon_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToChangeIcon() }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                Text(
                    text = "Icon Pack",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                val iconPacks = remember { viewModel.getInstalledIconPacks() }
                val currentPackName = remember(currentIconPack, iconPacks) {
                    if (currentIconPack.isEmpty()) "Default"
                    else iconPacks.find { it.packageName == currentIconPack }?.label ?: "Unknown"
                }

                ListItem(
                    headlineContent = { Text("Current Pack") },
                    supportingContent = { Text(currentPackName) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier.clickable { showIconPackPicker = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                ListItem(
                    headlineContent = { Text("Themed Icons (M3)") },
                    supportingContent = { Text("Apply dynamic colors from your wallpaper to icons") },
                    trailingContent = {
                        Switch(
                            enabled = currentIconPack.isEmpty(),
                            checked = isThemedIconsEnabled,
                            onCheckedChange = { viewModel.setThemedIconsEnabled(it) }
                        )
                    },
                    modifier = Modifier.clickable(enabled = currentIconPack.isEmpty()) { 
                        viewModel.setThemedIconsEnabled(!isThemedIconsEnabled) 
                    }
                )
            }
            
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            
            item {
                Text(
                    text = "Iteration Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (currentIconPack.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val styles = listOf(
                IconStyle.STANDARD to "Standard",
                IconStyle.BLACK to "Black",
                IconStyle.WHITE to "White",
                IconStyle.GLASS to "Glass"
            )

            items(styles) { (style, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(
                            enabled = currentIconPack.isEmpty(),
                            selected = currentStyle == style && currentIconPack.isEmpty(),
                            onClick = { viewModel.setIconStyle(style) }
                        )
                    },
                    modifier = Modifier.clickable(enabled = currentIconPack.isEmpty()) { 
                        viewModel.setIconStyle(style) 
                    }
                )
            }

            if (currentIconPack.isNotEmpty()) {
                item {
                    Text(
                        text = "Note: Iteration styles and Themed Icons are disabled when an Icon Pack is active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    if (showIconPackPicker) {
        IconPackPickerDialog(
            onDismiss = { showIconPackPicker = false },
            onPackSelected = { pkg ->
                viewModel.setIconPack(pkg)
                showIconPackPicker = false
            }
        )
    }
}

@Composable
fun IconPackPickerDialog(onDismiss: () -> Unit, onPackSelected: (String) -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val iconPacks = remember { viewModel.getInstalledIconPacks() }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Icon Pack", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    item {
                        ListItem(
                            headlineContent = { Text("Default (System + Iteration Style)") },
                            modifier = Modifier.clickable { onPackSelected("") }
                        )
                    }
                    items(iconPacks) { pack ->
                        ListItem(
                            headlineContent = { Text(pack.label) },
                            leadingContent = {
                                Image(
                                    bitmap = pack.icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                )
                            },
                            modifier = Modifier.clickable { onPackSelected(pack.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidGlassDockEnabled by viewModel.isLiquidGlassDockEnabled.collectAsState()
    val isLiquidGlassHomeFolderEnabled by viewModel.isLiquidGlassHomeFolderEnabled.collectAsState()
    val isLiquidGlassAppLibraryFolderEnabled by viewModel.isLiquidGlassAppLibraryFolderEnabled.collectAsState()
    val isLiquidGlassGlobalSearchEnabled by viewModel.isLiquidGlassGlobalSearchEnabled.collectAsState()
    val isLiquidGlassAppLibrarySearchEnabled by viewModel.isLiquidGlassAppLibrarySearchEnabled.collectAsState()
    val isLiquidGlassWidgetsEnabled by viewModel.isLiquidGlassWidgetsEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liquid Glass") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                ListItem(
                    headlineContent = { Text("Enable Liquid Glass") },
                    supportingContent = { Text("Master switch for real-time glass effects") },
                    trailingContent = {
                        Switch(
                            checked = isLiquidGlassEnabled,
                            onCheckedChange = { viewModel.setLiquidGlassEnabled(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setLiquidGlassEnabled(!isLiquidGlassEnabled) }
                )
            }

            if (isLiquidGlassEnabled) {
                item {
                    Text(
                        text = "Home Screen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Dock") },
                        supportingContent = { Text("Enable glass effect for the home screen dock") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassDockEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassDockEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassDockEnabled(!isLiquidGlassDockEnabled) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Folders") },
                        supportingContent = { Text("Enable glass effect for home screen folders") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassHomeFolderEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassHomeFolderEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassHomeFolderEnabled(!isLiquidGlassHomeFolderEnabled) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Global Search Bar") },
                        supportingContent = { Text("Enable glass effect for the swipe-down search bar") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassGlobalSearchEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassGlobalSearchEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassGlobalSearchEnabled(!isLiquidGlassGlobalSearchEnabled) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Widgets") },
                        supportingContent = { Text("Enable glass effect for home screen widgets in Glass mode") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassWidgetsEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassWidgetsEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassWidgetsEnabled(!isLiquidGlassWidgetsEnabled) }
                    )
                }

                item {
                    Text(
                        text = "App Library",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Folders") },
                        supportingContent = { Text("Enable glass effect for app library folders") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassAppLibraryFolderEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassAppLibraryFolderEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassAppLibraryFolderEnabled(!isLiquidGlassAppLibraryFolderEnabled) }
                    )
                }
                item {
                    ListItem(
                        headlineContent = { Text("Search Bar") },
                        supportingContent = { Text("Enable glass effect for the app library search bar") },
                        trailingContent = {
                            Switch(
                                checked = isLiquidGlassAppLibrarySearchEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassAppLibrarySearchEnabled(it) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.setLiquidGlassAppLibrarySearchEnabled(!isLiquidGlassAppLibrarySearchEnabled) }
                    )
                }
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
    
    // 動態計算所有當前存在但未排序的類別
    val unhandledCategories = remember(allApps, userCategories) {
        allApps.asSequence()
            .map { it.displayCategory }
            .distinct()
            .filter { it !in userCategories }
            .toList()
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    
    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }
    
    var selectingAppForCategory by remember { mutableStateOf<AppModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                Text(
                    "Managed Folders (Ordered)", 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (userCategories.isEmpty()) {
                item {
                    Text(
                        "No folders managed yet. Default folders are shown at the end of the list.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            itemsIndexed(userCategories) { index, category ->
                ListItem(
                    headlineContent = { Text(category) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { categoryToRename = category; renameInput = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename")
                            }
                            IconButton(
                                enabled = index > 0,
                                onClick = { viewModel.moveUserCategory(index, index - 1) }
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }
                            IconButton(
                                enabled = index < userCategories.size - 1,
                                onClick = { viewModel.moveUserCategory(index, index + 1) }
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                            }
                            IconButton(onClick = { viewModel.deleteUserCategory(category) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from list")
                            }
                        }
                    }
                )
            }
            
            if (unhandledCategories.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        "Default / Existing Folders", 
                        style = MaterialTheme.typography.titleSmall, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                items(unhandledCategories) { category ->
                    ListItem(
                        headlineContent = { Text(category) },
                        supportingContent = { Text("App Library folder detected") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { categoryToRename = category; renameInput = category }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                                }
                                Button(onClick = { viewModel.addUserCategory(category) }) {
                                    Text("Manage")
                                }
                            }
                        }
                    )
                }
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.assign_categories), 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SearchBar(searchQuery) { searchQuery = it }
            }
            
            val filteredApps = if (searchQuery.isEmpty()) allApps
                              else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

            items(filteredApps, key = { it.uniqueId }) { app ->
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text("Folder: ${app.displayCategory}") },
                    leadingContent = {
                        if (app.processedIcon != null) {
                            Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
                        }
                    },
                    modifier = Modifier.clickable { selectingAppForCategory = app }
                )
            }
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text(stringResource(R.string.add_category_title)) },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text(stringResource(R.string.category_name_label)) },
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
                    }) { Text(stringResource(R.string.add)) }
                },
                dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }

        if (categoryToRename != null) {
            AlertDialog(
                onDismissRequest = { categoryToRename = null },
                title = { Text("Rename Folder") },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.renameCategory(categoryToRename!!, renameInput)
                        categoryToRename = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToRename = null }) { Text("Cancel") }
                }
            )
        }

        if (selectingAppForCategory != null) {
            // 合併所有可用的類別供選取
            val allAvailableCategories = (userCategories + unhandledCategories).distinct()
            
            AlertDialog(
                onDismissRequest = { selectingAppForCategory = null },
                title = { Text(stringResource(R.string.select_category_for, selectingAppForCategory?.label ?: "")) },
                text = {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.default_system)) },
                                modifier = Modifier.clickable {
                                    viewModel.setAppCategory(selectingAppForCategory!!.packageName, "")
                                    selectingAppForCategory = null
                                }
                            )
                        }
                        items(allAvailableCategories) { category ->
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
                dismissButton = { TextButton(onClick = { selectingAppForCategory = null }) { Text(stringResource(R.string.close)) } }
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
                title = { Text(stringResource(R.string.settings_change_icon)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.uniqueId }) { app ->
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
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default))
                                }
                                IconButton(onClick = { 
                                    selectedApp = app
                                    launcher.launch("image/*")
                                }) {
                                    Icon(Icons.Default.Image, contentDescription = stringResource(R.string.change_icon))
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
                title = { Text(stringResource(R.string.settings_rename_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.uniqueId }) { app ->
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
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default))
                                }
                                IconButton(onClick = { editingApp = app; newLabel = app.label }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename))
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
                title = { Text(stringResource(R.string.rename_app_title, editingApp?.label ?: "")) },
                text = { OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text(stringResource(R.string.new_label_hint)) }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        editingApp?.let { viewModel.setCustomLabel(it.packageName, newLabel) }
                        editingApp = null
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = { editingApp = null }) { Text(stringResource(R.string.cancel)) } }
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
                title = { Text(stringResource(R.string.settings_hide_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                Text(text = stringResource(R.string.security_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { 
                        currentPassword = it
                        viewModel.setPassword(it)
                    },
                    label = { Text(stringResource(R.string.password_label)) },
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
                Text(text = stringResource(R.string.hide_apps_instruction), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                SearchBar(searchQuery) { searchQuery = it }
            }

            items(filteredApps, key = { it.uniqueId }) { app ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    
    var showAppPickerForSlot by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dock Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            Text(
                "Customize Dock Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            repeat(4) { index ->
                val pkgName = dockPkgNames.getOrNull(index) ?: ""
                val app = allApps.find { it.packageName == pkgName }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { showAppPickerForSlot = index },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(app?.label ?: "Empty Slot ${index + 1}") },
                        supportingContent = { Text(if (pkgName.isEmpty()) "Tap to select an app" else pkgName) },
                        leadingContent = {
                            if (app?.processedIcon != null) {
                                Image(
                                    bitmap = app.processedIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        },
                        trailingContent = {
                            if (pkgName.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateDockApp(index, "") }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showAppPickerForSlot != null) {
            val visibleApps = allApps.filter { !it.isHidden }
            AppPickerDialog(
                allApps = visibleApps,
                onDismiss = { showAppPickerForSlot = null },
                onAppSelected = { pkg ->
                    viewModel.updateDockApp(showAppPickerForSlot!!, pkg)
                    showAppPickerForSlot = null
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
        title = { Text(stringResource(R.string.adjust_icon_position)) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (originalBitmap != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.crop_instruction), style = MaterialTheme.typography.bodySmall)
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
                        val outputSize = 512
                        val cropped = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(cropped)
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            isFilterBitmap = true
                            isDither = true
                        }
                        val matrix = Matrix()
                        val baseScale = containerWidthPx / Math.max(bitmap.width.toFloat(), bitmap.height.toFloat())
                        val totalScale = scale * baseScale * (outputSize / containerWidthPx)
                        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                        matrix.postScale(totalScale, totalScale)
                        val userOffsetX = offset.x * (outputSize / containerWidthPx)
                        val userOffsetY = offset.y * (outputSize / containerWidthPx)
                        matrix.postTranslate(outputSize / 2f + userOffsetX, outputSize / 2f + userOffsetY)
                        canvas.drawBitmap(bitmap, matrix, paint)
                        onConfirm(cropped)
                    }
                }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
