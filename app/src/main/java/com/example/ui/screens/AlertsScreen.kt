package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RiskLevel
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*

@Composable
fun AlertsScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val allAlerts = scenarioState.activeAlerts

    val filteredAlerts = remember(allAlerts, selectedFilter) {
        when (selectedFilter) {
            "SEVERE" -> allAlerts.filter { it.severity == RiskLevel.SEVERE }
            "HIGH" -> allAlerts.filter { it.severity == RiskLevel.HIGH }
            "WATCH" -> allAlerts.filter { it.severity == RiskLevel.WATCH }
            else -> allAlerts
        }
    }

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
                text = "EARLY WARNING SYSTEM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Public & Agency Flood Alerts",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Coupled hydrodynamic alerts generated before road surface inundation occurs. Alerts specify physical root causes and recommended mitigation.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Severity Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "SEVERE", "HIGH", "WATCH").forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BlueDeep,
                            selectedLabelColor = CyanAccent
                        )
                    )
                }
            }
        }

        if (filteredAlerts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, SafeGreenDark)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SafeGreenBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO ACTIVE SEVERE FLOOD ALERTS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "All monitored pilot corridors (Bellandur, Agara, EcoSpace, Rainbow Drive) are currently within safe hydraulic thresholds.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.advanceScenario() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ADVANCE SCENARIO TO TRIGGER FLOOD", color = androidx.compose.ui.graphics.Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredAlerts.size) { index ->
                val alert = filteredAlerts[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_card_${alert.id}"),
                    colors = CardDefaults.cardColors(containerColor = NavySurface),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                when (alert.severity) {
                                    RiskLevel.SEVERE -> SevereRed
                                    RiskLevel.HIGH -> HighOrange
                                    RiskLevel.WATCH -> WatchAmber
                                    RiskLevel.SAFE -> SafeGreen
                                },
                                NavyBorder
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = when (alert.severity) {
                                        RiskLevel.SEVERE -> SevereRed
                                        RiskLevel.HIGH -> HighOrange
                                        RiskLevel.WATCH -> WatchAmber
                                        RiskLevel.SAFE -> SafeGreen
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (alert.isModelGenerated) "MODEL-GENERATED PREDICTION" else "VERIFIED OBSERVATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SkyRadar
                                )
                            }
                            RiskBadge(level = alert.severity, riskPercentage = alert.riskPercentage)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = alert.location,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Issued: ${alert.issuedAt}", fontSize = 11.sp, color = TextSecondary)
                            Text("Expected In: ${alert.expectedTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = NavyBorder)

                        Text("PHYSICAL REASON:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        Text(
                            text = alert.reason,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("RECOMMENDED ACTION:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighOrange)
                        Text(
                            text = alert.recommendedAction,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.SAFE_NAV) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAN DETOUR / SAFE NAVIGATION", color = androidx.compose.ui.graphics.Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
