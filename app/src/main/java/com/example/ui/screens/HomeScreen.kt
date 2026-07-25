package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.preferences.SortMode
import com.example.ui.components.AddCategoryDialog
import com.example.ui.components.AddEditTaskDialog
import com.example.ui.components.TaskCard
import com.example.ui.utils.ColorUtils
import com.example.ui.viewmodel.TaskFilter
import com.example.ui.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val stats by viewModel.taskStats.collectAsState()
    val settings by viewModel.userSettings.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Dialogs state
    var showAddEditTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search tasks...") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TaskFlow",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Outlined.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sort by Due Date") },
                                onClick = {
                                    viewModel.updateSortMode(SortMode.DUE_DATE)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Priority") },
                                onClick = {
                                    viewModel.updateSortMode(SortMode.PRIORITY)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Title") },
                                onClick = {
                                    viewModel.updateSortMode(SortMode.TITLE)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.SortByAlpha, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Date Created") },
                                onClick = {
                                    viewModel.updateSortMode(SortMode.CREATED_DATE)
                                    showSortMenu = false
                                },
                                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) }
                            )
                        }
                    }

                    // Categories Manager
                    IconButton(onClick = onNavigateToCategories) {
                        Icon(Icons.Outlined.Category, contentDescription = "Categories")
                    }

                    // Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }

                    // About
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddEditTaskDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Progress",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${stats.completedCount} of ${stats.totalCount} tasks completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }

                        if (stats.overdueCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${stats.overdueCount} Overdue",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { stats.completionPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Status Filter Chips (All, Pending, Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.values().forEach { filter ->
                    val isSelected = statusFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setStatusFilter(filter) },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Categories horizontal scroll chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isAllSelected = selectedCategory == null
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("All Categories") },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                items(categories) { category ->
                    val isSelected = selectedCategory.equals(category.name, ignoreCase = true)
                    val catColor = ColorUtils.parseHexColor(category.colorHex)

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setCategoryFilter(category.name) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(catColor)
                            )
                        }
                    )
                }

                item {
                    AssistChip(
                        onClick = { showAddCategoryDialog = true },
                        label = { Text("+ Category") }
                    )
                }
            }

            // Priority Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Priority.values().forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val priorityColor = priority.getColor()

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPriorityFilter(priority) },
                        label = { Text(priority.label) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = priorityColor.copy(alpha = 0.2f),
                            selectedLabelColor = priorityColor
                        )
                    )
                }

                if (selectedCategory != null || selectedPriority != null || searchQuery.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearFilters() }) {
                        Text("Reset", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tasks List / Empty State
            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategory != null || selectedPriority != null)
                                "No matching tasks found" else "All clear! No tasks right now",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategory != null || selectedPriority != null)
                                "Try clearing your search query or filters to view more tasks."
                            else "Tap the button below to capture your goals and stay productive.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                taskToEdit = null
                                showAddEditTaskDialog = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        val category = categories.find { it.name.equals(task.categoryName, ignoreCase = true) }
                        TaskCard(
                            task = task,
                            category = category,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(it) },
                            onEditClick = {
                                taskToEdit = it
                                showAddEditTaskDialog = true
                            },
                            onDeleteClick = { viewModel.deleteTask(it) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Extra padding for FAB
                    }
                }
            }
        }
    }

    // Add / Edit Task Dialog
    if (showAddEditTaskDialog) {
        AddEditTaskDialog(
            taskToEdit = taskToEdit,
            categories = categories,
            defaultCategory = settings.defaultCategory,
            defaultPriority = settings.defaultPriority,
            onDismiss = { showAddEditTaskDialog = false },
            onSave = { title, description, categoryName, priority, dueDate ->
                if (taskToEdit == null) {
                    viewModel.addTask(title, description, categoryName, priority, dueDate)
                } else {
                    val updated = taskToEdit!!.copy(
                        title = title,
                        description = description,
                        categoryName = categoryName,
                        priority = priority,
                        dueDateMillis = dueDate
                    )
                    viewModel.updateTask(updated)
                }
                showAddEditTaskDialog = false
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name, colorHex ->
                viewModel.addCategory(name, colorHex)
                showAddCategoryDialog = false
            }
        )
    }
}
