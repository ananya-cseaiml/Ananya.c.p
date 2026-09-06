package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationInfo
import com.example.data.model.RiskLevel
import com.example.data.model.RoadSegment
import com.example.ui.AppScreen
import com.example.ui.FloodSafeViewModel
import com.example.ui.components.LeafletMapView
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloodMapScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val scenarioState by viewModel.scenarioState.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedRoad by viewModel.selectedRoad.collectAsState()
    val aiExplanationText by viewModel.aiExplanationText.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showDetailsSheet by remember { mutableStateOf(false) }
    var recenterAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Layer toggle states
    var floodRiskActive by remember { mutableStateOf(true) }
    var roadsActive by remember { mutableStateOf(true) }
    var drainageActive by remember { mutableStateOf(true) }
    var waterBodiesActive by remember { mutableStateOf(true) }
    var vulnerableActive by remember { mutableStateOf(true) }

    val currentLat = currentLocation?.latitude ?: 12.9254
    val currentLng = currentLocation?.longitude ?: 77.6740
    val accuracyM = currentLocation?.accuracy?.toInt() ?: 8

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Live Real Location Telemetry Pill & Quick Recenter Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("exact_location_telemetry_banner"),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(BlueAccent.copy(alpha = 0.6f), NavyBorder))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(SafeGreen, shape = RoundedCornerShape(5.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "EXACT LOCATION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanAccent,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "±${accuracyM}m accuracy",
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "%.5f° N, %.5f° E • Bellandur Basin".format(currentLat, currentLng),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { recenterAction?.invoke() },
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("recenter_location_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BlueAccent.copy(alpha = 0.25f),
                            contentColor = SkyRadar
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Center", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Layer Toggle Controls Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavySurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = floodRiskActive,
                    onClick = { floodRiskActive = !floodRiskActive },
                    label = { Text("Flood Risk", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Water, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueDeep, selectedLabelColor = SkyRadar)
                )
                FilterChip(
                    selected = roadsActive,
                    onClick = { roadsActive = !roadsActive },
                    label = { Text("Roads", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueDeep, selectedLabelColor = SkyRadar)
                )
                FilterChip(
                    selected = drainageActive,
                    onClick = { drainageActive = !drainageActive },
                    label = { Text("Drainage", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Waves, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueDeep, selectedLabelColor = SkyRadar)
                )
                FilterChip(
                    selected = waterBodiesActive,
                    onClick = { waterBodiesActive = !waterBodiesActive },
                    label = { Text("Lakes", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueDeep, selectedLabelColor = SkyRadar)
                )
                FilterChip(
                    selected = vulnerableActive,
                    onClick = { vulnerableActive = !vulnerableActive },
                    label = { Text("Hotspots", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BlueDeep, selectedLabelColor = SkyRadar)
                )
            }

            // Interactive Leaflet Map Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LeafletMapView(
                    onLocationClick = { jsonStr ->
                        try {
                            val obj = JSONObject(jsonStr)
                            val loc = LocationInfo(
                                id = obj.optString("id", "loc_0"),
                                name = obj.optString("name", "Location"),
                                ward = "Bellandur–Agara Pilot",
                                lat = obj.optDouble("lat", 12.927),
                                lng = obj.optDouble("lng", 77.676),
                                elevationMeters = 872.4,
                                slopePercent = 0.8,
                                flowAccumulationIndex = 850,
                                imperviousnessPercent = 88,
                                currentRisk = obj.optInt("risk", 74),
                                riskLevel = RiskLevel.valueOf(obj.optString("severity", "HIGH")),
                                predictedTimeWindow = obj.optString("predTime", "30-60 min"),
                                confidence = obj.optInt("conf", 84),
                                whyAtRisk = obj.optString("why", "Hydraulic bottleneck"),
                                recommendedAction = obj.optString("action", "Deploy dewatering pumps")
                            )
                            viewModel.selectLocation(loc)
                            viewModel.selectRoad(null)
                            showDetailsSheet = true
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    onRoadClick = { jsonStr ->
                        try {
                            val obj = JSONObject(jsonStr)
                            val road = RoadSegment(
                                id = obj.optString("id", "road_0"),
                                roadName = obj.optString("name", "Road Segment"),
                                fromNode = "Origin",
                                toNode = "Dest",
                                lengthKm = 2.2,
                                baseTravelTimeMin = 14,
                                elevationMeters = 872.4,
                                slopePercent = 0.8,
                                flowAccumulation = 850,
                                drainageStressPercent = 92,
                                riskPercentage = obj.optInt("risk", 74),
                                severity = RiskLevel.valueOf(obj.optString("severity", "HIGH")),
                                predictionTime = "30–60 min",
                                confidence = 82,
                                currentRainfallMmHr = scenarioState.rainfallMmHr,
                                reasons = listOf(obj.optString("reasons", "Drainage surcharge")),
                                lat1 = 12.923, lng1 = 77.670, lat2 = 12.928, lng2 = 77.682
                            )
                            viewModel.selectRoad(road)
                            viewModel.selectLocation(null)
                            showDetailsSheet = true
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    liveLat = currentLat,
                    liveLng = currentLng,
                    onRecenterCallback = { action -> recenterAction = action },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Action Button to instantly re-center onto exact GPS location
                FloatingActionButton(
                    onClick = { recenterAction?.invoke() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 90.dp, end = 16.dp)
                        .size(48.dp)
                        .testTag("floating_my_location_button"),
                    containerColor = BlueAccent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Exact Location",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Hotspot Quick Selection Chips at the bottom of the map
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.92f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.5f)))
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONITORED PILOT HOTSPOTS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Tap for risk details",
                                fontSize = 10.sp,
                                color = CyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            scenarioState.locations.forEach { loc ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = NavyCard,
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(listOf(NavyBorder, SkyRadar.copy(alpha = 0.4f)))
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.selectLocation(loc)
                                            viewModel.selectRoad(null)
                                            showDetailsSheet = true
                                        }
                                        .testTag("hotspot_chip_${loc.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        RiskBadge(level = loc.riskLevel, riskPercentage = loc.currentRisk)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = loc.name.substringBefore(" -"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Location or Road Details Modal Bottom Sheet
        if (showDetailsSheet && (selectedLocation != null || selectedRoad != null)) {
            ModalBottomSheet(
                onDismissRequest = { showDetailsSheet = false },
                containerColor = NavySurface,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp)
                ) {
                    val loc = selectedLocation
                    val road = selectedRoad
                    if (loc != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = loc.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = loc.ward,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            RiskBadge(level = loc.riskLevel, riskPercentage = loc.currentRisk)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Predicted Horizon:", fontSize = 11.sp, color = TextSecondary)
                                    Text(loc.predictedTimeWindow, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Model Confidence:", fontSize = 11.sp, color = TextSecondary)
                                    Text("${loc.confidence}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Elevation / ASL:", fontSize = 11.sp, color = TextSecondary)
                                    Text("${loc.elevationMeters}m (Basin depression)", fontSize = 11.sp, color = TextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "WHY IS THIS LOCATION AT RISK?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loc.whyAtRisk,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 17.sp
                        )

                        if (isAiLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                color = CyanAccent
                            )
                        } else if (!aiExplanationText.isNullOrBlank()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BlueDeep.copy(alpha = 0.4f)),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SkyRadar, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI DECISION EXPLANATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SkyRadar)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(aiExplanationText ?: "", fontSize = 11.sp, color = TextPrimary, lineHeight = 15.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "RECOMMENDED ACTION:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighOrange,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = loc.recommendedAction,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    } else if (road != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = road.roadName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Length: ${road.lengthKm} km • Base Travel: ${road.baseTravelTimeMin}m",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            RiskBadge(level = road.severity, riskPercentage = road.riskPercentage)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Drainage Stress:", fontSize = 11.sp, color = TextSecondary)
                                    Text("${road.drainageStressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighOrange)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Expected Horizon:", fontSize = 11.sp, color = TextSecondary)
                                    Text(road.predictionTime, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Confidence:", fontSize = 11.sp, color = TextSecondary)
                                    Text("${road.confidence}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "PRIMARY REASONS:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            letterSpacing = 0.5.sp
                        )
                        road.reasons.forEach { r ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(r, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showDetailsSheet = false
                            viewModel.navigateTo(AppScreen.SAFE_NAV)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plan_safe_route_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PLAN SAFE NAVIGATION AROUND THIS AREA", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
