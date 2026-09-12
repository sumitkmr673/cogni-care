package com.example.cognicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.data.local.AppPreferences
import com.example.cognicare.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<RootUiState> = combine(
        authRepository.session,
        preferences.languageTag,
        preferences.consentAccepted
    ) { session, languageTag, consentAccepted ->
        if (session != null) {
            RootUiState.SignedIn(session)
        } else {
            RootUiState.SignedOut(onboardingComplete = languageTag != null && consentAccepted)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState.Loading)

    val language: StateFlow<AppLanguage> = preferences.languageTag
        .map { AppLanguage.fromTag(it) ?: AppLanguage.ENGLISH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppLanguage.ENGLISH)

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguageTag(language.tag) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
