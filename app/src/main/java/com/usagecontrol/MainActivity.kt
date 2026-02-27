package com.usagecontrol

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.usagecontrol.core.ui.theme.UsageControlTheme
import com.usagecontrol.feature.learning.LearningScreen
import com.usagecontrol.feature.lockcontrol.LockControlScreen
import com.usagecontrol.feature.notifications.NotificationsScreen
import com.usagecontrol.feature.screentime.ScreenTimeScreen
import com.usagecontrol.feature.settings.SettingsScreen
import com.usagecontrol.navigation.AppScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsageControlTheme {
                UsageControlApp()
            }
        }
    }
}

@Composable
private fun UsageControlApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
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
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Notifications.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(AppScreen.Notifications.route) { NotificationsScreen() }
            composable(AppScreen.ScreenTime.route) { ScreenTimeScreen() }
            composable(AppScreen.Controls.route) { LockControlScreen() }
            composable(AppScreen.Insights.route) { LearningScreen() }
            composable(AppScreen.Settings.route) { SettingsScreen() }
        }
    }
}
