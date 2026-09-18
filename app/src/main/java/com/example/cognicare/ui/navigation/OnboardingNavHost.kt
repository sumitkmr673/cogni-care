package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.ui.screens.onboarding.CaregiverLoginScreen
import com.example.cognicare.ui.screens.onboarding.ConsentScreen
import com.example.cognicare.ui.screens.onboarding.IdentifyScreen
import com.example.cognicare.ui.screens.onboarding.LanguageScreen
import com.example.cognicare.ui.screens.onboarding.PatientDeviceSetupScreen
import com.example.cognicare.ui.screens.onboarding.PatientLoginScreen
import com.example.cognicare.ui.screens.onboarding.WelcomeScreen
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.OnboardingViewModel

@Composable
fun OnboardingNavHost(
    onboardingComplete: Boolean,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val languageLabel = state.selectedLanguage?.nativeName
    // Captured once so preference writes during onboarding don't rebuild the graph.
    val startDestination: Any = remember { if (onboardingComplete) IdentifyRoute else WelcomeRoute }

    PatientTheme {
        NavHost(navController = navController, startDestination = startDestination) {
            composable<WelcomeRoute> {
                WelcomeScreen(
                    state = state,
                    onStart = { navController.navigate(LanguageRoute) { launchSingleTop = true } },
                    onDemoPatient = viewModel::startDemoPatient,
                    onDemoCaregiver = viewModel::startDemoCaregiver
                )
            }
            composable<LanguageRoute> {
                LanguageScreen(
                    state = state,
                    onSelect = viewModel::selectLanguage,
                    onBack = { navController.navigateUp() },
                    onContinue = { navController.navigate(ConsentRoute) { launchSingleTop = true } }
                )
            }
            composable<ConsentRoute> {
                ConsentScreen(
                    state = state,
                    onBack = { navController.navigateUp() },
                    onAgree = {
                        viewModel.acceptConsent()
                        navController.navigate(IdentifyRoute) { launchSingleTop = true }
                    }
                )
            }
            composable<IdentifyRoute> {
                IdentifyScreen(
                    state = state,
                    // Returning users start here, so "Go back" reopens language selection.
                    onBack = {
                        if (!navController.navigateUp()) {
                            navController.navigate(LanguageRoute) { launchSingleTop = true }
                        }
                    },
                    onContinue = { area ->
                        val route = when {
                            area != AppArea.PATIENT -> CaregiverLoginRoute
                            // A device that already has a patient set up goes straight to the PIN;
                            // otherwise it needs a real sign-in first — see AuthRepository.setupPatientDevice.
                            state.hasPatientDeviceSetup -> PatientLoginRoute
                            else -> PatientDeviceSetupRoute
                        }
                        navController.navigate(route) { launchSingleTop = true }
                    }
                )
            }
            composable<PatientLoginRoute> {
                PatientLoginScreen(
                    languageLabel = languageLabel,
                    onBack = { navController.navigateUp() },
                    onSetUpDeviceAgain = { navController.navigate(PatientDeviceSetupRoute) { launchSingleTop = true } }
                )
            }
            composable<PatientDeviceSetupRoute> {
                PatientDeviceSetupScreen(
                    languageLabel = languageLabel,
                    onBack = { navController.navigateUp() }
                )
            }
            composable<CaregiverLoginRoute> {
                CaregiverTheme {
                    CaregiverLoginScreen(onBack = { navController.navigateUp() })
                }
            }
        }
    }
}
