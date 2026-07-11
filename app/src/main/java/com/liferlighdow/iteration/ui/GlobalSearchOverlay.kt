package com.liferlighdow.iteration.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipboardManager
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
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.viewmodel.*
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.SettingsActivity
import com.liferlighdow.iteration.utils.CommandProcessor
import com.liferlighdow.iteration.utils.CommandResult
import org.json.JSONArray
import com.liferlighdow.iteration.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalSearchOverlay(
    isVisible: Boolean,
    dragOffset: Float = 0f,
    onDismiss: () -> Unit,
    allApps: List<AppModel>,
    suggestedApps: List<AppModel>,
    onAppClick: (AppModel) -> Unit,
    iconShape: IconShape,
    isLiquidGlassEnabled: Boolean,
    isLiquidGlassGlobalSearchEnabled: Boolean,
    backdrop: Backdrop?,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Boolean
) {
    var query by remember { mutableStateOf("") }
    var translationResult by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    var webSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFilesExpanded by remember(query) { mutableStateOf(false) }
    val mContext = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel()

    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
    val files by viewModel.files.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()

    var showFrozenManager by remember { mutableStateOf(false) }
    var appToUnfreeze by remember { mutableStateOf<AppModel?>(null) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }
    
    // 當關閉時清空搜尋詞並強制清除焦點
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            query = ""
            translationResult = null
            focusManager.clearFocus(force = true)
        } else {
            // 每次開啟搜尋時，異步更新一下外部資料
            viewModel.loadContacts()
            viewModel.loadCalendarEvents()
            viewModel.loadFiles()
        }
    }

    // 翻譯邏輯 (統一使用線上翻譯)
    LaunchedEffect(query) {
        val q = query.trim()
        if (!q.startsWith("tr ", ignoreCase = true)) {
            translationResult = null
            return@LaunchedEffect
        }

        val rawContent = q.substring(3).trim()
        if (rawContent.isBlank()) {
            translationResult = null
            return@LaunchedEffect
        }

        val textToTranslate: String
        val targetLang: String

        val pattern = Regex(
            """^(?:["'](.+?)["']|(.+?))\s+to\s+(?:["']([a-zA-Z-]+)["']|([a-zA-Z-]+))$""",
            RegexOption.IGNORE_CASE
        )
        val match = pattern.find(rawContent)

        if (match != null) {
            textToTranslate = match.groupValues[1].takeIf { it.isNotEmpty() } ?: match.groupValues[2]
            targetLang = match.groupValues[3].takeIf { it.isNotEmpty() } ?: match.groupValues[4]
        } else {
            textToTranslate = rawContent.removeSurrounding("\"").removeSurrounding("'")
            targetLang = Locale.getDefault().toLanguageTag()
        }

        delay(500)
        translationResult = null // 開始翻譯前先清空舊結果
        isTranslating = true

        // 聯網翻譯邏輯 (Legacy API)
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=${
                        URLEncoder.encode(
                            textToTranslate,
                            "UTF-8"
                        )
                    }"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(res)
                    val resultParts = jsonArray.getJSONArray(0)
                    val translatedText = StringBuilder()
                    for (i in 0 until resultParts.length()) {
                        val part = resultParts.getJSONArray(i).optString(0, "")
                        if (part != "null") translatedText.append(part)
                    }
                    withContext(Dispatchers.Main) { translationResult = translatedText.toString() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { isTranslating = false }
            }
        }
    }

    // 搜尋建議邏輯 (抓取 Google Autocomplete)
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2 || q.startsWith("tr ", ignoreCase = true) || q.all { it.isDigit() || "+-*/^%()".contains(it) }) {
            webSuggestions = emptyList()
            return@LaunchedEffect
        }
        
        delay(300)
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=${URLEncoder.encode(q, "UTF-8")}")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                if (conn.responseCode == 200) {
                    val res = conn.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(res)
                    val suggestions = jsonArray.getJSONArray(1)
                    val list = mutableListOf<String>()
                    for (i in 0 until minOf(suggestions.length(), 4)) {
                        list.add(suggestions.getString(i))
                    }
                    withContext(Dispatchers.Main) { webSuggestions = list }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { webSuggestions = emptyList() }
            }
        }
    }

    // --- 核心改進：互動式動畫交棒 ---
    val progress = remember { Animatable(0f) }

    // 當手指拖動時，直接更新進度數值 (Snap)
    LaunchedEffect(dragOffset) {
        if (!isVisible && dragOffset > 0) {
            progress.snapTo((dragOffset / 600f).coerceIn(0f, 1f))
        }
    }

    // 當開啟狀態改變時，執行平滑動畫 (Animate)
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // 從目前的拖動進度繼續跑向 1.0
            progress.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
        } else {
            progress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    val effectiveProgress = progress.value

    if (effectiveProgress > 0.001f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = effectiveProgress
                    translationY = (effectiveProgress - 1f) * 200f
                }
                .background(Color.Black.copy(alpha = 0.4f * effectiveProgress))
                .clickable { onDismiss() }
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                        .graphicsLayer { translationY = (effectiveProgress - 1f) * 50f }
                        .liquidGlass(
                            enabled = isLiquidGlassEnabled && isLiquidGlassGlobalSearchEnabled,
                            backdrop = backdrop,
                            cornerRadius = 28.dp,
                            blurRadius = blurRadius,
                            refractionHeight = refractionHeight,
                            refractionAmount = refractionAmount,
                            chromaticAberration = chromaticAberration
                        ),
                    placeholder = { Text(stringResource(R.string.search_hint), color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear), tint = Color.White)
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = glassFallbackColor(0.2f),
                        unfocusedContainerColor = glassFallbackColor(0.2f),
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                val filteredResults = remember(query, allApps) {
                    if (query.isBlank()) emptyList()
                    else allApps.filter { !it.isHidden && !it.isFrozen && it.label.contains(query, ignoreCase = true) }
                }

                val mathResult = remember(query) {
                    val q = query.lowercase().trim()
                    if (q.isEmpty()) return@remember null
                    val hasMathChar = q.any { it in "+-*/^%()π" }
                    val hasFunction = listOf("sqrt", "sin", "cos", "tan", "cot", "sec", "csc", "log", "abs", "pi", "e").any { q.contains(it) }
                    if ((q.any { it.isDigit() } || q.contains("pi") || q.contains("e") || q.contains("π")) && (hasMathChar || hasFunction)) {
                        try { evaluateExpression(q) } catch (_: Exception) { null }
                    } else if (q.replace(".", "").all { it.isDigit() } && q.any { it.isDigit() }) {
                        q
                    } else null
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

                val unitResult = remember(query) {
                    val q = query.lowercase().trim()
                    if (q.contains(" to ") || q.contains(" in ")) {
                        try { performUnitConversion(q) } catch (_: Exception) { null }
                    } else null
                }

                val isEquation = remember(query) {
                    val q = query.lowercase()
                    q.contains("=") && (q.contains("x") || q.contains("y") || q.contains("z"))
                }

                val isConversion = remember(query) {
                    val q = query.lowercase()
                    val keywords = listOf(" to ", " in ", "usd", "twd", "jpy", "hkd", "eur", "gbp", "cny")
                    q.any { it.isDigit() } && keywords.any { q.contains(it) }
                }

                val isLauncherSettingQuery = remember(query) {
                    val q = query.lowercase().trim()
                    q == "setting" || q == "settings" || q == "launcher settings" || q == "launcher setting" || q == "設定"
                }

                val isTranslationQuery = remember(query) {
                    query.trim().startsWith("tr ", ignoreCase = true)
                }

                val systemCommands = remember(query) {
                    if (query.isBlank()) emptyList()
                    else CommandProcessor.process(query, mContext)
                }

                val filteredContacts = remember(query, contacts) {
                    if (query.isBlank()) emptyList()
                    else contacts.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
                }

                val filteredCalendar = remember(query, calendarEvents) {
                    if (query.isBlank()) emptyList()
                    else calendarEvents.filter { it.title.contains(query, ignoreCase = true) || it.calendarName?.contains(query, ignoreCase = true) == true }
                }

                val filteredFiles = remember(query, files) {
                    if (query.isBlank()) emptyList()
                    else files.filter { it.name.contains(query, ignoreCase = true) }
                }

                val currencyResult = remember(query, exchangeRates) {
                    if (query.isBlank() || exchangeRates.isEmpty()) return@remember null
                    performCurrencyConversion(query, exchangeRates)
                }

                val autoUnitConversions = remember(query, exchangeRates) {
                    val q = query.lowercase().trim()
                    val regex = Regex("""^(\d+\.?\d*)\s*([a-zA-Z]{1,5})$""")
                    val match = regex.find(q)
                    
                    if (match != null && !q.contains(" to ") && !q.contains(" in ")) {
                        val value = match.groupValues[1].toDoubleOrNull() ?: return@remember null
                        val unitOrCurrency = match.groupValues[2]
                        
                        if (unitOrCurrency.length == 3) {
                            if (exchangeRates.isEmpty()) {
                                // 如果是幣別但沒資料，回傳一個特殊標記
                                listOf("STATUS" to "LOADING_RATES")
                            } else {
                                calculateRelatedCurrencies(value, unitOrCurrency, exchangeRates)
                            }
                        } else {
                            calculateRelatedUnits(value, unitOrCurrency)
                        }
                    } else null
                }

                val clipboardText = remember(isVisible, query) {
                    if (isVisible && query.isBlank()) clipboardManager.getText()?.text else null
                }
                val favoriteApps = remember(favoritePackages, allApps) {
                    allApps.filter { favoritePackages.contains(it.packageName) && !it.isHidden && !it.isFrozen }.take(8)
                }
                val finalSuggestions = remember(suggestedApps, allApps) {
                    if (suggestedApps.isNotEmpty()) suggestedApps.filter { !it.isFrozen }.take(8)
                    else allApps.filter { !it.isHidden && !it.isFrozen }.take(8)
                }

                val isFrozenSearch = remember(query) { query.trim().lowercase() == "#frozen" }

                val hashtagCategoryApps = remember(query, allApps) {
                    val q = query.trim()
                    if (q.startsWith("#") && q.length > 1 && q.lowercase() != "#frozen") {
                        val categoryName = q.substring(1).lowercase()
                        allApps.filter { 
                            !it.isHidden && !it.isFrozen && 
                            it.displayCategory.lowercase() == categoryName 
                        }
                    } else emptyList()
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                        .graphicsLayer {
                            alpha = effectiveProgress
                            scaleX = 0.95f + 0.05f * effectiveProgress
                            scaleY = 0.95f + 0.05f * effectiveProgress
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isFrozenSearch) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                                    showFrozenManager = true
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(Color.Cyan.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AcUnit, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.system_category), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        Text(text = stringResource(R.string.frozen_apps_title), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    if (hashtagCategoryApps.isNotEmpty()) {
                        item {
                            val categoryName = query.trim().substring(1)
                            Text(categoryName, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    hashtagCategoryApps.take(4).forEach { app ->
                                        ListItem(
                                            headlineContent = { Text(app.label, color = Color.White) },
                                            leadingContent = {
                                                viewModel.getIcon(app.uniqueId)?.let { appIcon ->
                                                    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                                                    Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(shape).background(Color.White))
                                                }
                                            },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            modifier = Modifier.clickable { onAppClick(app); onDismiss() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && mathResult != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Column {
                                    Row(modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(mathResult))
                                            Toast.makeText(mContext, mContext.getString(R.string.result_copied, mathResult), Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Calculate, null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(stringResource(R.string.calculator), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text(text = mathResult, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                    }

                                    baseConversions?.let { (bin, hex, oct) ->
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.1f))
                                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            BaseConversionRow("BIN", bin) {
                                                clipboardManager.setText(AnnotatedString(bin))
                                                Toast.makeText(mContext, mContext.getString(R.string.result_copied, bin), Toast.LENGTH_SHORT).show()
                                            }
                                            BaseConversionRow("HEX", hex) {
                                                clipboardManager.setText(AnnotatedString(hex))
                                                Toast.makeText(mContext, mContext.getString(R.string.result_copied, hex), Toast.LENGTH_SHORT).show()
                                            }
                                            BaseConversionRow("OCT", oct) {
                                                clipboardManager.setText(AnnotatedString(oct))
                                                Toast.makeText(mContext, mContext.getString(R.string.result_copied, oct), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && systemCommands.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Column {
                                    systemCommands.forEachIndexed { index, command ->
                                        Row(modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                command.action(mContext)
                                                onDismiss()
                                            }
                                            .padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                                Icon(command.icon, null, tint = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(command.description, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                                Text(text = command.label, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                        }
                                        if (index < systemCommands.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && autoUnitConversions != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Column {
                                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.AutoMirrored.Filled.CompareArrows, null, tint = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(stringResource(R.string.unit_converter), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                            Text(text = query.trim(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.1f))
                                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (autoUnitConversions.firstOrNull()?.first == "STATUS") {
                                            Text(
                                                text = "匯率資料獲取中，請檢查網路連線...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        } else {
                                            autoUnitConversions.forEach { (label, value) ->
                                                BaseConversionRow(label, value) {
                                                    clipboardManager.setText(AnnotatedString(value))
                                                    Toast.makeText(mContext, mContext.getString(R.string.result_copied, value), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && unitResult != null) {
                        item { UnitConverterCard(unitResult, mContext, clipboardManager, stringResource(R.string.unit_converter), Icons.AutoMirrored.Filled.CompareArrows, MaterialTheme.colorScheme.secondary) }
                    }

                    if (query.isNotBlank() && currencyResult != null) {
                        item { UnitConverterCard(currencyResult, mContext, clipboardManager, stringResource(R.string.currency_converter), Icons.Default.CurrencyExchange, Color(0xFF4CAF50)) }
                    }

                    if (query.isNotBlank() && (translationResult != null || isTranslating) && currencyResult == null && unitResult == null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                                    translationResult?.let {
                                        clipboardManager.setText(AnnotatedString(it))
                                        Toast.makeText(mContext, mContext.getString(R.string.translation_copied), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF2196F3).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Translate, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.translator), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        if (isTranslating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 8.dp), color = Color.White)
                                        else Text(text = translationResult ?: "", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                    if (!isTranslating) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && isLauncherSettingQuery) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
                                    mContext.startActivity(Intent(mContext, SettingsActivity::class.java))
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Settings, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(stringResource(R.string.system_category), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        Text(text = stringResource(R.string.menu_launcher_settings), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    if (query.isBlank()) {
                        if (clipboardText != null && clipboardText.isNotBlank()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { query = clipboardText },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                    border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.ContentPaste, null, tint = Color.White)
                                        }
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
                        }

                        if (favoriteApps.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.favorites), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), userScrollEnabled = false) {
                                        items(favoriteApps) { app ->
                                            AppItem(app = app, iconSize = 56.dp, iconShape = iconShape, getIcon = { pkg -> viewModel.getIcon(pkg) }, onAppClick = { onAppClick(app); onDismiss() })
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(0.15f))
                            }
                        }

                        item {
                            Text(stringResource(R.string.app_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), userScrollEnabled = false) {
                                    items(finalSuggestions) { app ->
                                        AppItem(app = app, iconSize = 56.dp, iconShape = iconShape, getIcon = { pkg -> viewModel.getIcon(pkg) }, onAppClick = { onAppClick(app); onDismiss() })
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(0.15f))
                        }
                    } else {
                        if (filteredResults.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.apps), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                    border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        filteredResults.take(4).forEach { app ->
                                            ListItem(
                                                headlineContent = { Text(app.label, color = Color.White) },
                                                leadingContent = {
                                                    viewModel.getIcon(app.uniqueId)?.let { appIcon ->
                                                        val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                                                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(shape).background(Color.White))
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                modifier = Modifier.clickable { onAppClick(app); onDismiss() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredContacts.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.contacts), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                    border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        filteredContacts.forEach { contact ->
                                            ListItem(
                                                headlineContent = { Text(contact.name, color = Color.White) },
                                                supportingContent = { Text(contact.phoneNumber, color = Color.White.copy(alpha = 0.6f)) },
                                                leadingContent = {
                                                    if (contact.photo != null) Image(bitmap = contact.photo.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                                    else Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White) }
                                                },
                                                trailingContent = {
                                                    IconButton(onClick = {
                                                        mContext.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")))
                                                        onDismiss()
                                                    }) { Icon(Icons.Default.Call, null, tint = Color.White) }
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                modifier = Modifier.clickable {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id) }
                                                    mContext.startActivity(intent)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredCalendar.isNotEmpty()) {
                            item {
                                Text(stringResource(R.string.calendar), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                    border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        filteredCalendar.take(5).forEach { event ->
                                            ListItem(
                                                headlineContent = { Text(event.title, color = Color.White) },
                                                supportingContent = { 
                                                    val timeStr = "${dateFormat.format(Date(event.startTime))} ${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}"
                                                    Text(timeStr, color = Color.White.copy(alpha = 0.6f)) 
                                                },
                                                leadingContent = {
                                                    Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Event, null, tint = Color.White)
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                modifier = Modifier.clickable {
                                                    val uri = Uri.parse("content://com.android.calendar/events/${event.id}")
                                                    val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                                                    mContext.startActivity(intent)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (filteredFiles.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(R.string.files), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
                                    Text("${filteredFiles.size} results", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
                                }
                                Card(
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                    border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        val displayFiles = if (isFilesExpanded) filteredFiles.take(50) else filteredFiles.take(3)
                                        displayFiles.forEachIndexed { index, file ->
                                            ListItem(
                                                headlineContent = { Text(file.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                supportingContent = { Text(file.path, color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                leadingContent = {
                                                    val icon = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description
                                                    Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.05f), CircleShape), contentAlignment = Alignment.Center) {
                                                        Icon(icon, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                                                    }
                                                },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                modifier = Modifier.clickable {
                                                    try {
                                                        val fileObj = java.io.File(file.path)
                                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                                            mContext,
                                                            "${mContext.packageName}.fileprovider",
                                                            fileObj
                                                        )
                                                        
                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                            setDataAndType(uri, mContext.contentResolver.getType(uri) ?: "*/*")
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                        mContext.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(mContext, "Cannot open: ${file.name}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    onDismiss()
                                                }
                                            )
                                            if (index < displayFiles.size - 1) {
                                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                                            }
                                        }

                                        if (!isFilesExpanded && filteredFiles.size > 3) {
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { isFilesExpanded = true }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.view_all),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank() && webSuggestions.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.gesture_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    webSuggestions.forEach { suggestion ->
                                        ListItem(
                                            headlineContent = { Text(suggestion, color = Color.White) },
                                            leadingContent = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) },
                                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                            modifier = Modifier.clickable { query = suggestion }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (query.isNotBlank()) {
                        item {
                            Text(stringResource(R.string.more_searches), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    if (isTranslationQuery) {
                                        val rawText = query.trim().substring(3).trim().removeSurrounding("\"").removeSurrounding("'")
                                        if (rawText.isNotEmpty()) {
                                            SearchLinkItem(stringResource(R.string.translator), Icons.Default.Translate) {
                                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/?sl=auto&text=${URLEncoder.encode(rawText, "UTF-8")}")))
                                                onDismiss()
                                            }
                                        }
                                    }
                                    if (isEquation) {
                                        SearchLinkItem(stringResource(R.string.solve_equation), Icons.Default.Functions) { mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wolframalpha.com/input/?i=${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
                                    }
                                    if (isConversion) {
                                        SearchLinkItem(stringResource(R.string.convert_currency_units), Icons.Default.CurrencyExchange) { mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
                                    }
                                    SearchLinkItem(stringResource(R.string.search_web), Icons.Default.Language) { mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${searchEngineUrl}${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
                                    SearchLinkItem(stringResource(R.string.search_store), Icons.Default.Shop) { mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${query}"))); onDismiss() }
                                    SearchLinkItem(stringResource(R.string.search_maps), Icons.Default.Place) { mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${query}"))); onDismiss() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFrozenManager) {
        FrozenAppsManagerDialog(
            allApps = allApps,
            onDismiss = { showFrozenManager = false },
            onUnfreezeClick = { appToUnfreeze = it }
        )
    }

    if (appToUnfreeze != null) {
        AlertDialog(
            onDismissRequest = { appToUnfreeze = null },
            title = { Text(stringResource(R.string.unfreeze_dialog_title)) },
            text = { Text(stringResource(R.string.unfreeze_dialog_msg)) },
            confirmButton = {
                Button(onClick = {
                    appToUnfreeze?.let { viewModel.toggleFreezeApp(it, mContext) }
                    appToUnfreeze = null
                }) { Text(stringResource(R.string.unfreeze)) }
            },
            dismissButton = {
                TextButton(onClick = { appToUnfreeze = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrozenAppsManagerDialog(
    allApps: List<AppModel>,
    onDismiss: () -> Unit,
    onUnfreezeClick: (AppModel) -> Unit
) {
    val frozenApps = remember(allApps) { allApps.filter { it.isFrozen } }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.frozen_apps_title)) },
        text = {
            if (frozenApps.isEmpty()) {
                Text(stringResource(R.string.no_frozen_apps))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(frozenApps, key = { it.uniqueId }) { app ->
                        var showMenu by remember { mutableStateOf(false) }
                        ListItem(
                            headlineContent = { Text(app.label, color = Color.White) },
                            supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.combinedClickable(
                                onClick = { onUnfreezeClick(app) },
                                onLongClick = { showMenu = true }
                            )
                        )
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.unfreeze)) },
                                onClick = { onUnfreezeClick(app); showMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    )
}

@Composable
private fun SearchLinkItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White) },
        leadingContent = { Icon(icon, contentDescription = null, tint = Color.White) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
private fun BaseConversionRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.4f), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun UnitConverterCard(result: String, context: Context, clipboard: ClipboardManager, label: String, icon: ImageVector, iconBgColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable {
            clipboard.setText(AnnotatedString(result))
            Toast.makeText(context, context.getString(R.string.result_copied, result), Toast.LENGTH_SHORT).show()
        },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(iconBgColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = result, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

fun performCurrencyConversion(str: String, rates: Map<String, Double>): String? {
    val regex = Regex("""^(.+?)\s*([a-zA-Z]{3})\s+(?:to|in)\s+([a-zA-Z]{3})$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]
    val amount = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null
    val fromRate = rates[from] ?: return null
    val toRate = rates[to] ?: return null
    val result = (amount / fromRate) * toRate
    return String.format("%,.2f", result) + " " + to.uppercase()
}

fun evaluateExpression(str: String): String? {
    return try {
        val cleanStr = str.replace(" ", "").lowercase()
        val result = object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < cleanStr.length) cleanStr[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double { nextChar(); val x = parseExpression(); if (pos < cleanStr.length) return Double.NaN; return x }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else if (ch == '('.code || ch == 'π'.code || (ch >= 'a'.code && ch <= 'z'.code) || (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) x *= parseFactor()
                    else return x
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) { x = parseExpression(); eat(')'.code) }
                else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = cleanStr.substring(startPos, pos).toDouble()
                } else if (ch == 'π'.code) { nextChar(); x = PI }
                else if (ch >= 'a'.code && ch <= 'z'.code) {
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    val func = cleanStr.substring(startPos, pos)
                    x = when (func) {
                        "pi" -> PI
                        "e" -> E
                        else -> {
                            eat('('.code)
                            val arg = parseExpression()
                            eat(')'.code)
                            val rad = arg * PI / 180.0
                            when (func) {
                                "sqrt" -> sqrt(arg)
                                "abs" -> abs(arg)
                                "sin" -> sin(rad)
                                "cos" -> cos(rad)
                                "tan" -> tan(rad)
                                "cot" -> 1.0 / tan(rad)
                                "sec" -> 1.0 / cos(rad)
                                "csc" -> 1.0 / sin(rad)
                                "log" -> log10(arg)
                                "ln" -> ln(arg)
                                else -> throw RuntimeException("Unknown function: $func")
                            }
                        }
                    }
                } else return Double.NaN
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }.parse()
        if (result.isNaN() || result.isInfinite()) null
        else if (result == result.toLong().toDouble()) result.toLong().toString()
        else String.format("%.8f", result).trimEnd('0').trimEnd('.')
    } catch (e: Exception) { null }
}

fun performUnitConversion(str: String): String? {
    val regex = Regex("""^(.+?)\s*([a-zA-Z]+)\s+(?:to|in)\s+([a-zA-Z]+)$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]
    val evaluatedValue = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null
    val lengthMap = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.34)
    val weightMap = mapOf("g" to 1.0, "kg" to 1000.0, "mg" to 0.001, "lb" to 453.592, "oz" to 28.3495)
    return when {
        lengthMap.containsKey(from) && lengthMap.containsKey(to) -> formatResult(evaluatedValue * lengthMap[from]!! / lengthMap[to]!!) + to
        weightMap.containsKey(from) && weightMap.containsKey(to) -> formatResult(evaluatedValue * weightMap[from]!! / weightMap[to]!!) + to
        from == "c" && to == "f" -> formatResult(evaluatedValue * 9/5 + 32) + "°F"
        from == "f" && to == "c" -> formatResult((evaluatedValue - 32) * 5/9) + "°C"
        from == "c" && to == "k" -> formatResult(evaluatedValue + 273.15) + "K"
        from == "k" && to == "c" -> formatResult(evaluatedValue - 273.15) + "°C"
        else -> null
    }
}

private fun formatResult(d: Double): String {
    return if (d == d.toLong().toDouble()) d.toLong().toString()
    else String.format("%.4f", d).trimEnd('0').trimEnd('.')
}

fun calculateRelatedUnits(value: Double, fromUnit: String): List<Pair<String, String>>? {
    val lengthMap = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mm" to 0.001, "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.34)
    val weightMap = mapOf("g" to 1.0, "kg" to 1000.0, "mg" to 0.001, "lb" to 453.592, "oz" to 28.3495)

    return when {
        lengthMap.containsKey(fromUnit) -> {
            val baseValue = value * lengthMap[fromUnit]!!
            listOf("KM", "CM", "FT", "IN").filter { it.lowercase() != fromUnit }.map { 
                it to (formatResult(baseValue / lengthMap[it.lowercase()]!!) + " " + it.lowercase())
            }
        }
        weightMap.containsKey(fromUnit) -> {
            val baseValue = value * weightMap[fromUnit]!!
            listOf("KG", "G", "LB", "OZ").filter { it.lowercase() != fromUnit }.map {
                it to (formatResult(baseValue / weightMap[it.lowercase()]!!) + " " + it.lowercase())
            }
        }
        fromUnit == "c" -> listOf("F" to (formatResult(value * 9/5 + 32) + " °F"), "K" to (formatResult(value + 273.15) + " K"))
        fromUnit == "f" -> listOf("C" to (formatResult((value - 32) * 5/9) + " °C"), "K" to (formatResult((value - 32) * 5/9 + 273.15) + " K"))
        fromUnit == "k" -> listOf("C" to (formatResult(value - 273.15) + " °C"), "F" to (formatResult((value - 273.15) * 9/5 + 32) + " °F"))
        else -> null
    }
}

fun calculateRelatedCurrencies(value: Double, from: String, rates: Map<String, Double>): List<Pair<String, String>>? {
    val targetCurrencies = listOf(
        "USD" to "美元", "EUR" to "歐元", "JPY" to "日圓", 
        "GBP" to "英鎊", "CNY" to "人民幣", "CHF" to "瑞士法郎", "TWD" to "新台幣"
    )
    
    // 智慧查表：同時嘗試大寫與小寫 Key
    val fromLower = from.lowercase()
    val fromUpper = from.uppercase()
    val fromRate = rates[fromLower] ?: rates[fromUpper] ?: return null
    
    return targetCurrencies.filter { it.first.lowercase() != fromLower }.mapNotNull { (code, label) ->
        val toRate = rates[code.lowercase()] ?: rates[code.uppercase()]
        toRate?.let {
            val result = (value / fromRate) * it
            val formatted = String.format("%,.2f", result)
            code to "$formatted $code"
        }
    }
}
