package com.example.cognicare.viewmodel

import com.example.cognicare.core.locale.AppLanguage

data class OnboardingUiState(
    val selectedLanguage: AppLanguage? = null,
    val consentAccepted: Boolean = false,
    val isStartingDemo: Boolean = false,
    /** Whether this device already has a patient email/password cached and a local PIN set — see AuthRepository.setupPatientDevice. */
    val hasPatientDeviceSetup: Boolean = false
)
