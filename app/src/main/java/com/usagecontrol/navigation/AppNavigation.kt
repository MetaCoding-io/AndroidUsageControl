package com.usagecontrol.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.usagecontrol.R

/** Bottom-bar tabs (D013: Now / Apps / Modes). Settings lives behind the top-bar gear. */
enum class AppScreen(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    NOW("now", R.string.tab_now, Icons.Default.Notifications),
    APPS("apps", R.string.tab_apps, Icons.Default.Apps),
    MODES("modes", R.string.tab_modes, Icons.Default.Tune),
}

/** Non-tab routes reachable from the shell but kept out of the bottom bar. */
object AppRoutes {
    const val SETTINGS = "settings"
    const val SCREEN_TIME = "screentime"
    const val CONTROLS = "controls"
    const val INSIGHTS = "insights"
    const val TOUR = "tour/standalone"
    const val APP_PROFILE = "apps/{packageName}"

    /** Package names are [A-Za-z0-9_.], but encode anyway so a bad value can't break the route. */
    fun appProfile(packageName: String): String = "apps/${Uri.encode(packageName)}"
}
