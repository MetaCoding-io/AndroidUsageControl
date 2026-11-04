package com.usagecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.usagecontrol.core.ui.theme.DoormanTheme
import com.usagecontrol.feature.learning.LearningScreen
import com.usagecontrol.feature.lockcontrol.LockControlScreen
import com.usagecontrol.feature.notifications.NowScreen
import com.usagecontrol.feature.notifications.apps.AppProfileScreen
import com.usagecontrol.feature.notifications.apps.AppsScreen
import com.usagecontrol.feature.onboarding.FeatureTourScreen
import com.usagecontrol.feature.onboarding.PermissionWalkthroughScreen
import com.usagecontrol.feature.onboarding.QuickSetupScreen
import com.usagecontrol.feature.onboarding.WelcomeScreen
import com.usagecontrol.feature.rules.RuleEditorScreen
import com.usagecontrol.feature.rules.RuleListScreen
import com.usagecontrol.feature.screentime.ScreenTimeScreen
import com.usagecontrol.feature.settings.SettingsScreen
import com.usagecontrol.navigation.AppRoutes
import com.usagecontrol.navigation.AppScreen
import com.usagecontrol.navigation.ModesPlaceholderScreen
import com.usagecontrol.service.context.ContextMonitorService
import com.usagecontrol.service.usage.UsageMonitorService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startMonitoringServices()
        setContent {
            DoormanTheme {
                UsageControlAppContent()
            }
        }
    }

    private fun startMonitoringServices() {
        try {
            startForegroundService(Intent(this, UsageMonitorService::class.java))
            startForegroundService(Intent(this, ContextMonitorService::class.java))
        } catch (_: Exception) {
            // Services may fail to start if permissions not yet granted
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageControlAppContent(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    // While DataStore read is in flight, render empty box to prevent NavHost flicker
    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom bar + top bar only on the three tab routes; settings/rules/tour/etc. are pushed screens
    val currentTab = AppScreen.entries.firstOrNull { it.route == currentRoute }
    val showBottomBar = currentTab != null

    Scaffold(
        topBar = {
            if (currentTab != null) {
                TopAppBar(
                    title = { Text(stringResource(currentTab.labelRes)) },
                    actions = {
                        IconButton(
                            modifier = Modifier.testTag("settings_gear"),
                            onClick = {
                                navController.navigate(AppRoutes.SETTINGS) { launchSingleTop = true }
                            },
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppScreen.entries.forEach { screen ->
                        val label = stringResource(screen.labelRes)
                        NavigationBarItem(
                            modifier = Modifier.testTag("nav_tab_${screen.route}"),
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination!!,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Onboarding nested graph -- removed from back stack on completion/skip
            navigation(startDestination = "onboarding/welcome", route = "onboarding") {
                composable("onboarding/welcome") {
                    WelcomeScreen(
                        onGetStarted = { navController.navigate("onboarding/tour") },
                        onSkip = {
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                    )
                }
                composable("onboarding/tour") {
                    FeatureTourScreen(
                        onNext = { navController.navigate("onboarding/permissions") },
                        onSkip = {
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                    )
                }
                composable("onboarding/permissions") {
                    PermissionWalkthroughScreen(
                        onComplete = {
                            navController.navigate("onboarding/quicksetup") {
                                popUpTo("onboarding/permissions") { inclusive = true }
                            }
                        }
                    )
                }
                composable("onboarding/quicksetup") {
                    QuickSetupScreen(
                        onComplete = {
                            navController.navigate("main") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Main app nested graph -- startDestination is the Now tab
            navigation(startDestination = AppScreen.NOW.route, route = "main") {
                composable(AppScreen.NOW.route) { NowScreen() }
                composable(AppScreen.APPS.route) {
                    AppsScreen(
                        onOpenApp = { pkg -> navController.navigate(AppRoutes.appProfile(pkg)) },
                    )
                }
                composable(AppScreen.MODES.route) { ModesPlaceholderScreen() }

                // Off-bar routes: still reachable by navigation, never shown in the bottom bar
                composable(AppRoutes.SCREEN_TIME) {
                    ScreenTimeScreen(
                        onNavigateToRules = { navController.navigate("rules/SCREEN_TIME") },
                    )
                }
                composable(AppRoutes.CONTROLS) {
                    LockControlScreen(
                        onNavigateToRules = { navController.navigate("rules/LOCK_SOUND") },
                    )
                }
                composable(AppRoutes.INSIGHTS) { LearningScreen() }
                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        onRevisitTour = { navController.navigate(AppRoutes.TOUR) },
                    )
                }

                composable(
                    route = AppRoutes.APP_PROFILE,
                    arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
                ) {
                    AppProfileScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(AppRoutes.TOUR) {
                    FeatureTourScreen(
                        onNext = { navController.popBackStack() },
                        onSkip = { navController.popBackStack() },
                    )
                }

                // Rule list (per-pillar or all)
                composable(
                    route = "rules/{pillar}",
                    arguments = listOf(navArgument("pillar") { type = NavType.StringType }),
                ) {
                    RuleListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onCreateRule = { pillar ->
                            val route = if (pillar != null) "rule-editor/0/$pillar" else "rule-editor/0/NOTIFICATION"
                            navController.navigate(route)
                        },
                        onEditRule = { ruleId ->
                            navController.navigate("rule-editor/$ruleId/NOTIFICATION")
                        },
                    )
                }

                // Rule editor (create or edit)
                composable(
                    route = "rule-editor/{ruleId}/{pillar}",
                    arguments = listOf(
                        navArgument("ruleId") { type = NavType.LongType; defaultValue = 0L },
                        navArgument("pillar") { type = NavType.StringType; defaultValue = "NOTIFICATION" },
                    ),
                ) {
                    RuleEditorScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
