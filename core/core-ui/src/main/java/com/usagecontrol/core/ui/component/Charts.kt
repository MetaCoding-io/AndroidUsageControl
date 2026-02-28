package com.usagecontrol.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Simple line chart drawn with Compose Canvas.
 * @param dataPoints List of values to plot
 * @param labels Bottom axis labels (same length as dataPoints)
 */
@Composable
fun SimpleLineChart(
    dataPoints: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
) {
    if (dataPoints.isEmpty()) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
        ) {
            val maxVal = dataPoints.max().coerceAtLeast(1f)
            val paddingLeft = 0f
            val paddingBottom = 0f
            val chartWidth = size.width - paddingLeft
            val chartHeight = size.height - paddingBottom

            if (dataPoints.size == 1) {
                val y = chartHeight - (dataPoints[0] / maxVal * chartHeight)
                drawCircle(lineColor, 4.dp.toPx(), Offset(size.width / 2, y))
                return@Canvas
            }

            val stepX = chartWidth / (dataPoints.size - 1)

            // Build path
            val linePath = Path()
            val fillPath = Path()

            dataPoints.forEachIndexed { index, value ->
                val x = paddingLeft + index * stepX
                val y = chartHeight - (value / maxVal * chartHeight)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, chartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close fill path
            fillPath.lineTo(paddingLeft + (dataPoints.size - 1) * stepX, chartHeight)
            fillPath.close()

            drawPath(fillPath, fillColor)
            drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            // Draw dots
            dataPoints.forEachIndexed { index, value ->
                val x = paddingLeft + index * stepX
                val y = chartHeight - (value / maxVal * chartHeight)
                drawCircle(lineColor, 3.dp.toPx(), Offset(x, y))
            }
        }

        // Labels
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Simple bar chart drawn with Compose Canvas.
 * @param values List of bar values
 * @param labels Bottom axis labels
 */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int = -1,
    highlightColor: Color = MaterialTheme.colorScheme.error,
) {
    if (values.isEmpty()) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val maxVal = values.max().coerceAtLeast(1f)
            val barWidth = size.width / values.size * 0.7f
            val gap = size.width / values.size * 0.3f / 2

            values.forEachIndexed { index, value ->
                val barHeight = (value / maxVal) * size.height
                val x = index * (barWidth + gap * 2) + gap
                val y = size.height - barHeight
                val color = if (index == highlightIndex) highlightColor else barColor

                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                )
            }
        }

        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Donut/ring chart for showing proportions.
 * @param segments List of (value, color) pairs
 * @param centerText Text displayed in the center
 */
@Composable
fun DonutChart(
    segments: List<Pair<Float, Color>>,
    centerText: String = "",
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(1f)

    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val radius = (minOf(size.width, size.height) - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            segments.forEach { (value, color) ->
                val sweepAngle = (value / total) * 360f
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweepAngle
            }
        }

        if (centerText.isNotEmpty()) {
            Text(
                text = centerText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
