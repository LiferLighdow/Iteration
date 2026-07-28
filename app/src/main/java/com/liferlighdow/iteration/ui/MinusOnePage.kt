package com.liferlighdow.iteration.ui

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.service.NotificationService
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.widgets.*
import com.liferlighdow.iteration.ui.dialogs.*
import com.liferlighdow.iteration.data.WeatherProvider
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.data.CustomComponent
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.utils.IconShape

@Composable
fun MinusOnePage(
    widgets: List<WidgetModel>,
    viewModel: MainViewModel,
    backdrop: Backdrop? = null,
    isEditMode: Boolean = false,
    onAddClick: () -> Unit,
    onRemoveWidget: (String) -> Unit,
    onUpdateWidgetMode: (String, WidgetDisplayMode) -> Unit,
    onAppClick: (AppModel) -> Unit
) {
    var isReorderMode by remember { mutableStateOf(false) }
    val effectiveEditMode = isEditMode || isReorderMode

    // 搜尋相關狀態
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    // 拖動相關狀態
    var draggingWidgetId by remember { mutableStateOf<String?>(null) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    val widgetPositions = remember { mutableStateMapOf<String, Rect>() }

    var stackToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var noteToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var todoToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var weatherToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var rssToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var countdownToEdit by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToAdjust by remember { mutableStateOf<WidgetModel?>(null) }
    var photoToPick by remember { mutableStateOf<WidgetModel?>(null) }
    var showCropDialogByUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val mContext = LocalContext.current

    // 主題偵測與顏色設定
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val baseColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    val isDesktopLocked by viewModel.isDesktopLocked.collectAsState()

    val activeContextMenuId by viewModel.activeContextMenuId.collectAsState()
    val isAnyMenuVisible = activeContextMenuId != null
    val menuAlpha by animateFloatAsState(
        targetValue = if (isAnyMenuVisible) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "MenuAlpha"
    )

    if (isSearching) {
        BackHandler {
            isSearching = false
            searchQuery = ""
            focusManager.clearFocus()
        }
    }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                mContext.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {}
            showCropDialogByUri = it
        }
    }
    val mediaInfo by NotificationService.currentMedia.collectAsState()

    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isMinusOneSearchGlassEnabled by viewModel.isLiquidGlassMinusOneSearchEnabled.collectAsState()
    val isMinusOneButtonGlassEnabled by viewModel.isLiquidGlassMinusOneButtonEnabled.collectAsState()

    val searchGlassActive = isLiquidGlassEnabled && isMinusOneSearchGlassEnabled
    val buttonGlassActive = isLiquidGlassEnabled && isMinusOneButtonGlassEnabled

    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer { alpha = menuAlpha },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = effectiveEditMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isDesktopLocked) {
                        FilledTonalIconButton(
                            onClick = onAddClick,
                            modifier = Modifier.liquidGlass(
                                enabled = isLiquidGlassEnabled && isMinusOneButtonGlassEnabled,
                                backdrop = backdrop,
                                cornerRadius = 12.dp,
                                blurRadius = blurRadius,
                                refractionHeight = refractionHeight,
                                refractionAmount = refractionAmount,
                                chromaticAberration = chromaticAberration
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = if (buttonGlassActive) Color.White else contentColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Button(
                        onClick = { isReorderMode = false },
                        modifier = Modifier.liquidGlass(
                            enabled = isLiquidGlassEnabled && isMinusOneButtonGlassEnabled,
                            backdrop = backdrop,
                            cornerRadius = 12.dp,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (buttonGlassActive) Color.White else contentColor
                        )
                    ) {
                        Text(
                            stringResource(R.string.done),
                            style = MaterialTheme.typography.labelLarge.withGlassShadow(buttonGlassActive),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = !effectiveEditMode,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        isSearching = it.isNotEmpty() || isSearching
                    },
                    modifier = Modifier.fillMaxWidth().liquidGlass(
                        enabled = isLiquidGlassEnabled && isMinusOneSearchGlassEnabled,
                        backdrop = backdrop,
                        cornerRadius = 30.dp,
                        blurRadius = blurRadius,
                        refractionHeight = refractionHeight,
                        refractionAmount = refractionAmount,
                        chromaticAberration = chromaticAberration
                    ),
                    placeholder = { 
                        Text(
                            stringResource(R.string.search_hint), 
                            style = MaterialTheme.typography.bodyLarge.withGlassShadow(searchGlassActive),
                            color = (if (searchGlassActive) Color.White else contentColor).copy(alpha = 0.6f)
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = null, 
                            tint = (if (searchGlassActive) Color.White else contentColor).copy(alpha = 0.7f) 
                        ) 
                    },
                    trailingIcon = {
                        if (isSearching) {
                            IconButton(onClick = { 
                                isSearching = false
                                searchQuery = ""
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = if (searchGlassActive) Color.White else contentColor)
                            }
                        }
                    },
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (searchGlassActive) Color.White else contentColor,
                        unfocusedTextColor = if (searchGlassActive) Color.White else contentColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = (if (searchGlassActive) Color.White else contentColor).copy(alpha = 0.1f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.withGlassShadow(searchGlassActive),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            // Widget Grid
            androidx.compose.animation.AnimatedVisibility(
                visible = !isSearching,
                enter = fadeIn() + scaleIn(initialScale = 0.95f),
                exit = fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(widgets, key = { it.id }, span = { widget ->
                        val span = if ((widget.type as? WidgetType.Photo)?.isWide == true ||
                            (widget.type as? WidgetType.Calendar)?.isWide == true ||
                            (widget.type as? WidgetType.Music)?.isWide == true ||
                            (widget.type as? WidgetType.Note)?.isWide == true ||
                            (widget.type as? WidgetType.ToDoList)?.isWide == true ||
                            (widget.type as? WidgetType.Weather)?.isWide == true ||
                            (widget.type as? WidgetType.RSS)?.isWide == true ||
                            (widget.type as? WidgetType.Countdown)?.isWide == true ||
                            (widget.type as? WidgetType.Stack)?.isWide == true ||
                            (widget.type as? WidgetType.Custom)?.size == "4x2" ||
                            widget.type is WidgetType.InfoHub ||
                            widget.type is WidgetType.InfoHub2) 4 else 2
                        GridItemSpan(span)
                    }) { widget ->
                        var showContextMenu by remember { mutableStateOf(false) }
                        val index = widgets.indexOfFirst { it.id == widget.id }
                        val isDragging = draggingWidgetId == widget.id

                        val removingItemIds by viewModel.removingItemIds.collectAsState()
                        val isRemoving = remember(removingItemIds, widget.id) { removingItemIds.contains(widget.id) }
                        val isActiveMenu = activeContextMenuId == widget.id
                        val pressedItemId by viewModel.pressedItemId.collectAsState()
                        val isBeingPressed = pressedItemId == widget.id

                        val itemMenuAlpha by animateFloatAsState(
                            targetValue = if (isAnyMenuVisible && !isActiveMenu) 0f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "ItemMenuAlpha"
                        )

                        val focusScale by animateFloatAsState(
                            targetValue = when {
                                isActiveMenu -> 1.1f
                                isBeingPressed -> 0.96f
                                else -> 1f
                            },
                            animationSpec = spring(
                                dampingRatio = if (isActiveMenu) Spring.DampingRatioMediumBouncy else Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "FocusScale"
                        )

                        val removalScale by animateFloatAsState(
                            targetValue = if (isRemoving) 0f else 1f,
                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                            label = "RemovingScale"
                        )

                        val removalAlpha by animateFloatAsState(
                            targetValue = if (isRemoving) 0f else 1f,
                            animationSpec = tween(durationMillis = 400),
                            label = "RemovingAlpha"
                        )

                        val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = -1.5f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(150, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "jiggle"
                        )

                        Box(
                            modifier = Modifier
                                .onGloballyPositioned { layoutCoordinates ->
                                    val pos = layoutCoordinates.positionInRoot()
                                    widgetPositions[widget.id] = Rect(pos, layoutCoordinates.size.toSize())
                                }
                                .graphicsLayer {
                                    if (effectiveEditMode && !isDragging) {
                                        rotationZ = rotation
                                    }
                                    if (isDragging) {
                                        val currentSlotPos = widgetPositions[widget.id]?.topLeft ?: Offset.Zero
                                        translationX = touchPosition.x - currentSlotPos.x - dragStartOffset.x
                                        translationY = touchPosition.y - currentSlotPos.y - dragStartOffset.y
                                        scaleX = 1.06f
                                        scaleY = 1.06f
                                        alpha = 0.8f
                                    } else {
                                        scaleX = removalScale * focusScale
                                        scaleY = removalScale * focusScale
                                        alpha = removalAlpha * itemMenuAlpha
                                        if (isRemoving) {
                                            rotationZ = (1f - removalScale) * 30f
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                val blurAmount = (1f - removalScale) * 30f
                                                if (blurAmount > 0.01f) {
                                                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                                        blurAmount,
                                                        blurAmount,
                                                        android.graphics.Shader.TileMode.CLAMP
                                                    ).asComposeRenderEffect()
                                                } else {
                                                    renderEffect = null
                                                }
                                            }
                                        }
                                    }
                                }
                                .zIndex(if (isDragging) 10f else 0f)
                                .pointerInput(widget.id, effectiveEditMode) {
                                    if (effectiveEditMode) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                draggingWidgetId = widget.id
                                                dragStartOffset = offset
                                                touchPosition = (widgetPositions[widget.id]?.topLeft ?: Offset.Zero) + offset
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                touchPosition += dragAmount

                                                val currentCenter = touchPosition - dragStartOffset + 
                                                    Offset(size.width / 2f, size.height / 2f)
                                                
                                                val targetEntry = widgetPositions.entries.find { (id, rect) ->
                                                    id != widget.id && rect.contains(currentCenter)
                                                }
                                                
                                                targetEntry?.let { (targetId, _) ->
                                                    val targetIndex = widgets.indexOfFirst { it.id == targetId }
                                                    if (targetIndex != -1 && index != -1 && targetIndex != index) {
                                                        viewModel.reorderMinusOneWidgets(index, targetIndex)
                                                    }
                                                }
                                            },
                                            onDragEnd = { draggingWidgetId = null },
                                            onDragCancel = { draggingWidgetId = null }
                                        )
                                    }
                                }
                                .pointerInput(widget.type, effectiveEditMode) {
                                    if (!effectiveEditMode) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.setPressedItemId(widget.id)
                                                try { awaitRelease() } finally { viewModel.setPressedItemId(null) }
                                            },
                                            onLongPress = {
                                                if (widget.type is WidgetType.Stack) {
                                                    stackToEdit = widget
                                                } else {
                                                    viewModel.setActiveContextMenuId(widget.id)
                                                    showContextMenu = true
                                                }
                                            }
                                        )
                                    }
                                }
                        ) {
                            when (val type = widget.type) {
                                is WidgetType.Battery -> BatteryWidget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Clock -> {
                                    if (type.isDigital) {
                                        DigitalClockWidget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                    } else {
                                        AnalogClockWidget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                    }
                                }
                                is WidgetType.Calendar -> CalendarWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Photo -> PhotoWidget(widget = widget, viewModel = viewModel)
                                is WidgetType.Music -> MusicWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Note -> NoteWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.ToDoList -> TodoWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Weather -> WeatherWidget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.RSS -> RSSWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Countdown -> CountdownWidget(widget = widget, displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.InfoHub -> InfoHubWidget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.InfoHub2 -> InfoHub2Widget(displayMode = widget.displayMode, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Stack -> StackWidget(widget = widget, viewModel = viewModel, backdrop = backdrop, isMinusOnePage = true)
                                is WidgetType.Custom -> CustomWidget(
                                    widget = widget, 
                                    backdrop = backdrop, 
                                    isMinusOnePage = true,
                                    onLongClick = { 
                                        viewModel.setActiveContextMenuId(widget.id)
                                        showContextMenu = true 
                                    }
                                )
                            }

                            if (effectiveEditMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = (-6).dp, y = (-6).dp)
                                        .size(24.dp)
                                        .background(Color.Gray.copy(alpha = 0.9f), CircleShape)
                                        .clickable { onRemoveWidget(widget.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showContextMenu,
                                onDismissRequest = { 
                                    showContextMenu = false 
                                    viewModel.setActiveContextMenuId(null)
                                }
                            ) {
                                if (widget.type !is WidgetType.Stack && widget.type !is WidgetType.Photo) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(if (widget.displayMode == WidgetDisplayMode.COLOR) R.string.widget_glass_mode else R.string.widget_color_mode)) },
                                        leadingIcon = { Icon(if (widget.displayMode == WidgetDisplayMode.COLOR) Icons.Default.BlurOn else Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            onUpdateWidgetMode(widget.id, if (widget.displayMode == WidgetDisplayMode.COLOR) WidgetDisplayMode.GLASS else WidgetDisplayMode.COLOR)
                                            showContextMenu = false
                                        }
                                    )
                                }
                                if (widget.type is WidgetType.Photo) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.choose_picture)) },
                                        leadingIcon = { Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary) },
                                        onClick = {
                                            photoToPick = widget
                                            photoPickerLauncher.launch("image/*")
                                            showContextMenu = false
                                        }
                                    )
                                    if ((widget.type as WidgetType.Photo).uri != null) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.adjust_position)) },
                                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
                                            onClick = { photoToAdjust = widget; showContextMenu = false }
                                        )
                                    }
                                }
                                if (widget.type is WidgetType.Stack) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_choose_widgets)) },
                                        leadingIcon = { Icon(Icons.Default.Settings, null) },
                                        onClick = { stackToEdit = widget; showContextMenu = false }
                                    )
                                }
                                if (widget.type is WidgetType.Note) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_note)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = { noteToEdit = widget; showContextMenu = false }
                                    )
                                }
                                if (widget.type is WidgetType.ToDoList) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_todo)) },
                                        leadingIcon = { Icon(Icons.Default.PlaylistAddCheck, null) },
                                        onClick = { todoToEdit = widget; showContextMenu = false }
                                    )
                                }
                                if (widget.type is WidgetType.Weather) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.choose_location)) },
                                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                        onClick = { weatherToEdit = widget; showContextMenu = false }
                                    )
                                }
                                if (widget.type is WidgetType.RSS) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_rss)) },
                                        leadingIcon = { Icon(Icons.Default.RssFeed, null) },
                                        onClick = { rssToEdit = widget; showContextMenu = false }
                                    )
                                }
                                if (widget.type is WidgetType.Countdown) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_countdown)) },
                                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                                        onClick = { countdownToEdit = widget; showContextMenu = false }
                                    )
                                }
                            }
                        }
                    }

                    // 底部 Edit 膠囊按鈕
                    if (!effectiveEditMode && !isDesktopLocked) {
                        item(span = { GridItemSpan(4) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).graphicsLayer { alpha = menuAlpha }, contentAlignment = Alignment.Center) {
                                Surface(
                                    onClick = { isReorderMode = true },
                                    modifier = Modifier.liquidGlass(
                                        enabled = isLiquidGlassEnabled && isMinusOneButtonGlassEnabled,
                                        backdrop = backdrop,
                                        cornerRadius = 20.dp,
                                        blurRadius = blurRadius,
                                        refractionHeight = refractionHeight,
                                        refractionAmount = refractionAmount,
                                        chromaticAberration = chromaticAberration
                                    ),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.Transparent,
                                    contentColor = contentColor
                                ) {
                                    Text("Edit", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(4) }) { Spacer(modifier = Modifier.height(64.dp)) }
                }
            }

            // Search Results
            androidx.compose.animation.AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                val allApps by viewModel.allApps.collectAsState()
                val contacts by viewModel.contacts.collectAsState()
                val iconShape by viewModel.iconShape.collectAsState()

                val filteredApps = remember(searchQuery, allApps) {
                    if (searchQuery.isBlank()) emptyList()
                    else allApps.filter { !it.isHidden && !it.isFrozen && !it.isPrivate && it.label.contains(searchQuery, ignoreCase = true) }
                }

                val filteredContacts = remember(searchQuery, contacts) {
                    if (searchQuery.isBlank()) emptyList()
                    else contacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery) }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = baseColor.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { focusManager.clearFocus() })
                            }
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            if (filteredApps.isNotEmpty()) {
                                item { Text(stringResource(R.string.apps), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                                items(filteredApps) { app ->
                                    val iconSignal by viewModel.iconUpdateSignal.collectAsState()
                                    val appIcon = remember(app.uniqueId, iconSignal) {
                                        viewModel.getIcon(app.uniqueId)
                                    }
                                    ListItem(
                                        headlineContent = { Text(app.label, color = contentColor) },
                                        leadingContent = {
                                            if (appIcon != null) {
                                                val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                                                Image(
                                                    bitmap = appIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(shape)
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = Color.White.copy(alpha = 0.3f),
                                                            shape = shape
                                                        )
                                                )
                                            } else {
                                                Box(modifier = Modifier.size(48.dp).background(contentColor.copy(alpha = 0.1f), CircleShape))
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable { 
                                            onAppClick(app)
                                            isSearching = false; searchQuery = ""; focusManager.clearFocus()
                                        }
                                    )
                                }
                            }

                            if (filteredContacts.isNotEmpty()) {
                                item { Text(stringResource(R.string.contacts), color = contentColor.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                                items(filteredContacts) { contact ->
                                    ListItem(
                                        headlineContent = { Text(contact.name, color = contentColor) },
                                        supportingContent = { Text(contact.phoneNumber, color = contentColor.copy(alpha = 0.6f)) },
                                        leadingContent = {
                                            if (contact.photo != null) Image(bitmap = contact.photo.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                            else Box(modifier = Modifier.size(40.dp).background(contentColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = contentColor) }
                                        },
                                        trailingContent = {
                                            IconButton(onClick = {
                                                mContext.startActivity(Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:${contact.phoneNumber}")))
                                                isSearching = false; searchQuery = ""; focusManager.clearFocus()
                                            }) { Icon(Icons.Default.Call, null, tint = contentColor) }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id) }
                                            mContext.startActivity(intent)
                                            isSearching = false; searchQuery = ""; focusManager.clearFocus()
                                        }
                                    )
                                }
                            }
                            
                            item {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = contentColor.copy(alpha = 0.1f))
                                val searchEngineUrl by viewModel.searchEngineUrl.collectAsState()
                                ListItem(
                                    headlineContent = { Text(stringResource(R.string.search_web), color = contentColor) },
                                    leadingContent = { Icon(Icons.Default.Language, null, tint = contentColor) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        mContext.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("${searchEngineUrl}${searchQuery}")))
                                        isSearching = false; searchQuery = ""; focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (stackToEdit != null) {
        val stack = stackToEdit!!.type as WidgetType.Stack
        WidgetStackPickerDialog(
            currentChildren = stack.children,
            isWide = stack.isWide,
            isCyclic = stack.isCyclic,
            viewModel = viewModel,
            onDismiss = { stackToEdit = null },
            onConfirm = { newChildren, isCyclic -> 
                viewModel.updateStackChildren(stackToEdit!!.id, newChildren, isCyclic) 
            }
        )
    }

    if (noteToEdit != null) {
        NoteEditDialog(
            widgetId = noteToEdit!!.id,
            initialText = (noteToEdit!!.type as WidgetType.Note).text,
            viewModel = viewModel,
            onDismiss = { noteToEdit = null }
        )
    }

    if (todoToEdit != null) {
        TodoEditDialog(
            widgetId = todoToEdit!!.id,
            initialTasks = (todoToEdit!!.type as WidgetType.ToDoList).tasks,
            viewModel = viewModel,
            onDismiss = { todoToEdit = null }
        )
    }

    if (weatherToEdit != null) {
        LocationSearchDialog(viewModel = viewModel, onDismiss = { weatherToEdit = null })
    }

    if (rssToEdit != null) {
        RssEditDialog(
            widgetId = rssToEdit!!.id,
            initialUrl = (rssToEdit!!.type as WidgetType.RSS).url,
            viewModel = viewModel,
            onDismiss = { rssToEdit = null }
        )
    }

    if (countdownToEdit != null) {
        CountdownEditDialog(
            widgetId = countdownToEdit!!.id,
            initialName = (countdownToEdit!!.type as WidgetType.Countdown).eventName,
            initialTimestamp = (countdownToEdit!!.type as WidgetType.Countdown).targetTimestamp,
            viewModel = viewModel,
            onDismiss = { countdownToEdit = null }
        )
    }

    if (photoToAdjust != null) {
        val type = photoToAdjust!!.type as? WidgetType.Photo
        val uriStr = type?.uri
        if (uriStr != null) {
            ImageCropDialog(
                uri = android.net.Uri.parse(uriStr),
                isWide = type.isWide,
                onDismiss = { photoToAdjust = null },
                onConfirm = { cropped ->
                    viewModel.saveWidgetPhoto(photoToAdjust!!.id, cropped)
                    photoToAdjust = null
                }
            )
        }
    }

    if (showCropDialogByUri != null && photoToPick != null) {
        val isWide = (photoToPick!!.type as? WidgetType.Photo)?.isWide ?: false
        ImageCropDialog(
            uri = showCropDialogByUri!!,
            isWide = isWide,
            onDismiss = { showCropDialogByUri = null; photoToPick = null },
            onConfirm = { cropped ->
                viewModel.saveWidgetPhoto(photoToPick!!.id, cropped)
                viewModel.updatePhotoWidgetUri(photoToPick!!.id, showCropDialogByUri.toString())
                showCropDialogByUri = null; photoToPick = null
            }
        )
    }
}
