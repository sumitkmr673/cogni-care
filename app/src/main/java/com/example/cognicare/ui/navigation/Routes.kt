package com.example.cognicare.ui.navigation

import kotlinx.serialization.Serializable

// Onboarding (signed out) — mirrors the web kiosk: welcome → language → consent → identify → sign in
@Serializable data object WelcomeRoute
@Serializable data object LanguageRoute
@Serializable data object ConsentRoute
@Serializable data object IdentifyRoute
@Serializable data object PatientLoginRoute
@Serializable data object PatientDeviceSetupRoute
@Serializable data object CaregiverLoginRoute
@Serializable data object CaregiverRegisterRoute

// Patient graph
@Serializable data object PatientHomeRoute
@Serializable data object GamesHubRoute
@Serializable data class GameRoute(val gameType: String)
/**
 * Shown after any game. For the levelled games (Memory Match, Pattern Recall) [gameType] and the
 * level outcome let the screen offer "Play level N" or "Play again"; talking games leave them unset.
 */
@Serializable
data class GameCompleteRoute(
    val gameType: String? = null,
    val nextLevel: Int = 0,
    val leveledUp: Boolean = false
)

// Caregiver (doctor) graph
@Serializable data object CaregiverOverviewRoute
@Serializable data object PerformanceRoute
@Serializable data object AlertsRoute
@Serializable data object CareSuggestionsRoute
@Serializable data object CaregiverSettingsRoute
@Serializable data object ReportsRoute
@Serializable data object PatientsRoute
