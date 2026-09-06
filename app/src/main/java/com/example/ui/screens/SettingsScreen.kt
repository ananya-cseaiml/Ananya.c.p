package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FloodSafeViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repo = viewModel.repository

    var safeThreshold by remember { mutableStateOf(repo.riskEngine.safeMaxThreshold.toFloat()) }
    var watchThreshold by remember { mutableStateOf(repo.riskEngine.watchMaxThreshold.toFloat()) }
    var highThreshold by remember { mutableStateOf(repo.riskEngine.highMaxThreshold.toFloat()) }

    var rainfallWeight by remember { mutableStateOf(repo.riskEngine.rainfallWeight) }
    var drainageWeight by remember { mutableStateOf(repo.riskEngine.drainageWeight) }
    var terrainWeight by remember { mutableStateOf(repo.riskEngine.terrainWeight) }
    var flowAccWeight by remember { mutableStateOf(repo.riskEngine.flowAccumulationWeight) }

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
                text = "CONFIGURATIONS & HYDRAULIC PARAMETERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SkyRadar,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Model Settings & Weights",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tune hydrodynamic risk thresholds and physical coupling weights for the Bellandur–Agara basin model.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        // Pilot Location Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, CyanAccent.copy(alpha = 0.3f))))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ACTIVE PILOT CATCHMENT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Bellandur–Agara Basin (Ward 150 & Ward 174, Bengaluru)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Coordinates: 12.927° N, 77.676° E • Elevation: 870–910m ASL", fontSize = 11.sp, color = TextSecondary)
                }
            }
        }

        // Risk Level Threshold Sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NavyBorder, NavyCard)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RISK LEVEL THRESHOLDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Safe Max Threshold: ${safeThreshold.toInt()}%", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = safeThreshold,
                        onValueChange = {
                            safeThreshold = it
                            viewModel.updateThresholds(safeThreshold.toInt(), watchThreshold.toInt(), highThreshold.toInt())
                        },
                        valueRange = 10f..40f,
                        colors = SliderDefaults.colors(thumbColor = SafeGreen, activeTrackColor = SafeGreen)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Watch Max Threshold: ${watchThreshold.toInt()}%", fontSize = 12.sp, color = WatchAmber, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = watchThreshold,
                        onValueChange = {
                            watchThreshold = it
                            viewModel.updateThresholds(safeThreshold.toInt(), watchThreshold.toInt(), highThreshold.toInt())
                        },
                        valueRange = 40f..70f,
                        colors = SliderDefaults.colors(thumbColor = WatchAmber, activeTrackColor = WatchAmber)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("High Max Threshold: ${highThreshold.toInt()}% (Above is SEVERE)", fontSize = 12.sp, color = HighOrange, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = highThreshold,
                        onValueChange = {
                            highThreshold = it
                            viewModel.updateThresholds(safeThreshold.toInt(), watchThreshold.toInt(), highThreshold.toInt())
                        },
                        valueRange = 70f..90f,
                        colors = SliderDefaults.colors(thumbColor = HighOrange, activeTrackColor = HighOrange)
                    )
                }
            }
        }

        // Model Weight Sliders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(NavyBorder, NavyCard)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COUPLING FACTOR WEIGHTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SkyRadar)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Rainfall Intensity Weight: ${(rainfallWeight * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = rainfallWeight,
                        onValueChange = { rainfallWeight = it },
                        valueRange = 0.1f..0.5f,
                        colors = SliderDefaults.colors(thumbColor = SkyRadar, activeTrackColor = SkyRadar)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Drainage Stress Weight: ${(drainageWeight * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = drainageWeight,
                        onValueChange = { drainageWeight = it },
                        valueRange = 0.1f..0.5f,
                        colors = SliderDefaults.colors(thumbColor = DrainageCyan, activeTrackColor = DrainageCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Terrain Depression Weight: ${(terrainWeight * 100).toInt()}%", fontSize = 12.sp, color = TextPrimary)
                    Slider(
                        value = terrainWeight,
                        onValueChange = { terrainWeight = it },
                        valueRange = 0.1f..0.4f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }
            }
        }

        // AI Explanation Configuration Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NavyBorder, SkyRadar.copy(alpha = 0.4f))))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SkyRadar, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GEMINI AI EXPLANATION STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SkyRadar)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (repo.geminiService.isConfigured) "Gemini 2.5 Flash API is ACTIVE via injected build secrets."
                        else "Gemini API key is not configured. The application seamlessly uses deterministic hydrologic template explanations.",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // Reset to Defaults Button
        item {
            OutlinedButton(
                onClick = {
                    safeThreshold = 30f
                    watchThreshold = 60f
                    highThreshold = 80f
                    rainfallWeight = 0.25f
                    drainageWeight = 0.15f
                    terrainWeight = 0.15f
                    flowAccWeight = 0.15f
                    viewModel.updateThresholds(30, 60, 80)
                    viewModel.updateWeights(0.25f, 0.15f, 0.15f, 0.15f, 0.15f, 0.10f, 0.05f)
                    Toast.makeText(context, "Reset model parameters to default", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reset_settings_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESET TO HYDRAULIC BASELINE DEFAULTS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
