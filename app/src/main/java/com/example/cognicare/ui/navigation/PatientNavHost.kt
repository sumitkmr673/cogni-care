package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.core.security.RequireArea
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.GameType
import com.example.cognicare.ui.screens.patient.GameCompleteScreen
import com.example.cognicare.ui.screens.patient.GamePlaceholderScreen
import com.example.cognicare.ui.screens.patient.GamesHubScreen
import com.example.cognicare.ui.screens.patient.PatientHomeScreen
import com.example.cognicare.ui.screens.patient.games.MemoryMatchScreen
import com.example.cognicare.ui.screens.patient.games.PatternRecallScreen

@Composable
fun PatientNavHost(
    session: AuthSession,
    languageLabel: String,
    onSignOut: () -> Unit
) {
    RequireArea(session = session, area = AppArea.PATIENT, onDenied = onSignOut) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = PatientHomeRoute) {
            composable<PatientHomeRoute> {
                PatientHomeScreen(
                    languageLabel = languageLabel,
                    onOpenGames = { navController.navigate(GamesHubRoute) { launchSingleTop = true } },
                    onSwitchUser = onSignOut
                )
            }
            composable<GamesHubRoute> {
                GamesHubScreen(
                    languageLabel = languageLabel,
                    onBack = { navController.navigateUp() },
                    onOpenGame = { game ->
                        navController.navigate(GameRoute(game.name)) { launchSingleTop = true }
                    }
                )
            }
            composable<GameRoute> { entry ->
                val gameName = entry.toRoute<GameRoute>().gameType
                val gameType = GameType.entries.firstOrNull { it.name == gameName }
                val onComplete = { navController.navigate(GameCompleteRoute) { launchSingleTop = true } }

                when (gameType) {
                    GameType.MEMORY_MATCH -> MemoryMatchScreen(languageLabel = languageLabel, onComplete = onComplete)
                    GameType.PATTERN_RECALL -> PatternRecallScreen(languageLabel = languageLabel, onComplete = onComplete)
                    // Every other game is still a placeholder until it gets a real build-out.
                    null -> Unit
                    else -> GamePlaceholderScreen(
                        gameType = gameType,
                        languageLabel = languageLabel,
                        onBackToGames = { navController.navigateUp() }
                    )
                }
            }
            composable<GameCompleteRoute> {
                GameCompleteScreen(
                    languageLabel = languageLabel,
                    // Pop the finished game and its completion screen off in one go.
                    onBackToGames = { navController.popBackStack(GamesHubRoute, inclusive = false) }
                )
            }
        }
    }
}
