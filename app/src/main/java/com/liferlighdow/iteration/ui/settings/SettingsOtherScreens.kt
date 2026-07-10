package com.liferlighdow.iteration.ui.settings

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.service.IterationAccessibilityService
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.viewmodel.*
import rikka.shizuku.Shizuku
import kotlin.math.roundToInt

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
    
    val iconShape by viewModel.iconShape.collectAsState()

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
