package com.liferlighdow.iteration.ui.widgets

import android.util.Xml
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kyant.backdrop.Backdrop
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.WidgetDisplayMode
import com.liferlighdow.iteration.data.WidgetModel
import com.liferlighdow.iteration.data.WidgetType
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.ui.liquidGlass
import com.liferlighdow.iteration.ui.withGlassShadow
import com.liferlighdow.iteration.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.URL

data class RssItem(val title: String, val link: String, val pubDate: String? = null)

@Composable
fun RSSWidget(
    widget: WidgetModel,
    displayMode: WidgetDisplayMode,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isMinusOnePage: Boolean = false
) {
    val rssType = widget.widgetType as? WidgetType.RSS ?: return
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<RssItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val viewModel: MainViewModel = viewModel()
    val isLiquidGlassEnabled by viewModel.isLiquidGlassEnabled.collectAsState()
    val isLiquidWidgetsEnabled by (if (isMinusOnePage) viewModel.isLiquidGlassMinusOneWidgetEnabled else viewModel.isLiquidGlassWidgetsEnabled).collectAsState()
    val blurRadius by viewModel.liquidGlassBlur.collectAsState()
    val refractionHeight by viewModel.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by viewModel.liquidGlassRefractionAmount.collectAsState()
    val chromaticAberration by viewModel.liquidGlassChromaticAberration.collectAsState()

    val useLiquid = displayMode == WidgetDisplayMode.GLASS && isLiquidGlassEnabled && isLiquidWidgetsEnabled && backdrop != null
    val isGlass = displayMode == WidgetDisplayMode.GLASS
    
    val containerColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> glassFallbackColor(0.2f)
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (displayMode) {
        WidgetDisplayMode.GLASS -> Color.White
        WidgetDisplayMode.COLOR -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    LaunchedEffect(rssType.url) {
        if (rssType.url.isBlank()) {
            items = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        try {
            val fetchedItems = withContext(Dispatchers.IO) {
                fetchRss(rssType.url)
            }
            items = fetchedItems
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (rssType.isTall) 1f else 2f)
            .then(if (useLiquid) Modifier.liquidGlass(
                enabled = true,
                backdrop = backdrop,
                cornerRadius = 24.dp,
                blurRadius = blurRadius,
                refractionHeight = refractionHeight,
                refractionAmount = refractionAmount,
                chromaticAberration = chromaticAberration
            ) else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (useLiquid) Color.Transparent else containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (rssType.url.isBlank()) {
                Text(
                    text = stringResource(R.string.edit_rss),
                    modifier = Modifier.align(Alignment.Center).clickable {
                        // handled by context menu
                    },
                    style = MaterialTheme.typography.bodyMedium.withGlassShadow(isGlass),
                    color = contentColor
                )
            } else if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else if (error != null) {
                Text(
                    text = stringResource(R.string.tap_to_retry),
                    modifier = Modifier.align(Alignment.Center).clickable {
                        // reload
                    },
                    style = MaterialTheme.typography.bodySmall.withGlassShadow(isGlass),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = widget.label,
                            style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass),
                            color = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(items) { item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(item.link)
                                        ).apply {
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                }
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                ).withGlassShadow(isGlass),
                                maxLines = if (rssType.isTall) 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                                color = contentColor
                            )
                            if (item.pubDate != null) {
                                Text(
                                    text = item.pubDate,
                                    style = MaterialTheme.typography.labelSmall.withGlassShadow(isGlass),
                                    color = contentColor.copy(alpha = 0.6f)
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 4.dp),
                                color = contentColor.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun fetchRss(originalUrl: String): List<RssItem> {
    return withContext(Dispatchers.IO) {
        var targetUrl = originalUrl
        try {
            val connection = URL(targetUrl).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val contentType = connection.getHeaderField("Content-Type") ?: ""
            
            if (contentType.contains("text/html", ignoreCase = true)) {
                val html = connection.getInputStream().bufferedReader().use { it.readText() }
                // 尋找 RSS 或 Atom 的 link 標籤
                val rssRegex = """<link[^>]+rel=["']alternate["'][^>]+type=["']application/(?:rss|atom)\+xml["'][^>]+href=["']([^"']+)["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
                val fallbackRegex = """<link[^>]+type=["']application/(?:rss|atom)\+xml["'][^>]+href=["']([^"']+)["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
                
                val match = rssRegex.find(html) ?: fallbackRegex.find(html)
                
                if (match != null) {
                    var foundUrl = match.groupValues[1]
                    if (!foundUrl.startsWith("http")) {
                        val baseUri = URL(targetUrl)
                        foundUrl = URL(baseUri, foundUrl).toString()
                    }
                    targetUrl = foundUrl
                }
            }
        } catch (e: Exception) {
            // 忽略自動偵測階段的錯誤，嘗試直接解析原始網址
        }

        val rssItems = mutableListOf<RssItem>()
        try {
            val connection = URL(targetUrl).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            val stream = connection.getInputStream()
            val p = Xml.newPullParser()
            p.setInput(stream, null)
            var event = p.eventType
            var inItem = false
            var curTitle: String? = null
            var curLink: String? = null
            var curPubDate: String? = null

            while (event != XmlPullParser.END_DOCUMENT) {
                val name = p.name
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (name == "item" || name == "entry") {
                            inItem = true
                        } else if (inItem) {
                            when (name) {
                                "title" -> curTitle = p.nextText()
                                "link" -> {
                                    val href = p.getAttributeValue(null, "href")
                                    curLink = href ?: p.nextText()
                                }
                                "pubDate", "published", "updated" -> curPubDate = p.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "item" || name == "entry") {
                            if (curTitle != null && curLink != null) {
                                rssItems.add(RssItem(curTitle!!, curLink!!, curPubDate))
                            }
                            curTitle = null
                            curLink = null
                            curPubDate = null
                            inItem = false
                        }
                    }
                }
                event = p.next()
            }
            stream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        rssItems
    }
}
