package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HistoryDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HowItWorksScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.SalimViewModel

object Destinations {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY_DETAIL = "history_detail/{id}"
    const val DIAGNOSTICS = "diagnostics"
    const val HOW_IT_WORKS = "how_it_works"

    fun historyDetailRoute(id: Long) = "history_detail/$id"
}

@Composable
fun SalimNavGraph(
    navController: NavHostController,
    viewModel: SalimViewModel
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val startDestination = if (onboardingCompleted) Destinations.HOME else Destinations.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onNavigateToHistoryDetail = { historyId ->
                    navController.navigate(Destinations.historyDetailRoute(historyId))
                }
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate(Destinations.DIAGNOSTICS) },
                onNavigateToHowItWorks = { navController.navigate(Destinations.HOW_IT_WORKS) }
            )
        }

        composable(
            route = Destinations.HISTORY_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            HistoryDetailScreen(
                historyId = id,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.DIAGNOSTICS) {
            DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.HOW_IT_WORKS) {
            HowItWorksScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
