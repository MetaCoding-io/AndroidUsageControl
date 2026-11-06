package com.usagecontrol.feature.notifications.lanes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.model.Lane

/** A user lane as a selectable chip with its colour dot. Tagged `lane_chip_<id>`. */
@Composable
fun LaneChoiceChip(
    lane: Lane,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(lane.name, maxLines = 1) },
        leadingIcon = { LaneSwatch(Color(lane.colorArgb)) },
        modifier = modifier.testTag("lane_chip_${lane.id}"),
    )
}

@Composable
fun LaneSwatch(color: Color, modifier: Modifier = Modifier, size: Int = 10) {
    Box(modifier = modifier.size(size.dp).background(color, CircleShape))
}
