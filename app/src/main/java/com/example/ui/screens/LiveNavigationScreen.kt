package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GpsStatus
import com.example.data.model.RiskLevel
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*

@Composable
fun LiveNavigationScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier,
    onRequestLocationPermission: () -> Unit = {}
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val gpsStatus by viewModel.gpsStatus.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedRouteType by viewModel.selectedRouteType.collectAsState()
    val navStepIndex by viewModel.navStepIndex.collectAsState()
    val showFloodAheadAlert by viewModel.showFloodRiskAheadAlert.collectAsState()
    val hazardDistance by viewModel.hazardDistanceMeters.collectAsState()

    val currentRoute = scenarioState.routes.find { it.routeType == selectedRouteType }
        ?: scenarioState.routes.firstOrNull()

    // Real GPS Distance calculation to EcoSpace depression hazard
    val realGpsHazardDistance = remember(currentLocation) {
        if (currentLocation != null) {
            viewModel.locationTracker.calculateDistanceMeters(12.9278, 77.6820)?.toInt()
        } else null
    }
    val effectiveHazardDistance = realGpsHazardDistance ?: hazardDistance

    // Position coordinates (real GPS coordinates in LIVE mode, or route step coordinates in DEMO)
    val currentCoord = if (!isDemoMode && currentLocation != null) {
        Pair(currentLocation?.latitude ?: 12.9254, currentLocation?.longitude ?: 77.6740)
    } else {
        currentRoute?.pathCoordinates?.getOrNull(navStepIndex) ?: Pair(12.9248, 77.6515)
    }

    var recenterAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val routesJson = remember(scenarioState.routes) {
        val arr = org.json.JSONArray()
        scenarioState.routes.forEach { r ->
            val obj = org.json.JSONObject()
            obj.put("type", r.routeType)
            obj.put("time", "${r.travelTimeMinutes} min")
            obj.put("distance", "${r.distanceKm} km")
            obj.put("risk", r.floodRiskPercentage)
            obj.put("riskySegments", r.riskySegmentsCount)
            obj.put("recommendation", r.recommendationNote)
            val coordsArr = org.json.JSONArray()
            r.pathCoordinates.forEach { (lat, lng) ->
                val pt = org.json.JSONArray()
                pt.put(lat)
                pt.put(lng)
                coordsArr.put(pt)
            }
            obj.put("coords", coordsArr)
            arr.put(obj)
        }
        arr.toString()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Nav Instruction Bar with GPS / DEMO status tag
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(CyanAccent, SkyRadar))
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Location permission banner if denied
                    if (gpsStatus == GpsStatus.PERMISSION_DENIED) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SevereRedBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SevereRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOff,
                                        contentDescription = null,
                                        tint = SevereRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Enable device GPS for real-time live navigation",
                                        fontSize = 11.sp,
                                        color = SevereRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { onRequestLocationPermission() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SevereRed),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "ALLOW GPS",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GPS or DEMO Mode Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDemoMode) WatchAmberBg else (if (gpsStatus == GpsStatus.GPS_ACTIVE) SafeGreenBg else SevereRedBg)
                        ) {
                            Text(
                                text = if (isDemoMode) "DEMO NAVIGATION"
                                else if (gpsStatus == GpsStatus.GPS_ACTIVE && currentLocation != null)
                                    "LIVE GPS: ${String.format(java.util.Locale.ENGLISH, "%.4f", currentLocation?.latitude ?: 12.9254)}, ${String.format(java.util.Locale.ENGLISH, "%.4f", currentLocation?.longitude ?: 77.6740)}"
                                else gpsStatus.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDemoMode) WatchAmber else (if (gpsStatus == GpsStatus.GPS_ACTIVE) SafeGreen else SevereRed),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        RiskBadge(
                            level = if (selectedRouteType == "SAFER") RiskLevel.SAFE else scenarioState.overallRiskLevel,
                            riskPercentage = if (selectedRouteType == "SAFER") 18 else scenarioState.overallFloodRiskPercent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedRouteType == "SAFER") "Continue via HSR 14th Main Ridge Bypass" else "Approaching Outer Ring Road Low Point",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedRouteType == "SAFER") "In 350m • Elevated corridor • Safe elevation (898m ASL)" else "In $effectiveHazardDistance meters • SWD Culvert Low Point (872m ASL)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Map View with live vehicle position marker
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LeafletMapView(
                    onLocationClick = { },
                    onRoadClick = { },
                    routesJson = routesJson,
                    liveLat = currentCoord.first,
                    liveLng = currentCoord.second,
                    onRecenterCallback = { action -> recenterAction = action },
                    modifier = Modifier.fillMaxSize()
                )

                // Recenter Vehicle Location Floating Button
                FloatingActionButton(
                    onClick = { recenterAction?.invoke() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                        .size(48.dp)
                        .testTag("nav_recenter_button"),
                    containerColor = BlueAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Recenter on Vehicle",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // FLOOD RISK AHEAD CRITICAL BANNER (Requirement 15)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = showFloodAheadAlert,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("flood_risk_ahead_alert_banner"),
                            colors = CardDefaults.cardColors(containerColor = SevereRedBg),
                            shape = RoundedCornerShape(14.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(SevereRedDark, SevereRed))
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = SevereRed, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚠ FLOOD RISK AHEAD",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SevereRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                // Distance, Risk, Expected, Reason
                                Text("Distance: $hazardDistance meters ahead", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Risk: 78% HIGH (Severe inundation likely)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SevereRed)
                                Text("Expected: Within 15–30 minutes", fontSize = 11.sp, color = TextSecondary)
                                Text("Reason: Drainage surcharge at EcoSpace culvert bottleneck throttled by heavy runoff", fontSize = 11.sp, color = TextSecondary)

                                Spacer(modifier = Modifier.height(10.dp))

                                // 3 Buttons: VIEW SAFER ROUTE, REROUTE, CONTINUE
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.navigateTo(AppScreen.SAFE_NAV) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueDeep),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("view_safer_route_button")
                                    ) {
                                        Text("VIEW SAFER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.rerouteToSafer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("reroute_button")
                                    ) {
                                        Text("REROUTE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.dismissFloodAheadAlert() },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(0.9f).testTag("dismiss_alert_button")
                                    ) {
                                        Text("CONTINUE", fontSize = 10.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Status Dashboard
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NavyBorder, NavyCard)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${currentRoute?.travelTimeMinutes ?: 17} MIN REMAINING",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${currentRoute?.distanceKm ?: 4.8} km • Route: ${currentRoute?.routeType}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.stopLiveNavigation()
                                viewModel.navigateTo(AppScreen.SAFE_NAV)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SevereRedBg),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(SevereRedDark, SevereRed))
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("stop_nav_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = SevereRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STOP", color = SevereRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyCard,
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("AHEAD RISK", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = if (selectedRouteType == "SAFER") "18% (SAFE)" else "${scenarioState.overallFloodRiskPercent}% (${scenarioState.overallRiskLevel.name})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRouteType == "SAFER") SafeGreen else HighOrange
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NavyCard,
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("DRAINAGE STRESS", fontSize = 9.sp, color = TextSecondary)
                                Text(
                                    text = "${scenarioState.drainageStressPercent}% Surcharge",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrainageCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
