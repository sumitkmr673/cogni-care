package com.example.cognicare.viewmodel

data class CaregiverLoginUiState(
    val email: String = "",
    val password: String = "",
    val isSigningIn: Boolean = false,
    val error: CaregiverLoginError? = null
) {
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isSigningIn
}

enum class CaregiverLoginError { INVALID_EMAIL, INVALID_CREDENTIALS, NETWORK_ERROR }
