package com.liferlighdow.iteration

import android.content.ComponentName
import android.content.Context
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.core.app.NotificationManagerCompat
import androidx.activity.enableEdgeToEdge
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import kotlin.math.roundToInt

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
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY, ICON_THEME, DOCK, LIQUID_GLASS, GESTURES
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
            onNavigateToLiquidGlass = { currentPage = SettingsPage.LIQUID_GLASS },
            onNavigateToGestures = { currentPage = SettingsPage.GESTURES }
        )
        SettingsPage.HIDE_APPS -> HideAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.RENAME_APPS -> RenameAppsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.CHANGE_ICON -> ChangeIconScreen(onBack = { currentPage = SettingsPage.ICON_THEME })
        SettingsPage.APP_LIBRARY -> AppLibrarySettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.ICON_THEME -> IconThemeScreen(
            onBack = { currentPage = SettingsPage.MAIN },
            onNavigateToChangeIcon = { currentPage = SettingsPage.CHANGE_ICON }
        )
        SettingsPage.DOCK -> DesktopSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.LIQUID_GLASS -> LiquidGlassSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.GESTURES -> GesturesSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
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
    onNavigateToLiquidGlass: () -> Unit,
    onNavigateToGestures: () -> Unit
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
    var showApiWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {}
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
                    modifier = Modifier.clickable { 
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            onNavigateToLiquidGlass() 
                        } else {
                            showApiWarningDialog = true
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Desktop Settings") },
                    supportingContent = { Text("Layout, Dock, and icon grid configuration") },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToDock() }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_gestures)) },
                    supportingContent = { Text(stringResource(R.string.settings_gestures_desc)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    modifier = Modifier.clickable { onNavigateToGestures() }
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

    if (showApiWarningDialog) {
        AlertDialog(
            onDismissRequest = { showApiWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Compatibility Warning") },
            text = { 
                Text("Liquid Glass effects rely on system APIs introduced in Android 12.\n\nYour current device (API ${android.os.Build.VERSION.SDK_INT}) does not support these advanced rendering features.") 
            },
            confirmButton = {
                TextButton(onClick = { showApiWarningDialog = false }) {
                    Text("I Understand")
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
    val currentShape by viewModel.iconShape.collectAsState()
    val currentIconPack by viewModel.iconPackPackage.collectAsState()
    
    var showIconPackPicker by remember { mutableStateOf(false) }
    var showStyleInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Icon Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {}
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

            item {
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text("Change Icon Shape") },
                    supportingContent = { Text(if (currentShape == IconShape.CIRCLE) "Circle" else "Default") },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Default") },
                                    onClick = { viewModel.setIconShape(IconShape.DEFAULT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Circle") },
                                    onClick = { viewModel.setIconShape(IconShape.CIRCLE); expanded = false }
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { expanded = true }
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
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Iteration Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (currentIconPack.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showStyleInfoDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Info, 
                            contentDescription = "Compatibility Info",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (currentIconPack.isEmpty()) {
                item {
                    var showExclusionPicker by remember { mutableStateOf(false) }
                    val excludedApps by viewModel.excludedThemedPackages.collectAsState()
                    
                    ListItem(
                        headlineContent = { Text("Custom Exclusions") },
                        supportingContent = { Text("${excludedApps.size} apps will never apply styles") },
                        trailingContent = { Icon(Icons.Default.Settings, null) },
                        modifier = Modifier.clickable { showExclusionPicker = true }
                    )
                    
                    if (showExclusionPicker) {
                        val allApps by viewModel.allApps.collectAsState()
                        MultiAppExclusionPickerDialog(
                            allApps = allApps,
                            excludedPackages = excludedApps,
                            onDismiss = { showExclusionPicker = false },
                            onToggle = { viewModel.toggleExcludedThemedApp(it) }
                        )
                    }
                }
            }

            val styles = listOf(
                IconStyle.STANDARD to "Standard",
                IconStyle.BLACK to "Black",
                IconStyle.WHITE to "White",
                IconStyle.GLASS to "Glass",
                IconStyle.CUSTOM to "Custom"
            )

            items(styles) { (style, label) ->
                var showCustomPicker by remember { mutableStateOf(false) }
                
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (style == IconStyle.CUSTOM && currentStyle == IconStyle.CUSTOM && currentIconPack.isEmpty()) {
                                IconButton(onClick = { showCustomPicker = true }) {
                                    Icon(Icons.Default.ColorLens, null)
                                }
                            }
                            RadioButton(
                                enabled = currentIconPack.isEmpty(),
                                selected = currentStyle == style && currentIconPack.isEmpty(),
                                onClick = { viewModel.setIconStyle(style) }
                            )
                        }
                    },
                    modifier = Modifier.clickable(enabled = currentIconPack.isEmpty()) { 
                        viewModel.setIconStyle(style) 
                        if (style == IconStyle.CUSTOM) showCustomPicker = true
                    }
                )

                if (showCustomPicker) {
                    CustomIconStylePickerDialog(
                        viewModel = viewModel,
                        onDismiss = { showCustomPicker = false }
                    )
                }
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

    if (showStyleInfoDialog) {
        AlertDialog(
            onDismissRequest = { showStyleInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text("Iteration Style Compatibility") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sdk = android.os.Build.VERSION.SDK_INT
                    Text("Different Android versions provide varying levels of icon precision:", style = MaterialTheme.typography.bodyMedium)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    CompatibilityRow("Android 13+", "Perfect", "Native monochrome layer support (finest detail).", sdk >= 33)
                    CompatibilityRow("Android 12", "High", "Adaptive layer filtering with native system colors.", sdk == 31 || sdk == 32)
                    CompatibilityRow("Android 8-11", "Good", "Adaptive layer filtering with manual color analysis.", sdk in 26..30)
                    CompatibilityRow("Android 6-7", "Basic", "Legacy whole-icon tinting.", sdk in 23..25)
                    
                    Text("\nNote: Iteration presets (Black, White, Glass, Custom) work on all supported versions.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStyleInfoDialog = false }) { Text("Got it") }
            }
        )
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
    val currentShape by viewModel.iconShape.collectAsState()
    val shape = if (currentShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)

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
                                    modifier = Modifier.size(40.dp).clip(shape)
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

    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()
    
    val rawWallpaper by viewModel.rawWallpaper.collectAsState()
    val backdrop = rememberLayerBackdrop()

    // 拖動狀態
    var glassOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liquid Glass") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.resetLiquidGlassParams()
                        glassOffset = androidx.compose.ui.geometry.Offset.Zero
                    }) {
                        Text("Reset", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                // Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Background: Wallpaper
                    if (rawWallpaper != null) {
                        Image(
                            bitmap = rawWallpaper!!,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(backdrop)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                    }

                    // Liquid Glass Preview Box (Free Dragging)
                    Box(
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    glassOffset.x.roundToInt(),
                                    glassOffset.y.roundToInt()
                                )
                            }
                            .size(150.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    glassOffset += dragAmount
                                }
                            }
                            .liquidGlass(
                                enabled = true,
                                backdrop = backdrop,
                                cornerRadius = 32.dp,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration
                            )
                    )

                    // 指引文字
                    Text(
                        "Drag the glass to preview",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                    )
                }
            }

            item {
                Text(
                    text = "Visual Effects",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Blur radius: ${blurRadius.toInt()}")
                    Slider(
                        value = blurRadius.coerceIn(0f, 40f),
                        onValueChange = { viewModel.setLiquidGlassBlur(it) },
                        valueRange = 0f..40f
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Refraction height: ${refractionHeight.toInt()}")
                    Slider(
                        value = refractionHeight,
                        onValueChange = { viewModel.setLiquidGlassRefractionHeight(it) },
                        valueRange = 0f..100f
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Refraction amount: ${refractionAmount.toInt()}")
                    Slider(
                        value = refractionAmount,
                        onValueChange = { viewModel.setLiquidGlassRefractionAmount(it) },
                        valueRange = 0f..100f
                    )
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Chromatic aberration") },
                    trailingContent = {
                        Switch(
                            checked = chromaticAberration,
                            onCheckedChange = { viewModel.setLiquidGlassChromaticAberration(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setLiquidGlassChromaticAberration(!chromaticAberration) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

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
                        text = "Apply to",
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
    val iconShape by viewModel.iconShape.collectAsState()
    val libraryShape by viewModel.libraryShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
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
                    "Appearance",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text("Folder Shape") },
                    supportingContent = { Text(if (libraryShape == IconShape.CIRCLE) "Circle" else "Default") },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Default") },
                                    onClick = { viewModel.setLibraryShape(IconShape.DEFAULT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Circle") },
                                    onClick = { viewModel.setLibraryShape(IconShape.CIRCLE); expanded = false }
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { expanded = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
                            Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
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
    val iconShape by viewModel.iconShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
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
                },
                actions = {}
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
                                    modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
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
    val iconShape by viewModel.iconShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
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
                },
                actions = {}
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
                                Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
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
                },
                actions = {}
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
fun DesktopSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val desktopRows by viewModel.desktopRows.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)

    var showAppPickerForSlot by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desktop Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            item {
                Text(
                    "Layout Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                val options = listOf(0 to "Auto (Adaptive)", 5 to "4 x 5", 6 to "4 x 6", 7 to "4 x 7")
                val currentLabel = options.find { it.first == desktopRows }?.second ?: "Auto (Adaptive)"

                ListItem(
                    headlineContent = { Text("Layout Rows") },
                    supportingContent = { Text("Current: $currentLabel") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text("Change")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDesktopRows(value)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            item {
                var expandedStyle by remember { mutableStateOf(false) }
                val styleOptions = listOf(DockStyle.MODERN to "Modern (Floating)", DockStyle.CLASSIC to "Classic (Full Width)")
                val currentStyleLabel = styleOptions.find { it.first == dockStyle }?.second ?: "Modern"

                ListItem(
                    headlineContent = { Text("Dock Style") },
                    supportingContent = { Text("Current: $currentStyleLabel") },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expandedStyle = true }) {
                                Text("Change")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = expandedStyle, onDismissRequest = { expandedStyle = false }) {
                                styleOptions.forEach { (style, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDockStyle(style)
                                            expandedStyle = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            item {
                Text(
                    "Dock Apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            items(4) { index ->
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
                                    modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val doubleTapAction by viewModel.doubleTapAction.collectAsState()
    val swipeUpAction by viewModel.swipeUpAction.collectAsState()
    val swipeDownAction by viewModel.swipeDownAction.collectAsState()
    val longPressAction by viewModel.longPressAction.collectAsState()
    val twoFingerSwipeUpAction by viewModel.twoFingerSwipeUpAction.collectAsState()
    val twoFingerSwipeDownAction by viewModel.twoFingerSwipeDownAction.collectAsState()
    val doubleTapApp by viewModel.doubleTapApp.collectAsState()
    val swipeUpApp by viewModel.swipeUpApp.collectAsState()
    val swipeDownApp by viewModel.swipeDownApp.collectAsState()
    val longPressApp by viewModel.longPressApp.collectAsState()
    val twoFingerSwipeUpApp by viewModel.twoFingerSwipeUpApp.collectAsState()
    val twoFingerSwipeDownApp by viewModel.twoFingerSwipeDownApp.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    var showDoubleTapDialog by remember { mutableStateOf(false) }
    var showSwipeUpDialog by remember { mutableStateOf(false) }
    var showSwipeDownDialog by remember { mutableStateOf(false) }
    var showLongPressDialog by remember { mutableStateOf(false) }
    var showTwoFingerSwipeUpDialog by remember { mutableStateOf(false) }
    var showTwoFingerSwipeDownDialog by remember { mutableStateOf(false) }

    var showAppPickerForDoubleTap by remember { mutableStateOf(false) }
    var showAppPickerForSwipeUp by remember { mutableStateOf(false) }
    var showAppPickerForSwipeDown by remember { mutableStateOf(false) }
    var showAppPickerForLongPress by remember { mutableStateOf(false) }
    var showAppPickerForTwoFingerSwipeUp by remember { mutableStateOf(false) }
    var showAppPickerForTwoFingerSwipeDown by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val isAccessibilityEnabled = {
        val expectedComponentName = ComponentName(context, IterationAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        enabledServices?.contains(expectedComponentName) == true
    }
    
    var isServiceActive by remember { mutableStateOf(isAccessibilityEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_gestures)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.resetGestures() }) {
                        Text(stringResource(R.string.gesture_reset))
                    }
                    TextButton(onClick = { viewModel.applySuggestedGestures() }) {
                        Text(stringResource(R.string.gesture_suggestions))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_double_tap),
                    action = doubleTapAction,
                    packageName = doubleTapApp,
                    allApps = allApps,
                    onClick = { showDoubleTapDialog = true }
                )
            }
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_swipe_up),
                    action = swipeUpAction,
                    packageName = swipeUpApp,
                    allApps = allApps,
                    onClick = { showSwipeUpDialog = true }
                )
            }
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_swipe_down),
                    action = swipeDownAction,
                    packageName = swipeDownApp,
                    allApps = allApps,
                    onClick = { showSwipeDownDialog = true }
                )
            }
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_long_press),
                    action = longPressAction,
                    packageName = longPressApp,
                    allApps = allApps,
                    onClick = { showLongPressDialog = true }
                )
            }
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_two_finger_swipe_up),
                    action = twoFingerSwipeUpAction,
                    packageName = twoFingerSwipeUpApp,
                    allApps = allApps,
                    onClick = { showTwoFingerSwipeUpDialog = true }
                )
            }
            item {
                GestureItem(
                    title = stringResource(R.string.gesture_two_finger_swipe_down),
                    action = twoFingerSwipeDownAction,
                    packageName = twoFingerSwipeDownApp,
                    allApps = allApps,
                    onClick = { showTwoFingerSwipeDownDialog = true }
                )
            }

            val needsAccessibility = listOf(doubleTapAction, swipeUpAction, swipeDownAction, longPressAction, twoFingerSwipeUpAction, twoFingerSwipeDownAction).any {
                it == GestureAction.LOCK_SCREEN || it == GestureAction.OPEN_NOTIFICATIONS
            }

            if (needsAccessibility) {
                item {
                    val toggleService = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                        Toast.makeText(context, "Find 'Iteration Gestures' and enable it", Toast.LENGTH_LONG).show()
                    }

                    ListItem(
                        headlineContent = { Text("Accessibility Service") },
                        supportingContent = { Text(if (isServiceActive) "Activated" else "Deactivated (Tap to configure)") },
                        trailingContent = {
                            Switch(
                                checked = isServiceActive,
                                onCheckedChange = { toggleService() }
                            )
                        },
                        modifier = Modifier.clickable { toggleService() }
                    )
                }
            }
        }
    }
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isServiceActive = isAccessibilityEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDoubleTapDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_double_tap_dialog_title),
            currentAction = doubleTapAction,
            onDismiss = { showDoubleTapDialog = false },
            onActionSelected = { action ->
                viewModel.setDoubleTapAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForDoubleTap = true
                showDoubleTapDialog = false
            }
        )
    }

    if (showSwipeUpDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_swipe_up_dialog_title),
            currentAction = swipeUpAction,
            onDismiss = { showSwipeUpDialog = false },
            onActionSelected = { action ->
                viewModel.setSwipeUpAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForSwipeUp = true
                showSwipeUpDialog = false
            }
        )
    }

    if (showSwipeDownDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_swipe_down_dialog_title),
            currentAction = swipeDownAction,
            onDismiss = { showSwipeDownDialog = false },
            onActionSelected = { action ->
                viewModel.setSwipeDownAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForSwipeDown = true
                showSwipeDownDialog = false
            }
        )
    }

    if (showLongPressDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_long_press_dialog_title),
            currentAction = longPressAction,
            onDismiss = { showLongPressDialog = false },
            onActionSelected = { action ->
                viewModel.setLongPressAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForLongPress = true
                showLongPressDialog = false
            }
        )
    }

    if (showTwoFingerSwipeUpDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_two_finger_swipe_up_dialog_title),
            currentAction = twoFingerSwipeUpAction,
            onDismiss = { showTwoFingerSwipeUpDialog = false },
            onActionSelected = { action ->
                viewModel.setTwoFingerSwipeUpAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForTwoFingerSwipeUp = true
                showTwoFingerSwipeUpDialog = false
            }
        )
    }

    if (showTwoFingerSwipeDownDialog) {
        GestureActionPicker(
            title = stringResource(R.string.gesture_two_finger_swipe_down_dialog_title),
            currentAction = twoFingerSwipeDownAction,
            onDismiss = { showTwoFingerSwipeDownDialog = false },
            onActionSelected = { action ->
                viewModel.setTwoFingerSwipeDownAction(action)
                if (action == GestureAction.LAUNCH_APP) showAppPickerForTwoFingerSwipeDown = true
                showTwoFingerSwipeDownDialog = false
            }
        )
    }

    if (showAppPickerForDoubleTap) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForDoubleTap = false }, onAppSelected = { viewModel.setDoubleTapApp(it); showAppPickerForDoubleTap = false })
    }
    if (showAppPickerForSwipeUp) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForSwipeUp = false }, onAppSelected = { viewModel.setSwipeUpApp(it); showAppPickerForSwipeUp = false })
    }
    if (showAppPickerForSwipeDown) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForSwipeDown = false }, onAppSelected = { viewModel.setSwipeDownApp(it); showAppPickerForSwipeDown = false })
    }
    if (showAppPickerForLongPress) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForLongPress = false }, onAppSelected = { viewModel.setLongPressApp(it); showAppPickerForLongPress = false })
    }
    if (showAppPickerForTwoFingerSwipeUp) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForTwoFingerSwipeUp = false }, onAppSelected = { viewModel.setTwoFingerSwipeUpApp(it); showAppPickerForTwoFingerSwipeUp = false })
    }
    if (showAppPickerForTwoFingerSwipeDown) {
        AppPickerDialog(allApps.filter { !it.isHidden }, onDismiss = { showAppPickerForTwoFingerSwipeDown = false }, onAppSelected = { viewModel.setTwoFingerSwipeDownApp(it); showAppPickerForTwoFingerSwipeDown = false })
    }
}

@Composable
fun GestureItem(title: String, action: GestureAction, packageName: String, allApps: List<AppModel>, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            val actionText = when (action) {
                GestureAction.NONE -> stringResource(R.string.gesture_action_none)
                GestureAction.LOCK_SCREEN -> stringResource(R.string.gesture_action_lock)
                GestureAction.LAUNCHER_SETTINGS -> stringResource(R.string.gesture_action_settings)
                GestureAction.OPEN_SYSTEM_SETTINGS -> stringResource(R.string.gesture_action_open_settings)
                GestureAction.OPEN_GLOBAL_SEARCH -> stringResource(R.string.gesture_action_open_global_search)
                GestureAction.OPEN_DESKTOP_MENU -> stringResource(R.string.gesture_action_open_desktop_menu)
                GestureAction.OPEN_NOTIFICATIONS -> stringResource(R.string.gesture_action_open_notifications)
                GestureAction.LAUNCH_APP -> {
                    val app = allApps.find { it.packageName == packageName }
                    val appName = app?.label ?: packageName
                    if (packageName.isNotEmpty()) "${stringResource(R.string.gesture_action_launch_app)}: $appName"
                    else stringResource(R.string.gesture_action_launch_app)
                }
            }
            Text(actionText)
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun MultiAppExclusionPickerDialog(
    allApps: List<AppModel>,
    excludedPackages: Set<String>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Style Exclusions",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                Text(
                    "Selected apps will always use their original colorful icons.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val filteredApps = remember(allApps, searchQuery) {
                    allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
                        .sortedBy { it.label.lowercase() }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps, key = { it.uniqueId }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val icon = app.processedIcon
                                if (icon != null) {
                                    Image(
                                        bitmap = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                                    )
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = excludedPackages.contains(app.packageName),
                                    onCheckedChange = { onToggle(app.packageName) }
                                )
                            },
                            modifier = Modifier.clickable { onToggle(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GestureActionPicker(title: String, currentAction: GestureAction, onDismiss: () -> Unit, onActionSelected: (GestureAction) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                GestureAction.entries.forEach { action ->
                    val label = when (action) {
                        GestureAction.NONE -> stringResource(R.string.gesture_action_none)
                        GestureAction.LOCK_SCREEN -> stringResource(R.string.gesture_action_lock)
                        GestureAction.LAUNCHER_SETTINGS -> stringResource(R.string.gesture_action_settings)
                        GestureAction.OPEN_SYSTEM_SETTINGS -> stringResource(R.string.gesture_action_open_settings)
                        GestureAction.OPEN_GLOBAL_SEARCH -> stringResource(R.string.gesture_action_open_global_search)
                        GestureAction.OPEN_DESKTOP_MENU -> stringResource(R.string.gesture_action_open_desktop_menu)
                        GestureAction.OPEN_NOTIFICATIONS -> stringResource(R.string.gesture_action_open_notifications)
                        GestureAction.LAUNCH_APP -> stringResource(R.string.gesture_action_launch_app)
                    }
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = { RadioButton(selected = (action == currentAction), onClick = null) },
                        modifier = Modifier.clickable { onActionSelected(action) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomIconStylePickerDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val bgColor by viewModel.customIconBgColor.collectAsState()
    val fgColor by viewModel.customIconFgColor.collectAsState()
    val useOriginal by viewModel.customIconUseOriginal.collectAsState()
    val useOriginalBg by viewModel.customIconUseOriginalBg.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    
    // 預覽用的虛擬 AppModel
    val context = LocalContext.current
    val previewIcon = remember { context.packageManager.getApplicationIcon(context.packageName) }
    
    val previewBitmap = remember(bgColor, fgColor, useOriginal, useOriginalBg, iconShape) {
        val processor = IconProcessor(context)
        val density = context.resources.displayMetrics.density
        processor.processIcon(
            icon = previewIcon,
            isThemed = false,
            themeColors = null,
            style = IconStyle.CUSTOM,
            shape = iconShape,
            sizePx = (64 * density).toInt(),
            customBgColor = bgColor,
            customFgColor = fgColor,
            customUseOriginal = useOriginal,
            customUseOriginalBg = useOriginalBg
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom Style", modifier = Modifier.weight(1f))
                Image(
                    bitmap = previewBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f))
                )
            }
        },
        text = {
            LazyColumn {
                item {
                    Text("Background Settings", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Use Original Background", modifier = Modifier.weight(1f))
                        Switch(checked = useOriginalBg, onCheckedChange = { viewModel.setCustomIconUseOriginalBg(it) })
                    }
                    if (!useOriginalBg) {
                        ColorPicker(
                            initialColor = bgColor,
                            onColorChanged = { viewModel.setCustomIconBgColor(it) }
                        )
                    } else {
                        Text("Background color disabled while using original background", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Foreground Settings", style = MaterialTheme.typography.titleSmall, color = if (useOriginal) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Use Original Icon (Color)", modifier = Modifier.weight(1f))
                        Switch(checked = useOriginal, onCheckedChange = { viewModel.setCustomIconUseOriginal(it) })
                    }
                    if (!useOriginal) {
                        ColorPicker(
                            initialColor = fgColor,
                            onColorChanged = { viewModel.setCustomIconFgColor(it) }
                        )
                    } else {
                        Text("Foreground color disabled while using original icon", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}


@Composable
fun CompatibilityRow(version: String, level: String, desc: String, isCurrentDevice: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(version, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(90.dp))
            Surface(
                color = when(level) {
                    "Perfect" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    "High" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                    "Good" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    else -> Color.Gray.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = level, 
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when(level) {
                        "Perfect" -> Color(0xFF2E7D32)
                        "High" -> Color(0xFF1565C0)
                        "Good" -> Color(0xFFE65100)
                        else -> Color.DarkGray
                    }
                )
            }
            
            if (isCurrentDevice) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        "Your Device",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .background(androidx.compose.ui.graphics.Color(initialColor), RoundedCornerShape(8.dp))
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
            label = { Text("Hex (AARRGGBB)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Hue: ${h.toInt()}", style = MaterialTheme.typography.labelSmall)
        Slider(value = h, onValueChange = { h = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..360f)
        
        Text("Saturation: ${(s * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        Slider(value = s, onValueChange = { s = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
        
        Text("Brightness: ${(v * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        Slider(value = v, onValueChange = { v = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
        
        Text("Alpha: ${(a * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        Slider(value = a, onValueChange = { a = it; onColorChanged(android.graphics.Color.HSVToColor((a * 255).toInt(), floatArrayOf(h, s, v))) }, valueRange = 0f..1f)
    }
}
