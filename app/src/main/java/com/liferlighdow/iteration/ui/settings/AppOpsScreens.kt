package com.liferlighdow.iteration.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppOpsItemModel(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?,
    val isSystem: Boolean,
    val isEnabled: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOpsListScreen(onBack: () -> Unit, onSelectApp: (String) -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<AppOpsItemModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Filters
    var showSystemApps by remember { mutableStateOf(true) }
    var showUserApps by remember { mutableStateOf(true) }
    var showEnabledApps by remember { mutableStateOf(true) }
    var showDisabledApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            val list = packages.map { pkg ->
                val appInfo = pkg.applicationInfo
                val appName = appInfo?.loadLabel(pm)?.toString() ?: pkg.packageName
                val iconDrawable = appInfo?.loadIcon(pm)
                val iconBitmap = try {
                    iconDrawable?.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)?.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
                val isSystem = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM)) != 0
                val isEnabled = appInfo?.enabled ?: true
                AppOpsItemModel(pkg.packageName, appName, iconBitmap, isSystem, isEnabled)
            }.sortedBy { it.appName.lowercase() }
            withContext(Dispatchers.Main) {
                apps = list
                isLoading = false
            }
        }
    }

    val filteredApps = remember(apps, searchQuery, showSystemApps, showUserApps, showEnabledApps, showDisabledApps) {
        apps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)

            val typeMatch = when {
                showSystemApps && showUserApps -> true
                showSystemApps -> app.isSystem
                showUserApps -> !app.isSystem
                else -> true
            }

            val stateMatch = when {
                showEnabledApps && showDisabledApps -> true
                showEnabledApps -> app.isEnabled
                showDisabledApps -> !app.isEnabled
                else -> true
            }

            matchesSearch && typeMatch && stateMatch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_app_ops)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showUserApps,
                    onClick = { showUserApps = !showUserApps },
                    label = { Text(stringResource(R.string.filter_app_user)) }
                )
                FilterChip(
                    selected = showSystemApps,
                    onClick = { showSystemApps = !showSystemApps },
                    label = { Text(stringResource(R.string.filter_app_system)) }
                )
                FilterChip(
                    selected = showEnabledApps,
                    onClick = { showEnabledApps = !showEnabledApps },
                    label = { Text(stringResource(R.string.filter_app_enabled)) }
                )
                FilterChip(
                    selected = showDisabledApps,
                    onClick = { showDisabledApps = !showDisabledApps },
                    label = { Text(stringResource(R.string.filter_app_disabled)) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.appName) },
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectApp(app.packageName) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

fun getPermissionResId(perm: String): Int {
    return when (perm) {
        android.Manifest.permission.CAMERA -> R.string.appops_perm_camera
        android.Manifest.permission.ACCESS_FINE_LOCATION -> R.string.appops_perm_fine_location
        android.Manifest.permission.ACCESS_COARSE_LOCATION -> R.string.appops_perm_coarse_location
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION -> R.string.appops_perm_bg_location
        android.Manifest.permission.READ_CONTACTS -> R.string.appops_perm_read_contacts
        android.Manifest.permission.WRITE_CONTACTS -> R.string.appops_perm_write_contacts
        android.Manifest.permission.READ_CALENDAR -> R.string.appops_perm_read_calendar
        android.Manifest.permission.WRITE_CALENDAR -> R.string.appops_perm_write_calendar
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS -> R.string.appops_perm_record_audio
        android.Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.appops_perm_read_storage
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.appops_perm_write_storage
        android.Manifest.permission.READ_MEDIA_IMAGES -> R.string.appops_perm_read_media_images
        android.Manifest.permission.READ_MEDIA_VIDEO -> R.string.appops_perm_read_media_video
        android.Manifest.permission.READ_MEDIA_AUDIO -> R.string.appops_perm_read_media_audio
        android.Manifest.permission.MANAGE_EXTERNAL_STORAGE -> R.string.appops_perm_manage_storage
        android.Manifest.permission.POST_NOTIFICATIONS -> R.string.appops_perm_notifications
        android.Manifest.permission.READ_PHONE_STATE -> R.string.appops_perm_read_phone_state
        android.Manifest.permission.CALL_PHONE -> R.string.appops_perm_call_phone
        android.Manifest.permission.READ_CALL_LOG -> R.string.appops_perm_read_call_log
        android.Manifest.permission.WRITE_CALL_LOG -> R.string.appops_perm_write_call_log
        android.Manifest.permission.READ_PHONE_NUMBERS -> R.string.appops_perm_read_phone_numbers
        android.Manifest.permission.ANSWER_PHONE_CALLS -> R.string.appops_perm_answer_phone_calls
        android.Manifest.permission.SEND_SMS -> R.string.appops_perm_send_sms
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.RECEIVE_WAP_PUSH,
        android.Manifest.permission.RECEIVE_MMS -> R.string.appops_perm_receive_sms
        android.Manifest.permission.BODY_SENSORS -> R.string.appops_perm_body_sensors
        android.Manifest.permission.BLUETOOTH_CONNECT -> R.string.appops_perm_bluetooth_connect
        android.Manifest.permission.BLUETOOTH_SCAN -> R.string.appops_perm_bluetooth_scan
        android.Manifest.permission.SYSTEM_ALERT_WINDOW -> R.string.appops_perm_system_alert
        android.Manifest.permission.WRITE_SETTINGS -> R.string.appops_perm_write_settings
        android.Manifest.permission.REQUEST_INSTALL_PACKAGES -> R.string.appops_perm_install_packages
        android.Manifest.permission.PACKAGE_USAGE_STATS -> R.string.appops_perm_usage_stats
        android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> R.string.appops_perm_battery_ignore
        android.Manifest.permission.GET_ACCOUNTS -> R.string.appops_perm_accounts
        else -> 0
    }
}

@Composable
fun getLocalizedPermissionName(perm: String): String {
    val resId = getPermissionResId(perm)
    return if (resId != 0) stringResource(resId) else perm.substringAfterLast('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOpsDetailScreen(packageName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val actionMode by viewModel.actionMode.collectAsState()
    val scope = rememberCoroutineScope()

    var packageInfo by remember { mutableStateOf<android.content.pm.PackageInfo?>(null) }
    var appName by remember { mutableStateOf(packageName) }
    var isLoading by remember { mutableStateOf(true) }

    // Common / Uncommon filters
    var showCommonPermissions by remember { mutableStateOf(true) }
    var showUncommonPermissions by remember { mutableStateOf(true) }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val pInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                val appInfo = pInfo.applicationInfo
                val name = appInfo?.loadLabel(pm)?.toString() ?: packageName
                withContext(Dispatchers.Main) {
                    packageInfo = pInfo
                    appName = name
                    isLoading = false
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun executePrivilegedCmd(command: Array<String>): Boolean {
        return when (actionMode) {
            ActionMode.SHIZUKU -> executeShizukuCmdSilent(command)
            ActionMode.DHIZUKU -> executeDhizukuCmdSilent(command)
            ActionMode.ROOT -> executeRootCmdSilent(command)
            else -> false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val permissions = packageInfo?.requestedPermissions?.toList() ?: emptyList()
            val isPrivileged = actionMode == ActionMode.SHIZUKU || actionMode == ActionMode.DHIZUKU || actionMode == ActionMode.ROOT

            val filteredPermissions = remember(permissions, showCommonPermissions, showUncommonPermissions) {
                permissions.filter { perm ->
                    val isCommon = getPermissionResId(perm) != 0
                    when {
                        showCommonPermissions && showUncommonPermissions -> true
                        showCommonPermissions -> isCommon
                        showUncommonPermissions -> !isCommon
                        else -> true
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(
                                    R.string.app_ops_mode_status,
                                    actionMode.name,
                                    if (isPrivileged) stringResource(R.string.app_ops_mode_enabled) else stringResource(R.string.app_ops_mode_disabled)
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Filter Chips for Common / Uncommon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showCommonPermissions,
                            onClick = { showCommonPermissions = !showCommonPermissions },
                            label = { Text(stringResource(R.string.filter_perm_common)) }
                        )
                        FilterChip(
                            selected = showUncommonPermissions,
                            onClick = { showUncommonPermissions = !showUncommonPermissions },
                            label = { Text(stringResource(R.string.filter_perm_uncommon)) }
                        )
                    }

                    Text(
                        stringResource(R.string.requested_permissions_count, filteredPermissions.size, permissions.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (filteredPermissions.isEmpty()) {
                    item {
                        Text(stringResource(R.string.no_matching_permissions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(filteredPermissions) { perm ->
                        val localizedName = getLocalizedPermissionName(perm)
                        var currentMode by remember {
                            mutableStateOf(
                                if (context.packageManager.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED) "Allow" else "Deny"
                            )
                        }
                        var expanded by remember { mutableStateOf(false) }

                        ListItem(
                            headlineContent = { Text(localizedName) },
                            supportingContent = { Text(perm, style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Box {
                                    OutlinedButton(
                                        onClick = { if (isPrivileged) expanded = true },
                                        enabled = isPrivileged
                                    ) {
                                        Text(currentMode)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        listOf("Allow", "Ignore", "Deny").forEach { mode ->
                                            DropdownMenuItem(
                                                text = { Text(mode) },
                                                onClick = {
                                                    expanded = false
                                                    scope.launch(Dispatchers.IO) {
                                                        val success = when (mode) {
                                                            "Allow" -> {
                                                                executePrivilegedCmd(arrayOf("pm", "grant", packageName, perm)) &&
                                                                executePrivilegedCmd(arrayOf("appops", "set", packageName, perm.substringAfterLast('.'), "allow"))
                                                            }
                                                            "Ignore" -> {
                                                                executePrivilegedCmd(arrayOf("appops", "set", packageName, perm.substringAfterLast('.'), "ignore"))
                                                            }
                                                            "Deny" -> {
                                                                executePrivilegedCmd(arrayOf("pm", "revoke", packageName, perm)) &&
                                                                executePrivilegedCmd(arrayOf("appops", "set", packageName, perm.substringAfterLast('.'), "deny"))
                                                            }
                                                            else -> false
                                                        }
                                                        withContext(Dispatchers.Main) {
                                                            if (success) {
                                                                currentMode = mode
                                                            } else {
                                                                android.widget.Toast.makeText(context, R.string.action_failed_check_permission, android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

internal fun executeShizukuCmdSilent(command: Array<String>): Boolean {
    if (!rikka.shizuku.Shizuku.pingBinder()) return false
    return try {
        val method = rikka.shizuku.Shizuku::class.java.declaredMethods.find {
            it.name == "newProcess" && it.parameterTypes.size == 3 && it.parameterTypes[0].isArray
        }
        if (method != null) {
            method.isAccessible = true
            val process = method.invoke(null, command, null, null) as rikka.shizuku.ShizukuRemoteProcess
            process.waitFor() == 0
        } else false
    } catch (_: Exception) { false }
}

internal fun executeDhizukuCmdSilent(command: Array<String>): Boolean {
    return try {
        if (com.rosan.dhizuku.api.Dhizuku.isPermissionGranted()) {
            val process = com.rosan.dhizuku.api.Dhizuku.newProcess(command, null, null)
            process.waitFor() == 0
        } else false
    } catch (_: Exception) { false }
}

internal fun executeRootCmdSilent(command: Array<String>): Boolean {
    return try {
        val fullCmd = arrayOf("su", "-c", command.joinToString(" "))
        Runtime.getRuntime().exec(fullCmd).waitFor() == 0
    } catch (_: Exception) { false }
}
