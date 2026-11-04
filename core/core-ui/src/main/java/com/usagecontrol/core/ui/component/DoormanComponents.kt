package com.usagecontrol.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.usagecontrol.core.ui.theme.ChipShape
import com.usagecontrol.core.ui.theme.DoormanMonoType
import com.usagecontrol.core.ui.theme.Lane
import com.usagecontrol.core.ui.theme.LocalLaneColors
import com.usagecontrol.core.ui.theme.RowShape

/*
 * Shared Doorman building blocks (D013). Every lane-coloured element reads
 * LocalLaneColors so dark/light (and any future user lane palette) apply uniformly;
 * counts and times use DoormanMonoType (tabular figures).
 */

/** The LED motif: a solid dot with a soft halo when [lit]; an outlined ring when off. */
@Composable
fun LedDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
    lit: Boolean = true,
) {
    Canvas(modifier = modifier.size(size * 2)) {
        val r = size.toPx() / 2
        if (lit) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.45f), Color.Transparent),
                    center = center,
                    radius = r * 2,
                ),
                radius = r * 2,
            )
            drawCircle(color = color, radius = r)
        } else {
            drawCircle(
                color = color.copy(alpha = 0.6f),
                radius = r - 0.75.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}

/** [LedDot] in the colour of [lane]. */
@Composable
fun LaneDot(lane: Lane, modifier: Modifier = Modifier, size: Dp = 8.dp, lit: Boolean = true) {
    LedDot(color = LocalLaneColors.current[lane], modifier = modifier, size = size, lit = lit)
}

/** Full-round lane pill: LED dot plus label, tinted with the lane colour. */
@Composable
fun LaneChip(
    lane: Lane,
    modifier: Modifier = Modifier,
    label: String = lane.name,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val laneColor = LocalLaneColors.current[lane]
    val container = if (selected) laneColor.copy(alpha = 0.22f) else laneColor.copy(alpha = 0.10f)
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedDot(color = laneColor, size = 8.dp, lit = selected || onClick == null)
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
    val contentColor = MaterialTheme.colorScheme.onSurface
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = ChipShape,
            color = container,
            contentColor = contentColor,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = ChipShape,
            color = container,
            contentColor = contentColor,
            content = content,
        )
    }
}

/** Uppercase-free section label with an optional trailing mono meta (e.g. a count). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    lane: Lane? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lane != null) LaneDot(lane, size = 6.dp)
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (meta != null) {
            Text(
                text = meta,
                style = DoormanMonoType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Single-choice segmented control for a per-app / per-lane delivery policy.
 * Generic over the option type so callers bring their own policy enum.
 */
@Composable
fun <T> PolicySegment(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                enabled = enabled,
                label = {
                    Text(
                        text = label(option),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

/** Shared 12dp row container used by notification and bundle rows. */
@Composable
private fun DoormanRow(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RowShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * One notification in the Now stream: lane dot, optional app icon slot,
 * app/title/text and a mono timestamp.
 */
@Composable
fun NotificationRow(
    lane: Lane,
    appName: String,
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    text: String? = null,
    unread: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    DoormanRow(modifier = modifier, onClick = onClick) {
        LaneDot(lane, lit = unread)
        if (icon != null) Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { icon() }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!text.isNullOrEmpty()) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = time,
            style = DoormanMonoType.meta,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Top),
        )
    }
}

/** A collapsed group of notifications (per app or per batch) with a lane-tinted count badge. */
@Composable
fun BundleRow(
    lane: Lane,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    preview: String? = null,
    time: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val laneColor = LocalLaneColors.current[lane]
    val countText = bundleCountLabel(count)
    DoormanRow(modifier = modifier, onClick = onClick) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(laneColor.copy(alpha = 0.16f), ChipShape)
                .semantics { contentDescription = countText },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = countText, style = DoormanMonoType.meta, color = laneColor)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!preview.isNullOrEmpty()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (time != null) {
            Text(
                text = time,
                style = DoormanMonoType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Badge text for [BundleRow]: negatives clamp to 0, anything above 99 reads "99+". */
internal fun bundleCountLabel(count: Int): String = when {
    count <= 0 -> "0"
    count > 99 -> "99+"
    else -> count.toString()
}

/** Large mono figure with a caption, e.g. "37 / held today". */
@Composable
fun HeroCount(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    lane: Lane? = null,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = value,
            style = DoormanMonoType.hero,
            color = lane?.let { LocalLaneColors.current[it] } ?: MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (lane != null) LaneDot(lane, size = 6.dp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Thin status strip: amber LED (accent) plus current mode/context and a mono detail. */
@Composable
fun ContextStrip(
    text: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    active: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, ChipShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedDot(color = MaterialTheme.colorScheme.primary, lit = active)
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = DoormanMonoType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
