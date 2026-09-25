package com.pharmsync.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmsync.ai.model.SyncPlan
import java.time.format.DateTimeFormatter

@Composable
fun PharmacyHandoffCard(
    syncPlan: SyncPlan,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.5.dp, Color(0xFF34D399).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Trust Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PHARMACY COUNTER HANDOFF",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF34D399),
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "SCC 47 ATTACHED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Patient & Synchronization metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "PATIENT NAME", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    Text(text = syncPlan.patientName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column {
                    Text(text = "DOB", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    Text(text = syncPlan.patientDob, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ANCHOR REFILL", fontSize = 9.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = syncPlan.anchorDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF1E293B))

            // Override directives & Breakdown
            Text(
                text = "CLAIM ADJUDICATION INSTRUCTIONS (NCPDP REJECT 79 OVERRIDE)",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(8.dp))

            syncPlan.overrides.forEach { override ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${override.drugName} (Rx #${override.rxNumber})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (override.ncpdpField420DK == "47")
                                "Field 420-DK=47 · Bridge Qty: ${override.bridgeQuantity} units"
                            else
                                "Anchor Driver · Full 30-Day Refill (${override.bridgeQuantity} units)",
                            fontSize = 10.sp,
                            color = if (override.ncpdpField420DK == "47") Color(0xFF38BDF8) else Color(0xFF34D399)
                        )
                    }

                    Text(
                        text = "$${String.format("%.2f", override.proratedCopay)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34D399)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF1E293B))

            // Total Prorated Copay & Copy Claim String button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TOTAL PATIENT COPAY", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "$${String.format("%.2f", syncPlan.totalCopay)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        val fullNcpdp = syncPlan.overrides.joinToString(" | ") { it.billingClaimString }
                        onCopyCode(fullNcpdp)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Copy NCPDP Code", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }
        }
    }
}
