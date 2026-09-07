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

        // Two Tabs: DEMO SCENARIOS and FIELD GROUND-TRUTH
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
                            "DEMO SCENARIOS",
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
                            "FIELD GROUND-TRUTH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) CyanAccent else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("tab_real_validation")
                )
            }
        }

        // Scientific Honesty Notice Box & Label
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (selectedTab == 0) WatchAmberBg else SevereRedBg),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (selectedTab == 0) WatchAmberDark else SevereRedDark,
                            if (selectedTab == 0) WatchAmber else SevereRed
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (selectedTab == 0) WatchAmber else SevereRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTab == 0) "DEMO SCENARIO — NOT VALIDATION" else "VALIDATION STATUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) WatchAmber else SevereRed,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (selectedTab == 0) {
                            "Synthetic simulation mode. Validation pending verified ground-truth data."
                        } else {
                            "Validation pending verified ground-truth data. Operational validation requires high-density ultrasonic water level IoT telemetry and field log calibration."
                        },
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Validation Framework Overview Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.4f))))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "HYDROLOGICAL VALIDATION FRAMEWORK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ground Truth Source:", fontSize = 11.sp, color = TextSecondary)
                        Text("KSNDMC AWS & BBMP Telemetry", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spatial Validation:", fontSize = 11.sp, color = TextSecondary)
                        Text(activeMetrics.spatialValidationStatus, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Temporal Validation:", fontSize = 11.sp, color = TextSecondary)
                        Text(activeMetrics.temporalValidationStatus, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        // Metric Stat Cards Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Accuracy",
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.accuracyPercent}%" else "Pending",
                    subtitle = "Overall classification",
                    icon = Icons.Default.Analytics,
                    iconColor = SafeGreen,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Recall (Sensitivity)",
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.recallPercent}%" else "Pending",
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
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.precisionPercent}%" else "Pending",
                    subtitle = "Alert trustworthiness",
                    icon = Icons.Default.Analytics,
                    iconColor = SkyRadar,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "F1 Score",
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.f1ScorePercent}%" else "Pending",
                    subtitle = "Harmonic mean metric",
                    icon = Icons.Default.Analytics,
                    iconColor = SkyRadar,
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
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.falseAlarmRatePercent}%" else "Pending",
                    subtitle = "Over-prediction rate",
                    icon = Icons.Default.Analytics,
                    iconColor = WatchAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Missed Event Rate",
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.missedEventRatePercent}%" else "Pending",
                    subtitle = "Under-prediction rate",
                    icon = Icons.Default.Analytics,
                    iconColor = SevereRed,
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
                    title = "Lead Time",
                    value = if (activeMetrics.hasSufficientData) "${activeMetrics.averageLeadTimeMinutes} min" else "Pending",
                    subtitle = "Average advance warning",
                    icon = Icons.Default.Analytics,
                    iconColor = HighOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Verified Events",
                    value = "${activeMetrics.verifiedEventsCount} records",
                    subtitle = "Field ground truth sample",
                    icon = Icons.Default.Analytics,
                    iconColor = TextSecondary,
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
                    Text("VERIFICATION OBSERVATIONS MESH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (selectedTab == 0) "DEMO SCENARIO — NOT VALIDATION" else "Field Telemetry Ground-Truth Network",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Continuous inundation depth and discharge monitoring at 4 critical nodes: Outer Ring Road EcoSpace (SWD-3), Agara Junction Underpass (SWD-1), Ibblur Sump (SWD-2), and Rainbow Drive (SWD-5).",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
