package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GpsStatus
import com.example.data.model.RiskLevel
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.MetricStatCard
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier,
    onRequestLocationPermission: () -> Unit = {}
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val currentAppMode by viewModel.currentAppMode.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val liveWeather by viewModel.liveWeather.collectAsState()
    val gpsStatus by viewModel.gpsStatus.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val realTimeClock by viewModel.realTimeClock.collectAsState()
    val isRefreshingWeather by viewModel.isRefreshingLiveWeather.collectAsState()

    val currentRiskColor = when (scenarioState.overallRiskLevel) {
        RiskLevel.SAFE -> SafeGreen
        RiskLevel.WATCH -> WatchAmberDark
        RiskLevel.HIGH -> HighOrange
        RiskLevel.SEVERE -> SevereRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp)
    ) {
        // Dedicated Real-Time Telemetry & Live Sync Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("realtime_telemetry_stream_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentAppMode == "LIVE") Color(0xFFF8FAFC) else Color(0xFFFFFBEB)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (currentAppMode == "LIVE") BrandBlueLight.copy(alpha = 0.5f) else WatchAmber.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with pulsating LED and clock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (currentAppMode == "LIVE") SafeGreen.copy(alpha = pulseAlpha)
                                        else WatchAmber.copy(alpha = pulseAlpha)
                                    )
                            )
                            Column {
                                Text(
                                    text = if (currentAppMode == "LIVE") "REAL-TIME TELEMETRY ACTIVE" else "SIMULATED SCENARIO DEMO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (currentAppMode == "LIVE") SafeGreenDark else WatchAmberDark,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = if (realTimeClock.isNotEmpty()) realTimeClock else "Live Feed Connected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Refresh / Sync Button
                        Button(
                            onClick = { viewModel.refreshLiveWeather() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentAppMode == "LIVE") BrandBlue else BlueDeep
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sync_live_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(15.dp)
                                    .then(if (isRefreshingWeather) Modifier.rotate(spinAngle) else Modifier)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isRefreshingWeather) "SYNCING..." else "SYNC NOW",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = BorderSlate.copy(alpha = 0.7f))

                    // Open-Meteo Live NWP Metrics Strip
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📡 Open-Meteo ECMWF / DWD NWP Model",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandBlueLight.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = liveWeather?.weatherDesc ?: "Live Telemetry",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Live Rain Rate
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Rain Rate", fontSize = 9.sp, color = TextMuted)
                                    Text(
                                        text = "${String.format(Locale.ENGLISH, "%.1f", liveWeather?.rainfallMmHr ?: scenarioState.rainfallMmHr)} mm/h",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if ((liveWeather?.rainfallMmHr ?: scenarioState.rainfallMmHr) > 15.0) HighOrange else TextPrimary
                                    )
                                }
                            }

                            // Past 1h Accumulation
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Past 1h", fontSize = 9.sp, color = TextMuted)
                                    Text(
                                        text = "${String.format(Locale.ENGLISH, "%.1f", liveWeather?.recentRainfall1hMm ?: 0.0)} mm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Past 24h Antecedent
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Past 24h", fontSize = 9.sp, color = TextMuted)
                                    Text(
                                        text = "${String.format(Locale.ENGLISH, "%.1f", liveWeather?.antecedent24hRainfallMm ?: 0.0)} mm",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            // Ambient Temp & Humidity
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSlate),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Temp / RH", fontSize = 9.sp, color = TextMuted)
                                    Text(
                                        text = "${(liveWeather?.temperatureC ?: 26.5).toInt()}°C • ${(liveWeather?.humidityPercent ?: 75)}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Device GPS Telemetry Bar
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderSlate),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateTo(AppScreen.FLOOD_MAP) }
                            ) {
                                Icon(
                                    imageVector = when (gpsStatus) {
                                        GpsStatus.GPS_ACTIVE -> Icons.Default.MyLocation
                                        GpsStatus.PERMISSION_DENIED -> Icons.Default.LocationOff
                                        else -> Icons.Default.SatelliteAlt
                                    },
                                    contentDescription = null,
                                    tint = when (gpsStatus) {
                                        GpsStatus.GPS_ACTIVE -> SafeGreen
                                        GpsStatus.PERMISSION_DENIED -> SevereRed
                                        else -> BrandBlue
                                    },
                                    modifier = Modifier.size(18.dp)
                                )

                                Column {
                                    Text(
                                        text = when {
                                            gpsStatus == GpsStatus.GPS_ACTIVE && currentLocation != null ->
                                                "Real GPS Fix: ${String.format(Locale.ENGLISH, "%.4f", currentLocation?.latitude ?: 12.9254)}°N, ${String.format(Locale.ENGLISH, "%.4f", currentLocation?.longitude ?: 77.6740)}°E"
                                            gpsStatus == GpsStatus.PERMISSION_DENIED ->
                                                "Location Permission Needed for Live GPS"
                                            else ->
                                                "GPS Acquiring Satellites (Agara Ingress Default)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Catchment: Bellandur–Agara Basin • Lat 12.927°N, Lng 77.676°E",
                                        fontSize = 9.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (gpsStatus == GpsStatus.PERMISSION_DENIED) {
                                Button(
                                    onClick = { onRequestLocationPermission() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SevereRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("enable_gps_permission_button")
                                ) {
                                    Text("GRANT GPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else if (currentAppMode != "LIVE") {
                                OutlinedButton(
                                    onClick = { viewModel.setAppMode("LIVE") },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("GO LIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Geometric Balance: Current Critical Risk Hero Section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("home_hero_card"),
                colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "CURRENT CRITICAL RISK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${scenarioState.overallFloodRiskPercent}% ${scenarioState.overallRiskLevel.label} Risk",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = currentRiskColor,
                                letterSpacing = (-0.5).sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Expected In",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (scenarioState.overallFloodRiskPercent > 50) "35–45 mins" else "60+ mins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Geometric progress bar
                    LinearProgressIndicator(
                        progress = { scenarioState.overallFloodRiskPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = currentRiskColor,
                        trackColor = SurfaceVariant
                    )

                    // Horizontal Telemetry Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rainfall", fontSize = 10.sp, color = TextMuted)
                            Text(
                                "${scenarioState.rainfallMmHr.toInt()} mm/h",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(BorderSlate)
                        )

                        Column {
                            Text("Drainage Load", fontSize = 10.sp, color = TextMuted)
                            Text(
                                "${scenarioState.drainageStressPercent}% Stress",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (scenarioState.drainageStressPercent > 70) HighOrange else TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(BorderSlate)
                        )

                        Column {
                            Text("Elevation", fontSize = 10.sp, color = TextMuted)
                            Text(
                                "884m (Basin)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(BorderSlate)
                        )

                        Column {
                            Text("Mode", fontSize = 10.sp, color = TextMuted)
                            Text(
                                when (currentAppMode) {
                                    "HISTORICAL" -> "Historical"
                                    "LIVE" -> "Live"
                                    else -> "Demo"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (currentAppMode) {
                                    "HISTORICAL" -> WatchAmberDark
                                    "LIVE" -> BrandBlue
                                    else -> SafeGreenDark
                                }
                            )
                        }
                    }
                }
            }
        }

        // Main Statistics Grid (4 Cards)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Flood Risk",
                        value = "${scenarioState.overallFloodRiskPercent}%",
                        subtitle = "Composite score (0–100)",
                        icon = Icons.Default.Water,
                        iconColor = currentRiskColor,
                        badgeLevel = scenarioState.overallRiskLevel,
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Rainfall",
                        value = "${scenarioState.rainfallMmHr.toInt()} mm/h",
                        subtitle = if (isDemoMode) "Simulated rate" else (liveWeather?.weatherDesc ?: "Live rate"),
                        icon = Icons.Default.Cloud,
                        iconColor = BrandBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "Drainage Stress",
                        value = "${scenarioState.drainageStressPercent}%",
                        subtitle = "${scenarioState.drainageLoadM3Sec.toInt()} / ${scenarioState.drainageCapacityM3Sec.toInt()} m³/s load",
                        icon = Icons.Default.Troubleshoot,
                        iconColor = DrainageCyan,
                        badgeLevel = when {
                            scenarioState.drainageStressPercent > 80 -> RiskLevel.SEVERE
                            scenarioState.drainageStressPercent > 60 -> RiskLevel.HIGH
                            scenarioState.drainageStressPercent > 40 -> RiskLevel.WATCH
                            else -> RiskLevel.SAFE
                        },
                        modifier = Modifier.weight(1f)
                    )

                    MetricStatCard(
                        title = "Risky Roads",
                        value = "${scenarioState.highRiskRoadsCount} Segments",
                        subtitle = "ORR & Sarjapur corridor",
                        icon = Icons.Default.DirectionsCar,
                        iconColor = if (scenarioState.highRiskRoadsCount > 0) HighOrange else SafeGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Flood Scenario Engine Controller
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("scenario_controller_card"),
                colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIMULATION ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Step ${scenarioState.currentStep.stepNumber} / 10",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = scenarioState.currentStep.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = scenarioState.currentStep.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.advanceScenario() },
                            modifier = Modifier
                                .weight(1.6f)
                                .height(44.dp)
                                .testTag("start_flood_scenario_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = HeaderDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scenarioState.currentStep.stepNumber == 1) "START SCENARIO" else "STEP FORWARD",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetScenario() },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("reset_demo_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSlate)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }
        }

        // Primary Navigation Operational Modules
        item {
            Text(
                text = "OPERATIONAL MODULES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        val primaryActions = listOf(
            Triple("CHECK FLOOD MAP", "Interactive Leaflet & OSM Bellandur–Agara map", AppScreen.FLOOD_MAP),
            Triple("SAFE NAVIGATION", "Fastest vs Safer route calculation", AppScreen.SAFE_NAV),
            Triple("LIVE ALERTS", "Active citizen warnings and severe hotspots", AppScreen.ALERTS),
            Triple("NOWCAST", "+15m, +30m, +60m, +120m probabilistic predictions", AppScreen.NOWCAST),
            Triple("AUTHORITY DASHBOARD", "BBMP & Traffic Police decision-support mode", AppScreen.AUTHORITY),
            Triple("DRAINAGE + RAINFALL", "Physical chain coupling visualization", AppScreen.DRAINAGE)
        )

        items(primaryActions.size) { index ->
            val (title, subtitle, targetScreen) = primaryActions[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(14.dp))
                    .clickable { viewModel.navigateTo(targetScreen) }
                    .testTag("nav_btn_${targetScreen.name.lowercase()}"),
                colors = CardDefaults.cardColors(containerColor = WhiteSurface),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = BlueDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
