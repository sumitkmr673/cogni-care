package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.Reminder

data class PatientHomeUiState(
    val firstName: String = "",
    val todayReminders: List<Reminder> = emptyList(),
    val isLoading: Boolean = true
) {
    val completedCount: Int get() = todayReminders.count { it.completed }
}
