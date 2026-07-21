package com.liferlighdow.iteration.ui.settings

import android.graphics.Bitmap
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService
import com.liferlighdow.iteration.data.*
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.ui.widgets.*
import com.liferlighdow.iteration.ui.showNativeUninstallDialog
import rikka.shizuku.Shizuku
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val isNetworkEnabled by viewModel.isNetworkAccessEnabled.collectAsState()
    val isSystemNetworkEnabled by viewModel.isSystemNetworkEnabled.collectAsState()
    val actionMode by viewModel.actionMode.collectAsState()
    
    // 聯絡人權限狀態
    var hasContactsPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    // 日曆權限狀態
    var hasCalendarPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var hasAllFilesPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else true
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

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCalendarPermission = isGranted
    }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
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
                    headlineContent = { Text(stringResource(R.string.network_access_usage)) },
                    supportingContent = { 
                        Text(if (isSystemNetworkEnabled) stringResource(R.string.network_connected) else stringResource(R.string.network_restricted))
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        try {
                            // 嘗試直接進入 App 的數據用量頁面 (部分系統支持)
                            val intent = Intent("android.settings.APP_DATA_USAGE")
                            intent.putExtra("package", context.packageName)
                            intent.putExtra("uid", context.applicationInfo.uid)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 備選：進入 App Info 頁面，用戶可從中進入數據用量
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.internal_network_toggle)) },
                    supportingContent = { Text(stringResource(R.string.internal_network_desc)) },
                    trailingContent = {
                        Switch(
                            checked = isNetworkEnabled,
                            onCheckedChange = { viewModel.setNetworkAccessEnabled(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setNetworkAccessEnabled(!isNetworkEnabled) }
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
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
                    headlineContent = { Text(stringResource(R.string.permission_calendar)) },
                    supportingContent = { Text(stringResource(R.string.permission_calendar_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hasCalendarPermission,
                            onCheckedChange = {
                                if (!hasCalendarPermission) {
                                    calendarLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        if (!hasCalendarPermission) {
                            calendarLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.permission_files)) },
                    supportingContent = { Text(stringResource(R.string.permission_files_desc)) },
                    trailingContent = {
                        Switch(
                            checked = hasStoragePermission,
                            onCheckedChange = {
                                if (!hasStoragePermission) {
                                    storageLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        if (!hasStoragePermission) {
                            storageLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.permission_files_manage)) },
                        supportingContent = { Text(stringResource(R.string.permission_files_manage_desc)) },
                        trailingContent = {
                            Switch(
                                checked = hasAllFilesPermission,
                                onCheckedChange = {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
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
                var expanded by remember { mutableStateOf(false) }
                val options = listOf(
                    ActionMode.ACCESSIBILITY to stringResource(R.string.action_mode_accessibility),
                    ActionMode.SHIZUKU to stringResource(R.string.action_mode_shizuku),
                    ActionMode.ROOT to stringResource(R.string.action_mode_root)
                )
                val currentLabel = options.find { it.first == actionMode }?.second ?: stringResource(R.string.action_mode_accessibility)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_action_mode)) },
                    supportingContent = { Text(stringResource(R.string.action_mode_desc)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(currentLabel)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                options.forEach { (mode, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            when (mode) {
                                                ActionMode.ROOT -> {
                                                    viewModel.requestRootAccess { success ->
                                                        if (success) {
                                                            viewModel.setActionMode(mode)
                                                            Toast.makeText(context, context.getString(R.string.root_permission_granted), Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, context.getString(R.string.root_permission_failed), Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                                ActionMode.SHIZUKU -> {
                                                    if (Shizuku.pingBinder()) {
                                                        if (viewModel.checkShizukuPermission()) {
                                                            viewModel.setActionMode(mode)
                                                        } else {
                                                            Toast.makeText(context, context.getString(R.string.shizuku_permission_request), Toast.LENGTH_SHORT).show()
                                                            Shizuku.requestPermission(0)
                                                        }
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.shizuku_not_running), Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                else -> {
                                                    viewModel.setActionMode(mode)
                                                }
                                            }
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.clickable { expanded = true }
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PaddingRemaining(16.dp) {
                    Text(
                        stringResource(R.string.privacy_note),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.privacy_note_desc),
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
        val shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                viewModel.setActionMode(ActionMode.SHIZUKU)
                Toast.makeText(context, "Shizuku permission granted", Toast.LENGTH_SHORT).show()
            }
        }
        
        try {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
        } catch (e: Exception) {}

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasContactsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                isNotificationEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                isServiceActive = isAccessibilityEnabled()
                viewModel.checkSystemNetworkStatus()
                viewModel.loadContacts()
                viewModel.loadCalendarEvents()
                viewModel.loadFiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { 
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                Shizuku.removeRequestPermissionResultListener(shizukuListener)
            } catch (e: Exception) {}
        }
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
                        placeholder = { Text(stringResource(R.string.search_url_placeholder)) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val appLanguage by viewModel.appLanguage.collectAsState()
    
    val langOptions = listOf(
        "" to stringResource(R.string.language_system_default),
        "en" to stringResource(R.string.language_english),
        "zh" to stringResource(R.string.language_chinese_special),
        "zh-TW" to stringResource(R.string.language_chinese_tw),
        "zh-CN" to stringResource(R.string.language_chinese_cn)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_language)) },
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
                        stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.language_system_default_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(langOptions) { (code, label) ->
                val isSelected = appLanguage == code
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = {
                        RadioButton(selected = isSelected, onClick = { viewModel.setAppLanguage(code) })
                    },
                    modifier = Modifier.clickable { viewModel.setAppLanguage(code) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val cacheSize by viewModel.iconCacheSize.collectAsState()
    val iconSize by viewModel.iconSizePx.collectAsState()
    val updateInterval by viewModel.updateCheckInterval.collectAsState()
    val density = LocalContext.current.resources.displayMetrics.density
    
    // 預設值同樣套用 16px 階梯對齊邏輯，確保 UI 顯示一致
    val snappedDefault = (Math.round(62 * density / 16f) * 16).toInt().coerceIn(32, 256)
    val effectiveIconSize = if (iconSize > 0) iconSize else snappedDefault

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_advanced)) },
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
                        stringResource(R.string.icon_size_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.icon_size_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.icon_size_current, effectiveIconSize),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.setIconSizePx((effectiveIconSize - 16).coerceAtLeast(32))
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Slider(
                            value = effectiveIconSize.toFloat(),
                            onValueChange = { 
                                // 滑動時同樣進行 16px 對齊
                                val snapped = (Math.round(it / 16f) * 16).coerceIn(32, 256)
                                viewModel.setIconSizePx(snapped) 
                            },
                            valueRange = 32f..256f,
                            steps = 13, // (256-32)/16 - 1 = 13 steps
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            viewModel.setIconSizePx((effectiveIconSize + 16).coerceAtMost(256))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.applyRecommendedIconSize() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.icon_size_recommend))
                        }
                        
                        TextButton(
                            onClick = { viewModel.setIconSizePx(-1) },
                            enabled = iconSize != -1,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.restore_default))
                        }
                    }

                    Text(
                        stringResource(R.string.icon_size_recommend_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                    // --- 新增：Icon 尺寸微調 (95% - 105%) ---
                    val iconScale by viewModel.iconScale.collectAsState()
                    Text(
                        stringResource(R.string.icon_scale_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.icon_scale_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.icon_scale_current, (iconScale * 100).roundToInt()),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.setIconScale((iconScale - 0.002f).coerceAtLeast(0.9f))
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Slider(
                            value = iconScale,
                            onValueChange = { viewModel.setIconScale(it) },
                            valueRange = 0.9f..1.1f,
                            steps = 99, // 100等分
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            viewModel.setIconScale((iconScale + 0.002f).coerceAtMost(1.1f))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                    Text(
                        stringResource(R.string.icon_cache_size_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.icon_cache_size_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        stringResource(R.string.icon_cache_current, cacheSize),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        IconButton(onClick = { 
                            viewModel.setIconCacheSize((cacheSize - 10).coerceAtLeast(250)) 
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        Slider(
                            value = cacheSize.toFloat(),
                            onValueChange = { viewModel.setIconCacheSize(it.roundToInt()) },
                            valueRange = 250f..1000f,
                            modifier = Modifier.weight(1f)
                        )
                        
                        IconButton(onClick = { 
                            viewModel.setIconCacheSize((cacheSize + 10).coerceAtMost(1000)) 
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        stringResource(R.string.settings_update_check_interval),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.settings_update_check_interval_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(R.string.settings_update_check_hours, updateInterval),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        IconButton(onClick = {
                            viewModel.setUpdateCheckInterval((updateInterval - 1).coerceAtLeast(0))
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }

                        Slider(
                            value = updateInterval.toFloat(),
                            onValueChange = { viewModel.setUpdateCheckInterval(it.roundToInt()) },
                            valueRange = 0f..36f,
                            steps = 35,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            viewModel.setUpdateCheckInterval((updateInterval + 1).coerceAtMost(36))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PwaMakerScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    
    var label by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var bgColor by remember { mutableIntStateOf(0xFF2196F3.toInt()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pwa_maker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(stringResource(R.string.pwa_maker_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            item {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.pwa_label_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.pwa_url_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://...") }
                )
            }
            
            item {
                Text(stringResource(R.string.background_color), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                ColorPicker(
                    initialColor = bgColor,
                    onColorChanged = { bgColor = it }
                )
            }

            item {
                val useVNavi by viewModel.useVNaviForPwa.collectAsState()
                SettingsItem(
                    headline = stringResource(R.string.vnavi_use_vnavi_headline),
                    supporting = stringResource(R.string.vnavi_use_vnavi_supporting),
                    icon = Icons.Default.Web,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        val next = !useVNavi
                        viewModel.setUseVNaviForPwa(next)
                        if (next) {
                            val vNaviPackage = "com.liferlighdow.vnavi"
                            try {
                                context.packageManager.getPackageInfo(vNaviPackage, 0)
                            } catch (e: Exception) {
                                viewModel._showVNaviInstallDialog.value = true
                            }
                        }
                    },
                    trailing = {
                        Switch(checked = useVNavi, onCheckedChange = null)
                    }
                )
            }
            
            item {
                Button(
                    onClick = {
                        if (label.isNotBlank() && url.isNotBlank()) {
                            viewModel.createPWA(label, url, bgColor)
                            Toast.makeText(context, context.getString(R.string.pwa_created), Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, context.getString(R.string.pwa_invalid_input), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.create))
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PwaManageScreen(onBack: () -> Unit, onNavigateToPwaMaker: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val pwaApps by viewModel.pwaApps.collectAsState()
    var editingApp by remember { mutableStateOf<AppModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pwa_manage_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.vnavi_pwa_engine),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                val useVNavi by viewModel.useVNaviForPwa.collectAsState()
                SettingsGroup {
                    SettingsItem(
                        headline = stringResource(R.string.vnavi_use_vnavi_headline),
                        supporting = stringResource(R.string.vnavi_use_vnavi_supporting),
                        icon = Icons.Default.Web,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val next = !useVNavi
                            viewModel.setUseVNaviForPwa(next)
                            if (next) {
                                val vNaviPackage = "com.liferlighdow.vnavi"
                                try {
                                    context.packageManager.getPackageInfo(vNaviPackage, 0)
                                } catch (e: Exception) {
                                    viewModel._showVNaviInstallDialog.value = true
                                }
                            }
                        },
                        trailing = {
                            Switch(checked = useVNavi, onCheckedChange = null)
                        }
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.pwa_maker_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsItem(
                        headline = stringResource(R.string.pwa_maker_title),
                        supporting = stringResource(R.string.pwa_maker_desc),
                        icon = Icons.Default.Add,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToPwaMaker
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.pwa_manage_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (pwaApps.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.pwa_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    SettingsGroup {
                        pwaApps.forEachIndexed { index, app ->
                            SettingsItem(
                                headline = app.label,
                                supporting = app.shortcutId,
                                icon = Icons.Default.Public,
                                iconColor = Color(app.pwaBgColor),
                                onClick = { editingApp = app }
                            )
                            if (index < pwaApps.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingApp != null) {
        EditPwaDialog(
            app = editingApp!!,
            onDismiss = { editingApp = null },
            onSave = { label, url, color ->
                viewModel.updatePWA(editingApp!!.uniqueId, label, url, color)
                editingApp = null
            },
            onDelete = {
                showNativeUninstallDialog(context, editingApp!!.label) {
                    viewModel.deletePWA(editingApp!!)
                }
                editingApp = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPwaDialog(
    app: AppModel,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    val viewModel: MainViewModel = viewModel()
    var label by remember { mutableStateOf(app.label) }
    var url by remember { mutableStateOf(app.shortcutId ?: "") }
    var bgColor by remember { mutableIntStateOf(app.pwaBgColor) }
    
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pickedImageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_pwa)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.pwa_label_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.pwa_url_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { launcher.launch("image/*") }
                    ) {
                        Text(stringResource(R.string.settings_change_icon), style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.weight(1f))
                        val appIcon = viewModel.getIcon(app.uniqueId)
                        if (appIcon != null) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.background_color), style = MaterialTheme.typography.titleSmall)
                    ColorPicker(initialColor = bgColor, onColorChanged = { bgColor = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(label, url, bgColor) }) {
                Text(stringResource(R.string.save_changes))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    )

    if (pickedImageUri != null) {
        IconCropperDialog(
            uri = pickedImageUri!!,
            onDismiss = { pickedImageUri = null },
            onConfirm = { croppedBitmap ->
                viewModel.setCustomIcon(app.uniqueId, croppedBitmap)
                pickedImageUri = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetMakerScreen(onBack: () -> Unit, onNavigateToWorkshop: (String) -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val customWidgets by viewModel.customWidgets.collectAsState()
    var showSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_maker_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.widget_maker_new),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsItem(
                        headline = stringResource(R.string.widget_maker_new),
                        supporting = stringResource(R.string.widget_maker_desc),
                        icon = Icons.Default.Add,
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = { showSizeDialog = true }
                    )
                }
            }

            item {
                Text(
                    stringResource(R.string.widget_maker_list_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            if (customWidgets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.widget_maker_no_widgets), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                item {
                    SettingsGroup {
                        customWidgets.forEachIndexed { index, widget ->
                            val size = (widget.type as? WidgetType.Custom)?.size ?: "Unknown"
                            SettingsItem(
                                headline = widget.label,
                                supporting = "Size: $size",
                                icon = Icons.Default.Widgets,
                                iconColor = Color(0xFF673AB7),
                                onClick = { onNavigateToWorkshop(widget.id) }
                            )
                            if (index < customWidgets.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSizeDialog) {
        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            title = { Text(stringResource(R.string.widget_maker_select_size)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.widget_size_2x2)) },
                        leadingContent = { Icon(Icons.Default.Square, null) },
                        modifier = Modifier.clickable {
                            viewModel.createCustomWidget("2x2")
                            showSizeDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.widget_size_4x2)) },
                        leadingContent = { Icon(Icons.Default.Rectangle, null) },
                        modifier = Modifier.clickable {
                            viewModel.createCustomWidget("4x2")
                            showSizeDialog = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSizeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWorkshopScreen(widgetId: String, onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val customWidgets by viewModel.customWidgets.collectAsState()
    val widget = remember(widgetId, customWidgets) { customWidgets.find { it.id == widgetId } }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingComponent by remember { mutableStateOf<CustomComponent?>(null) }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    // 用於圖片選取的 Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            if (editingComponent is CustomComponent.Image) {
                val updated = (editingComponent as CustomComponent.Image).copy(uri = it.toString())
                viewModel.updateComponentInCustomWidget(widgetId, updated)
                editingComponent = updated
            }
        }
    }

    val tabs = listOf(
        stringResource(R.string.workshop_tab_items),
        stringResource(R.string.workshop_tab_layer),
        stringResource(R.string.workshop_tab_touch)
    )

    if (widget == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        LaunchedEffect(Unit) { onBack() }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { showRenameDialog = true }) {
                        Text(widget.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editingComponent != null) editingComponent = null else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (editingComponent == null) {
                        IconButton(onClick = { /* Undo */ }) { Icon(Icons.Default.Undo, null) }
                        IconButton(onClick = { /* Redo */ }) { Icon(Icons.Default.Redo, null) }
                        IconButton(onClick = { /* Save */ }) { Icon(Icons.Default.Save, null) }
                        IconButton(onClick = { showAddComponentDialog = true }) { Icon(Icons.Default.Add, null) }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. Preview Area
            val density = LocalDensity.current.density
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val size = (widget.type as? WidgetType.Custom)?.size ?: "2x2"
                val aspectRatio = if (size == "4x2") 2f else 1f
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(aspectRatio)
                ) {
                    CustomWidget(
                        widget = widget,
                        workshopComponentModifier = { comp ->
                            // 拖動邏輯
                            Modifier.pointerInput(comp.id) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val updated = when(comp) {
                                        is CustomComponent.Text -> comp.copy(x = comp.x + dragAmount.x / density, y = comp.y + dragAmount.y / density)
                                        is CustomComponent.Shape -> comp.copy(x = comp.x + dragAmount.x / density, y = comp.y + dragAmount.y / density)
                                        is CustomComponent.Progress -> comp.copy(x = comp.x + dragAmount.x / density, y = comp.y + dragAmount.y / density)
                                        is CustomComponent.Image -> comp.copy(x = comp.x + dragAmount.x / density, y = comp.y + dragAmount.y / density)
                                    }
                                    viewModel.updateComponentInCustomWidget(widgetId, updated)
                                    if (editingComponent?.id == comp.id) editingComponent = updated
                                }
                            }
                        }
                    )
                }
            }

            // 2. Editor Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (editingComponent == null) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { 
                                    Text(
                                        text = title, 
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selectedTab == index) androidx.compose.ui.text.font.FontWeight.Bold else null
                                    ) 
                                }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> WorkshopItemsTab(
                            widget = widget,
                            onEdit = { editingComponent = it },
                            onToggleVisible = { componentId ->
                                val comp = (widget.type as? WidgetType.Custom)?.components?.find { it.id == componentId }
                                if (comp != null) {
                                    viewModel.updateComponentInCustomWidget(widgetId, when(comp) {
                                        is CustomComponent.Text -> comp.copy(isVisible = !comp.isVisible)
                                        is CustomComponent.Shape -> comp.copy(isVisible = !comp.isVisible)
                                        is CustomComponent.Progress -> comp.copy(isVisible = !comp.isVisible)
                                        is CustomComponent.Image -> comp.copy(isVisible = !comp.isVisible)
                                    })
                                }
                            },
                            onMove = { id, up -> viewModel.moveComponentInCustomWidget(widgetId, id, up) }
                        )
                        1 -> WorkshopLayerTab(widget)
                        2 -> WorkshopTouchTab(widget)
                    }
                } else {
                    WorkshopComponentDetailEditor(
                        component = editingComponent!!,
                        onUpdate = { updated: CustomComponent ->
                            viewModel.updateComponentInCustomWidget(widgetId, updated)
                            editingComponent = updated
                        },
                        onDelete = {
                            viewModel.removeComponentFromCustomWidget(widgetId, editingComponent!!.id)
                            editingComponent = null
                        },
                        onPickImage = { imagePickerLauncher.launch("image/*") }
                    )
                }
            }
        }
    }

    if (showAddComponentDialog) {
        AddComponentDialog(
            onDismiss = { showAddComponentDialog = false },
            onAdd = { type ->
                viewModel.addComponentToCustomWidget(widgetId, type)
                showAddComponentDialog = false
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(widget.label) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateCustomWidgetLabel(widgetId, newName)
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun WorkshopItemsTab(
    widget: WidgetModel,
    onEdit: (CustomComponent) -> Unit,
    onToggleVisible: (String) -> Unit,
    onMove: (String, Boolean) -> Unit
) {
    val components = (widget.type as? WidgetType.Custom)?.components ?: emptyList()
    
    if (components.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.widget_maker_no_widgets), style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(components) { index, component ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(component) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (component) {
                        is CustomComponent.Text -> Icons.Default.TextFields
                        is CustomComponent.Shape -> Icons.Default.Category
                        is CustomComponent.Progress -> Icons.Default.DonutLarge
                        is CustomComponent.Image -> Icons.Default.Image
                    }
                    Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                    
                    Spacer(Modifier.width(16.dp))

                    Text(
                        text = component.name, 
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

                    Row {
                        IconButton(onClick = { onMove(component.id, true) }, enabled = index > 0, modifier = Modifier.size(36.dp)) { 
                            Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(20.dp), tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) 
                        }
                        IconButton(onClick = { onMove(component.id, false) }, enabled = index < components.size - 1, modifier = Modifier.size(36.dp)) { 
                            Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(20.dp), tint = if (index < components.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) 
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Checkbox(
                        checked = component.isVisible, 
                        onCheckedChange = { onToggleVisible(component.id) },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddComponentDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.workshop_add_component)) },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Text") },
                    supportingContent = { Text("Support variables like [TIME], [BATT]") },
                    leadingContent = { Icon(Icons.Default.TextFields, null) },
                    modifier = Modifier.clickable { onAdd("TEXT") }
                )
                ListItem(
                    headlineContent = { Text("Shape") },
                    supportingContent = { Text("Simple rectangle or circle background") },
                    leadingContent = { Icon(Icons.Default.Category, null) },
                    modifier = Modifier.clickable { onAdd("SHAPE") }
                )
                ListItem(
                    headlineContent = { Text("Progress Bar") },
                    supportingContent = { Text("Dynamic data progress indicator") },
                    leadingContent = { Icon(Icons.Default.DonutLarge, null) },
                    modifier = Modifier.clickable { onAdd("PROGRESS") }
                )
                ListItem(
                    headlineContent = { Text("Image") },
                    supportingContent = { Text("Custom picture from gallery") },
                    leadingContent = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.clickable { onAdd("IMAGE") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun WorkshopLayerTab(widget: WidgetModel) {
    val viewModel: MainViewModel = viewModel()
    val custom = widget.type as? WidgetType.Custom ?: return
    
    LazyColumn(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(stringResource(R.string.workshop_tab_layer), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        item {
            Text("Global Scale", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Slider(
                value = custom.globalScale,
                onValueChange = { viewModel.updateCustomWidgetGlobal(widget.id, scale = it) },
                valueRange = 0.5f..2.5f,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            Text("Global Offset", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                CoordinateControl("X", custom.globalX, { viewModel.updateCustomWidgetGlobal(widget.id, x = it) }, Modifier.weight(1f))
                CoordinateControl("Y", custom.globalY, { viewModel.updateCustomWidgetGlobal(widget.id, y = it) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WorkshopTouchTab(widget: WidgetModel) {
    Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.workshop_tab_touch), style = MaterialTheme.typography.titleMedium)
        Text("Configure touch actions within each component's detail editor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CoordinateControl(
    label: String,
    value: Float,
    onUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onUpdate(value - 1f) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
            }
            
            OutlinedTextField(
                value = value.toInt().toString(),
                onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(v) } },
                modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 14.sp
                ),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            FilledTonalIconButton(
                onClick = { onUpdate(value + 1f) },
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun WorkshopComponentDetailEditor(
    component: CustomComponent,
    onUpdate: (CustomComponent) -> Unit,
    onDelete: () -> Unit,
    onPickImage: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Back */ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
        
        item {
            Text("Position", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CoordinateControl(
                    label = "X Offset",
                    value = component.x,
                    onUpdate = { v ->
                        onUpdate(when(component) {
                            is CustomComponent.Text -> component.copy(x = v)
                            is CustomComponent.Shape -> component.copy(x = v)
                            is CustomComponent.Progress -> component.copy(x = v)
                            is CustomComponent.Image -> component.copy(x = v)
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
                CoordinateControl(
                    label = "Y Offset",
                    value = component.y,
                    onUpdate = { v ->
                        onUpdate(when(component) {
                            is CustomComponent.Text -> component.copy(y = v)
                            is CustomComponent.Shape -> component.copy(y = v)
                            is CustomComponent.Progress -> component.copy(y = v)
                            is CustomComponent.Image -> component.copy(y = v)
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (component is CustomComponent.Text) {
            item {
                Text("Content", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Variables: [TIME], [DATE], [BATT], [GREET], [RAM], [WIFI]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = component.content,
                        onValueChange = { onUpdate(component.copy(content = it)) },
                        label = { Text("Formula") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            item {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Font Size: ${component.fontSize}")
                    Slider(
                        value = component.fontSize.toFloat(),
                        onValueChange = { onUpdate(component.copy(fontSize = it.toInt())) },
                        valueRange = 8f..120f
                    )
                    Text("Color")
                    ColorPicker(initialColor = component.color, onColorChanged = { onUpdate(component.copy(color = it)) })
                }
            }
        }

        if (component is CustomComponent.Shape) {
            item {
                Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomShapeType.entries.forEach { type ->
                        FilterChip(
                            selected = component.shapeType == type,
                            onClick = { onUpdate(component.copy(shapeType = type)) },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            item {
                Text("Dimensions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    CoordinateControl("Width", component.width, { onUpdate(component.copy(width = it)) }, Modifier.weight(1f))
                    CoordinateControl("Height", component.height, { onUpdate(component.copy(height = it)) }, Modifier.weight(1f))
                }
            }

            if (component.shapeType == CustomShapeType.RECTANGLE) {
                item {
                    Text("Corner Radius: ${component.cornerRadius.toInt()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Slider(
                        value = component.cornerRadius,
                        onValueChange = { onUpdate(component.copy(cornerRadius = it)) },
                        valueRange = 0f..200f,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                ColorPicker(initialColor = component.color, onColorChanged = { onUpdate(component.copy(color = it)) })
            }
        }

        if (component is CustomComponent.Progress) {
            item {
                Text("Data Binding", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Formula (e.g. [BATT] or 50)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = component.valueFormula,
                        onValueChange = { onUpdate(component.copy(valueFormula = it)) },
                        label = { Text("Progress Formula") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
            item {
                Text("Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Circular Style", modifier = Modifier.weight(1f))
                    Switch(checked = component.isCircular, onCheckedChange = { onUpdate(component.copy(isCircular = it)) })
                }
            }
            item {
                Text("Dimensions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Size: ${component.size.toInt()}")
                    Slider(value = component.size, onValueChange = { onUpdate(component.copy(size = it)) }, valueRange = 10f..400f)
                    
                    if (component.isCircular) {
                        Text("Stroke Width: ${component.strokeWidth.toInt()}")
                        Slider(value = component.strokeWidth, onValueChange = { onUpdate(component.copy(strokeWidth = it)) }, valueRange = 1f..60f)
                    }
                }
            }
            item {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Progress Color")
                    ColorPicker(initialColor = component.color, onColorChanged = { onUpdate(component.copy(color = it)) })
                    Spacer(Modifier.height(16.dp))
                    Text("Track Color")
                    ColorPicker(initialColor = component.trackColor, onColorChanged = { onUpdate(component.copy(trackColor = it)) })
                }
            }
        }

        if (component is CustomComponent.Image) {
            item {
                Text("Image Source", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Button(onClick = onPickImage, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pick from Gallery")
                }
            }
            item {
                Text("Dimensions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    CoordinateControl("Width", component.width, { onUpdate(component.copy(width = it)) }, Modifier.weight(1f))
                    CoordinateControl("Height", component.height, { onUpdate(component.copy(height = it)) }, Modifier.weight(1f))
                }
            }
            item {
                Text("Appearance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Corner Radius: ${component.cornerRadius.toInt()}")
                    Slider(value = component.cornerRadius, onValueChange = { onUpdate(component.copy(cornerRadius = it)) }, valueRange = 0f..200f)
                    Text("Opacity: ${(component.opacity * 100).toInt()}%")
                    Slider(value = component.opacity, onValueChange = { onUpdate(component.copy(opacity = it)) }, valueRange = 0f..1f)
                }
            }
        }

        item {
            Text("Logic Binding", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = component.visibilityFormula ?: "",
                    onValueChange = { onUpdate(when(component) {
                        is CustomComponent.Text -> component.copy(visibilityFormula = it.ifBlank { null })
                        is CustomComponent.Shape -> component.copy(visibilityFormula = it.ifBlank { null })
                        is CustomComponent.Progress -> component.copy(visibilityFormula = it.ifBlank { null })
                        is CustomComponent.Image -> component.copy(visibilityFormula = it.ifBlank { null })
                    }) },
                    label = { Text("Visibility (e.g. HIDE_LOW_BATT)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (component !is CustomComponent.Image) {
                    OutlinedTextField(
                        value = when(component) {
                            is CustomComponent.Text -> component.colorFormula
                            is CustomComponent.Shape -> component.colorFormula
                            is CustomComponent.Progress -> component.colorFormula
                            else -> null
                        } ?: "",
                        onValueChange = { onUpdate(when(component) {
                            is CustomComponent.Text -> component.copy(colorFormula = it.ifBlank { null })
                            is CustomComponent.Shape -> component.copy(colorFormula = it.ifBlank { null })
                            is CustomComponent.Progress -> component.copy(colorFormula = it.ifBlank { null })
                            else -> component
                        }) },
                        label = { Text("Color Formula (e.g. BATT_COLOR)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Text("Interaction", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            val currentAction = component.clickAction ?: WidgetClickAction()
            
            var expanded by remember { mutableStateOf(false) }
            val options = WidgetActionType.entries.filter { it != WidgetActionType.NONE }
            
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { expanded = true }, 
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Action: ${currentAction.type}")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("NONE") }, onClick = {
                            onUpdate(when(component) {
                                is CustomComponent.Text -> component.copy(clickAction = null)
                                is CustomComponent.Shape -> component.copy(clickAction = null)
                                is CustomComponent.Progress -> component.copy(clickAction = null)
                                is CustomComponent.Image -> component.copy(clickAction = null)
                            })
                            expanded = false
                        })
                        options.forEach { type ->
                            DropdownMenuItem(text = { Text(type.name) }, onClick = {
                                onUpdate(when(component) {
                                    is CustomComponent.Text -> component.copy(clickAction = WidgetClickAction(type = type))
                                    is CustomComponent.Shape -> component.copy(clickAction = WidgetClickAction(type = type))
                                    is CustomComponent.Progress -> component.copy(clickAction = WidgetClickAction(type = type))
                                    is CustomComponent.Image -> component.copy(clickAction = WidgetClickAction(type = type))
                                })
                                expanded = false
                            })
                        }
                    }
                }

                if (currentAction.type == WidgetActionType.OPEN_APP) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentAction.value ?: "",
                        onValueChange = { v ->
                            onUpdate(when(component) {
                                is CustomComponent.Text -> component.copy(clickAction = currentAction.copy(value = v))
                                is CustomComponent.Shape -> component.copy(clickAction = currentAction.copy(value = v))
                                is CustomComponent.Progress -> component.copy(clickAction = currentAction.copy(value = v))
                                is CustomComponent.Image -> component.copy(clickAction = currentAction.copy(value = v))
                            })
                        },
                        label = { Text("App Package Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
