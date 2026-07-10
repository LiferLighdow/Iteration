package com.liferlighdow.iteration.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.exportConfig
import com.liferlighdow.iteration.viewmodel.importConfig

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
    onNavigateToPermissions: () -> Unit,
    onNavigateToManuals: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToPwaMaker: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    var showRestartDialog by remember { mutableStateOf(false) }
    var showPasswordGate by remember { mutableStateOf(false) }
    var showApiWarningDialog by remember { mutableStateOf(false) }

    // 定義所有設定項的元數據，以便進行搜尋
    val allSettingsItems = remember {
        listOf(
            SettingsMetadata(context.getString(R.string.settings_icon_theme), context.getString(R.string.settings_icon_theme_desc), Icons.Default.Palette, Color(0xFF4285F4), onNavigateToIconTheme),
            SettingsMetadata(context.getString(R.string.pwa_maker_title), context.getString(R.string.pwa_maker_desc), Icons.Default.Public, Color(0xFF009688), onNavigateToPwaMaker),
            SettingsMetadata(context.getString(R.string.liquid_glass_title), context.getString(R.string.settings_liquid_glass_desc), Icons.Default.BlurOn, Color(0xFF34A853), {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                    showApiWarningDialog = true
                } else {
                    onNavigateToLiquidGlass()
                }
            }, isLiquidGlass = true),
            SettingsMetadata(context.getString(R.string.settings_home_screen), context.getString(R.string.settings_home_screen_desc), Icons.Default.Dashboard, Color(0xFFFBBC04), onNavigateToDock),
            SettingsMetadata(context.getString(R.string.settings_library), context.getString(R.string.settings_library_desc), Icons.Default.Apps, Color(0xFFEA4335), onNavigateToAppLibrary),
            SettingsMetadata(context.getString(R.string.settings_gestures), context.getString(R.string.settings_gestures_desc), Icons.Default.TouchApp, Color(0xFF9C27B0), onNavigateToGestures),
            SettingsMetadata(context.getString(R.string.settings_search), context.getString(R.string.settings_search_desc), Icons.Default.Search, Color(0xFF00ACC1), onNavigateToSearch),
            SettingsMetadata(context.getString(R.string.settings_permissions), context.getString(R.string.settings_permissions_desc), Icons.Default.Security, Color(0xFF607D8B), onNavigateToPermissions),
            SettingsMetadata(context.getString(R.string.settings_language), context.getString(R.string.language_system_default), Icons.Default.Language, Color(0xFF3F51B5), onNavigateToLanguage),
            SettingsMetadata(context.getString(R.string.settings_advanced), context.getString(R.string.settings_advanced_desc), Icons.Default.Settings, Color(0xFF607D8B), onNavigateToAdvanced),
            SettingsMetadata(context.getString(R.string.user_manual_title), context.getString(R.string.user_manual_desc), Icons.AutoMirrored.Filled.MenuBook, Color(0xFFFF9800), onNavigateToManuals),
            SettingsMetadata(context.getString(R.string.settings_hide_apps), context.getString(R.string.settings_hide_apps_desc), Icons.Default.VisibilityOff, Color(0xFF795548), {
                if (viewModel.getPassword().isNullOrEmpty()) onNavigateToHideApps() else {} // 觸發 PasswordGate
            }, isHideApps = true),
            SettingsMetadata(context.getString(R.string.settings_rename_apps), context.getString(R.string.settings_rename_apps_desc), Icons.Default.Edit, Color(0xFF673AB7), onNavigateToRenameApps),
            SettingsMetadata(context.getString(R.string.settings_export), context.getString(R.string.settings_backup_restore_desc), Icons.Default.Backup, Color(0xFF4CAF50), { /* Launcher Logic */ }, isExport = true),
            SettingsMetadata(context.getString(R.string.settings_import), context.getString(R.string.import_from_backup), Icons.Default.Restore, Color(0xFF03A9F4), { /* Launcher Logic */ }, isImport = true),
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
                Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, context.getString(R.string.import_failed_msg, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                            Text(stringResource(R.string.no_results_found, searchQuery), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                                    showApiWarningDialog = true
                                                } else {
                                                    onNavigateToLiquidGlass()
                                                }
                                            }
                                            item.isHideApps -> {
                                                if (viewModel.getPassword().isNullOrEmpty()) onNavigateToHideApps()
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
                        stringResource(R.string.customization),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.settings_icon_theme),
                            supporting = stringResource(R.string.settings_icon_theme_desc),
                            icon = Icons.Default.Palette,
                            iconColor = Color(0xFF4285F4),
                            onClick = onNavigateToIconTheme
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.pwa_maker_title),
                            supporting = stringResource(R.string.pwa_maker_desc),
                            icon = Icons.Default.Public,
                            iconColor = Color(0xFF009688),
                            onClick = onNavigateToPwaMaker
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.liquid_glass_title),
                            supporting = stringResource(R.string.settings_liquid_glass_desc),
                            icon = Icons.Default.BlurOn,
                            iconColor = Color(0xFF34A853),
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                    showApiWarningDialog = true
                                } else {
                                    onNavigateToLiquidGlass()
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.settings_home_screen),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.settings_home_screen),
                            supporting = stringResource(R.string.settings_home_screen_desc),
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
                        stringResource(R.string.system_interaction),
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
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        val appLanguage by viewModel.appLanguage.collectAsState()
                        val langOptions = listOf(
                            "" to stringResource(R.string.language_system_default),
                            "en" to stringResource(R.string.language_english),
                            "zh" to stringResource(R.string.language_chinese_special),
                            "zh-TW" to stringResource(R.string.language_chinese_tw),
                            "zh-CN" to stringResource(R.string.language_chinese_cn)
                        )
                        val currentLangLabel = langOptions.find { it.first == appLanguage }?.second ?: stringResource(R.string.language_system_default)

                        SettingsItem(
                            headline = stringResource(R.string.settings_language),
                            supporting = if (appLanguage == "") stringResource(R.string.language_system_default_desc) else currentLangLabel,
                            icon = Icons.Default.Language,
                            iconColor = Color(0xFF3F51B5),
                            onClick = onNavigateToLanguage
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsItem(
                            headline = stringResource(R.string.settings_advanced),
                            supporting = stringResource(R.string.settings_advanced_desc),
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF607D8B),
                            onClick = onNavigateToAdvanced
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.security_section),
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
                                if (viewModel.getPassword().isNullOrEmpty()) onNavigateToHideApps()
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
                        stringResource(R.string.user_manual_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    SettingsGroup {
                        SettingsItem(
                            headline = stringResource(R.string.user_manual_title),
                            supporting = stringResource(R.string.user_manual_desc),
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            iconColor = Color(0xFFFF9800),
                            onClick = onNavigateToManuals
                        )
                    }
                }

                item {
                    Text(
                        stringResource(R.string.settings_backup_restore),
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
                            supporting = stringResource(R.string.import_from_backup),
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

    if (showApiWarningDialog) {
        AlertDialog(
            onDismissRequest = { showApiWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.compat_warning_title)) },
            text = { 
                Text(stringResource(R.string.compat_warning_msg, android.os.Build.VERSION.SDK_INT)) 
            },
            confirmButton = {
                TextButton(onClick = { 
                    showApiWarningDialog = false 
                    onNavigateToLiquidGlass()
                }) {
                    Text(stringResource(R.string.understand))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiWarningDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                            text = stringResource(R.string.incorrect_password),
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
