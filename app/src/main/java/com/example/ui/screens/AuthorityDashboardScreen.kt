package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GovernmentActionItem
import com.example.data.model.RiskLevel
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*

@Composable
fun AuthorityDashboardScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val context = LocalContext.current

    // Dynamic metrics computed from repository state
    val highRiskLocations = scenarioState.locations.count { it.riskLevel == RiskLevel.HIGH }
    val severeRiskLocations = scenarioState.locations.count { it.riskLevel == RiskLevel.SEVERE }
    val highRiskRoads = scenarioState.roads.count { it.severity == RiskLevel.HIGH || it.severity == RiskLevel.SEVERE }
    val affectedRoadsCount = scenarioState.roads.count { it.riskPercentage >= 40 }
    val averageConfidence = if (scenarioState.locations.isNotEmpty()) {
        scenarioState.locations.map { it.confidence }.average().toInt()
    } else 85

    // Dynamic rule-based government action recommendations (Point 20)
    val dynamicActions = remember(scenarioState) {
        val list = mutableListOf<GovernmentActionItem>()

        if (scenarioState.rainfallMmHr >= 40.0 && scenarioState.drainageStressPercent >= 70) {
            list.add(
                GovernmentActionItem(
                    location = "ORR EcoSpace Culvert (SWD-3)",
                    severity = RiskLevel.SEVERE,
                    suggestedCheck = "Inspect nearby drainage inlet, clear debris at culverts, and position 50HP mobile dewatering pumps",
                    agency = "BBMP Stormwater Drain (SWD) Dept",
                    priority = "CRITICAL"
                )
            )
        }

        if (highRiskRoads > 0) {
            list.add(
                GovernmentActionItem(
                    location = "Outer Ring Road (Ibblur to EcoSpace)",
                    severity = RiskLevel.HIGH,
                    suggestedCheck = "Notify traffic-management team and prepare diversion signage via HSR 14th Main Ridge Bypass",
                    agency = "Bengaluru Traffic Police (BTP)",
                    priority = "HIGH"
                )
            )
        }

        if (severeRiskLocations > 0 || scenarioState.overallRiskLevel == RiskLevel.SEVERE) {
            list.add(
                GovernmentActionItem(
                    location = "Agara Junction Underpass & Rainbow Drive",
                    severity = RiskLevel.SEVERE,
                    suggestedCheck = "Deploy mobile dewatering pumps and issue public travel advisory / route diversion",
                    agency = "KSDMA & BBMP Road Infrastructure",
                    priority = "IMMEDIATE"
                )
            )
        }

        if (scenarioState.drainageStressPercent >= 60) {
            list.add(
                GovernmentActionItem(
                    location = "Agara Lake & Bellandur Lake Outflow Sluices",
                    severity = RiskLevel.WATCH,
                    suggestedCheck = "Monitor retention pond / lake outflow weir levels and open emergency sluice gates",
                    agency = "BBMP Lakes Division & BWSSB",
                    priority = "HIGH"
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                GovernmentActionItem(
                    location = "Bellandur–Agara Pilot Monitored Catchment",
                    severity = RiskLevel.SAFE,
                    suggestedCheck = "Routine monitoring of primary stormwater trunk drains and silt trap inspection",
                    agency = "BBMP Ward 150 & 174",
                    priority = "ROUTINE"
                )
            )
        }
        list
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
                text = "GOVERNMENT AGENCY OPERATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Command & Decision Support",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Multi-agency coordination panel for BBMP, Bengaluru Traffic Police (BTP), and Karnataka State Disaster Management Authority (KSDMA).",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Operational Overview Grid (Requirement 19)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Rainfall Rate",
                    value = "${scenarioState.rainfallMmHr.toInt()} mm/hr",
                    subtitle = "Surface intensity",
                    icon = Icons.Default.WaterDrop,
                    iconColor = SkyRadar,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Drainage Stress",
                    value = "${scenarioState.drainageStressPercent}%",
                    subtitle = "SWD hydraulic capacity",
                    icon = Icons.Default.Compress,
                    iconColor = if (scenarioState.drainageStressPercent >= 70) SevereRed else CyanAccent,
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
                    title = "Severe Locations",
                    value = "$severeRiskLocations",
                    subtitle = "Critical inundation",
                    icon = Icons.Default.Warning,
                    iconColor = SevereRed,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "High-Risk Roads",
                    value = "$highRiskRoads",
                    subtitle = "$affectedRoadsCount total affected",
                    icon = Icons.Default.AltRoute,
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
                    title = "Water Level",
                    value = if (scenarioState.drainageStressPercent > 70) "2.6m / 3.0m" else "1.4m / 3.0m",
                    subtitle = "Bellandur Sluice Weir",
                    icon = Icons.Default.ShowChart,
                    iconColor = DrainageCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Model Confidence",
                    value = "$averageConfidence%",
                    subtitle = "${scenarioState.activeAlerts.size} active alerts",
                    icon = Icons.Default.Verified,
                    iconColor = SafeGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Decision-Support Mandatory Notice (Requirement 20)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BlueDeep.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.5f))))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DECISION SUPPORT MANDATE: Decision-support recommendation. Final operational decisions remain with authorities.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Prioritized Action Checklist
        item {
            Text(
                text = "ACTIONABLE FIELD INTERVENTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        items(dynamicActions.size) { index ->
            val act = dynamicActions[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("action_item_${index}"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (act.priority == "CRITICAL" || act.priority == "IMMEDIATE") SevereRed else (if (act.priority == "HIGH") HighOrange else NavyBorder),
                            NavyCard
                        )
                    )
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (act.priority) {
                                "CRITICAL", "IMMEDIATE" -> SevereRedBg
                                "HIGH" -> HighOrangeBg
                                else -> SafeGreenBg
                            }
                        ) {
                            Text(
                                text = "PRIORITY: ${act.priority}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (act.priority) {
                                    "CRITICAL", "IMMEDIATE" -> SevereRed
                                    "HIGH" -> HighOrange
                                    else -> SafeGreen
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        RiskBadge(level = act.severity, riskPercentage = if (act.severity == RiskLevel.SEVERE) 88 else (if (act.severity == RiskLevel.HIGH) 68 else 20))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = act.location, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = act.suggestedCheck, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lead: ${act.agency}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyanAccent
                        )

                        Button(
                            onClick = {
                                Toast.makeText(context, "Acknowledged: ${act.location}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlueDeep),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Acknowledge", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
