package com.example.cognicare.viewmodel

import com.example.cognicare.core.locale.AppLanguage

data class OnboardingUiState(
    val selectedLanguage: AppLanguage? = null,
    val consentAccepted: Boolean = false,
    val isStartingDemo: Boolean = false
)
