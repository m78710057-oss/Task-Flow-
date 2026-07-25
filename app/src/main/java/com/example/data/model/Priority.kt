package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium

enum class Priority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    fun getColor(): Color = when (this) {
        LOW -> PriorityLow
        MEDIUM -> PriorityMedium
        HIGH -> PriorityHigh
    }
}
