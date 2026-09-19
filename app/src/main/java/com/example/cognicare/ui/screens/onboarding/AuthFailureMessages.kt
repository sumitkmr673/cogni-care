package com.example.cognicare.ui.screens.onboarding

import com.example.cognicare.R
import com.example.cognicare.repository.AuthFailure

/** Shared across the patient name, device-setup and caregiver sign-in screens. */
fun AuthFailure.toMessageRes(): Int = when (this) {
    AuthFailure.NETWORK_ERROR -> R.string.error_network
    AuthFailure.NAME_NOT_RECOGNIZED -> R.string.patient_login_error
    AuthFailure.LOCKED_OUT -> R.string.patient_login_locked_body
    AuthFailure.INVALID_CREDENTIALS, AuthFailure.NOT_SET_UP -> R.string.error_invalid_credentials
    AuthFailure.EMAIL_TAKEN -> R.string.error_email_taken
    AuthFailure.INVALID_DETAILS -> R.string.error_register_invalid
}
