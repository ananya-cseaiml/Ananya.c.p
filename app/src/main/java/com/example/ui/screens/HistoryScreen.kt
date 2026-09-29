package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.ScenarioStep
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.theme.*

@Composable
fun HistoryScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val historicalEvents by viewModel.historicalEvents.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HISTORICAL BENCHMARK SCENARIOS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SkyRadar,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WatchAmberBg
                ) {
                    Text(
                        text = "● DEMO SCENARIOS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WatchAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "Catchment Inundation Benchmarks",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Prototype historical benchmark scenarios for the Bellandur–Agara catchment. External KSNDMC/BBMP datasets are planned for future validation.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        items(historicalEvents.size) { index ->
            val event = historicalEvents[index]
            val isHit = event.predictionStatus == "DEMO CORRECT-HIT SCENARIO" || event.predictionStatus == "CORRECT HIT"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("historical_event_${event.id}"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(if (isHit) SafeGreen else WatchAmber, NavyBorder)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = event.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = NavyCard
                            ) {
                                Text(
                                    text = "HISTORICAL BENCHMARK — NOT VERIFIED",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyRadar,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isHit) SafeGreenBg else WatchAmberBg
                            ) {
                                Text(
                                    text = event.predictionStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHit) SafeGreen else WatchAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = event.location, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rainfall: ${event.rainfallRecordedMm} mm", fontSize = 11.sp, color = TextSecondary)
                        Text("Model Predicted: ${event.predictedRiskPercentage}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyRadar)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Observed: ${event.observedCondition}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Analysis: ${event.notes}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.setAppMode("HISTORICAL")
                            if (event.rainfallRecordedMm > 70.0) {
                                viewModel.setScenarioStep(ScenarioStep.FLOOD_RISK_INCREASES)
                            } else {
                                viewModel.setScenarioStep(ScenarioStep.DRAINAGE_STRESS_INCREASES)
                            }
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueDeep),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REPLAY EVENT IN HISTORICAL MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent
                        )
                    }
                }
            }
        }
    }
}
