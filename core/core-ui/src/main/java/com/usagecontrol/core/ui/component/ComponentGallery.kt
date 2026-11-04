package com.usagecontrol.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.ui.theme.DoormanTheme
import com.usagecontrol.core.ui.theme.Lane

/** Renders every shared Doorman component; used by the dark/light previews below. */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContextStrip(
            text = "Focus mode",
            detail = "until 17:00",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HeroCount(value = "37", label = "held today")

        SectionHeader(title = "Lanes", meta = "6")
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Lane.entries.forEachIndexed { i, lane ->
                LaneChip(lane = lane, selected = i == 0, onClick = {})
            }
        }
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Lane.entries.forEach { LaneDot(it) }
            Lane.entries.forEach { LaneDot(it, lit = false) }
        }

        SectionHeader(title = "Policy")
        var policy by remember { mutableStateOf("Batch") }
        PolicySegment(
            options = listOf("Instant", "Batch", "Silent", "Block"),
            selected = policy,
            onSelect = { policy = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionHeader(title = "Now", meta = "12:04", lane = Lane.People)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NotificationRow(
                lane = Lane.People,
                appName = "Signal",
                title = "Sam",
                text = "Running ten minutes late, save me a seat",
                time = "12:01",
            )
            NotificationRow(
                lane = Lane.Money,
                appName = "Bank",
                title = "Card payment 24.90",
                time = "11:42",
                unread = false,
            )
            BundleRow(
                lane = Lane.Deliveries,
                title = "Deliveries",
                count = 3,
                preview = "Parcel out for delivery",
                time = "11:30",
            )
            BundleRow(lane = Lane.Noise, title = "Noise", count = 128, time = "09:15")
        }
    }
}

@Composable
private fun GallerySurface(darkTheme: Boolean) {
    DoormanTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ComponentGallery()
        }
    }
}

@Preview(name = "Gallery - dark", widthDp = 380, heightDp = 900, showBackground = true)
@Composable
private fun ComponentGalleryDarkPreview() = GallerySurface(darkTheme = true)

@Preview(name = "Gallery - light", widthDp = 380, heightDp = 900, showBackground = true)
@Composable
private fun ComponentGalleryLightPreview() = GallerySurface(darkTheme = false)
