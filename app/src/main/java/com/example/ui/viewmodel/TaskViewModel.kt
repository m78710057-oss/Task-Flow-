package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Category
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.preferences.SortMode
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

data class TaskStats(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val overdueCount: Int = 0,
    val completionPercentage: Float = 0f
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = TaskRepository(database.taskDao(), database.categoryDao())
    private val prefsRepository = UserPreferencesRepository(application)

    // User settings
    val userSettings: StateFlow<UserSettings> = prefsRepository.userSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    // Search and Filters State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow(TaskFilter.ALL)
    val statusFilter: StateFlow<TaskFilter> = _statusFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedPriority = MutableStateFlow<Priority?>(null)
    val selectedPriority: StateFlow<Priority?> = _selectedPriority.asStateFlow()

    // Raw streams from DB
    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _rawTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private data class FilterState(
        val query: String,
        val status: TaskFilter,
        val category: String?,
        val priority: Priority?
    )

    private val filterStateFlow = combine(
        _searchQuery,
        _statusFilter,
        _selectedCategory,
        _selectedPriority
    ) { query, status, category, priority ->
        FilterState(query, status, category, priority)
    }

    // Filtered & Sorted Tasks Flow
    val tasks: StateFlow<List<Task>> = combine(
        _rawTasks,
        filterStateFlow,
        userSettings
    ) { tasksList, filter, settings ->
        var filtered = tasksList

        // Search query
        if (filter.query.isNotBlank()) {
            val q = filter.query.trim().lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.categoryName.lowercase().contains(q)
            }
        }

        // Status filter
        filtered = when (filter.status) {
            TaskFilter.ALL -> filtered
            TaskFilter.PENDING -> filtered.filter { !it.isCompleted }
            TaskFilter.COMPLETED -> filtered.filter { it.isCompleted }
        }

        // Category filter
        if (filter.category != null) {
            filtered = filtered.filter { it.categoryName.equals(filter.category, ignoreCase = true) }
        }

        // Priority filter
        if (filter.priority != null) {
            filtered = filtered.filter { it.priority == filter.priority }
        }

        // Sorting
        when (settings.sortMode) {
            SortMode.DUE_DATE -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAtMillis }
            )
            SortMode.PRIORITY -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.priority.level }
                    .thenBy { it.dueDateMillis ?: Long.MAX_VALUE }
            )
            SortMode.TITLE -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
            SortMode.CREATED_DATE -> filtered.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAtMillis }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Calculated Task Statistics
    val taskStats: StateFlow<TaskStats> = _rawTasks.map { list ->
        val now = System.currentTimeMillis()
        val total = list.size
        val completed = list.count { it.isCompleted }
        val pending = list.count { !it.isCompleted }
        val overdue = list.count { !it.isCompleted && it.dueDateMillis != null && it.dueDateMillis < now }
        val percentage = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        TaskStats(
            totalCount = total,
            pendingCount = pending,
            completedCount = completed,
            overdueCount = overdue,
            completionPercentage = percentage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    // Filter Actions
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: TaskFilter) {
        _statusFilter.value = filter
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun setPriorityFilter(priority: Priority?) {
        _selectedPriority.value = if (_selectedPriority.value == priority) null else priority
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _statusFilter.value = TaskFilter.ALL
        _selectedCategory.value = null
        _selectedPriority.value = null
    }

    // Task Actions
    fun addTask(
        title: String,
        description: String = "",
        categoryName: String,
        priority: Priority,
        dueDateMillis: Long? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = Task(
                title = title.trim(),
                description = description.trim(),
                categoryName = categoryName,
                priority = priority,
                dueDateMillis = dueDateMillis
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        if (task.title.isBlank()) return
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAtMillis = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun deleteCompletedTasks() {
        viewModelScope.launch {
            repository.deleteCompletedTasks()
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
        }
    }

    // Category Actions
    fun addCategory(name: String, colorHex: String, iconName: String = "Bookmark") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val category = Category(
                name = name.trim(),
                colorHex = colorHex,
                iconName = iconName
            )
            repository.insertCategory(category)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // Settings Actions
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsRepository.updateThemeMode(mode)
        }
    }

    fun updateDefaultPriority(priority: Priority) {
        viewModelScope.launch {
            prefsRepository.updateDefaultPriority(priority)
        }
    }

    fun updateDefaultCategory(categoryName: String) {
        viewModelScope.launch {
            prefsRepository.updateDefaultCategory(categoryName)
        }
    }

    fun updateSortMode(sortMode: SortMode) {
        viewModelScope.launch {
            prefsRepository.updateSortMode(sortMode)
        }
    }
}

class TaskViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
