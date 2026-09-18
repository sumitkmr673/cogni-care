package com.example.cognicare.viewmodel

import com.example.cognicare.repository.AuthFailure

data class PatientLoginUiState(
    val typedName: String = "",
    /** What the speech recognizer last heard, shown back so the patient knows what was checked. */
    val heardName: String? = null,
    val isVerifying: Boolean = false,
    val error: AuthFailure? = null,
    val isLockedOut: Boolean = false
) {
    val canSubmitTypedName: Boolean
        get() = typedName.isNotBlank() && !isVerifying && !isLockedOut
}
