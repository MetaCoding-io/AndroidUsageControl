package com.usagecontrol.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    Notifications("notifications", "Notifications", Icons.Default.Notifications),
    ScreenTime("screentime", "Screen Time", Icons.Default.PhoneAndroid),
    Controls("controls", "Controls", Icons.Default.Tune),
    Insights("insights", "Insights", Icons.Default.Insights),
    Settings("settings", "Settings", Icons.Default.Settings),
}
