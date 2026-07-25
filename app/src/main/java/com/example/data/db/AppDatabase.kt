package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.Priority
import com.example.data.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Task::class, Category::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskflow_database"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database.categoryDao(), database.taskDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(categoryDao: CategoryDao, taskDao: TaskDao) {
            val defaultCategories = listOf(
                Category(name = "Work", colorHex = "#3B82F6", iconName = "Work"),
                Category(name = "Personal", colorHex = "#EC4899", iconName = "Person"),
                Category(name = "Shopping", colorHex = "#8B5CF6", iconName = "ShoppingCart"),
                Category(name = "Health", colorHex = "#10B981", iconName = "FitnessCenter"),
                Category(name = "Finance", colorHex = "#F59E0B", iconName = "AccountBalance"),
                Category(name = "Study", colorHex = "#06B6D4", iconName = "School")
            )
            defaultCategories.forEach { categoryDao.insertCategory(it) }

            val now = System.currentTimeMillis()
            val dayInMillis = 86400000L
            val sampleTasks = listOf(
                Task(
                    title = "Welcome to TaskFlow! 🎉",
                    description = "Explore adding, searching, and managing your daily goals seamlessly.",
                    isCompleted = false,
                    categoryName = "Personal",
                    priority = Priority.HIGH,
                    dueDateMillis = now + dayInMillis
                ),
                Task(
                    title = "Review Q3 Project Milestones",
                    description = "Prepare key metrics and deliverables for team sync.",
                    isCompleted = false,
                    categoryName = "Work",
                    priority = Priority.HIGH,
                    dueDateMillis = now + (2 * dayInMillis)
                ),
                Task(
                    title = "Buy fresh groceries & fruit",
                    description = "Organic milk, oats, blueberries, sourdough bread.",
                    isCompleted = false,
                    categoryName = "Shopping",
                    priority = Priority.MEDIUM,
                    dueDateMillis = now + dayInMillis
                ),
                Task(
                    title = "30-minute evening jog",
                    description = "Maintain active daily health streak.",
                    isCompleted = true,
                    categoryName = "Health",
                    priority = Priority.LOW,
                    dueDateMillis = now - (1 * dayInMillis)
                )
            )
            sampleTasks.forEach { taskDao.insertTask(it) }
        }
    }
}
