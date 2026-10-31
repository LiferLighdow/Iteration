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
                    title = stringResource(R.string.manual_search_basic_title),
                    content = stringResource(R.string.manual_search_basic_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_system_title),
                    content = stringResource(R.string.manual_search_system_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_expanded_title),
                    content = stringResource(R.string.manual_search_expanded_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_calc_title),
                    content = stringResource(R.string.manual_search_calc_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_translator_title),
                    content = stringResource(R.string.manual_search_translator_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_matrix_title),
                    content = stringResource(R.string.manual_search_matrix_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_suggestions_title),
                    content = stringResource(R.string.manual_search_suggestions_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_solver_title),
                    content = stringResource(R.string.manual_search_solver_content)
                )
            }
            
            item {
                ManualSection(
                    title = stringResource(R.string.manual_search_clipboard_title),
                    content = stringResource(R.string.manual_search_clipboard_content)
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
                    title = stringResource(R.string.manual_dock_modern_title),
                    content = stringResource(R.string.manual_dock_modern_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_dock_classic_title),
                    content = stringResource(R.string.manual_dock_classic_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_dock_platform_title),
                    content = stringResource(R.string.manual_dock_platform_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_dock_lite_title),
                    content = stringResource(R.string.manual_dock_lite_content)
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
                    title = stringResource(R.string.manual_engine_q1_title),
                    content = stringResource(R.string.manual_engine_q1_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_engine_q2_title),
                    content = stringResource(R.string.manual_engine_q2_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_engine_q3_title),
                    content = stringResource(R.string.manual_engine_q3_content)
                )
            }

            item {
                ManualSection(
                    title = stringResource(R.string.manual_engine_q4_title),
                    content = stringResource(R.string.manual_engine_q4_content)
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
