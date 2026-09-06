package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*

@Composable
fun NowcastScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val horizons by viewModel.nowcastHorizons.collectAsState()
    val scenarioState by viewModel.scenarioState.collectAsState()

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
                text = "HYDRODYNAMIC PROJECTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Short-Term Urban Nowcasting",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lagged response modeling: overland runoff takes 15–45 minutes to travel through upstream catchments before surcharging downstream low points.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Scientific Disclaimer Note
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, SkyRadar.copy(alpha = 0.3f))))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SkyRadar, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Prototype probabilistic flood-risk estimates. Predictions incorporate terrain depressions, drainage capacity, and radar rainfall rates.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Horizon Cards (+15m, +30m, +60m, +120m, 0-6h)
        items(horizons.size) { index ->
            val horizon = horizons[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nowcast_horizon_${horizon.horizonLabel}"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NavyBorder, NavyCard))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = horizon.horizonLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        RiskBadge(level = horizon.riskLevel, riskPercentage = horizon.riskPercentage)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CONFIDENCE", fontSize = 10.sp, color = TextSecondary)
                            Text("${horizon.confidence}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Column {
                            Text("EXPECTED RAIN", fontSize = 10.sp, color = TextSecondary)
                            Text("${horizon.expectedRainfallMmHr.toInt()} mm/hr", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SkyRadar)
                        }
                        Column {
                            Text("DRAINAGE STRESS", fontSize = 10.sp, color = TextSecondary)
                            Text("${horizon.predictedDrainageStressPercent}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (horizon.predictedDrainageStressPercent >= 70) HighOrange else SafeGreen)
                        }
                        Column {
                            Text("FLOOD RISK", fontSize = 10.sp, color = TextSecondary)
                            Text("${horizon.riskPercentage}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BlueDeep.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "READINESS LEVEL: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                            Text(
                                text = horizon.recommendedReadinessLevel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("PRIMARY PHYSICAL CONTRIBUTOR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    horizon.mainReasons.forEach { reason ->
                        Text("• $reason", fontSize = 11.sp, color = TextPrimary, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}
