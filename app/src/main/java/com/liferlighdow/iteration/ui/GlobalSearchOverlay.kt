package com.liferlighdow.iteration.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.MainViewModel
import com.liferlighdow.iteration.R
import org.json.JSONArray
import com.liferlighdow.iteration.data.AppModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.*

@Composable
fun GlobalSearchOverlay(
    isVisible: Boolean,
    dragOffset: Float = 0f,
    onDismiss: () -> Unit,
    allApps: List<AppModel>,
    suggestedApps: List<AppModel>,
    onAppClick: (String) -> Unit,
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
    val mContext = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // 當關閉時清空搜尋詞
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            query = ""
            translationResult = null
        }
    }

    // 聯網翻譯邏輯 (防抖)
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.isBlank()) {
            translationResult = null
            return@LaunchedEffect
        }

        // 支援格式: "Text to zh-TW" 或原本的 "tr Text"
        val toRegex = Regex("(.+)\\s+to\\s+([a-zA-Z-]+)$", RegexOption.IGNORE_CASE)
        val toMatch = toRegex.find(q)
        
        val textToTranslate: String
        val targetLang: String
        
        if (toMatch != null) {
            textToTranslate = toMatch.groupValues[1].trim()
            targetLang = toMatch.groupValues[2].trim()
        } else if (q.startsWith("tr ", ignoreCase = true)) {
            textToTranslate = q.substring(3).trim()
            // 獲取手機系統預設語言
            targetLang = Locale.getDefault().toLanguageTag()
        } else {
            translationResult = null
            return@LaunchedEffect
        }

        if (textToTranslate.isBlank()) {
            translationResult = null
            return@LaunchedEffect
        }

        delay(500) // 防抖
        isTranslating = true
        
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=${URLEncoder.encode(textToTranslate, "UTF-8")}")
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
                    withContext(Dispatchers.Main) {
                        translationResult = translatedText.toString()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        translationResult = "Service Unavailable (${conn.responseCode})"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    translationResult = "Error: ${e.message ?: "Unknown"}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isTranslating = false
                }
            }
        }
    }

    // 使用 Animatable 來實現流暢的從拖拽切換到動畫
    val animOffset = remember { Animatable(-150f) }
    val density = LocalContext.current.resources.displayMetrics.density

    // 同步拖拽位移與動畫
    LaunchedEffect(isVisible, dragOffset) {
        if (isVisible) {
            // 當觸發顯示後，從當前位置彈到 0
            animOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        } else {
            // 拖拽中，1:1 更新，起始隱藏位置設為 -150dp
            val dragPos = -150f + (dragOffset / density)
            animOffset.snapTo(dragPos.coerceAtMost(0f))
        }
    }

    val totalProgress = ((animOffset.value + 150f) / 150f).coerceIn(0f, 1f)

    if (totalProgress > 0f || isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * totalProgress))
                .then(if (isVisible) Modifier.clickable { onDismiss() } else Modifier)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        translationY = animOffset.value * density
                        alpha = totalProgress
                    }
            ) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, 
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
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
                    else allApps.filter { !it.isHidden && it.label.contains(query, ignoreCase = true) }
                }

                val mathResult = remember(query) {
                    val q = query.lowercase().trim()
                    if (q.isEmpty()) return@remember null
                    // 只要包含數字或常數(pi/e)，且包含數學特徵符號或函數名，就嘗試計算
                    val hasMathChar = q.any { it in "+-*/^%()π" }
                    val hasFunction = listOf("sqrt", "sin", "cos", "tan", "cot", "sec", "csc", "log", "abs", "pi", "e").any { q.contains(it) }
                    
                    if ((q.any { it.isDigit() } || q.contains("pi") || q.contains("e") || q.contains("π")) && (hasMathChar || hasFunction)) {
                        try { evaluateExpression(q) } catch (_: Exception) { null }
                    } else null
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
                    // 偵測如 "100 usd to twd" 或 "50kg in lb"
                    val keywords = listOf(" to ", " in ", "usd", "twd", "jpy", "hkd", "eur", "gbp", "cny")
                    q.any { it.isDigit() } && keywords.any { q.contains(it) }
                }

                val viewModel: MainViewModel = viewModel()
    val favoritePackages by viewModel.favoritePackages.collectAsState()
    val searchEngineUrl by viewModel.searchEngineUrl.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val isNetworkEnabled by viewModel.isNetworkAccessEnabled.collectAsState()
    val exchangeRates by viewModel.exchangeRates.collectAsState()

    // 載入聯絡人 (如果尚未載入)
    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.loadContacts()
            // 如果匯率為空且開啟網路，嘗試刷新
            if (exchangeRates.isEmpty() && isNetworkEnabled) {
                viewModel.fetchExchangeRates()
            }
        }
    }

    val filteredContacts = remember(query, contacts) {
        if (query.isBlank()) emptyList()
        else contacts.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) }
    }

    val currencyResult = remember(query, exchangeRates) {
        if (query.isBlank() || exchangeRates.isEmpty()) return@remember null
        performCurrencyConversion(query, exchangeRates)
    }

    val clipboardText = remember(isVisible, query) {
        if (isVisible && query.isBlank()) {
            clipboardManager.getText()?.text
        } else null
    }
    val favoriteApps = remember(favoritePackages, allApps) {
        allApps.filter { favoritePackages.contains(it.packageName) && !it.isHidden }.take(8)
    }

    val finalSuggestions = remember(suggestedApps, allApps) {
                    if (suggestedApps.isNotEmpty()) suggestedApps.take(8)
                    else allApps.filter { !it.isHidden }.take(8)
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. 計算機卡片
                    if (query.isNotBlank() && mathResult != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(mathResult!!))
                                        Toast.makeText(mContext, "Result copied: $mathResult", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(
                                    0.15f
                                )
                                ),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Calculate, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Calculator", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        Text(text = mathResult ?: "", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // 1.5 單位換算卡片
                    if (query.isNotBlank() && unitResult != null) {
                        item {
                            UnitConverterCard(unitResult, mContext, clipboardManager, "Unit Converter", Icons.AutoMirrored.Filled.CompareArrows, MaterialTheme.colorScheme.secondary)
                        }
                    }

                    // 1.6 匯率換算卡片 (直接顯示結果)
                    if (query.isNotBlank() && currencyResult != null) {
                        item {
                            UnitConverterCard(currencyResult!!, mContext, clipboardManager, "Currency Converter", Icons.Default.CurrencyExchange, Color(0xFF4CAF50))
                        }
                    }

                    // 1.7 翻譯卡片
                    if (query.isNotBlank() && (translationResult != null || isTranslating)) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        translationResult?.let {
                                            clipboardManager.setText(AnnotatedString(it))
                                            Toast.makeText(mContext, "Translation copied", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(
                                    0.15f
                                )
                                ),
                                border = BorderStroke(1.dp, glassFallbackColor(0.1f))
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).background(Color(0xFF2196F3).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Translate, null, tint = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Translator", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                        if (isTranslating) {
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 8.dp), color = Color.White)
                                        } else {
                                            Text(text = translationResult ?: "", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (!isTranslating) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.ContentCopy, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 2. 建議網格 與 收藏網格
                    if (query.isBlank()) {
                        // 智慧剪貼簿卡片
                        if (clipboardText != null && clipboardText.isNotBlank()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable { query = clipboardText },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = glassFallbackColor(
                                        0.15f
                                    )
                                    ),
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
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        userScrollEnabled = false
                                    ) {
                                        items(favoriteApps) { app ->
                                            AppItem(
                                                app = app, iconSize = 56.dp, iconShape = iconShape,
                                                getIcon = { pkg -> viewModel.getIcon(pkg) },
                                                onAppClick = { onAppClick(app.packageName); onDismiss() }
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(
                                    0.15f
                                )
                                )
                            }
                        }

                        item {
                            Text(stringResource(R.string.app_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(finalSuggestions) { app ->
                                        AppItem(
                                            app = app, iconSize = 56.dp, iconShape = iconShape,
                                            getIcon = { pkg -> viewModel.getIcon(pkg) },
                                            onAppClick = { onAppClick(app.packageName); onDismiss() }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(
                                0.15f
                            )
                            )
                        }
                    } else {
                        // 3. 搜尋結果列表
                        if (filteredResults.isNotEmpty()) {
                            item { Text(stringResource(R.string.apps), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                        items(filteredResults, key = { it.uniqueId }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label, color = Color.White) }, 
                                leadingContent = { 
                                    val appIcon = viewModel.getIcon(app.packageName)
                                    if (appIcon != null) {
                                        val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(shape).background(Color.White))
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent), 
                                modifier = Modifier.clickable { onAppClick(app.packageName); onDismiss() }
                            )
                        }

                        if (filteredContacts.isNotEmpty()) {
                            item { Text(stringResource(R.string.contacts), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                        items(filteredContacts, key = { it.id }) { contact ->
                            ListItem(
                                headlineContent = { Text(contact.name, color = Color.White) },
                                supportingContent = { Text(contact.phoneNumber, color = Color.White.copy(alpha = 0.6f)) },
                                leadingContent = {
                                    if (contact.photo != null) {
                                        Image(bitmap = contact.photo.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                                    } else {
                                        Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Person, null, tint = Color.White)
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                        mContext.startActivity(intent)
                                        onDismiss()
                                    }) {
                                        Icon(Icons.Default.Call, null, tint = Color.White)
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
                                    mContext.startActivity(intent)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    // 4. 外部搜尋連結
                    if (query.isNotBlank()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = glassFallbackColor(
                                0.2f
                            )
                            )
                            Text(stringResource(R.string.more_searches), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        if (isEquation) {
                            item {
                                SearchLinkItem("Solve Equation (WolframAlpha)", Icons.Default.Functions) {
                                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                                    mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wolframalpha.com/input/?i=$encodedQuery")))
                                    onDismiss()
                                }
                            }
                        }
                        if (isConversion) {
                            item {
                                SearchLinkItem("Convert Currency & Units (Google)", Icons.Default.CurrencyExchange) {
                                    val encodedQuery = URLEncoder.encode(query, "UTF-8")
                                    mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encodedQuery")))
                                    onDismiss()
                                }
                            }
                        }
                        item {
                            SearchLinkItem(stringResource(R.string.search_web), Icons.Default.Language) {
                                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${searchEngineUrl}$encodedQuery")))
                                onDismiss()
                            }
                        }
                        item {
                            SearchLinkItem(stringResource(R.string.search_store), Icons.Default.Shop) {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${query}")))
                                onDismiss()
                            }
                        }
                        item {
                            SearchLinkItem(stringResource(R.string.search_maps), Icons.Default.Place) {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${query}")))
                                onDismiss()
                            }
                        }
                    }
                }
            }
        }
    }
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
private fun UnitConverterCard(
    result: String,
    context: Context,
    clipboard: ClipboardManager,
    label: String,
    icon: ImageVector,
    iconBgColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                clipboard.setText(AnnotatedString(result))
                Toast.makeText(context, "Result copied: $result", Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(iconBgColor.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White)
            }
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

/**
 * 貨幣換算邏輯
 */
fun performCurrencyConversion(str: String, rates: Map<String, Double>): String? {
    // 正則：支持算式開頭，例如 (100+50) usd to twd
    val regex = Regex("""^(.+?)\s*([a-zA-Z]{3})\s+(?:to|in)\s+([a-zA-Z]{3})$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]

    val amount = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null

    val fromRate = rates[from]
    val toRate = rates[to]

    if (fromRate == null || toRate == null) return null

    // 換算公式：(金額 / 原始幣對USD匯率) * 目標幣對USD匯率
    val result = (amount / fromRate) * toRate
    
    return String.format("%,.2f", result) + " " + to.uppercase()
}

/**
 * 加強版輕量級數學表達式解析器
 * 支持: +, -, *, /, ^, %, (), sqrt, abs, sin, cos, tan, pi, e
 */
fun evaluateExpression(str: String): String? {
    return try {
        val cleanStr = str.replace(" ", "").lowercase()
        val result = object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < cleanStr.length) cleanStr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < cleanStr.length) return Double.NaN
                return x
            }

            // 加減
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            // 乘除、百分比
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else if (ch == '('.code || ch == 'π'.code || (ch >= 'a'.code && ch <= 'z'.code) || (ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                        x *= parseFactor()
                    } else return x
                }
            }

            // 次方、一元運算、括號、函數
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = cleanStr.substring(startPos, pos).toDouble()
                } else if (ch == 'π'.code) {
                    nextChar()
                    x = PI
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions or constants
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

                if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                return x
            }
        }.parse()

        if (result.isNaN() || result.isInfinite()) null
        else if (result == result.toLong().toDouble()) result.toLong().toString()
        else {
            val formatted = String.format("%.8f", result).trimEnd('0').trimEnd('.')
            if (formatted == "-0") "0" else formatted
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * 本地離線單位換算器
 * 支持: 長度 (m, km, cm, mm, in, ft, yd, mi), 重量 (g, kg, mg, lb, oz), 溫度 (c, f, k)
 * 改良版：支持在單位前使用算式，例如 (1+2)*5 kg to g
 */
fun performUnitConversion(str: String): String? {
    // 改良正則：支持括號與算式開頭 (^(.+?) 配合後續的單位識別)
    val regex = Regex("""^(.+?)\s*([a-zA-Z]+)\s+(?:to|in)\s+([a-zA-Z]+)$""")
    val match = regex.find(str.lowercase().trim()) ?: return null
    
    val valuePart = match.groupValues[1].trim()
    val from = match.groupValues[2]
    val to = match.groupValues[3]

    // 嘗試將 valuePart 當作算式解析，若失敗則嘗試直接轉 Double
    val evaluatedValue = evaluateExpression(valuePart)?.toDoubleOrNull() ?: valuePart.toDoubleOrNull() ?: return null

    // 單位係數基準 (Length -> meter, Weight -> gram)
    val lengthMap = mapOf(
        "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
        "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
        "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
        "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
        "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
        "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
        "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144,
        "mi" to 1609.34, "mile" to 1609.34, "miles" to 1609.34
    )

    val weightMap = mapOf(
        "g" to 1.0, "gram" to 1.0, "grams" to 1.0,
        "kg" to 1000.0, "kilogram" to 1000.0, "kilograms" to 1000.0,
        "mg" to 0.001, "milligram" to 0.001, "milligrams" to 0.001,
        "lb" to 453.592, "pound" to 453.592, "pounds" to 453.592,
        "oz" to 28.3495, "ounce" to 28.3495, "ounces" to 28.3495
    )

    return when {
        // 長度換算
        lengthMap.containsKey(from) && lengthMap.containsKey(to) -> {
            val res = evaluatedValue * lengthMap[from]!! / lengthMap[to]!!
            formatResult(res) + to
        }
        // 重量換算
        weightMap.containsKey(from) && weightMap.containsKey(to) -> {
            val res = evaluatedValue * weightMap[from]!! / weightMap[to]!!
            formatResult(res) + to
        }
        // 溫度換算 (特殊邏輯)
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
