package com.liferlighdow.iteration.ui.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liferlighdow.iteration.R
import com.liferlighdow.iteration.data.AppModel
import com.liferlighdow.iteration.utils.IconShape
import com.liferlighdow.iteration.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLibrarySettingsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val userCategories by viewModel.userCategories.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val libraryShape by viewModel.libraryShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
    var searchQuery by remember { mutableStateOf("") }

    // 動態計算所有當前存在但未排序的類別
    val unhandledCategories = remember(allApps, userCategories) {
        allApps.asSequence()
            .map { it.displayCategory }
            .distinct()
            .filter { it !in userCategories }
            .toList()
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    var selectingAppForCategory by remember { mutableStateOf<AppModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.desc_add))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            item {
                Text(
                    stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.folder_shape)) },
                    supportingContent = { Text(if (libraryShape == IconShape.CIRCLE) stringResource(R.string.shape_circle) else stringResource(R.string.shape_default)) },
                    trailingContent = {
                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.shape_default)) },
                                    onClick = { viewModel.setLibraryShape(IconShape.DEFAULT); expanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.shape_circle)) },
                                    onClick = { viewModel.setLibraryShape(IconShape.CIRCLE); expanded = false }
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { expanded = true }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.managed_folders_ordered), 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (userCategories.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_folders_managed),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            itemsIndexed(userCategories) { index, category ->
                ListItem(
                    headlineContent = { Text(category) },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { categoryToRename = category; renameInput = category }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename), tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                enabled = index > 0,
                                onClick = { viewModel.moveUserCategory(index, index - 1) }
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.move_up), tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                            IconButton(
                                enabled = index < userCategories.size - 1,
                                onClick = { viewModel.moveUserCategory(index, index + 1) }
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.move_down), tint = if (index < userCategories.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                            }
                            IconButton(onClick = { viewModel.deleteUserCategory(category) }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_from_list), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
            
            if (unhandledCategories.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        stringResource(R.string.default_existing_folders), 
                        style = MaterialTheme.typography.titleSmall, 
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                items(unhandledCategories) { category ->
                    ListItem(
                        headlineContent = { Text(category) },
                        supportingContent = { Text(stringResource(R.string.library_folder_detected)) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { categoryToRename = category; renameInput = category }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename), tint = MaterialTheme.colorScheme.primary)
                                }
                                Button(onClick = { viewModel.addUserCategory(category) }) {
                                    Text(stringResource(R.string.manage))
                                }
                            }
                        }
                    )
                }
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.assign_categories), 
                    style = MaterialTheme.typography.titleSmall, 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SearchBar(searchQuery) { searchQuery = it }
            }
            
            val filteredApps = if (searchQuery.isEmpty()) allApps
                              else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

            items(filteredApps, key = { it.uniqueId }) { app ->
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = { Text(stringResource(R.string.folder_label_format, app.displayCategory)) },
                    leadingContent = {
                        val appIcon = viewModel.getIcon(app.uniqueId)
                        if (appIcon != null) {
                            Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
                        }
                    },
                    modifier = Modifier.clickable { selectingAppForCategory = app }
                )
            }
        }

        if (showAddCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text(stringResource(R.string.add_category_title)) },
                text = {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text(stringResource(R.string.category_name_label)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addUserCategory(newCategoryName)
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    }) { Text(stringResource(R.string.add)) }
                },
                dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }

        if (categoryToRename != null) {
            AlertDialog(
                onDismissRequest = { categoryToRename = null },
                title = { Text(stringResource(R.string.folder_delete_confirm_title)) },
                text = {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text(stringResource(R.string.new_name_label)) },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.renameCategory(categoryToRename!!, renameInput)
                        categoryToRename = null
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToRename = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        if (selectingAppForCategory != null) {
            // 合併所有可用的類別供選取
            val allAvailableCategories = (userCategories + unhandledCategories).distinct()

            AlertDialog(
                onDismissRequest = { selectingAppForCategory = null },
                title = { Text(stringResource(R.string.select_category_for, selectingAppForCategory?.label ?: "")) },
                text = {
                    LazyColumn {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.default_system)) },
                                modifier = Modifier.clickable {
                                    viewModel.setAppCategory(selectingAppForCategory!!.packageName, "")
                                    selectingAppForCategory = null
                                }
                            )
                        }
                        items(allAvailableCategories) { category ->
                            ListItem(
                                headlineContent = { Text(category) },
                                modifier = Modifier.clickable {
                                    viewModel.setAppCategory(selectingAppForCategory!!.packageName, category)
                                    selectingAppForCategory = null
                                }
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { selectingAppForCategory = null }) { Text(stringResource(R.string.close)) } }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameAppsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    val iconShape by viewModel.iconShape.collectAsState()
    val shape = if (iconShape == IconShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isEmpty()) allApps
        else allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    var editingApp by remember { mutableStateOf<AppModel?>(null) }
    var newLabel by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_rename_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {}
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(searchQuery) { searchQuery = it }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredApps, key = { it.uniqueId }) { app ->
                    ListItem(
                        headlineContent = { Text(app.label) },
                        leadingContent = {
                            val appIcon = viewModel.getIcon(app.uniqueId)
                            if (appIcon != null) {
                                Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(shape).background(Color.White))
                            }
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.setCustomLabel(app.packageName, "") }) {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.restore_default), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { editingApp = app; newLabel = app.label }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.rename), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )
                }
            }
        }
        if (editingApp != null) {
            AlertDialog(
                onDismissRequest = { editingApp = null },
                title = { Text(stringResource(R.string.rename_app_title, editingApp?.label ?: "")) },
                text = { OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, label = { Text(stringResource(R.string.new_label_hint)) }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        editingApp?.let { viewModel.setCustomLabel(it.packageName, newLabel) }
                        editingApp = null
                    }) { Text(stringResource(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = { editingApp = null }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }
}

enum class AppFilter { ALL, HIDDEN, VISIBLE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HideAppsScreen(onBack: () -> Unit) {
    val viewModel: MainViewModel = viewModel()
    val allApps by viewModel.allApps.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf(AppFilter.ALL) }

    val filteredApps = remember(allApps, searchQuery, appFilter) {
        allApps.filter { app ->
            val matchesSearch = if (searchQuery.isEmpty()) true else app.label.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (appFilter) {
                AppFilter.ALL -> true
                AppFilter.HIDDEN -> app.isHidden
                AppFilter.VISIBLE -> !app.isHidden
            }
            matchesSearch && matchesFilter
        }
    }

    var currentPassword by remember { mutableStateOf(viewModel.getPassword()) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_hide_apps)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {}
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(R.string.security_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                OutlinedTextField(
                    value = currentPassword ?: "",
                    onValueChange = {
                        currentPassword = it
                        viewModel.setPassword(it)
                    },
                    label = { Text(stringResource(R.string.password_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
            item {
                HorizontalDivider()
                Text(text = stringResource(R.string.hide_apps_instruction), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Column {
                    SearchBar(searchQuery) { searchQuery = it }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = appFilter == AppFilter.ALL,
                            onClick = { appFilter = AppFilter.ALL },
                            label = { Text(stringResource(R.string.all_label)) }
                        )
                        FilterChip(
                            selected = appFilter == AppFilter.HIDDEN,
                            onClick = { appFilter = AppFilter.HIDDEN },
                            label = { Text(stringResource(R.string.hidden_label)) },
                            leadingIcon = {
                                if (appFilter == AppFilter.HIDDEN) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = appFilter == AppFilter.VISIBLE,
                            onClick = { appFilter = AppFilter.VISIBLE },
                            label = { Text(stringResource(R.string.visible_label)) },
                            leadingIcon = {
                                if (appFilter == AppFilter.VISIBLE) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }

            items(filteredApps, key = { it.uniqueId }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleHiddenApp(app.packageName) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val appIcon = viewModel.getIcon(app.packageName)
                    if (appIcon != null) {
                        Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White))
                    }
                    Text(text = app.label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                    Checkbox(checked = app.isHidden, onCheckedChange = { viewModel.toggleHiddenApp(app.packageName) })
                }
            }
        }
    }
}
