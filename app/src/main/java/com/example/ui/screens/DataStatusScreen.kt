package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FloodSafeViewModel
import com.example.ui.theme.*

@Composable
fun DataStatusScreen(
    viewModel: FloodSafeViewModel,
    modifier: Modifier = Modifier
) {
    val dataSources by viewModel.dataSources.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DATA TRANSPARENCY & INTEGRITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyRadar,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Telemetry & Sensor Mesh Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = { viewModel.repository.refreshLiveWeather() },
                    modifier = Modifier.testTag("refresh_data_status_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanAccent)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clear distinction between LIVE sensor telemetry, STATIC GIS layers, HISTORICAL ground truth, and simulated DEMO scenario data.",
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }

        items(dataSources.size) { index ->
            val src = dataSources[index]
            val typeColor = when (src.type) {
                "LIVE" -> SafeGreen
                "DEMO" -> WatchAmber
                "STATIC" -> SkyRadar
                "HISTORICAL" -> CyanAccent
                "STALE" -> HighOrange
                else -> SevereRed
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavySurface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(typeColor.copy(alpha = 0.6f), NavyBorder))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = src.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = src.category, fontSize = 10.sp, color = TextSecondary)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = typeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = src.type,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = typeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = src.statusText, fontSize = 12.sp, color = TextPrimary)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Updated: ${src.lastUpdatedText}", fontSize = 10.sp, color = TextSecondary)
                        Text("Freshness: ${src.freshness}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = typeColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NavyDark.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Transparency Note: ${src.honestyNote}",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
