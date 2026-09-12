package com.example.cognicare.viewmodel

import com.example.cognicare.data.model.AuthSession

sealed interface RootUiState {
    data object Loading : RootUiState

    /** [onboardingComplete] means language and consent are done, so start at role selection. */
    data class SignedOut(val onboardingComplete: Boolean) : RootUiState

    data class SignedIn(val session: AuthSession) : RootUiState
}
