package com.liferlighdow.iteration.ui.settings

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.ui.AppPickerDialog
import com.liferlighdow.iteration.ui.DockStyle
import com.liferlighdow.iteration.ui.ThemeMode
import com.liferlighdow.iteration.utils.GestureAction
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopSettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val desktopRows by viewModel.desktopRows.collectAsState()
    val dockStyle by viewModel.dockStyle.collectAsState()
    val dockCornerRadius by viewModel.dockCornerRadius.collectAsState()
    val showMinusOnePage by viewModel.showMinusOnePage.collectAsState()
    val showAppLibrary by viewModel.showAppLibrary.collectAsState()
    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()
    val autoAddAppsToHome by viewModel.autoAddAppsToHome.collectAsState()
    val showStatusBar by viewModel.showStatusBar.collectAsState()
    val showNavigationBar by viewModel.showNavigationBar.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isAmoledBlack by viewModel.isAmoledBlack.collectAsState()

    val context = LocalContext.current
    val window = (context as? AppCompatActivity)?.window

    LaunchedEffect(showStatusBar, showNavigationBar) {
        window?.let { win ->
            val windowInsetsController = WindowCompat.getInsetsController(win, win.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            
            if (showStatusBar) {
                windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
            }

            if (showNavigationBar) {
                windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
            } else {
                windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.desktop_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // --- Layout & Behavior ---
            item {
                SettingsSection(title = stringResource(R.string.layout_settings)) {
                    SettingSwitchItem(
                        icon = Icons.Default.AutoAwesomeMotion,
                        title = stringResource(R.string.show_minus_one),
                        supportingText = stringResource(R.string.show_minus_one_desc),
                        checked = showMinusOnePage,
                        onCheckedChange = { viewModel.setShowMinusOnePage(it) }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.Apps,
                        title = stringResource(R.string.show_library),
                        supportingText = stringResource(R.string.show_library_desc),
                        checked = showAppLibrary,
                        onCheckedChange = { viewModel.setShowAppLibrary(it) }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.lock_desktop),
                        supportingText = stringResource(R.string.lock_desktop_desc),
                        checked = isDesktopLocked,
                        onCheckedChange = { viewModel.setDesktopLocked(it) }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.AddBox,
                        title = stringResource(R.string.settings_auto_add_apps),
                        supportingText = stringResource(R.string.settings_auto_add_apps_desc),
                        checked = autoAddAppsToHome,
                        onCheckedChange = { viewModel.setAutoAddAppsToHome(it) }
                    )
                }
            }

            // --- System UI ---
            item {
                SettingsSection(title = "系統介面") {
                    SettingSwitchItem(
                        icon = Icons.Default.Expand,
                        title = stringResource(R.string.show_status_bar),
                        supportingText = stringResource(R.string.show_status_bar_desc),
                        checked = showStatusBar,
                        onCheckedChange = { viewModel.setShowStatusBar(it) }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.HorizontalRule,
                        title = stringResource(R.string.show_nav_handle),
                        supportingText = stringResource(R.string.show_nav_handle_desc),
                        checked = showNavigationBar,
                        onCheckedChange = { viewModel.setShowNavigationBar(it) }
                    )
                }
            }

            // --- Theme ---
            item {
                SettingsSection(title = stringResource(R.string.settings_theme_mode)) {
                    var expanded by remember { mutableStateOf(false) }
                    val options = listOf(
                        ThemeMode.LIGHT to stringResource(R.string.theme_light),
                        ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ThemeMode.FOLLOW_SYSTEM to stringResource(R.string.theme_follow_system)
                    )
                    val currentLabel = options.find { it.first == themeMode }?.second ?: stringResource(R.string.theme_follow_system)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_theme_mode)) },
                        supportingContent = { Text(currentLabel) },
                        leadingContent = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    options.forEach { (mode, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                viewModel.setThemeMode(mode)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { expanded = true }
                    )
                    SettingSwitchItem(
                        icon = Icons.Default.Contrast,
                        title = stringResource(R.string.amoled_black),
                        supportingText = stringResource(R.string.amoled_black_desc),
                        checked = isAmoledBlack,
                        onCheckedChange = { viewModel.setAmoledBlack(it) }
                    )
                }
            }

            // --- Desktop Grid & Dock ---
            item {
                SettingsSection(title = "佈局與底欄") {
                    var expandedGrid by remember { mutableStateOf(false) }
                    val configuration = LocalConfiguration.current
                    val isLongScreen = configuration.screenHeightDp.toFloat() / configuration.screenWidthDp.toFloat() >= 2.0f

                    val gridOptions = remember(isLongScreen) {
                        val list = mutableListOf(0 to 0, 5 to 5, 6 to 6)
                        if (isLongScreen) {
                            list.add(7 to 7)
                            list.add(-1 to -1)
                        }
                        list
                    }.map { (value, _) ->
                        value to when(value) {
                            0 -> stringResource(R.string.auto_adaptive)
                            5 -> stringResource(R.string.layout_4x5)
                            6 -> stringResource(R.string.layout_4x6)
                            7 -> stringResource(R.string.layout_4x7)
                            else -> stringResource(R.string.layout_4x6_balanced)
                        }
                    }
                    val currentGridLabel = gridOptions.find { it.first == desktopRows }?.second ?: stringResource(R.string.auto_adaptive)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.layout_rows)) },
                        supportingContent = { Text(stringResource(R.string.layout_rows_current, currentGridLabel)) },
                        leadingContent = { Icon(Icons.Default.GridOn, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { expandedGrid = true }
                    )
                    DropdownMenu(expanded = expandedGrid, onDismissRequest = { expandedGrid = false }) {
                        gridOptions.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setDesktopRows(value); expandedGrid = false })
                        }
                    }

                    var expandedStyle by remember { mutableStateOf(false) }
                    val styleOptions = listOf(
                        DockStyle.MODERN to stringResource(R.string.dock_style_modern),
                        DockStyle.CLASSIC to stringResource(R.string.dock_style_classic),
                        DockStyle.PLATFORM to stringResource(R.string.dock_style_platform),
                        DockStyle.LITE to stringResource(R.string.dock_style_lite)
                    )
                    val currentStyleLabel = styleOptions.find { it.first == dockStyle }?.second ?: stringResource(R.string.dock_style_modern)

                    ListItem(
                        headlineContent = { Text(stringResource(R.string.dock_style)) },
                        supportingContent = { Text(stringResource(R.string.dock_style_current, currentStyleLabel)) },
                        leadingContent = { Icon(Icons.Default.ViewStream, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = { Icon(Icons.Default.ArrowDropDown, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { expandedStyle = true }
                    )
                    DropdownMenu(expanded = expandedStyle, onDismissRequest = { expandedStyle = false }) {
                        styleOptions.forEach { (style, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { viewModel.setDockStyle(style); expandedStyle = false })
                        }
                    }

                    if (dockStyle == DockStyle.MODERN) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RoundedCorner, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(R.string.dock_radius_label, dockCornerRadius.toInt()), style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.setDockCornerRadius((dockCornerRadius - 1f).coerceAtLeast(0f)) }) {
                                    Icon(Icons.Default.Remove, contentDescription = null)
                                }
                                Slider(
                                    value = dockCornerRadius,
                                    onValueChange = { viewModel.setDockCornerRadius(it) },
                                    valueRange = 0f..100f,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.setDockCornerRadius((dockCornerRadius + 1f).coerceAtMost(100f)) }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            // --- Home Menu Options ---
            item {
                SettingsSection(title = stringResource(R.string.home_menu_options)) {
                    Text(
                        stringResource(R.string.home_menu_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    val menuOptions by viewModel.homeMenuOptions.collectAsState()
                    val availableOptions = listOf(
                        "delete_home" to R.string.menu_delete_home,
                        "edit" to R.string.menu_edit,
                        "uninstall" to R.string.menu_uninstall,
                        "shortcuts" to R.string.menu_shortcuts,
                        "freeze" to R.string.menu_freeze,
                        "hide" to R.string.menu_hide,
                        "favorite" to R.string.menu_add_favorite,
                        "app_info" to R.string.menu_app_info
                    )
                    availableOptions.forEach { (key, resId) ->
                        val isChecked = menuOptions.contains(key)
                        ListItem(
                            headlineContent = { Text(stringResource(resId)) },
                            trailingContent = { Switch(checked = isChecked, onCheckedChange = { viewModel.setHomeMenuOption(key, it) }) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { viewModel.setHomeMenuOption(key, !isChecked) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                SettingsSection(title = stringResource(R.string.settings_gestures)) {
                    GestureItem(
                        icon = Icons.Default.TouchApp,
                        title = stringResource(R.string.gesture_double_tap),
                        action = doubleTapAction,
                        packageName = doubleTapApp,
                        allApps = allApps,
                        onClick = { showDoubleTapDialog = true }
                    )
                    GestureItem(
                        icon = Icons.Default.VerticalAlignTop,
                        title = stringResource(R.string.gesture_swipe_up),
                        action = swipeUpAction,
                        packageName = swipeUpApp,
                        allApps = allApps,
                        onClick = { showSwipeUpDialog = true }
                    )
                    GestureItem(
                        icon = Icons.Default.VerticalAlignBottom,
                        title = stringResource(R.string.gesture_swipe_down),
                        action = swipeDownAction,
                        packageName = swipeDownApp,
                        allApps = allApps,
                        onClick = { showSwipeDownDialog = true }
                    )
                    GestureItem(
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.gesture_long_press),
                        action = longPressAction,
                        packageName = longPressApp,
                        allApps = allApps,
                        onClick = { showLongPressDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.gesture_two_finger_swipe_up)) {
                    GestureItem(
                        icon = Icons.Default.KeyboardDoubleArrowUp,
                        title = stringResource(R.string.gesture_two_finger_swipe_up),
                        action = twoFingerSwipeUpAction,
                        packageName = twoFingerSwipeUpApp,
                        allApps = allApps,
                        onClick = { showTwoFingerSwipeUpDialog = true }
                    )
                    GestureItem(
                        icon = Icons.Default.KeyboardDoubleArrowDown,
                        title = stringResource(R.string.gesture_two_finger_swipe_down),
                        action = twoFingerSwipeDownAction,
                        packageName = twoFingerSwipeDownApp,
                        allApps = allApps,
                        onClick = { showTwoFingerSwipeDownDialog = true }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
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
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForDoubleTap = false },
            onAppSelected = { viewModel.setDoubleTapApp(it.packageName); showAppPickerForDoubleTap = false })
    }
    if (showAppPickerForSwipeUp) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForSwipeUp = false },
            onAppSelected = { viewModel.setSwipeUpApp(it.packageName); showAppPickerForSwipeUp = false })
    }
    if (showAppPickerForSwipeDown) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForSwipeDown = false },
            onAppSelected = { viewModel.setSwipeDownApp(it.packageName); showAppPickerForSwipeDown = false })
    }
    if (showAppPickerForLongPress) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForLongPress = false },
            onAppSelected = { viewModel.setLongPressApp(it.packageName); showAppPickerForLongPress = false })
    }
    if (showAppPickerForTwoFingerSwipeUp) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForTwoFingerSwipeUp = false },
            onAppSelected = {
                viewModel.setTwoFingerSwipeUpApp(it.packageName); showAppPickerForTwoFingerSwipeUp = false
            })
    }
    if (showAppPickerForTwoFingerSwipeDown) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForTwoFingerSwipeDown = false },
            onAppSelected = {
                viewModel.setTwoFingerSwipeDownApp(it.packageName); showAppPickerForTwoFingerSwipeDown = false
            })
    }
}

@Composable
fun GestureItem(icon: ImageVector, title: String, action: GestureAction, packageName: String, allApps: List<AppModel>, onClick: () -> Unit) {
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
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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
                        stringResource(R.string.style_exclusions),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                }
                Text(
                    stringResource(R.string.style_exclusions_desc),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                var searchQuery by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                val filteredApps = remember(allApps, searchQuery) {
                    allApps.filter { !it.isFrozen && !it.isPrivate && it.label.contains(searchQuery, ignoreCase = true) }
                        .sortedBy { it.label.lowercase() }
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredApps, key = { it.uniqueId }) { app ->
                        ListItem(
                            headlineContent = { Text(app.label) },
                            leadingContent = {
                                val appIcon = viewModel.getIcon(app.uniqueId)
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
