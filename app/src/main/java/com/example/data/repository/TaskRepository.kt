package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.TaskDao
import com.example.data.model.Category
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun deleteCompletedTasks() = taskDao.deleteCompletedTasks()

    suspend fun clearAllTasks() = taskDao.clearAllTasks()

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
}
