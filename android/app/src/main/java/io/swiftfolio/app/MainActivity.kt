package io.swiftfolio.app

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.swiftfolio.app.data.ThemeMode
import io.swiftfolio.app.ui.navigation.AppViewModel
import io.swiftfolio.app.ui.navigation.SwiftFolioNavHost
import io.swiftfolio.app.ui.theme.SwiftFolioTheme
import io.swiftfolio.app.ui.theme.aurora

/** Single activity: splash, edge-to-edge, theme resolution and the nav host. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = (application as SwiftFolioApplication).container
        val appViewModel = ViewModelProvider(
            this,
            viewModelFactory { initializer { AppViewModel(container.sessionManager, container.settingsStore) } },
        )[AppViewModel::class.java]
        val launchedAt = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            appViewModel.state.value.startRoute == null && SystemClock.elapsedRealtime() - launchedAt < AppConstants.SPLASH_MAX_MILLIS
        }
        val reduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()
            val dark = when (state.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            SwiftFolioTheme(darkTheme = dark, reduceMotion = reduceMotion) {
                val startRoute = state.startRoute
                if (startRoute == null) {
                    Box(Modifier.fillMaxSize().aurora())
                } else {
                    SwiftFolioNavHost(container, startRoute, BuildConfig.VERSION_NAME)
                }
            }
        }
    }
}
