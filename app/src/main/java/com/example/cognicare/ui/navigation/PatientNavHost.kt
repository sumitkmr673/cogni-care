package com.example.cognicare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.cognicare.core.game.LevelResult
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
    languageLabel: String,
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
            onOpenGame = { game ->
                // Always Home → Games → game, whatever screen the request came from, so "Back to
                // games" and "Play again" behave exactly as when the game is tapped in the list.
                navController.navigate(GamesHubRoute) {
                    popUpTo<PatientHomeRoute> { inclusive = false }
                    launchSingleTop = true
                }
                navController.navigate(GameRoute(game.name))
            },
            onGoHome = { navController.popBackStack(PatientHomeRoute, inclusive = false) }
        ) {
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
                    val onBack: () -> Unit = { navController.navigateUp() }
                    val onComplete: () -> Unit = {
                        navController.navigate(GameCompleteRoute()) { launchSingleTop = true }
                    }
                    // Levelled games also pass which level comes next, for "Play level N".
                    val onLevelComplete: (LevelResult) -> Unit = { result ->
                        navController.navigate(GameCompleteRoute(gameName, result.nextLevel, result.leveledUp)) {
                            launchSingleTop = true
                        }
                    }

                    when (gameType) {
                        GameType.MEMORY_MATCH -> MemoryMatchScreen(languageLabel = languageLabel, onComplete = onLevelComplete)
                        GameType.PATTERN_RECALL -> PatternRecallScreen(languageLabel = languageLabel, onComplete = onLevelComplete)
                        GameType.OBJECT_NAMING -> ObjectNamingScreen(languageLabel, onBack, onComplete)
                        GameType.DAILY_RECALL -> DailyRecallScreen(languageLabel, onBack, onComplete)
                        GameType.ORIENTATION -> OrientationScreen(languageLabel, onBack, onComplete)
                        GameType.FAMILY_IDENTIFICATION -> FamilyIdentificationScreen(languageLabel, onBack, onComplete)
                        null -> Unit
                    }
                }
                composable<GameCompleteRoute> { entry ->
                    val completed = entry.toRoute<GameCompleteRoute>()
                    val onPlayNext: (() -> Unit)? = completed.gameType?.let { game ->
                        {
                            // Swap the finished game and this screen for a fresh game; its
                            // ViewModel reads the level that was just saved.
                            navController.navigate(GameRoute(game)) {
                                popUpTo<GamesHubRoute> { inclusive = false }
                            }
                        }
                    }
                    GameCompleteScreen(
                        languageLabel = languageLabel,
                        levelResult = completed.gameType?.let { LevelResult(completed.nextLevel, completed.leveledUp) },
                        onPlayNext = onPlayNext,
                        // Pop the finished game and its completion screen off in one go.
                        onBackToGames = { navController.popBackStack(GamesHubRoute, inclusive = false) }
                    )
                }
            }
        }
    }
}
