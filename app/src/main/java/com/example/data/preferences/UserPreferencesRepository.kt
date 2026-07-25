package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.Priority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class SortMode {
    DUE_DATE, PRIORITY, TITLE, CREATED_DATE
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultPriority: Priority = Priority.MEDIUM,
    val defaultCategory: String = "Personal",
    val sortMode: SortMode = SortMode.DUE_DATE
)

class UserPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_DEFAULT_PRIORITY = stringPreferencesKey("default_priority")
        val KEY_DEFAULT_CATEGORY = stringPreferencesKey("default_category")
        val KEY_SORT_MODE = stringPreferencesKey("sort_mode")
    }

    val userSettings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
        val theme = try { ThemeMode.valueOf(themeString) } catch (e: Exception) { ThemeMode.SYSTEM }

        val priorityString = preferences[KEY_DEFAULT_PRIORITY] ?: Priority.MEDIUM.name
        val priority = try { Priority.valueOf(priorityString) } catch (e: Exception) { Priority.MEDIUM }

        val category = preferences[KEY_DEFAULT_CATEGORY] ?: "Personal"

        val sortString = preferences[KEY_SORT_MODE] ?: SortMode.DUE_DATE.name
        val sort = try { SortMode.valueOf(sortString) } catch (e: Exception) { SortMode.DUE_DATE }

        UserSettings(theme, priority, category, sort)
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun updateDefaultPriority(priority: Priority) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_PRIORITY] = priority.name
        }
    }

    suspend fun updateDefaultCategory(category: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CATEGORY] = category
        }
    }

    suspend fun updateSortMode(sortMode: SortMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SORT_MODE] = sortMode.name
        }
    }
}
