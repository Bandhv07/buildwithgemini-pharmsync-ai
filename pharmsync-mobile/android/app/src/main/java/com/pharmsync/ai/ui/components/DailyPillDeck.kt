package com.pharmsync.ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmsync.ai.model.DosingScheduleItem
import com.pharmsync.ai.model.PillGeometry
import com.pharmsync.ai.model.TimeOfDayWindow

@Composable
fun DailyPillDeck(
    pillItems: List<DosingScheduleItem>,
    onToggleTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val grouped = pillItems.groupBy { it.window }

        TimeOfDayWindow.values().forEach { window ->
            val itemsInWindow = grouped[window] ?: emptyList()
            if (itemsInWindow.isNotEmpty()) {
                PillWindowSection(
                    window = window,
                    items = itemsInWindow,
                    onToggleTaken = onToggleTaken
                )
            }
        }
    }
}

@Composable
fun PillWindowSection(
    window: TimeOfDayWindow,
    items: List<DosingScheduleItem>,
    onToggleTaken: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${window.title.uppercase()} (${window.defaultTime})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = when (window) {
                    TimeOfDayWindow.MORNING -> Color(0xFFFBBF24) // Warm amber
                    TimeOfDayWindow.DINNER -> Color(0xFF34D399)  // Emerald
                    TimeOfDayWindow.BEDTIME -> Color(0xFF818CF8) // Indigo
                }
            )
            Text(
                text = "${items.count { it.isTaken }}/${items.size} Taken",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }

        items.forEach { item ->
            PillCard(item = item, onToggleTaken = { onToggleTaken(item.id) })
        }
    }
}

@Composable
fun PillCard(
    item: DosingScheduleItem,
    onToggleTaken: () -> Unit
) {
    val cardBg by animateColorAsState(
        targetValue = if (item.isTaken) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFF1E293B),
        label = "cardBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleTaken() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, if (item.isTaken) Color(0xFF334155) else Color(0xFF38BDF8).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tactile Pill Shape Graphic
            PillGraphic(geometry = item.geometry, isTaken = item.isTaken)

            Spacer(modifier = Modifier.width(14.dp))

            // Drug details & Context Chips
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.drugName} ${item.strength}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.isTaken) Color(0xFF64748B) else Color.White
                )

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.contextChips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = chip, fontSize = 9.sp, color = Color(0xFF38BDF8))
                        }
                    }
                }

                Text(
                    text = item.clinicalRationale,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Interactive Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (item.isTaken) Color(0xFF34D399) else Color.Transparent)
                    .border(1.5.dp, if (item.isTaken) Color(0xFF34D399) else Color(0xFF64748B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (item.isTaken) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Taken",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PillGraphic(geometry: PillGeometry, isTaken: Boolean) {
    val pillColor = if (isTaken) Color.Gray else Color(geometry.colorHex)

    when (geometry) {
        PillGeometry.ROUND_PINK, PillGeometry.YELLOW_ROUND -> {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(pillColor)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
            )
        }
        PillGeometry.WHITE_OVAL_ER -> {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(pillColor)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            )
        }
        PillGeometry.WHITE_ELLIPTICAL -> {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(pillColor)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(9.dp))
            )
        }
    }
}
