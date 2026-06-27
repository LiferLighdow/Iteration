package com.liferlighdow.iteration.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.liferlighdow.iteration.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualsScreen(
    onBack: () -> Unit,
    onNavigateToGlobalSearchManual: () -> Unit,
    onNavigateToIconEngineManual: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_manual_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToGlobalSearchManual),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color(0xFF00ACC1).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF00ACC1), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.manual_global_search), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.manual_global_search_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToIconEngineManual),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color(0xFF8E24AA).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dashboard, null, tint = Color(0xFF8E24AA), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.manual_icon_cache_engine), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.manual_icon_cache_engine_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().alpha(0.5f), // Placeholder for future manuals
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color.Gray.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dashboard, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.manual_desktop_gestures), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchManualScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.global_search_manual_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ManualSection(
                    title = "Basic Usage",
                    content = "Swipe down on the home screen to open Global Search. It searches your apps and contacts simultaneously."
                )
            }

            item {
                ManualSection(
                    title = "Scientific Calculator",
                    content = "Enter any math expression like '2 * (3 + 4)' or 'sin(30) * pi'. It supports powers (^), modulo (%), and scientific functions (log, ln, abs, etc.)."
                )
            }

            item {
                ManualSection(
                    title = "Smart Translator",
                    content = "Type 'tr [text]' to translate into your system language, or use '[text] to [lang]' for specific languages. Example: 'Hello to ja' for Japanese."
                )
            }

            item {
                ManualSection(
                    title = "Unit & Currency Converter",
                    content = "Convert units instantly: '50 kg to lb' or '100 usd to twd'. It works offline for units and online for live exchange rates."
                )
            }

            item {
                ManualSection(
                    title = "Equation Solver",
                    content = "Detected equations like 'x^2 - 4 = 0' will provide a direct link to WolframAlpha for step-by-step solutions."
                )
            }
            
            item {
                ManualSection(
                    title = "Smart Clipboard",
                    content = "When the search bar is empty, your last copied text appears as a card for one-tap search/calculation."
                )
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconEngineManualScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.icon_cache_engine_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ManualSection(
                    title = "What is the Icon Cache Engine?",
                    content = "The Icon Cache Engine is the core system responsible for generating, processing, and storing all application icons in Iteration Launcher. It ensures that complex visual effects (like glass refraction and Material You theming) run smoothly without draining your battery."
                )
            }

            item {
                ManualSection(
                    title = "Version 13: The Unification Era",
                    content = "Iteration currently uses the V13 Engine. This version introduced the 'ID Unification Mechanism,' ensuring that icons in your folders, desktop, and app library are always perfectly synchronized and never mismatched."
                )
            }

            item {
                ManualSection(
                    title = "PWA & Web Shortcut Support",
                    content = "Unlike standard launchers, the engine assigns unique identities to web shortcuts (PWA). This allows you to have multiple shortcuts from the same browser (e.g., Chrome) with completely independent icons and labels."
                )
            }

            item {
                ManualSection(
                    title = "High-Performance Processing",
                    content = "The engine utilizes a multi-threaded architecture (Semaphore 8), allowing it to process hundreds of icons simultaneously in the background. This ensures the UI remains responsive even when you change your entire icon style or theme color."
                )
            }

            item {
                ManualSection(
                    title = "Self-Healing Cache",
                    content = "If a visual bug occurs, the engine can be manually reset. It will automatically detect outdated cache files and regenerate them using the latest rendering algorithms to keep your desktop looking sharp."
                )
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun ManualSection(title: String, content: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
    }
}
