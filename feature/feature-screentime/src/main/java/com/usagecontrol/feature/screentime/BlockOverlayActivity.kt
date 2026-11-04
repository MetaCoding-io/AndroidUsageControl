package com.usagecontrol.feature.screentime

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.ui.theme.DoormanTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Full-screen overlay activity for soft/hard blocking.
 * Launched by the usage monitor when a screen time rule triggers.
 *
 * Intent extras:
 * - "appName": Name of the blocked app
 * - "packageName": Package of the blocked app
 * - "blockType": "soft" or "hard"
 * - "dailyLimitMinutes": The daily limit that was exceeded
 * - "usedMinutes": How many minutes were actually used
 */
@AndroidEntryPoint
class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appName = intent.getStringExtra("appName") ?: "App"
        val blockType = intent.getStringExtra("blockType") ?: "soft"
        val dailyLimitMinutes = intent.getIntExtra("dailyLimitMinutes", 0)
        val usedMinutes = intent.getIntExtra("usedMinutes", 0)

        setContent {
            DoormanTheme {
                if (blockType == "hard") {
                    HardBlockScreen(
                        appName = appName,
                        dailyLimitMinutes = dailyLimitMinutes,
                        usedMinutes = usedMinutes,
                        onClose = { finish() },
                    )
                } else {
                    SoftBlockScreen(
                        appName = appName,
                        dailyLimitMinutes = dailyLimitMinutes,
                        usedMinutes = usedMinutes,
                        onContinueAnyway = {
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        onGoBack = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SoftBlockScreen(
    appName: String,
    dailyLimitMinutes: Int,
    usedMinutes: Int,
    onContinueAnyway: () -> Unit,
    onGoBack: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Time Limit Reached",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "You've used $appName for ${usedMinutes}min today.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
            )
            if (dailyLimitMinutes > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Your daily limit is ${dailyLimitMinutes}min.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onGoBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Take a Break")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onContinueAnyway,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                ),
            ) {
                Text("Continue Anyway")
            }
        }
    }
}

@Composable
private fun HardBlockScreen(
    appName: String,
    dailyLimitMinutes: Int,
    usedMinutes: Int,
    onClose: () -> Unit,
) {
    var countdownSeconds by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            delay(1000)
            countdownSeconds--
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onError,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "App Blocked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onError,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "$appName is blocked for the rest of the day.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onError,
                textAlign = TextAlign.Center,
            )
            if (dailyLimitMinutes > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "You used ${usedMinutes}min of your ${dailyLimitMinutes}min limit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(32.dp))

            if (countdownSeconds > 0) {
                Text(
                    text = "Closing in ${countdownSeconds}s...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.5f),
                )
            } else {
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onError,
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Close")
                }
            }
        }
    }
}
