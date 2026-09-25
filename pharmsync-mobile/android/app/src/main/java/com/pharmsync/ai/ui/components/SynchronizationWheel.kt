package com.pharmsync.ai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SynchronizationWheel(
    isSynchronized: Boolean,
    modifier: Modifier = Modifier
) {
    // Target sweep angles:
    // Fragmented state: Metformin (90 deg), Lisinopril (200 deg), Atorvastatin (310 deg)
    // Synchronized state: All snap smoothly to 360 deg on Sept 29
    val metforminSweep by animateFloatAsState(
        targetValue = if (isSynchronized) 360f else 90f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "metforminSweep"
    )

    val lisinoprilSweep by animateFloatAsState(
        targetValue = if (isSynchronized) 360f else 200f,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "lisinoprilSweep"
    )

    val atorvastatinSweep by animateFloatAsState(
        targetValue = if (isSynchronized) 360f else 290f,
        animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
        label = "atorvastatinSweep"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)

            // Outer Track & Arc (Metformin - Driver - Emerald Green)
            val outerRadius = (size.width / 2) - strokeWidth
            drawCircle(
                color = Color(0xFF1E293B),
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = Color(0xFF34D399),
                startAngle = -90f,
                sweepAngle = metforminSweep,
                useCenter = false,
                topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Middle Track & Arc (Lisinopril - Sky Blue)
            val midRadius = outerRadius - strokeWidth - 6.dp.toPx()
            drawCircle(
                color = Color(0xFF1E293B),
                radius = midRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = Color(0xFF38BDF8),
                startAngle = -90f,
                sweepAngle = lisinoprilSweep,
                useCenter = false,
                topLeft = Offset(center.x - midRadius, center.y - midRadius),
                size = Size(midRadius * 2, midRadius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Inner Track & Arc (Atorvastatin - Radiant Pink)
            val innerRadius = midRadius - strokeWidth - 6.dp.toPx()
            drawCircle(
                color = Color(0xFF1E293B),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
            drawArc(
                color = Color(0xFFF472B6),
                startAngle = -90f,
                sweepAngle = atorvastatinSweep,
                useCenter = false,
                topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                size = Size(innerRadius * 2, innerRadius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Illuminated Badge
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isSynchronized) "SYNCHRONIZED" else "FRAGMENTED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSynchronized) Color(0xFF34D399) else Color(0xFF94A3B8)
            )
            Text(
                text = "Sept 29",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = if (isSynchronized) "1 Consolidated Bag" else "3 Depletion Dates",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
