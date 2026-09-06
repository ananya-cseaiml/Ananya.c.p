package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ward: String,
    val lat: Double,
    val lng: Double,
    val elevationMeters: Double,
    val slopePercent: Double,
    val flowAccumulation: Int,
    val imperviousnessPercent: Int,
    val baseHistoricalRisk: Int,
    val dataStatus: String = "STATIC"
)

@Entity(tableName = "roads")
data class RoadEntity(
    @PrimaryKey val id: String,
    val roadName: String,
    val fromNode: String,
    val toNode: String,
    val lengthKm: Double,
    val baseTravelTimeMin: Int,
    val elevationMeters: Double,
    val slopePercent: Double,
    val drainageCapacityM3Sec: Double,
    val lat1: Double,
    val lng1: Double,
    val lat2: Double,
    val lng2: Double
)

@Entity(tableName = "rainfall")
data class RainfallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rainfallMmHr: Double,
    val recentRainfall1hMm: Double,
    val antecedentRainfall24hMm: Double,
    val intensityCategory: String,
    val dataSource: String // "LIVE", "DEMO", "STATIC"
)

@Entity(tableName = "water_levels")
data class WaterLevelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sensorId: String,
    val locationName: String,
    val timestamp: Long,
    val waterLevelMeters: Double,
    val dangerLevelMeters: Double,
    val status: String // "NORMAL", "WARNING", "OVERFLOW"
)

@Entity(tableName = "drainage")
data class DrainageEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val capacityM3Sec: Double,
    val currentLoadM3Sec: Double,
    val stressPercentage: Int,
    val isBottleneck: Boolean,
    val lastUpdated: Long
)

@Entity(tableName = "predictions")
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val locationId: String,
    val timestamp: Long,
    val predictionHorizon: String,
    val riskPercentage: Int,
    val riskLevel: String,
    val confidence: Int,
    val reasonsSummary: String,
    val modelVersion: String,
    val dataStatus: String
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val location: String,
    val severity: String,
    val riskPercentage: Int,
    val issuedAt: String,
    val expectedTime: String,
    val reason: String,
    val recommendedAction: String,
    val status: String,
    val isModelGenerated: Boolean
)

@Entity(tableName = "historical_events")
data class HistoricalEventEntity(
    @PrimaryKey val id: String,
    val eventDate: String,
    val location: String,
    val rainfallMm: Double,
    val predictedRiskPercentage: Int,
    val observedCondition: String,
    val predictionStatus: String,
    val notes: String
)

@Entity(tableName = "route_predictions")
data class RoutePredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeName: String,
    val origin: String,
    val destination: String,
    val travelTimeMin: Int,
    val distanceKm: Double,
    val routeRiskScore: Int,
    val riskySegmentsCount: Int,
    val timestamp: Long,
    val recommendation: String
)
