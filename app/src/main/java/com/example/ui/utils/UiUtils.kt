package com.example.ui.utils

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun formatDate(millis: Long?): String {
        if (millis == null) return ""
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()

        val isToday = calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = calendar.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(millis))

        return when {
            isToday -> "Today, $formattedTime"
            isTomorrow -> "Tomorrow, $formattedTime"
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
                dateFormat.format(Date(millis))
            }
        }
    }

    fun isOverdue(dueDateMillis: Long?): Boolean {
        if (dueDateMillis == null) return false
        return dueDateMillis < System.currentTimeMillis()
    }
}

object ColorUtils {
    fun parseHexColor(hexString: String): Color {
        return try {
            val cleanedHex = hexString.removePrefix("#")
            val colorInt = android.graphics.Color.parseColor("#$cleanedHex")
            Color(colorInt)
        } catch (e: Exception) {
            Color(0xFF4F46E5) // Fallback primary indigo
        }
    }
}
