package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.Reminder

data class PatientHomeUiState(
    val firstName: String = "",
    val fullName: String = "",
    val publicId: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val nextReminder: Reminder? = null,
    val isRemindersUnavailable: Boolean = false,
    val isLoading: Boolean = true
)
