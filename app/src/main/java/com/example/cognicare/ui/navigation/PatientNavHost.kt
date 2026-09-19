package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.core.security.RequireArea
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.data.model.GameType
import com.example.cognicare.ui.components.VoiceAssistantOverlay
import com.example.cognicare.ui.screens.patient.GameCompleteScreen
import com.example.cognicare.ui.screens.patient.GamesHubScreen
import com.example.cognicare.ui.screens.patient.PatientHomeScreen
import com.example.cognicare.ui.screens.patient.games.DailyRecallScreen
import com.example.cognicare.ui.screens.patient.games.FamilyIdentificationScreen
import com.example.cognicare.ui.screens.patient.games.MemoryMatchScreen
import com.example.cognicare.ui.screens.patient.games.ObjectNamingScreen
import com.example.cognicare.ui.screens.patient.games.OrientationScreen
import com.example.cognicare.ui.screens.patient.games.PatternRecallScreen

@Composable
fun PatientNavHost(
    session: AuthSession,
    currentLanguage: com.example.cognicare.core.locale.AppLanguage,
    onLanguageSelected: (com.example.cognicare.core.locale.AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    RequireArea(session = session, area = AppArea.PATIENT, onDenied = onSignOut) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        // Games own the centre of the screen, so the assistant minimises while one is running.
        val isInGame = backStackEntry?.destination?.hasRoute<GameRoute>() == true
        // Mounted once, around the whole patient graph, rather than on each screen.
        VoiceAssistantOverlay(
            isInGame = isInGame,
            onOpenGames = { navController.navigate(GamesHubRoute) { launchSingleTop = true } },
            onGoHome = { navController.popBackStack(PatientHomeRoute, inclusive = false) }
        ) {
            NavHost(navController = navController, startDestination = PatientHomeRoute) {
                composable<PatientHomeRoute> {
                    PatientHomeScreen(
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected,
                        onOpenGames = { navController.navigate(GamesHubRoute) { launchSingleTop = true } },
                        onSwitchUser = onSignOut
                    )
                }
                composable<GamesHubRoute> {
                    GamesHubScreen(
                        onBack = { navController.navigateUp() },
                        onOpenGame = { game ->
                            navController.navigate(GameRoute(game.name)) { launchSingleTop = true }
                        }
                    )
                }
                composable<GameRoute> { entry ->
                    val gameName = entry.toRoute<GameRoute>().gameType
                    val gameType = GameType.entries.firstOrNull { it.name == gameName }
                    val onBack: () -> Unit = { navController.navigateUp() }
                    val onComplete: () -> Unit = {
                        navController.navigate(GameCompleteRoute) { launchSingleTop = true }
                    }

                    when (gameType) {
                        GameType.MEMORY_MATCH -> MemoryMatchScreen(onBack = onBack, onComplete = onComplete)
                        GameType.PATTERN_RECALL -> PatternRecallScreen(onBack = onBack, onComplete = onComplete)
                        GameType.OBJECT_NAMING -> ObjectNamingScreen(onBack = onBack, onComplete = onComplete)
                        GameType.DAILY_RECALL -> DailyRecallScreen(onBack = onBack, onComplete = onComplete)
                        GameType.ORIENTATION -> OrientationScreen(onBack = onBack, onComplete = onComplete)
                        GameType.FAMILY_IDENTIFICATION -> FamilyIdentificationScreen(onBack = onBack, onComplete = onComplete)
                        null -> Unit
                    }
                }
                composable<GameCompleteRoute> {
                    GameCompleteScreen(
                        // Pop the finished game and its completion screen off in one go.
                        onBackToGames = { navController.popBackStack(GamesHubRoute, inclusive = false) }
                    )
                }
            }
        }
    }
}
