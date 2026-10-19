package io.swiftfolio.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.swiftfolio.app.AppContainer
import io.swiftfolio.app.ui.browser.BrowserScreen
import io.swiftfolio.app.ui.browser.BrowserViewModel
import io.swiftfolio.app.ui.connect.ConnectScreen
import io.swiftfolio.app.ui.connect.ConnectViewModel
import io.swiftfolio.app.ui.settings.SettingsScreen
import io.swiftfolio.app.ui.settings.SettingsViewModel
import io.swiftfolio.app.ui.theme.ConsoleTheme

/**
 * Connect -> Browser (Connect popped) -> Settings. Sign-out clears the back stack.
 * ViewModels are built by factories from the explicitly passed [container].
 */
@Composable
fun SwiftFolioNavHost(container: AppContainer, startRoute: String, appVersion: String) {
    val navController = rememberNavController()
    val enterMillis = ConsoleTheme.motion.standardMillis
    val exitMillis = ConsoleTheme.motion.exitMillis
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { fadeIn(tween(enterMillis)) },
        exitTransition = { fadeOut(tween(exitMillis)) },
    ) {
        composable(Routes.CONNECT) {
            val viewModel = viewModel { ConnectViewModel(container.sessionManager, container.settingsStore) }
            ConnectScreen(viewModel, onConnected = {
                navController.navigate(Routes.BROWSER) { popUpTo(Routes.CONNECT) { inclusive = true } }
            })
        }
        composable(Routes.BROWSER) {
            val viewModel = viewModel { BrowserViewModel(container.sessionManager, container.transferScheduler) }
            BrowserScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onSignedOut = { navController.navigateToConnect() },
            )
        }
        composable(Routes.SETTINGS) {
            val viewModel = viewModel { SettingsViewModel(container.sessionManager, container.settingsStore, appVersion) }
            SettingsScreen(viewModel, onBack = { navController.popBackStack() }, onSignedOut = { navController.navigateToConnect() })
        }
    }
}

private fun NavController.navigateToConnect() {
    navigate(Routes.CONNECT) { popUpTo(graph.id) { inclusive = true } }
}
