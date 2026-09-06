package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.FactorBarItem
import com.example.ui.theme.*

@Composable
fun DrainageCouplingScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val repo = viewModel.repository

    // Evaluate factor breakdown for the current scenario state at EcoSpace
    val ecospace = scenarioState.locations.firstOrNull() ?: com.example.data.model.LocationInfo(
        "loc_ecospace", "Outer Ring Road - EcoSpace", "Ward 150", 12.9278, 77.6820, 872.4, 0.8, 850, 88, 78, com.example.data.model.RiskLevel.HIGH, "30-60 min", 84, "Depression bottleneck", "Dewatering"
    )

    val riskResult = repo.riskEngine.evaluateLocationRisk(
        location = ecospace,
        currentRainfallMmHr = scenarioState.rainfallMmHr,
        recentRainfall1hMm = scenarioState.rainfallMmHr * 0.8,
        rainfallDurationMin = 45,
        antecedent24hMm = scenarioState.antecedentRainfallMm,
        drainageStressPercent = scenarioState.drainageStressPercent,
        isDrainBottleneck = scenarioState.drainageStressPercent > 70
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        item {
            Text(
                text = "PHYSICAL HYDROLOGIC COUPLING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Rainfall + Drainage Coupling Engine",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Flooding is not caused by rainfall alone. Our physical coupling engine models the transformation of precipitation into overland runoff, conduit hydraulic loading, and culvert backwater throttling.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // The 8-Stage Physical Chain Flow
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.4f)))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "THE PHYSICAL FLOOD PROCESS CHAIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val chainSteps = listOf(
                        "1. RAINFALL" to "${scenarioState.rainfallMmHr.toInt()} mm/hr intensity",
                        "2. RUNOFF" to scenarioState.runoffStatus,
                        "3. FLOW ACCUMULATION" to "Index ${scenarioState.flowAccumulationIndex} (Catchment convergence)",
                        "4. DRAINAGE LOADING" to "${scenarioState.drainageLoadM3Sec} m³/s stormwater inflow",
                        "5. DRAINAGE CAPACITY" to "${scenarioState.drainageCapacityM3Sec} m³/s max design capacity",
                        "6. BOTTLENECK" to if (scenarioState.drainageStressPercent > 70) "CRITICAL THROTTLING (${scenarioState.drainageStressPercent}% stress)" else "Clear camber gravity outflow",
                        "7. WATER ACCUMULATION" to if (scenarioState.drainageStressPercent > 70) "Surcharging into 872m low depression" else "Negligible ponding",
                        "8. FLOOD IMPACT" to "${scenarioState.overallFloodRiskPercent}% Risk (${scenarioState.overallRiskLevel.name})"
                    )

                    chainSteps.forEachIndexed { i, (step, desc) ->
                        val isBottleneckStep = i == 5 && scenarioState.drainageStressPercent > 70
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isBottleneckStep) SevereRed else (if (i <= 4) BlueDeep else SkyRadar)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${i + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBottleneckStep) SevereRed else TextPrimary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 11.sp,
                                    color = if (isBottleneckStep) SevereRed else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Contributing Factor Weight Breakdown (0-100 bars)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NavyBorder, NavyCard)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONTRIBUTING FACTOR SCORES (OUTER RING ROAD)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyRadar,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    riskResult.contributingFactors.forEach { factor ->
                        FactorBarItem(factor = factor)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        // Monitored Rajakaluve Channels
        item {
            Text(
                text = "MONITORED RAJAKALUVE STORM DRAINS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        val channels = listOf(
            Triple("SWD-1 Agara Overflow Trunk", "Capacity: 18.0 m³/s • Stress: ${if (scenarioState.drainageStressPercent > 50) 84 else 22}%", false),
            Triple("SWD-3 EcoSpace Culvert Underpass", "Capacity: 14.0 m³/s • Stress: ${scenarioState.drainageStressPercent}%", scenarioState.drainageStressPercent > 70),
            Triple("SWD-5 Rainbow Drive Feeder", "Capacity: 8.5 m³/s • Stress: ${if (scenarioState.drainageStressPercent > 60) 96 else 28}%", scenarioState.drainageStressPercent > 60)
        )

        items(channels.size) { index ->
            val (name, details, isBottleneck) = channels[index]
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(if (isBottleneck) SevereRed else NavyBorder, NavyCard)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isBottleneck) SevereRedBg else BlueDeep),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBottleneck) Icons.Default.Warning else Icons.Default.Water,
                            contentDescription = null,
                            tint = if (isBottleneck) SevereRed else DrainageCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = details, fontSize = 11.sp, color = TextSecondary)
                        if (isBottleneck) {
                            Text(
                                text = "BOTTLENECK DETECTED: Upstream runoff exceeds hydraulic culvert clearance",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SevereRed
                            )
                        }
                    }
                }
            }
        }
    }
}
