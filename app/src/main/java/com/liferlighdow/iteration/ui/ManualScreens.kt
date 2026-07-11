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
    onNavigateToIconEngineManual: () -> Unit,
    onNavigateToDockManual: () -> Unit
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
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToDockManual),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(56.dp).background(Color(0xFF43A047).copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Dashboard, null, tint = Color(0xFF43A047), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.manual_dock_style), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.manual_dock_style_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    content = "Swipe down on the home screen to open Global Search. It searches your apps, contacts, calendar events, and files simultaneously."
                )
            }

            item {
                ManualSection(
                    title = "System Commands",
                    content = "Trigger system actions directly. Type 'wifi', 'bt', 'alarm 7:30', 'timer 60', 'battery', or 'settings' to perform quick actions or jump to specific system pages."
                )
            }

            item {
                ManualSection(
                    title = "Expanded Data Search",
                    content = "Beyond apps, it now searches your Contacts (one-tap to call), Calendar Events (shows upcoming schedule), and Files/Folders across your device storage."
                )
            }

            item {
                ManualSection(
                    title = "Scientific Calculator & Bases",
                    content = "Enter math like 'sin(30) * pi'. For integers, it automatically shows BIN (grouped by 4), HEX, and OCT conversions. Tap any result to copy it."
                )
            }

            item {
                ManualSection(
                    title = "Smart Translator (tr)",
                    content = "Type 'tr [text]' for system language or 'tr \"[text]\" to [lang]' for specific ones. Using quotes helps when the content contains 'to'. You can also jump to Google Translate from the 'More Searches' section."
                )
            }

            item {
                ManualSection(
                    title = "Unit & Currency Matrix",
                    content = "Type '100usd' or '100m' to see a full matrix of 7 major currencies or related units instantly. For a specific conversion, use '100usd to twd'."
                )
            }

            item {
                ManualSection(
                    title = "Search Suggestions",
                    content = "As you type, the launcher fetches the top 4 search suggestions from the web. Tap a suggestion to fill it into the search bar."
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
fun DockStyleManualScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_dock_style)) },
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
                    title = "Modern (Floating)",
                    content = "A tribute to iOS 11 to the latest versions. This style features a high-radius rounded container that floats above the wallpaper, creating a modern and layered visual experience."
                )
            }

            item {
                ManualSection(
                    title = "Classic (Full Width)",
                    content = "A tribute to iOS 7 through iOS 10 and /e/OS. A familiar, reliable edge-to-edge design with a clean translucent look that provides a solid foundation for your most-used applications."
                )
            }

            item {
                ManualSection(
                    title = "Platform (3D Glass)",
                    content = "A tribute to iPhone OS 1 through iOS 6. This style brings back the iconic 'Glass Shelf' look, simulating a thick, refractive 3D glass platform for your apps."
                )
            }

            item {
                ManualSection(
                    title = "Lite (Minimalist)",
                    content = "A tribute to Android, MIUI/HyperOS, and minimalist philosophies. By removing the background container entirely, your icons breathe freely on the wallpaper for a clean, pure look."
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
