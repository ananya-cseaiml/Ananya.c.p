package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
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

@Composable
fun ValidationScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val demoMetrics by viewModel.demoValidationMetrics.collectAsState()
    val realMetrics by viewModel.realValidationMetrics.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = DEMO, 1 = REAL

    val activeMetrics = if (selectedTab == 0) demoMetrics else realMetrics

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
                text = "MODEL BENCHMARKING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Hydrological Validation Metrics",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Objective validation scores evaluated against verified inundation records in the Bellandur–Agara catchment basin.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Two Tabs: DEMO VALIDATION and REAL VALIDATION
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavySurface,
                contentColor = CyanAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "DEMO VALIDATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) CyanAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_demo_validation")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "REAL VALIDATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) CyanAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_real_validation")
                )
            }
        }

        // Scientific Honesty Notice Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (activeMetrics.hasSufficientData) WatchAmberBg else SevereRedBg),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (activeMetrics.hasSufficientData) WatchAmberDark else SevereRedDark,
                            if (activeMetrics.hasSufficientData) WatchAmber else SevereRed
                        )
                    )
                )
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (activeMetrics.hasSufficientData) Icons.Default.Info else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (activeMetrics.hasSufficientData) WatchAmber else SevereRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = activeMetrics.notice,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        if (!activeMetrics.hasSufficientData) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, SevereRedDark)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Insufficient real-world validation data",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SevereRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scientific Honesty Directive: Operational real-world validation requires high-density ultrasonic water level IoT telemetry and continuous physical storm records. Field deployment in Bellandur–Agara pilot is pending live sensor hardware telemetry.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Metric Stat Cards Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Accuracy",
                        value = "${activeMetrics.accuracyPercent}%",
                        subtitle = "Overall classification",
                        icon = Icons.Default.Analytics,
                        iconColor = SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Recall (Sensitivity)",
                        value = "${activeMetrics.recallPercent}%",
                        subtitle = "True positive flood hits",
                        icon = Icons.Default.Analytics,
                        iconColor = CyanAccent,
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
                        value = "${activeMetrics.precisionPercent}%",
                        subtitle = "Alert trustworthiness",
                        icon = Icons.Default.Analytics,
                        iconColor = SkyRadar,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Lead Time",
                        value = "${activeMetrics.averageLeadTimeMinutes} min",
                        subtitle = "Average advance warning",
                        icon = Icons.Default.Analytics,
                        iconColor = HighOrange,
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
                        value = "${activeMetrics.falseAlarmRatePercent}%",
                        subtitle = "Over-prediction rate",
                        icon = Icons.Default.Analytics,
                        iconColor = WatchAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Missed Event Rate",
                        value = "${activeMetrics.missedEventRatePercent}%",
                        subtitle = "Under-prediction rate",
                        icon = Icons.Default.Analytics,
                        iconColor = SevereRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.3f))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("EVALUATION SAMPLE SIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${activeMetrics.verifiedEventsCount} Verified Inundation Ground-Truth Records", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Monitored across 4 major low points: EcoSpace, Agara Underpass, Ibblur Junction, and Rainbow Drive.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
