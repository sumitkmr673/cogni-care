package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cognicare.core.locale.LocalizedContent
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.core.security.canAccess
import com.example.cognicare.ui.screens.onboarding.SplashScreen
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.RootUiState
import com.example.cognicare.viewmodel.SessionViewModel

@Composable
fun CogniCareRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    // Every screen below reads its text in the chosen language.
    LocalizedContent(language) {
        when (val current = state) {
            RootUiState.Loading -> SplashScreen()

            is RootUiState.SignedOut -> OnboardingNavHost(onboardingComplete = current.onboardingComplete)

            // Each role gets its own NavHost, so the other role's destinations do not exist.
            is RootUiState.SignedIn -> key(current.session.userId) {
                val session = current.session
                if (session.role.canAccess(AppArea.CAREGIVER)) {
                    CaregiverTheme {
                        CaregiverNavHost(
                            session = session,
                            currentLanguage = language,
                            onLanguageSelected = viewModel::selectLanguage,
                            onSignOut = viewModel::signOut
                        )
                    }
                } else {
                    PatientTheme {
                        PatientNavHost(
                            session = session,
                            currentLanguage = language,
                            onLanguageSelected = viewModel::selectLanguage,
                            onSignOut = viewModel::signOut
                        )
                    }
                }
            }
        }
    }
}
