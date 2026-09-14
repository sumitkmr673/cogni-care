package com.example.cognicare.ui.navigation

import kotlinx.serialization.Serializable

// Onboarding (signed out) — mirrors the web kiosk: welcome → language → consent → identify → sign in
@Serializable data object WelcomeRoute
@Serializable data object LanguageRoute
@Serializable data object ConsentRoute
@Serializable data object IdentifyRoute
@Serializable data object PatientLoginRoute
@Serializable data object CaregiverLoginRoute

// Patient graph
@Serializable data object PatientHomeRoute
@Serializable data object GamesHubRoute
@Serializable data class GameRoute(val gameType: String)
@Serializable data object GameCompleteRoute

// Caregiver (doctor) graph
@Serializable data object CaregiverOverviewRoute
@Serializable data object PerformanceRoute
@Serializable data object AlertsRoute
@Serializable data object CareSuggestionsRoute
@Serializable data object CaregiverSettingsRoute
@Serializable data object ReportsRoute
@Serializable data object PatientsRoute
