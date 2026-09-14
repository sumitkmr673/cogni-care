package com.example.cognicare.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cognicare.R
import com.example.cognicare.core.locale.AppLanguage
import com.example.cognicare.core.security.AppArea
import com.example.cognicare.core.security.RequireArea
import com.example.cognicare.data.model.AuthSession
import com.example.cognicare.ui.screens.caregiver.AlertsScreen
import com.example.cognicare.ui.screens.caregiver.CareSuggestionsScreen
import com.example.cognicare.ui.screens.caregiver.CaregiverOverviewScreen
import com.example.cognicare.ui.screens.caregiver.CaregiverSettingsScreen
import com.example.cognicare.ui.screens.caregiver.CaregiverTopBar
import com.example.cognicare.ui.screens.caregiver.PatientsScreen
import com.example.cognicare.ui.screens.caregiver.PerformanceScreen
import com.example.cognicare.ui.screens.caregiver.ReportsScreen
import com.example.cognicare.ui.theme.CaregiverTheme

private data class CaregiverTab(
    val route: Any,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val matches: (NavDestination) -> Boolean
)

private val caregiverTabs = listOf(
    CaregiverTab(CaregiverOverviewRoute, R.string.nav_overview, Icons.Rounded.GridView) { it.hasRoute<CaregiverOverviewRoute>() },
    CaregiverTab(PerformanceRoute, R.string.nav_performance, Icons.Rounded.Insights) { it.hasRoute<PerformanceRoute>() },
    CaregiverTab(AlertsRoute, R.string.nav_alerts, Icons.Rounded.NotificationsNone) { it.hasRoute<AlertsRoute>() },
    CaregiverTab(CareSuggestionsRoute, R.string.nav_suggestions, Icons.Rounded.Lightbulb) { it.hasRoute<CareSuggestionsRoute>() },
    CaregiverTab(CaregiverSettingsRoute, R.string.nav_settings, Icons.Rounded.Settings) { it.hasRoute<CaregiverSettingsRoute>() }
)

@Composable
fun CaregiverNavHost(
    session: AuthSession,
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    RequireArea(session = session, area = AppArea.CAREGIVER, onDenied = onSignOut) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        val colors = CaregiverTheme.colors

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CaregiverTopBar(
                    session = session,
                    hasUnreadAlerts = true,
                    onOpenAlerts = { navController.navigateToTab(AlertsRoute) },
                    onOpenProfile = { navController.navigateToTab(CaregiverSettingsRoute) }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    caregiverTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any(tab.matches) == true,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = colors.mint.container,
                                unselectedIconColor = colors.mutedText,
                                unselectedTextColor = colors.mutedText
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = CaregiverOverviewRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable<CaregiverOverviewRoute> {
                    CaregiverOverviewScreen(
                        onOpenPerformance = { navController.navigateToTab(PerformanceRoute) },
                        onOpenPatients = { navController.navigate(PatientsRoute) { launchSingleTop = true } }
                    )
                }
                composable<PerformanceRoute> {
                    PerformanceScreen(
                        onOpenReports = { navController.navigate(ReportsRoute) { launchSingleTop = true } },
                        onOpenPatients = { navController.navigate(PatientsRoute) { launchSingleTop = true } }
                    )
                }
                composable<AlertsRoute> { AlertsScreen() }
                composable<CareSuggestionsRoute> { CareSuggestionsScreen() }
                composable<CaregiverSettingsRoute> {
                    CaregiverSettingsScreen(
                        session = session,
                        currentLanguage = currentLanguage,
                        onLanguageSelected = onLanguageSelected,
                        onSignOut = onSignOut
                    )
                }
                composable<ReportsRoute> { ReportsScreen(onBack = { navController.navigateUp() }) }
                composable<PatientsRoute> { PatientsScreen(onBack = { navController.navigateUp() }) }
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
