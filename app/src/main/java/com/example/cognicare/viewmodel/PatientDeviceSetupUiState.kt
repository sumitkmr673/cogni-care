package com.example.cognicare.viewmodel

import com.example.cognicare.repository.AuthFailure

data class PatientDeviceSetupUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: AuthFailure? = null
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}
