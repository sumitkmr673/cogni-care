package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cognicare.ui.screens.onboarding.CaregiverLoginScreen
import com.example.cognicare.ui.screens.onboarding.PatientDeviceSetupScreen
import com.example.cognicare.ui.screens.onboarding.PatientLoginScreen
import com.example.cognicare.ui.theme.CaregiverTheme
import com.example.cognicare.ui.theme.PatientTheme
import com.example.cognicare.viewmodel.OnboardingViewModel

@Composable
fun OnboardingNavHost(
    onboardingComplete: Boolean,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Direct returning users to login; fresh devices start at guided Simple Mode setup.
    val startDestination: Any = remember(state.hasPatientDeviceSetup) {
        if (state.hasPatientDeviceSetup) PatientLoginRoute else PatientDeviceSetupRoute
    }

    PatientTheme {
        key(state.hasPatientDeviceSetup) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = startDestination) {
                composable<PatientDeviceSetupRoute> {
                    PatientDeviceSetupScreen(
                        onBack = { navController.navigateUp() },
                        onCaregiverSignIn = {
                            navController.navigate(CaregiverLoginRoute) { launchSingleTop = true }
                        }
                    )
                }
                composable<PatientLoginRoute> {
                    PatientLoginScreen(
                        onBack = {
                            if (!navController.navigateUp()) {
                                navController.navigate(PatientDeviceSetupRoute) { launchSingleTop = true }
                            }
                        },
                        onSetUpDeviceAgain = {
                            navController.navigate(PatientDeviceSetupRoute) { launchSingleTop = true }
                        }
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
}
