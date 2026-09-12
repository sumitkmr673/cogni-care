package com.example.cognicare.viewmodel

data class PatientLoginUiState(
    val pin: String = "",
    val isVerifying: Boolean = false,
    val showError: Boolean = false
) {
    companion object {
        const val PIN_LENGTH = 4
    }
}
