package com.usagecontrol.feature.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Usage Control",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Take control of your phone usage. Track notifications, manage screen time, and set smart rules -- all while keeping your data private and on-device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("welcome_button_get_started"),
            ) {
                Text("Get Started")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .testTag("welcome_button_skip"),
            ) {
                Text("Skip")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
