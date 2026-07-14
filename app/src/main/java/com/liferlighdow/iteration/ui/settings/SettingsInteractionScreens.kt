package com.liferlighdow.iteration.ui.settings

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
    val allApps by viewModel.allApps.collectAsState()
    val dockPkgNames by viewModel.dockPackageNames.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
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

    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)

    var showAppPickerForSlot by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.desktop_settings_title)) },
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
                    stringResource(R.string.layout_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_minus_one)) },
                    supportingContent = { Text(stringResource(R.string.show_minus_one_desc)) },
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
                    headlineContent = { Text(stringResource(R.string.show_library)) },
                    supportingContent = { Text(stringResource(R.string.show_library_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showAppLibrary,
                            onCheckedChange = { viewModel.setShowAppLibrary(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setShowAppLibrary(!showAppLibrary) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.lock_desktop)) },
                    supportingContent = { Text(stringResource(R.string.lock_desktop_desc)) },
                    trailingContent = {
                        Switch(
                            checked = isDesktopLocked,
                            onCheckedChange = { viewModel.setDesktopLocked(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setDesktopLocked(!isDesktopLocked) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_auto_add_apps)) },
                    supportingContent = { Text(stringResource(R.string.settings_auto_add_apps_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoAddAppsToHome,
                            onCheckedChange = { viewModel.setAutoAddAppsToHome(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setAutoAddAppsToHome(!autoAddAppsToHome) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_status_bar)) },
                    supportingContent = { Text(stringResource(R.string.show_status_bar_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showStatusBar,
                            onCheckedChange = { viewModel.setShowStatusBar(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setShowStatusBar(!showStatusBar) }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_nav_handle)) },
                    supportingContent = { Text(stringResource(R.string.show_nav_handle_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showNavigationBar,
                            onCheckedChange = { viewModel.setShowNavigationBar(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setShowNavigationBar(!showNavigationBar) }
                )
            }

            item {
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
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(stringResource(R.string.change))
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
                    modifier = Modifier.clickable { expanded = true }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.amoled_black)) },
                    supportingContent = { Text(stringResource(R.string.amoled_black_desc)) },
                    trailingContent = {
                        Switch(
                            checked = isAmoledBlack,
                            onCheckedChange = { viewModel.setAmoledBlack(it) }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setAmoledBlack(!isAmoledBlack) }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                var expanded by remember { mutableStateOf(false) }
                val options = listOf(
                    0 to stringResource(R.string.auto_adaptive), 
                    5 to stringResource(R.string.layout_4x5), 
                    6 to stringResource(R.string.layout_4x6), 
                    7 to stringResource(R.string.layout_4x7),
                    -1 to stringResource(R.string.layout_4x6_balanced)
                )
                val currentLabel = options.find { it.first == desktopRows }?.second ?: stringResource(R.string.auto_adaptive)

                ListItem(
                    headlineContent = { Text(stringResource(R.string.layout_rows)) },
                    supportingContent = { Text(stringResource(R.string.layout_rows_current, currentLabel)) },
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expanded = true }) {
                                Text(stringResource(R.string.change))
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
                    trailingContent = {
                        Box {
                            TextButton(onClick = { expandedStyle = true }) {
                                Text(stringResource(R.string.change))
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

            if (dockStyle == DockStyle.MODERN) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.dock_radius_label, dockCornerRadius.toInt()))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.setDockCornerRadius((dockCornerRadius - 1f).coerceAtLeast(0f)) }) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.decrease))
                            }
                            Slider(
                                value = dockCornerRadius,
                                onValueChange = { viewModel.setDockCornerRadius(it) },
                                valueRange = 0f..100f,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.setDockCornerRadius((dockCornerRadius + 1f).coerceAtMost(100f)) }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.increase))
                            }
                        }
                    }
                }
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
                    stringResource(R.string.dock_apps),
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
                        headlineContent = { Text(app?.label ?: stringResource(R.string.empty_slot, index + 1)) },
                        supportingContent = { Text(if (pkgName.isEmpty()) stringResource(R.string.tap_to_select_app) else pkgName) },
                        leadingContent = {
                            val appIcon = if (app != null) viewModel.getIcon(app.uniqueId) else null
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
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove), tint = MaterialTheme.colorScheme.error)
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
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForDoubleTap = false },
            onAppSelected = { viewModel.setDoubleTapApp(it); showAppPickerForDoubleTap = false })
    }
    if (showAppPickerForSwipeUp) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForSwipeUp = false },
            onAppSelected = { viewModel.setSwipeUpApp(it); showAppPickerForSwipeUp = false })
    }
    if (showAppPickerForSwipeDown) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForSwipeDown = false },
            onAppSelected = { viewModel.setSwipeDownApp(it); showAppPickerForSwipeDown = false })
    }
    if (showAppPickerForLongPress) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForLongPress = false },
            onAppSelected = { viewModel.setLongPressApp(it); showAppPickerForLongPress = false })
    }
    if (showAppPickerForTwoFingerSwipeUp) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForTwoFingerSwipeUp = false },
            onAppSelected = {
                viewModel.setTwoFingerSwipeUpApp(it); showAppPickerForTwoFingerSwipeUp = false
            })
    }
    if (showAppPickerForTwoFingerSwipeDown) {
        AppPickerDialog(
            allApps.filter { !it.isHidden },
            iconShape,
            viewModel,
            onDismiss = { showAppPickerForTwoFingerSwipeDown = false },
            onAppSelected = {
                viewModel.setTwoFingerSwipeDownApp(it); showAppPickerForTwoFingerSwipeDown = false
            })
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
