package com.liferlighdow.iteration

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.*

@Composable
fun GlobalSearchOverlay(
    isVisible: Boolean,
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
    val mContext = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // 當關閉時清空搜尋詞
    LaunchedEffect(isVisible) {
        if (!isVisible) query = ""
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable { onDismiss() }
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
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
                    if (query.any { it in "0123456789+-*/(). " } && query.any { it in "+-*/" }) {
                        try { evaluateExpression(query) } catch (e: Exception) { null }
                    } else null
                }

                val viewModel: MainViewModel = viewModel()
    val favoritePackages by viewModel.favoritePackages.collectAsState()
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
                                        android.widget.Toast.makeText(mContext, "Result copied: $mathResult", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, glassFallbackColor(0.1f))
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

                    // 2. 建議網格 與 收藏網格
                    if (query.isBlank()) {
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
                                                onAppClick = { onAppClick(app.packageName); onDismiss() }
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(0.15f))
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
                                            onAppClick = { onAppClick(app.packageName); onDismiss() }
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = glassFallbackColor(0.15f))
                        }
                    } else {
                        // 3. 搜尋結果列表
                        items(filteredResults, key = { it.uniqueId }) { app ->
                            ListItem(
                                headlineContent = { Text(app.label, color = Color.White) }, 
                                leadingContent = { 
                                    if (app.processedIcon != null) {
                                        val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                                        Image(bitmap = app.processedIcon, contentDescription = null, modifier = Modifier.size(48.dp).clip(shape).background(Color.White)) 
                                    }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent), 
                                modifier = Modifier.clickable { onAppClick(app.packageName); onDismiss() }
                            )
                        }
                    }

                    // 4. 外部搜尋連結
                    if (query.isNotBlank()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = glassFallbackColor(0.2f))
                            Text(stringResource(R.string.more_searches), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        item {
                            SearchLinkItem(stringResource(R.string.search_web), Icons.Default.Language) {
                                mContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${query}")))
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
private fun SearchLinkItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = Color.White) },
        leadingContent = { Icon(icon, contentDescription = null, tint = Color.White) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onClick() }
    )
}

/**
 * 輕量級數學表達式解析器
 * 支持: +, -, *, /, (), 小數
 */
fun evaluateExpression(str: String): String? {
    return try {
        val cleanStr = str.replace(" ", "")
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
                    else return x
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double; val startPos = pos
                if (eat('('.code)) { x = parseExpression(); eat(')'.code) }
                else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = cleanStr.substring(startPos, pos).toDouble()
                } else return Double.NaN
                return x
            }
        }.parse()
        if (result.isNaN()) null
        else if (result == result.toLong().toDouble()) result.toLong().toString()
        else String.format("%.4f", result).trimEnd('0').trimEnd('.')
    } catch (e: Exception) { null }
}
