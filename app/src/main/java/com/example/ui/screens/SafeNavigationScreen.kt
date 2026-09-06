package com.example.ui.screens

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
import com.example.data.model.RiskLevel
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun SafeNavigationScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val originInput by viewModel.originInput.collectAsState()
    val destinationInput by viewModel.destinationInput.collectAsState()
    val selectedRouteType by viewModel.selectedRouteType.collectAsState()

    val routesJson = remember(scenarioState.routes) {
        val arr = JSONArray()
        scenarioState.routes.forEach { r ->
            val obj = JSONObject()
            obj.put("type", r.routeType)
            obj.put("time", "${r.travelTimeMinutes} min")
            obj.put("distance", "${r.distanceKm} km")
            obj.put("risk", r.floodRiskPercentage)
            obj.put("riskySegments", r.riskySegmentsCount)
            obj.put("recommendation", r.recommendationNote)
            val coordsArr = JSONArray()
            r.pathCoordinates.forEach { (lat, lng) ->
                val pt = JSONArray()
                pt.put(lat)
                pt.put(lng)
                coordsArr.put(pt)
            }
            obj.put("coords", coordsArr)
            arr.put(obj)
        }
        arr.toString()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // Header
        item {
            Text(
                text = "SAFE NAVIGATION ENGINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Drainage-Coupled Flood Avoidance",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Calculates route risk by integrating road-segment elevations, culvert hydraulic surcharge, and runoff accumulation.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Origin & Destination Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NavyBorder, NavyCard)))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Origin Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SafeGreen)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(
                                text = originInput,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                val loc = currentLocation
                                val label = if (loc != null) "%.4f° N, %.4f° E (Live GPS)".format(loc.latitude, loc.longitude) else "Agara Junction (GPS Detected)"
                                viewModel.setOrigin(label)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Use GPS", tint = CyanAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = NavyBorder)

                    // Destination Input
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SevereRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text(
                                text = destinationInput,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset corridors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavyCard,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setOrigin("Agara Junction")
                                    viewModel.setDestination("Bellandur EcoWorld (ORR)")
                                }
                                .padding(2.dp)
                        ) {
                            Text("Agara → EcoWorld", fontSize = 10.sp, color = SkyRadar, modifier = Modifier.padding(6.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NavyCard,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setOrigin("HSR Sector 1")
                                    viewModel.setDestination("Rainbow Drive Sarjapur")
                                }
                                .padding(2.dp)
                        ) {
                            Text("HSR → Rainbow Dr", fontSize = 10.sp, color = SkyRadar, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }

        // Routes Comparison Header
        item {
            Text(
                text = "CALCULATED ROUTE ALTERNATIVES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }

        // Real Interactive Map Preview of Routes & Live Exact Location
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .testTag("safe_nav_routes_map_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BlueDeep, CyanAccent.copy(alpha = 0.5f)))
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LeafletMapView(
                        onLocationClick = {},
                        onRoadClick = {},
                        routesJson = routesJson,
                        liveLat = currentLocation?.latitude ?: 12.9248,
                        liveLng = currentLocation?.longitude ?: 77.6515,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Display Routes (FASTEST vs SAFER)
        val routes = scenarioState.routes
        items(routes.size) { index ->
            val route = routes[index]
            val isSelected = selectedRouteType == route.routeType
            val isSafer = route.routeType == "SAFER"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectRoute(route.routeType) }
                    .testTag("route_card_${route.routeType.lowercase()}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) BlueDeep.copy(alpha = 0.35f) else NavySurface
                ),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = if (isSelected) {
                        Brush.horizontalGradient(listOf(if (isSafer) SafeGreen else HighOrange, CyanAccent))
                    } else {
                        Brush.horizontalGradient(listOf(NavyBorder, NavyCard))
                    }
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
                                imageVector = if (isSafer) Icons.Default.Shield else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isSafer) SafeGreen else HighOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = route.routeType,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSafer) SafeGreen else HighOrange
                            )
                        }

                        RiskBadge(
                            level = if (route.floodRiskPercentage <= 30) RiskLevel.SAFE else (if (route.floodRiskPercentage <= 60) RiskLevel.WATCH else RiskLevel.HIGH),
                            riskPercentage = route.floodRiskPercentage
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = route.routeTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("EST. TIME", fontSize = 10.sp, color = TextSecondary)
                            Text("${route.travelTimeMinutes} min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("DISTANCE", fontSize = 10.sp, color = TextSecondary)
                            Text("${route.distanceKm} km", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Column {
                            Text("RISKY SECTORS", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = "${route.riskySegmentsCount} segments",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (route.riskySegmentsCount > 0) HighOrange else SafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NavyDark.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = route.recommendationNote,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Recommendation Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SafeGreenBg),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SafeGreenDark, SafeGreen)))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("DECISION SUPPORT RECOMMENDATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                        Text(
                            text = "Safer route recommended because predicted flood risk is substantially lower by bypassing Outer Ring Road depression bottlenecks.",
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Start Navigation Button
        item {
            Button(
                onClick = { viewModel.startLiveNavigation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_live_navigation_button"),
                colors = ButtonDefaults.buttonColors(containerColor = HeaderDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START NAVIGATION (${selectedRouteType})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
