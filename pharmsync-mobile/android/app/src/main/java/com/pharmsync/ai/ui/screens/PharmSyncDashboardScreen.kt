package com.pharmsync.ai.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmsync.ai.ui.components.AgentThoughtDrawer
import com.pharmsync.ai.ui.components.DailyPillDeck
import com.pharmsync.ai.ui.components.PharmacyHandoffCard
import com.pharmsync.ai.ui.components.SynchronizationWheel
import com.pharmsync.ai.ui.viewmodel.PharmSyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmSyncDashboardScreen(
    viewModel: PharmSyncViewModel = remember { PharmSyncViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PharmSync AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Smart Regimen & Refill Synchronization",
                            fontSize = 11.sp,
                            color = Color(0xFF34D399)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "OCR Scanner: Ingested 3 active Rx labels.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan Label",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            AgentThoughtDrawer(
                isExpanded = uiState.isStreamDrawerExpanded,
                logs = uiState.liveLogs,
                onToggle = { viewModel.toggleStreamDrawer() }
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            // 1. Synchronization Wheel Canvas Component
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SynchronizationWheel(isSynchronized = uiState.isSynchronized)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Stats Chip
                    Box(
                        modifier = Modifier
                            .background(
                                if (uiState.isSynchronized) Color(0xFF34D399).copy(alpha = 0.15f)
                                else Color(0xFFFBBF24).copy(alpha = 0.15f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (uiState.isSynchronized)
                                "Consolidated: 1 Trip / Month (24 trips saved/year)"
                            else
                                "Current Regimen: 3 Trips / Month (Fragmented)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isSynchronized) Color(0xFF34D399) else Color(0xFFFBBF24)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Synchronize Button
                    Button(
                        onClick = { viewModel.synchronizeRegimen() },
                        enabled = !uiState.isOptimizing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34D399))
                    ) {
                        if (uiState.isOptimizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF020617),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Running Clinical Optimization...", color = Color(0xFF020617), fontWeight = FontWeight.Bold)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF020617))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isSynchronized) "Regimen Synchronized (Sept 29)" else "Synchronize Regimen",
                                color = Color(0xFF020617),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Pharmacy Counter Handoff Card (Revealed on Sync)
            item {
                AnimatedVisibility(visible = uiState.isSynchronized && uiState.syncPlan != null) {
                    uiState.syncPlan?.let { plan ->
                        PharmacyHandoffCard(
                            syncPlan = plan,
                            onCopyCode = { code ->
                                viewModel.copyNCPDP(code)
                                Toast.makeText(context, "NCPDP SCC 47 Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // 3. Daily Smart Pill-Deck
            item {
                uiState.syncPlan?.let { plan ->
                    DailyPillDeck(
                        pillItems = plan.dailyPillDeck,
                        onToggleTaken = { itemId -> viewModel.toggleMedicationTaken(itemId) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}
