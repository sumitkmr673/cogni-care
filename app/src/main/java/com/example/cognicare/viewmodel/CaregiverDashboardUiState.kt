package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.PatientDashboard
import com.example.cognicare.data.model.PatientProfile
import com.example.cognicare.repository.LinkPatientFailure

data class CaregiverDashboardUiState(
    val caregiverName: String = "",
    val patients: List<PatientProfile> = emptyList(),
    val dashboard: PatientDashboard? = null,
    val isLoading: Boolean = true,
    val isLinkPatientDialogVisible: Boolean = false,
    val linkPublicId: String = "",
    val isLinkingPatient: Boolean = false,
    val linkPatientError: LinkPatientFailure? = null
) {
    val canSubmitLinkPatient: Boolean
        get() = linkPublicId.trim().isNotBlank() && !isLinkingPatient
}
