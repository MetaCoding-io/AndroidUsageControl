package com.usagecontrol

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.usagecontrol.core.ui.theme.UsageControlTheme
import com.usagecontrol.feature.learning.LearningScreen
import com.usagecontrol.feature.lockcontrol.LockControlScreen
import com.usagecontrol.feature.notifications.NotificationsScreen
import com.usagecontrol.feature.rules.RuleEditorScreen
import com.usagecontrol.feature.rules.RuleListScreen
import com.usagecontrol.feature.screentime.ScreenTimeScreen
import com.usagecontrol.feature.settings.SettingsScreen
import com.usagecontrol.navigation.AppScreen
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
            UsageControlTheme {
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

@Composable
private fun UsageControlAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on detail screens
    val showBottomBar = currentRoute in AppScreen.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    AppScreen.entries.forEach { screen ->
                        NavigationBarItem(
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
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Notifications.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Main tabs
            composable(AppScreen.Notifications.route) {
                NotificationsScreen(
                    onNavigateToRules = { navController.navigate("rules/NOTIFICATION") },
                )
            }
            composable(AppScreen.ScreenTime.route) {
                ScreenTimeScreen(
                    onNavigateToRules = { navController.navigate("rules/SCREEN_TIME") },
                )
            }
            composable(AppScreen.Controls.route) {
                LockControlScreen(
                    onNavigateToRules = { navController.navigate("rules/LOCK_SOUND") },
                )
            }
            composable(AppScreen.Insights.route) { LearningScreen() }
            composable(AppScreen.Settings.route) { SettingsScreen() }

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
