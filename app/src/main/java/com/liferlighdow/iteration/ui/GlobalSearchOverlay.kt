package com.liferlighdow.iteration.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.ui.search.*
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.utils.ActionMode
import com.liferlighdow.iteration.viewmodel.getIcon
import com.liferlighdow.iteration.viewmodel.toggleFreezeApp
import com.liferlighdow.iteration.viewmodel.loadContacts
import com.liferlighdow.iteration.viewmodel.loadCalendarEvents
import com.liferlighdow.iteration.viewmodel.loadFiles
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.ui.settings.IterationSearchBar
import com.liferlighdow.iteration.utils.CommandProcessor
import com.liferlighdow.iteration.data.AppModel

@Composable
fun GlobalSearchOverlay(
    isVisible: Boolean,
    dragOffset: Float = 0f,
    onDismiss: () -> Unit,
    allApps: List<AppModel>,
    suggestedApps: List<AppModel>,
    onAppClick: (AppModel, Offset) -> Unit,
    iconShape: IconShape,
    isLiquidGlassEnabled: Boolean,
    isLiquidGlassGlobalSearchEnabled: Boolean,
    backdrop: Backdrop?,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean
) {
    val searchViewModel: SearchViewModel = viewModel()
    val viewModel: MainViewModel = viewModel()
    
    val query by searchViewModel.query.collectAsState()
    val translationResult by searchViewModel.translationResult.collectAsState()
    val isTranslating by searchViewModel.isTranslating.collectAsState()
    val webSuggestions by searchViewModel.webSuggestions.collectAsState()

    val mContext = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFilesExpanded by remember(query) { mutableStateOf(false) }

    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
    val files by viewModel.files.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()
    val iconScaleFactor by viewModel.iconScale.collectAsState()

    val searchKeywordsEnabled by viewModel.searchKeywordsEnabled.collectAsState()
    val searchCalculatorEnabled by viewModel.searchCalculatorEnabled.collectAsState()
    val searchUnitConvEnabled by viewModel.searchUnitConvEnabled.collectAsState()
    val searchCurrencyConvEnabled by viewModel.searchCurrencyConvEnabled.collectAsState()
    val searchFilesEnabled by viewModel.searchFilesEnabled.collectAsState()
    val searchQuickSettingsEnabled by viewModel.searchQuickSettingsEnabled.collectAsState()

    var showFrozenManager by remember { mutableStateOf(false) }
    var showPrivateManager by remember { mutableStateOf(false) }
    var appToUnfreeze by remember { mutableStateOf<AppModel?>(null) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            searchViewModel.clear()
            focusManager.clearFocus(force = true)
        } else {
            viewModel.loadContacts()
            viewModel.loadCalendarEvents()
            viewModel.loadFiles()
        }
    }

    // 動畫處理
    val progress = remember { Animatable(0f) }
    LaunchedEffect(dragOffset) {
        if (!isVisible && dragOffset > 0) {
            progress.snapTo((dragOffset / 600f).coerceIn(0f, 1f))
        }
    }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
        } else {
            progress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val effectiveProgress = progress.value

    LaunchedEffect(effectiveProgress >= 0.01f, isVisible) {
        if (isVisible && effectiveProgress >= 0.01f) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    if (effectiveProgress < 0.001f) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = effectiveProgress; translationY = (effectiveProgress - 1f) * 200f }
            .background(Color.Black.copy(alpha = 0.4f * effectiveProgress))
            .clickable { onDismiss() }
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            IterationSearchBar(
                query = query,
                onQueryChange = { searchViewModel.setQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).graphicsLayer { translationY = (effectiveProgress - 1f) * 50f },
                isGlass = true,
                backdrop = backdrop,
                focusRequester = focusRequester
            )

            // 搜尋結果計算邏輯
            val filteredResults = remember(query, allApps) {
                if (query.isBlank()) emptyList()
                else allApps.filter { !it.isHidden && !it.isFrozen && !it.isPrivate && it.label.contains(query, ignoreCase = true) }
            }

            val mathResult = remember(query) {
                val q = query.lowercase().trim()
                if (q.isEmpty()) return@remember null
                val hasMathChar = q.any { it in "+-*/^%()π" }
                val hasFunction = listOf("sqrt", "sin", "cos", "tan", "cot", "sec", "csc", "log", "abs", "pi", "e").any { q.contains(it) }
                if ((q.any { it.isDigit() } || q.contains("pi") || q.contains("e") || q.contains("π")) && (hasMathChar || hasFunction)) {
                    try { evaluateExpression(q) } catch (_: Exception) { null }
                } else if (q.replace(".", "").all { it.isDigit() } && q.any { it.isDigit() }) q
                else null
            }

            val baseConversions = remember(mathResult) {
                mathResult?.toDoubleOrNull()?.toLong()?.let { num ->
                    if (num < 0) return@let null
                    val bin = num.toString(2).reversed().chunked(4).joinToString(" ").reversed()
                    val hex = num.toString(16).uppercase()
                    val oct = num.toString(8)
                    Triple(bin, hex, oct)
                }
            }

            val unitResult = remember(query) { performUnitConversion(query) }
            val currencyResult = remember(query, exchangeRates) { performCurrencyConversion(query, exchangeRates) }
            val systemCommands = remember(query) { if (query.isBlank()) emptyList() else CommandProcessor.process(query, mContext) }

            LazyColumn(
                modifier = Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .graphicsLayer {
                        alpha = effectiveProgress
                        scaleX = 0.95f + 0.05f * effectiveProgress
                        scaleY = 0.95f + 0.05f * effectiveProgress
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. 特殊指令 (#frozen, #private)
                val isAdvancedMode = viewModel.actionMode.value != ActionMode.ACCESSIBILITY

                if (query.trim().lowercase() == "#frozen" && isAdvancedMode) {
                    item { SpecialActionCard(stringResource(R.string.frozen_apps_title), Icons.Default.AcUnit, Color.Cyan) { showFrozenManager = true } }
                }
                if (query.trim().lowercase() == "#private" && android.os.Build.VERSION.SDK_INT >= 35) {
                    item {
                        val isLocked by viewModel.isPrivateSpaceLocked.collectAsState()
                        SpecialActionCard(if (isLocked) stringResource(R.string.private_space_locked) else stringResource(R.string.view_all), if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen, Color.Magenta) { showPrivateManager = true }
                    }
                }

                // 2. 核心結果
                if (query.isNotBlank()) {
                    if (searchCalculatorEnabled && mathResult != null) item { CalculatorResultSection(mathResult, baseConversions, clipboardManager, mContext) }
                    if (searchQuickSettingsEnabled && systemCommands.isNotEmpty()) item { SystemCommandSection(systemCommands, mContext, onDismiss) }
                    if (searchUnitConvEnabled && unitResult != null) item { UnitConverterCard(unitResult, mContext, clipboardManager, stringResource(R.string.unit_converter), Icons.AutoMirrored.Filled.CompareArrows, MaterialTheme.colorScheme.secondary) }
                    if (searchCurrencyConvEnabled && currencyResult != null) item { UnitConverterCard(currencyResult, mContext, clipboardManager, stringResource(R.string.currency_converter), Icons.Default.CurrencyExchange, Color(0xFF4CAF50)) }

                    if ((translationResult != null || isTranslating) && currencyResult == null && unitResult == null) {
                        item { TranslationResultCard(translationResult, isTranslating, clipboardManager, mContext) }
                    }

                    if (filteredResults.isNotEmpty()) item { AppResultSection(filteredResults, iconShape, { viewModel.getIcon(it) }, { app, pos -> onAppClick(app, pos); onDismiss() }) }

                    val filteredContacts = contacts.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
                    if (filteredContacts.isNotEmpty()) item { ContactResultSection(filteredContacts, mContext, onDismiss) }

                    val filteredFiles = files.filter { it.name.contains(query, ignoreCase = true) }
                    if (searchFilesEnabled && filteredFiles.isNotEmpty()) item { FileResultSection(filteredFiles, isFilesExpanded, { isFilesExpanded = true }, mContext, onDismiss) }

                    if (searchKeywordsEnabled && webSuggestions.isNotEmpty()) item { WebSuggestionSection(webSuggestions) { searchViewModel.setQuery(it) } }

                    item {
                        MoreSearchesSection(
                            query = query,
                            searchEngineUrl = searchEngineUrl,
                            isTranslationQuery = query.trim().startsWith("tr ", ignoreCase = true),
                            isEquation = query.contains("=") && (query.contains("x") || query.contains("y")),
                            isConversion = query.any { it.isDigit() } && listOf("to", "in").any { query.contains(it) },
                            context = mContext,
                            onDismiss = onDismiss
                        )
                    }
                } else {
                    // 空搜尋時顯示推薦與最愛
                    item { SearchStartPage(viewModel, clipboardManager, { searchViewModel.setQuery(it) }, onAppClick, onDismiss) }
                }
            }
        }
        if (query.contains("❄️")) SnowfallEffect()
        if (query.contains("☀️")) SunlightEffect()
    }

    // 對話框邏輯保持不變 (但已移至 SearchDialogs.kt)
    if (showFrozenManager) FrozenAppsManagerDialog(allApps, { showFrozenManager = false }, { appToUnfreeze = it })
    if (showPrivateManager) PrivateSpaceManagerDialog(allApps, { showPrivateManager = false }, { app, pos -> onAppClick(app, pos); onDismiss() })
    if (appToUnfreeze != null) {
        AlertDialog(
            onDismissRequest = { appToUnfreeze = null },
            title = { Text(stringResource(R.string.unfreeze_dialog_title)) },
            text = { Text(stringResource(R.string.unfreeze_dialog_msg)) },
            confirmButton = { Button(onClick = { appToUnfreeze?.let { viewModel.toggleFreezeApp(it, mContext) }; appToUnfreeze = null }) { Text(stringResource(R.string.unfreeze)) } },
            dismissButton = { TextButton(onClick = { appToUnfreeze = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun SpecialActionCard(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(stringResource(R.string.system_category), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = label, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TranslationResultCard(result: String?, isTranslating: Boolean, clipboard: androidx.compose.ui.platform.ClipboardManager, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
            result?.let { clipboard.setText(AnnotatedString(it)); Toast.makeText(context, "Translation Copied", Toast.LENGTH_SHORT).show() }
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Translate, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.translator), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                if (isTranslating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 8.dp), color = Color.White)
                else Text(text = result ?: "", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
            }
            if (!isTranslating) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SearchStartPage(
    viewModel: MainViewModel,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    onQueryChange: (String) -> Unit,
    onAppClick: (AppModel, Offset) -> Unit,
    onDismiss: () -> Unit
) {
    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val suggestedApps by viewModel.suggestedApps.collectAsState()
    val iconScaleFactor by viewModel.iconScale.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val clipboardText = clipboard.getText()?.text

    Column {
        if (!clipboardText.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onQueryChange(clipboardText) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.ContentPaste, null, tint = Color.White) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.clipboard_copied), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                        Text(text = clipboardText, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
        }

        val favoriteApps = allApps.filter { favoritePackages.contains(it.packageName) && !it.isHidden && !it.isFrozen && !it.isPrivate }.take(8)
        if (favoriteApps.isNotEmpty()) {
            Text(stringResource(R.string.favorites), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
            AppGrid(favoriteApps, iconScaleFactor, iconShape, viewModel) { app, pos -> onAppClick(app, pos); onDismiss() }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(0.15f))
        }

        val suggestions = if (suggestedApps.isNotEmpty()) suggestedApps.filter { !it.isFrozen && !it.isPrivate }.take(8)
        else allApps.filter { !it.isHidden && !it.isFrozen && !it.isPrivate }.take(8)
        
        Text(stringResource(R.string.app_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
        AppGrid(suggestions, iconScaleFactor, iconShape, viewModel) { app, pos -> onAppClick(app, pos); onDismiss() }
    }
}

@Composable
private fun AppGrid(apps: List<AppModel>, scale: Float, shape: IconShape, viewModel: MainViewModel, onClick: (AppModel, Offset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        apps.chunked(4).forEach { rowApps ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowApps.forEach { app ->
                    var itemPosition by remember { mutableStateOf(Offset.Zero) }
                    AppItem(
                        app = app,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { itemPosition = it.positionInRoot() },
                        iconSize = 56.dp * scale,
                        iconShape = shape,
                        getIcon = { viewModel.getIcon(it) },
                        onAppClick = { onClick(app, itemPosition) }
                    )
                }
                repeat(4 - rowApps.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}
