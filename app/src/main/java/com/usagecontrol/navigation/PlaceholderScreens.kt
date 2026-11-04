package com.usagecontrol.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.usagecontrol.R

@Composable
fun AppsPlaceholderScreen() = ComingSoon(testTag = "apps_placeholder")

@Composable
fun ModesPlaceholderScreen() = ComingSoon(testTag = "modes_placeholder")

@Composable
private fun ComingSoon(testTag: String) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.coming_in_s04),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
