package com.usagecontrol.feature.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class TourSlide(val title: String, val description: String, val icon: ImageVector)

private val tourSlides = listOf(
    TourSlide(
        "Notification Tracking",
        "See which apps notify you most and understand your notification patterns over time.",
        Icons.Default.Notifications,
    ),
    TourSlide(
        "Screen Time",
        "Track how long you spend in each app and set time-based goals to build healthier habits.",
        Icons.Default.PhoneAndroid,
    ),
    TourSlide(
        "Smart Controls",
        "Create sound presets and Do Not Disturb rules that activate automatically based on your schedule.",
        Icons.Default.Tune,
    ),
    TourSlide(
        "Insights",
        "Get personalized suggestions based on your usage patterns to help you stay in control.",
        Icons.Default.Insights,
    ),
    TourSlide(
        "Rules Engine",
        "Build custom rules with conditions and actions -- your phone, your rules.",
        Icons.Default.Rule,
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeatureTourScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { tourSlides.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("tour_pager"),
            ) { page ->
                val slide = tourSlides[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = slide.title,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Skip button -- top-end, visible on all slides
            TextButton(
                onClick = onSkip,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .testTag("tour_button_skip"),
            ) {
                Text("Skip")
            }

            // Dot indicators
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tourSlides.indices.forEach { index ->
                    val isActive = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                },
                            ),
                    )
                }
            }

            // Next / Continue button
            val isLastPage = pagerState.currentPage == tourSlides.size - 1
            Button(
                onClick = {
                    if (isLastPage) {
                        onNext()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .testTag("tour_button_next"),
            ) {
                Text(if (isLastPage) "Continue" else "Next")
            }
        }
    }
}
