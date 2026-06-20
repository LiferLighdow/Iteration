package com.liferlighdow.iteration

import android.annotation.SuppressLint
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class SettingsMetadata(
    val label: String,
    val supporting: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconColor: Color,
    val action: () -> Unit,
    val isLiquidGlass: Boolean = false,
    val isHideApps: Boolean = false,
    val isExport: Boolean = false,
    val isImport: Boolean = false,
    val isRestart: Boolean = false
)

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
    MAIN, HIDE_APPS, RENAME_APPS, CHANGE_ICON, APP_LIBRARY, ICON_THEME, DOCK, LIQUID_GLASS, GESTURES, SEARCH, PERMISSIONS
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
            onNavigateToGestures = { currentPage = SettingsPage.GESTURES },
            onNavigateToSearch = { currentPage = SettingsPage.SEARCH },
            onNavigateToPermissions = { currentPage = SettingsPage.PERMISSIONS }
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
        SettingsPage.SEARCH -> SearchSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
        SettingsPage.PERMISSIONS -> PermissionsSettingsScreen(onBack = { currentPage = SettingsPage.MAIN })
    }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@SuppressLint("LocalContextGetResourceValueCall")
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
    onNavigateToGestures: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToPermissions: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // 定義所有設定項的元數據，以便進行搜尋
    val allSettingsItems = remember {
        listOf(
            SettingsMetadata("Icon Theme", "Styles, shapes, and icon packs", Icons.Default.Palette, Color(0xFF4285F4), onNavigateToIconTheme),
            SettingsMetadata("Liquid Glass", "Real-time glassmorphism effects", Icons.Default.BlurOn, Color(0xFF34A853), {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) onNavigateToLiquidGlass() 
                else {} // 會觸發 Dialog 的邏輯在下面處理
            }, isLiquidGlass = true),
            SettingsMetadata("Home Screen", "Grid, Dock, and menu options", Icons.Default.Dashboard, Color(0xFFFBBC04), onNavigateToDock),
            SettingsMetadata(context.getString(R.string.settings_library), context.getString(R.string.settings_library_desc), Icons.Default.Apps, Color(0xFFEA4335), onNavigateToAppLibrary),
            SettingsMetadata(context.getString(R.string.settings_gestures), context.getString(R.string.settings_gestures_desc), Icons.Default.TouchApp, Color(0xFF9C27B0), onNavigateToGestures),
            SettingsMetadata(context.getString(R.string.settings_search), context.getString(R.string.settings_search_desc), Icons.Default.Search, Color(0xFF00ACC1), onNavigateToSearch),
            SettingsMetadata(context.getString(R.string.settings_permissions), context.getString(R.string.settings_permissions_desc), Icons.Default.Security, Color(0xFF607D8B), onNavigateToPermissions),
            SettingsMetadata(context.getString(R.string.settings_hide_apps), context.getString(R.string.settings_hide_apps_desc), Icons.Default.VisibilityOff, Color(0xFF795548), {
                if (viewModel.getPassword().isEmpty()) onNavigateToHideApps() else {} // 觸發 PasswordGate
            }, isHideApps = true),
            SettingsMetadata(context.getString(R.string.settings_rename_apps), context.getString(R.string.settings_rename_apps_desc), Icons.Default.Edit, Color(0xFF673AB7), onNavigateToRenameApps),
            SettingsMetadata(context.getString(R.string.settings_export), context.getString(R.string.settings_backup_restore_desc), Icons.Default.Backup, Color(0xFF4CAF50), { /* Launcher Logic */ }, isExport = true),
            SettingsMetadata(context.getString(R.string.settings_import), "Restore from backup file", Icons.Default.Restore, Color(0xFF03A9F4), { /* Launcher Logic */ }, isImport = true),
            SettingsMetadata(context.getString(R.string.settings_restart_launcher), context.getString(R.string.settings_restart_desc), Icons.Default.RestartAlt, Color.Red, { /* Launcher Logic */ }, isRestart = true)
        )
    }

    val filteredItems = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else allSettingsItems.filter { 
            it.label.contains(searchQuery, ignoreCase = true) || 
            it.supporting.contains(searchQuery, ignoreCase = true) 
        }
    }

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

    var showRestartDialog by remember { mutableStateOf(false) }
    var showPasswordGate by remember { mutableStateOf(false) }
    var showApiWarningDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                SearchBar(searchQuery) { searchQuery = it }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                // --- 搜尋模式：顯示過濾後的列表 ---
                if (filteredItems.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No results found for \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    item {
                        SettingsGroup {
                            filteredItems.forEachIndexed { index, item ->
                                SettingsItem(
                                    headline = item.label,
                                    supporting = item.supporting,
                                    icon = item.icon,
                                    iconColor = item.iconColor,
                                    onClick = {
                                        when {
                                            item.isLiquidGlass -> {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) onNavigateToLiquidGlass()
                                                else showApiWarningDialog = true
                                            }
                                            item.isHideApps -> {
                                                if (viewModel.getPassword().isEmpty()) onNavigateToHideApps()
                                                else showPasswordGate = true
                                            }
                                            item.isExport -> exportLauncher.launch("iteration_backup.json")
                                            item.isImport -> importLauncher.launch("application/json")
                                            item.isRestart -> showRestartDialog = true
                                            else -> item.action()
                                        }
                                    }
                                )
                                if (index < filteredItems.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            } else {
                // --- 標準模式：原有的卡片佈局 ---
                item {
                    Text(
                        "Personalization",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = "Icon Theme",
                            supporting = "Styles, shapes, and icon packs",
                            icon = Icons.Default.Palette,
                            iconColor = Color(0xFF4285F4),
                            onClick = onNavigateToIconTheme
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = "Liquid Glass",
                            supporting = "Real-time glassmorphism effects",
                            icon = Icons.Default.BlurOn,
                            iconColor = Color(0xFF34A853),
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                    onNavigateToLiquidGlass()
                                } else {
                                    showApiWarningDialog = true
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        "Desktop & Layout",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = "Home Screen",
                            supporting = "Grid, Dock, and menu options",
                            icon = Icons.Default.Dashboard,
                            iconColor = Color(0xFFFBBC04),
                            onClick = onNavigateToDock
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_library),
                            supporting = stringResource(R.string.settings_library_desc),
                            icon = Icons.Default.Apps,
                            iconColor = Color(0xFFEA4335),
                            onClick = onNavigateToAppLibrary
                        )
                    }
                }

                item {
                    Text(
                        "System & Interaction",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.settings_gestures),
                            supporting = stringResource(R.string.settings_gestures_desc),
                            icon = Icons.Default.TouchApp,
                            iconColor = Color(0xFF9C27B0),
                            onClick = onNavigateToGestures
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_search),
                            supporting = stringResource(R.string.settings_search_desc),
                            icon = Icons.Default.Search,
                            iconColor = Color(0xFF00ACC1),
                            onClick = onNavigateToSearch
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_permissions),
                            supporting = stringResource(R.string.settings_permissions_desc),
                            icon = Icons.Default.Security,
                            iconColor = Color(0xFF607D8B),
                            onClick = onNavigateToPermissions
                        )
                    }
                }

                item {
                    Text(
                        "Privacy & Content",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.settings_hide_apps),
                            supporting = stringResource(R.string.settings_hide_apps_desc),
                            icon = Icons.Default.VisibilityOff,
                            iconColor = Color(0xFF795548),
                            onClick = {
                                if (viewModel.getPassword().isEmpty()) onNavigateToHideApps()
                                else showPasswordGate = true
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_rename_apps),
                            supporting = stringResource(R.string.settings_rename_apps_desc),
                            icon = Icons.Default.Edit,
                            iconColor = Color(0xFF673AB7),
                            onClick = onNavigateToRenameApps
                        )
                    }
                }

                item {
                    Text(
                        "Backup & Maintenance",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 4.dp)
                    )
                    Text(
                        stringResource(R.string.backup_image_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp, end = 32.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.settings_export),
                            supporting = stringResource(R.string.settings_backup_restore_desc),
                            icon = Icons.Default.Backup,
                            iconColor = Color(0xFF4CAF50),
                            onClick = { exportLauncher.launch("iteration_backup.json") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_import),
                            supporting = "Restore from backup file",
                            icon = Icons.Default.Restore,
                            iconColor = Color(0xFF03A9F4),
                            onClick = { importLauncher.launch("application/json") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_restart_launcher),
                            supporting = stringResource(R.string.settings_restart_desc),
                            icon = Icons.Default.RestartAlt,
                            iconColor = MaterialTheme.colorScheme.error,
                            onClick = { showRestartDialog = true }
                        )
                    }
                }
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
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                // 優化點：將 PackageManager 的查詢移至 IO 線程，避免阻塞 UI
                val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
                    value = withContext(Dispatchers.IO) {
                        viewModel.getInstalledIconPacks()
                    }
                }

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
                        trailingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable { showExclusionPicker = true }
                    )
                    
                    if (showExclusionPicker) {
                        val allApps by viewModel.allApps.collectAsState()
                        MultiAppExclusionPickerDialog(
                            allApps = allApps,
                            excludedPackages = excludedApps,
                            viewModel = viewModel,
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
                                    Icon(Icons.Default.ColorLens, null, tint = MaterialTheme.colorScheme.primary)
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
    // 優化點：異步獲取圖標包列表
    val iconPacks by produceState<List<IconPackInfo>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            viewModel.getInstalledIconPacks()
        }
    }
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
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                enabled = index > 0,
                                onClick = { viewModel.moveUserCategory(index, index - 1) }
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                            IconButton(
                                enabled = index < userCategories.size - 1,
                                onClick = { viewModel.moveUserCategory(index, index + 1) }
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = if (index < userCategories.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                            IconButton(onClick = { viewModel.deleteUserCategory(category) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from list", tint = MaterialTheme.colorScheme.error)
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
                                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = MaterialTheme.colorScheme.primary)
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
                        val appIcon = viewModel.getIcon(app.packageName)
                        if (appIcon != null) {
                            Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
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
                            val appIcon = viewModel.getIcon(app.packageName)
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
                                )
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.resetCustomIcon(app.packageName) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    selectedApp = app
                                    launcher.launch("image/*")
                                }) {
                                    Icon(Icons.Default.Image, contentDescription = stringResource(R.string.change_icon), tint = MaterialTheme.colorScheme.primary)
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
                            val appIcon = viewModel.getIcon(app.packageName)
                            if (appIcon != null) {
                                Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.setCustomLabel(app.packageName, "") }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { editingApp = app; newLabel = app.label }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename), tint = MaterialTheme.colorScheme.primary)
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

enum class AppFilter { ALL, HIDDEN, VISIBLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf(AppFilter.ALL) }

    val filteredApps = remember(allApps, searchQuery, appFilter) {
        allApps.filter { app ->
            val matchesSearch = if (searchQuery.isEmpty()) true else app.label.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (appFilter) {
                AppFilter.ALL -> true
                AppFilter.HIDDEN -> app.isHidden
                AppFilter.VISIBLE -> !app.isHidden
            }
            matchesSearch && matchesFilter
        }
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
                            Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
            item {
                HorizontalDivider()
                Text(text = stringResource(R.string.hide_apps_instruction), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Column {
                    SearchBar(searchQuery) { searchQuery = it }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = appFilter == AppFilter.ALL,
                            onClick = { appFilter = AppFilter.ALL },
                            label = { Text("All") }
                        )
                        FilterChip(
                            selected = appFilter == AppFilter.HIDDEN,
                            onClick = { appFilter = AppFilter.HIDDEN },
                            label = { Text("Hidden") },
                            leadingIcon = {
                                if (appFilter == AppFilter.HIDDEN) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = appFilter == AppFilter.VISIBLE,
                            onClick = { appFilter = AppFilter.VISIBLE },
                            label = { Text("Visible") },
                            leadingIcon = {
                                if (appFilter == AppFilter.VISIBLE) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
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
                    val appIcon = viewModel.getIcon(app.packageName)
                    if (appIcon != null) {
                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
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
    val showMinusOnePage by viewModel.showMinusOnePage.collectAsState()
    val showAppLibrary by viewModel.showAppLibrary.collectAsState()
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
                ListItem(
                    headlineContent = { Text("Show Minus One Page") },
                    supportingContent = { Text("Toggle the widget page on the left") },
                    trailingContent = {
                        Switch(
                            checked = showMinusOnePage,
                            onCheckedChange = { viewModel.setShowMinusOnePage(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setShowMinusOnePage(!showMinusOnePage) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Show App Library") },
                    supportingContent = { Text("Toggle the app library page on the right") },
                    trailingContent = {
                        Switch(
                            checked = showAppLibrary,
                            onCheckedChange = { viewModel.setShowAppLibrary(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setShowAppLibrary(!showAppLibrary) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

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
                    stringResource(R.string.home_menu_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    stringResource(R.string.home_menu_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                val menuOptions by viewModel.homeMenuOptions.collectAsState()
                val availableOptions = listOf(
                    "delete_home" to R.string.menu_delete_home,
                    "edit" to R.string.menu_edit,
                    "uninstall" to R.string.menu_uninstall,
                    "hide" to R.string.menu_hide,
                    "favorite" to R.string.menu_add_favorite,
                    "app_info" to R.string.menu_app_info
                )

                Column {
                    availableOptions.forEach { (key, resId) ->
                        ListItem(
                            headlineContent = { Text(stringResource(resId)) },
                            trailingContent = {
                                Switch(
                                    checked = menuOptions.contains(key),
                                    onCheckedChange = { viewModel.setHomeMenuOption(key, it) }
                                )
                            },
                            modifier = Modifier.clickable { viewModel.setHomeMenuOption(key, !menuOptions.contains(key)) }
                        )
                    }
                }
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
                            val appIcon = if (app != null) viewModel.getIcon(app.packageName) else null
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(shape).background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            if (pkgName.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateDockApp(index, "") }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
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
                iconShape = iconShape,
                viewModel = viewModel,
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
    val iconShape by viewModel.iconShape.collectAsState()

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
        }
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
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForDoubleTap = false }, onAppSelected = { viewModel.setDoubleTapApp(it); showAppPickerForDoubleTap = false })
    }
    if (showAppPickerForSwipeUp) {
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForSwipeUp = false }, onAppSelected = { viewModel.setSwipeUpApp(it); showAppPickerForSwipeUp = false })
    }
    if (showAppPickerForSwipeDown) {
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForSwipeDown = false }, onAppSelected = { viewModel.setSwipeDownApp(it); showAppPickerForSwipeDown = false })
    }
    if (showAppPickerForLongPress) {
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForLongPress = false }, onAppSelected = { viewModel.setLongPressApp(it); showAppPickerForLongPress = false })
    }
    if (showAppPickerForTwoFingerSwipeUp) {
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForTwoFingerSwipeUp = false }, onAppSelected = { viewModel.setTwoFingerSwipeUpApp(it); showAppPickerForTwoFingerSwipeUp = false })
    }
    if (showAppPickerForTwoFingerSwipeDown) {
        AppPickerDialog(allApps.filter { !it.isHidden }, iconShape, viewModel, onDismiss = { showAppPickerForTwoFingerSwipeDown = false }, onAppSelected = { viewModel.setTwoFingerSwipeDownApp(it); showAppPickerForTwoFingerSwipeDown = false })
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
    viewModel: MainViewModel,
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
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
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
                                val appIcon = viewModel.getIcon(app.packageName)
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
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
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

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

    var scale by remember { mutableFloatStateOf(1f) }
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
    
    // 優化點：將耗時的圖標處理移至後台線程，避免阻塞 UI 滑動
    val previewBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, 
        bgColor, fgColor, useOriginal, useOriginalBg, iconShape
    ) {
        value = withContext(Dispatchers.Default) {
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
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Custom Style", modifier = Modifier.weight(1f))
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.1f))
                    )
                } else {
                    Box(modifier = Modifier.size(48.dp).background(Color.Gray.copy(alpha = 0.1f), if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)))
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // 聯絡人權限狀態
    var hasContactsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // 通知權限狀態
    var isNotificationEnabled by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    // 無障礙服務狀態
    val isAccessibilityEnabled = {
        val expectedComponentName = ComponentName(context, IterationAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        enabledServices?.contains(expectedComponentName) == true
    }
    var isServiceActive by remember { mutableStateOf(isAccessibilityEnabled()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_permissions)) },
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
                    headlineContent = { Text(stringResource(R.string.permission_contacts)) },
                    supportingContent = { Text(stringResource(R.string.permission_contacts_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hasContactsPermission,
                            onCheckedChange = {
                                if (!hasContactsPermission) {
                                    launcher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        if (!hasContactsPermission) {
                            launcher.launch(android.Manifest.permission.READ_CONTACTS)
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.permission_notifications)) },
                    supportingContent = { Text(stringResource(R.string.permission_notifications_desc)) },
                    trailingContent = {
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.permission_accessibility)) },
                    supportingContent = { Text(stringResource(R.string.permission_accessibility_desc)) },
                    trailingContent = {
                        Switch(
                            checked = isServiceActive,
                            onCheckedChange = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PaddingRemaining(16.dp) {
                    Text(
                        "Privacy Note",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Iteration Launcher processes all data locally on your device. Your contacts and notifications are never uploaded to any server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 生命週期監聽，用於從系統設定回來後更新狀態
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasContactsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                isNotificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                isServiceActive = isAccessibilityEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val currentSearchEngineUrl by viewModel.searchEngineUrl.collectAsState()
    var customUrl by remember { mutableStateOf(currentSearchEngineUrl) }

    val searchEngines = listOf(
        "Google" to "https://www.google.com/search?q=",
        "Microsoft Bing" to "https://www.bing.com/search?q=",
        "Baidu" to "https://www.baidu.com/s?wd=",
        "Yahoo!" to "https://search.yahoo.com/search?p=",
        "DuckDuckGo" to "https://duckduckgo.com/?q=",
        "Yandex" to "https://yandex.com/search/?text=",
        "WolframAlpha" to "https://www.wolframalpha.com/input/?i=",
        "Brave Search" to "https://search.brave.com/search?q=",
        "Ecosia" to "https://www.ecosia.org/search?q=",
        "Startpage" to "https://www.startpage.com/do/search?q=",
        "Swisscows" to "https://swisscows.com/web?query=",
        "Perplexity" to "https://www.perplexity.ai/search?q=",
        "You.com" to "https://you.com/search?q=",
        "Naver" to "https://search.naver.com/search.naver?query=",
        "Qwant" to "https://www.qwant.com/?q=",
        "Seznam" to "https://search.seznam.cz/?q="
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_search)) },
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
                PaddingRemaining(16.dp) {
                    Text(
                        stringResource(R.string.search_engine),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.search_engine_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(searchEngines) { (name, url) ->
                val isSelected = currentSearchEngineUrl == url
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = { Text(url, maxLines = 1) },
                    leadingContent = {
                        RadioButton(selected = isSelected, onClick = { viewModel.setSearchEngineUrl(url) })
                    },
                    modifier = Modifier.clickable { viewModel.setSearchEngineUrl(url) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.custom_search_url), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.custom_search_url_desc), style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/search?q=") },
                        singleLine = true,
                        trailingIcon = {
                            if (customUrl != currentSearchEngineUrl) {
                                IconButton(onClick = { viewModel.setSearchEngineUrl(customUrl) }) {
                                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }
        }
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
