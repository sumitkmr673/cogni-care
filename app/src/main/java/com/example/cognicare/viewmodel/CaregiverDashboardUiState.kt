package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile

data class CaregiverDashboardUiState(
    val caregiverName: String = "",
    val patients: List<PatientProfile> = emptyList(),
    val dashboard: PatientDashboard? = null,
    val isLoading: Boolean = true
)
