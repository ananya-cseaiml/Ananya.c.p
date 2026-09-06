package com.example.data.model

enum class RiskLevel(val label: String) {
    SAFE("SAFE"),
    WATCH("WATCH"),
    HIGH("HIGH"),
    SEVERE("SEVERE");

    companion object {
        fun fromScore(score: Int): RiskLevel {
            return when {
                score <= 30 -> SAFE
                score <= 60 -> WATCH
                score <= 80 -> HIGH
                else -> SEVERE
            }
        }
    }
}

enum class DataStatus(val label: String) {
    LIVE("LIVE"),
    DEMO("DEMO"),
    HISTORICAL("HISTORICAL"),
    STATIC("STATIC"),
    STALE("STALE"),
    UNAVAILABLE("UNAVAILABLE")
}

data class FactorBreakdown(
    val factorName: String,
    val score: Int, // 0-100
    val weight: Float,
    val contributingDesc: String
)

data class RiskCalculationResult(
    val riskPercentage: Int,
    val riskLevel: RiskLevel,
    val timestamp: Long,
    val predictionHorizon: String,
    val confidence: Int,
    val reasons: List<String>,
    val contributingFactors: List<FactorBreakdown>,
    val dataStatus: DataStatus,
    val modelVersion: String = "v1.4.2-baseline-drainage-coupled"
)

data class LocationInfo(
    val id: String,
    val name: String,
    val ward: String,
    val lat: Double,
    val lng: Double,
    val elevationMeters: Double,
    val slopePercent: Double,
    val flowAccumulationIndex: Int,
    val imperviousnessPercent: Int,
    val currentRisk: Int,
    val riskLevel: RiskLevel,
    val predictedTimeWindow: String,
    val confidence: Int,
    val whyAtRisk: String,
    val recommendedAction: String,
    val isVulnerableHotspot: Boolean = true
)

data class RoadSegment(
    val id: String,
    val roadName: String,
    val fromNode: String,
    val toNode: String,
    val lengthKm: Double,
    val baseTravelTimeMin: Int,
    val elevationMeters: Double,
    val slopePercent: Double,
    val flowAccumulation: Int,
    val drainageStressPercent: Int,
    val riskPercentage: Int,
    val severity: RiskLevel,
    val predictionTime: String,
    val confidence: Int,
    val currentRainfallMmHr: Double,
    val reasons: List<String>,
    val lat1: Double,
    val lng1: Double,
    val lat2: Double,
    val lng2: Double
)

data class DrainageChannel(
    val id: String,
    val name: String,
    val type: String, // e.g., Primary Trunk, Secondary Arterial
    val designCapacityM3Sec: Double,
    val currentDischargeM3Sec: Double,
    val stressPercentage: Int,
    val isBottleneck: Boolean,
    val bottleneckReason: String
)

data class NowcastHorizon(
    val horizonLabel: String, // NOW, +15m, +30m, +60m, +120m, 0-6h Outlook
    val riskPercentage: Int,
    val riskLevel: RiskLevel,
    val confidence: Int,
    val expectedRainfallMmHr: Double,
    val predictedDrainageStressPercent: Int = 20,
    val recommendedReadinessLevel: String = "Normal monitoring",
    val mainReasons: List<String>
)

data class FloodAlert(
    val id: String,
    val location: String,
    val severity: RiskLevel,
    val riskPercentage: Int,
    val issuedAt: String,
    val expectedTime: String,
    val reason: String,
    val recommendedAction: String,
    val status: String = "ACTIVE",
    val isModelGenerated: Boolean = true
)

enum class GpsStatus(val label: String) {
    GPS_ACTIVE("GPS ACTIVE"),
    GPS_SEARCHING("SEARCHING GPS"),
    GPS_UNAVAILABLE("GPS UNAVAILABLE"),
    PERMISSION_DENIED("PERMISSION DENIED")
}

data class RouteOption(
    val routeType: String, // "FASTEST" or "SAFER"
    val routeTitle: String,
    val travelTimeMinutes: Int,
    val distanceKm: Double,
    val floodRiskPercentage: Int,
    val riskySegmentsCount: Int,
    val maximumSegmentRisk: Int = floodRiskPercentage,
    val averageSegmentRisk: Int = (floodRiskPercentage * 0.7).toInt(),
    val floodRiskPenaltyMinutes: Int = (floodRiskPercentage * 0.25).toInt(),
    val recommendationNote: String,
    val segments: List<RoadSegment>,
    val pathCoordinates: List<Pair<Double, Double>>
)

data class HistoricalEvent(
    val id: String,
    val date: String,
    val location: String,
    val rainfallRecordedMm: Double,
    val predictedRiskPercentage: Int,
    val observedCondition: String, // Inundation (1.2m), Waterlogged (0.4m), Minor pooling
    val predictionStatus: String, // "CORRECT HIT", "OVER-PREDICTION", "UNDER-PREDICTION"
    val notes: String
)

data class ValidationMetrics(
    val accuracyPercent: Double,
    val precisionPercent: Double,
    val recallPercent: Double,
    val falseAlarmRatePercent: Double,
    val missedEventRatePercent: Double,
    val averageLeadTimeMinutes: Int,
    val verifiedEventsCount: Int,
    val isDemo: Boolean = true,
    val hasSufficientData: Boolean = true,
    val notice: String = "Synthetic/demo validation data. Insufficient real-world verified sensor records."
)

data class DataSourceStatus(
    val name: String,
    val category: String, // Rainfall, Weather, Water Level, Drainage, Terrain, Historical
    val type: String, // LIVE, STATIC, HISTORICAL, DEMO, STALE, UNAVAILABLE
    val statusText: String,
    val lastUpdatedText: String,
    val freshness: String,
    val honestyNote: String = ""
)

data class GovernmentActionItem(
    val location: String,
    val severity: RiskLevel,
    val suggestedCheck: String,
    val agency: String,
    val priority: String,
    val isAcknowledged: Boolean = false
)
