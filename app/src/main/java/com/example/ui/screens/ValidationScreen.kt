package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.theme.*

/**
 * Validation Screen
 * Displays transparent, honest validation status:
 * 1. Primary Status: PENDING VERIFIED GROUND TRUTH
 *    "Verified model validation requires genuine observed flood-event and rainfall datasets. The current project contains demonstration scenarios only."
 * 2. Real-World metrics explicitly show "Pending" until genuine ground truth telemetry is connected.
 * 3. Separate DEMO section: "DEMO ONLY — NOT REAL MODEL VALIDATION" showing synthetic benchmark demonstration.
 */
@Composable
fun ValidationScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val demoMetrics by viewModel.demoValidationMetrics.collectAsState()
    val realMetrics by viewModel.realValidationMetrics.collectAsState()
    val historicalEvents by viewModel.historicalEvents.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Page Title & Context
        item {
            Text(
                text = "MODEL BENCHMARKING & VALIDATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Hydrological Validation Status",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Model verification framework for the Bellandur–Agara catchment basin.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Section 1: MANDATORY REAL-WORLD VALIDATION STATUS (Pending Verified Ground Truth)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = WatchAmberBg),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(WatchAmberDark, WatchAmber))
                ),
                modifier = Modifier.testTag("validation_status_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = WatchAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "VALIDATION STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WatchAmber,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "PENDING VERIFIED GROUND TRUTH",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WatchAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Verified model validation requires genuine observed flood-event and rainfall datasets. The current project contains demonstration scenarios only.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = NavyBorder.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ground-Truth Dataset:", fontSize = 11.sp, color = TextSecondary)
                        Text("Planned external integration", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Agencies:", fontSize = 11.sp, color = TextSecondary)
                        Text("KSNDMC AWS & BBMP Gauging Records", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CyanAccent)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sensor Network:", fontSize = 11.sp, color = TextSecondary)
                        Text("Ultrasonic IoT water-level deployment pending", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        // Section 2: Real-World Operational Metrics (Honest "Pending" state)
        item {
            Text(
                text = "REAL-WORLD MODEL PERFORMANCE METRICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Accuracy",
                    value = "Pending",
                    subtitle = "Awaiting observed ground truth",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Recall (Sensitivity)",
                    value = "Pending",
                    subtitle = "Awaiting verified inundation logs",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Precision",
                    value = "Pending",
                    subtitle = "Awaiting field alert verification",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "F1 Score",
                    value = "Pending",
                    subtitle = "Harmonic mean evaluation pending",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "False Alarm Rate",
                    value = "Pending",
                    subtitle = "Awaiting continuous telemetry",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Lead Time",
                    value = "Pending",
                    subtitle = "Awaiting sensor time-series",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section 3: SEPARATE DEMO SECTION
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.5f)))
                ),
                modifier = Modifier.fillMaxWidth().testTag("demo_validation_workflow_section")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                            Text(
                                text = "DEMO VALIDATION WORKFLOW — NOT REAL MODEL VALIDATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WatchAmberBg
                        ) {
                            Text(
                                text = "● DEMO ONLY — NOT REAL MODEL VALIDATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = WatchAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This demonstration shows how the mathematical validation pipeline computes contingency matrices once official ground truth is imported. All figures below are evaluated strictly against 5 synthetic benchmark scenarios, NOT real-world observations.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Demo workflow metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyDark,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Demo Accuracy", fontSize = 9.sp, color = TextMuted)
                                Text("80%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SafeGreen)
                                Text("Demo workflow", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyDark,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Demo Precision", fontSize = 9.sp, color = TextMuted)
                                Text("75%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SkyRadar)
                                Text("Demo workflow", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyDark,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Demo Recall", fontSize = 9.sp, color = TextMuted)
                                Text("85%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyanAccent)
                                Text("Demo workflow", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyDark,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Demo Lead Time", fontSize = 9.sp, color = TextMuted)
                                Text("42 min", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = HighOrange)
                                Text("Demo workflow", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyDark,
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Synthetic Benchmark Scenarios", fontSize = 9.sp, color = TextMuted)
                                Text("5 Demo Events", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = CyanAccent)
                                Text("Demo scenarios (not verified records)", fontSize = 8.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Synthetic Benchmark Scenarios List (Renamed from "Verified Events")
        item {
            Text(
                text = "SYNTHETIC BENCHMARK SCENARIOS (DEMO EVENTS)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        items(historicalEvents.size) { index ->
            val event = historicalEvents[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("benchmark_event_${event.id}"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(NavyBorder, NavyCard))
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = event.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = WatchAmberBg
                        ) {
                            Text(
                                text = "HISTORICAL BENCHMARK — NOT VERIFIED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = WatchAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = event.location, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Recorded Rainfall: ${event.rainfallRecordedMm} mm • Model Match: ${event.predictionStatus}", fontSize = 11.sp, color = SkyRadar)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = event.observedCondition, fontSize = 11.sp, color = TextSecondary)
                }
            }
        }
    }
}
