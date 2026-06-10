package com.saavdhan.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.saavdhan.app.i18n.LocaleManager
import com.saavdhan.app.ui.cleanup.CleanupScreen
import com.saavdhan.app.ui.cleanup.CleanupViewModel
import com.saavdhan.app.ui.detail.AppDetailScreen
import com.saavdhan.app.ui.onboarding.LanguageScreen
import com.saavdhan.app.ui.scan.ScanScreen
import com.saavdhan.app.ui.scan.ScanViewModel
import com.saavdhan.app.ui.settings.SettingsScreen
import com.saavdhan.app.ui.theme.SaavdhanTheme

class MainActivity : ComponentActivity() {

    // Registered up-front; result ignored (the watchdog simply won't notify if the user declines).
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Apply the chosen language BEFORE any screen is built.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeRequestNotificationPermission()
        // Schedule the background watchdog + run an immediate new-app scan.
        com.saavdhan.app.system.watchdog.Watchdog.onAppOpen(this)
        setContent {
            SaavdhanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SaavdhanApp(
                        hasChosenLanguage = LocaleManager.hasChosen(this),
                        currentLanguage = LocaleManager.getLanguage(this) ?: LocaleManager.ENGLISH,
                        onChooseLanguage = { language ->
                            LocaleManager.setLanguage(this, language)
                            // Rebuild the activity so the new language takes effect everywhere.
                            recreate()
                        },
                    )
                }
            }
        }
    }

    /** On Android 13+, ask once for notification permission so the watchdog can alert the user. */
    private fun maybeRequestNotificationPermission() {
        // Don't interrupt first-launch onboarding: only ask once a language has been chosen, so the
        // user picks Hindi/English first and the notification prompt appears over the home screen.
        if (!LocaleManager.hasChosen(this)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{pkg}"
    fun detail(packageName: String) = "detail/$packageName"
    const val CLEANUP = "cleanup/{pkg}"
    fun cleanup(packageName: String) = "cleanup/$packageName"
}

@Composable
fun SaavdhanApp(
    hasChosenLanguage: Boolean,
    currentLanguage: String,
    onChooseLanguage: (String) -> Unit,
) {
    val navController = rememberNavController()
    // One shared scan view-model so Home and Detail see the same results.
    val scanViewModel: ScanViewModel = viewModel()

    // Picking a language recreates the activity, and Navigation then restores the back stack it
    // saved BEFORE the pick — which still points at onboarding, overriding startDestination. So
    // once a language exists, steer any restored onboarding entry to Home (and drop onboarding
    // from the stack so the back button exits the app rather than reopening it).
    LaunchedEffect(hasChosenLanguage) {
        if (hasChosenLanguage && navController.currentDestination?.route == Routes.ONBOARDING) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasChosenLanguage) Routes.HOME else Routes.ONBOARDING,
    ) {
        composable(Routes.ONBOARDING) {
            LanguageScreen(onChosen = onChooseLanguage)
        }
        composable(Routes.HOME) {
            ScanScreen(
                viewModel = scanViewModel,
                onAppClick = { pkg -> navController.navigate(Routes.detail(pkg)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("pkg") { type = NavType.StringType }),
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("pkg").orEmpty()
            AppDetailScreen(
                viewModel = scanViewModel,
                packageName = pkg,
                onBack = { navController.popBackStack() },
                onStartCleanup = { navController.navigate(Routes.cleanup(pkg)) },
            )
        }
        composable(
            route = Routes.CLEANUP,
            arguments = listOf(navArgument("pkg") { type = NavType.StringType }),
        ) { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("pkg").orEmpty()
            // A fresh CleanupViewModel per cleanup, scoped to this nav entry.
            val cleanupViewModel: CleanupViewModel = viewModel()
            CleanupScreen(
                viewModel = cleanupViewModel,
                packageName = pkg,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                currentLanguage = currentLanguage,
                onChooseLanguage = onChooseLanguage,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
