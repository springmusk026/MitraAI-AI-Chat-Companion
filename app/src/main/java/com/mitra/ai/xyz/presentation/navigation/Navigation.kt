package com.mitra.ai.xyz.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mitra.ai.xyz.presentation.chat.ChatScreen
import com.mitra.ai.xyz.presentation.settings.SettingsScreen
import com.mitra.ai.xyz.presentation.splash.SplashScreen
import com.mitra.ai.xyz.presentation.splash.SplashViewModel
import com.mitra.ai.xyz.presentation.settings.backup.BackupScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Backup : Screen("backup")
}

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            val viewModel = hiltViewModel<SplashViewModel>()
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                viewModel = viewModel
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 