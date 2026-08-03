package com.liferlighdow.iteration.ui.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.SettingsActivity
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.ui.AppItem
import com.liferlighdow.iteration.ui.glassFallbackColor
import com.liferlighdow.iteration.utils.CommandResult
import com.liferlighdow.iteration.utils.IconShape
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun CalculatorResultSection(
    mathResult: String,
    baseConversions: Triple<String, String, String>?,
    clipboardManager: ClipboardManager,
    context: Context
) {
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
                    Toast.makeText(context, "Result Copied: $mathResult", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Result Copied: $bin", Toast.LENGTH_SHORT).show()
                    }
                    BaseConversionRow("HEX", hex) {
                        clipboardManager.setText(AnnotatedString(hex))
                        Toast.makeText(context, "Result Copied: $hex", Toast.LENGTH_SHORT).show()
                    }
                    BaseConversionRow("OCT", oct) {
                        clipboardManager.setText(AnnotatedString(oct))
                        Toast.makeText(context, "Result Copied: $oct", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppResultSection(
    apps: List<AppModel>,
    iconShape: IconShape,
    getIcon: (String) -> androidx.compose.ui.graphics.ImageBitmap?,
    onAppClick: (AppModel, Offset) -> Unit
) {
    Text(stringResource(R.string.apps), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            apps.take(4).forEach { app ->
                var itemPosition by remember { mutableStateOf(Offset.Zero) }
                ListItem(
                    headlineContent = { Text(app.label, color = Color.White) },
                    leadingContent = {
                        getIcon(app.uniqueId)?.let { appIcon ->
                            val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(48.dp * 0.238f)
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(shape)
                                    .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.3f), shape = shape)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .onGloballyPositioned { itemPosition = it.positionInRoot() }
                        .combinedClickable(
                            onClick = { onAppClick(app, itemPosition) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // 注意：這裡如果需要 context menu 也可以加，但目前好像只有 clickable
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun ContactResultSection(
    contacts: List<com.liferlighdow.iteration.data.ContactModel>,
    context: Context,
    onDismiss: () -> Unit
) {
    Text(stringResource(R.string.contacts), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            contacts.forEach { contact ->
                ListItem(
                    headlineContent = { Text(contact.name, color = Color.White) },
                    supportingContent = { Text(contact.phoneNumber, color = Color.White.copy(alpha = 0.6f)) },
                    leadingContent = {
                        if (contact.photo != null) Image(bitmap = contact.photo.asImageBitmap(), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                        else Box(modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.White) }
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")))
                            onDismiss()
                        }) { Icon(Icons.Default.Call, null, tint = Color.White) }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id) }
                        context.startActivity(intent)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun FileResultSection(
    files: List<com.liferlighdow.iteration.data.FileModel>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    context: Context,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(R.string.files), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
        Text("${files.size} results", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.labelSmall)
    }
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            val displayFiles = if (isExpanded) files.take(50) else files.take(3)
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
                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileObj)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open: ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    }
                )
                if (index < displayFiles.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.White.copy(alpha = 0.05f))
                }
            }

            if (!isExpanded && files.size > 3) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Box(modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.view_all), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun SystemCommandSection(
    commands: List<CommandResult>,
    context: Context,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f))
    ) {
        Column {
            commands.forEachIndexed { index, command ->
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { command.action(context); onDismiss() }
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
                if (index < commands.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.1f))
                }
            }
        }
    }
}

@Composable
fun WebSuggestionSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Text(stringResource(R.string.gesture_suggestions), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(vertical = 8.dp))
    val haptic = LocalHapticFeedback.current
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = glassFallbackColor(0.15f)),
        border = BorderStroke(1.dp, glassFallbackColor(0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            suggestions.forEach { suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion, color = Color.White) },
                    leadingContent = { Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun MoreSearchesSection(
    query: String,
    searchEngineUrl: String,
    isTranslationQuery: Boolean,
    isEquation: Boolean,
    isConversion: Boolean,
    context: Context,
    onDismiss: () -> Unit
) {
    Text(stringResource(R.string.more_searches), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
    val haptic = LocalHapticFeedback.current
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
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://translate.google.com/?sl=auto&text=${URLEncoder.encode(rawText, "UTF-8")}")))
                        onDismiss()
                    }
                }
            }
            if (isEquation) {
                SearchLinkItem(stringResource(R.string.solve_equation), Icons.Default.Functions) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wolframalpha.com/input/?i=${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
            }
            if (isConversion) {
                SearchLinkItem(stringResource(R.string.convert_currency_units), Icons.Default.CurrencyExchange) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
            }
            SearchLinkItem(stringResource(R.string.search_web), Icons.Default.Language) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${searchEngineUrl}${URLEncoder.encode(query, "UTF-8")}"))); onDismiss() }
            SearchLinkItem(stringResource(R.string.search_store), Icons.Default.Shop) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${query}"))); onDismiss() }
            SearchLinkItem(stringResource(R.string.search_maps), Icons.Default.Place) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${query}"))); onDismiss() }
        }
    }
}
