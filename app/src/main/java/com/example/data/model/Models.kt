package com.example.data.model

enum class RiskLevel(val label: String) {
    LOW("LOW"),
    MODERATE("MODERATE"),
    HIGH("HIGH"),
    SEVERE("SEVERE");

    companion object {
        // Backwards-compatibility aliases
        val SAFE: RiskLevel get() = LOW
        val WATCH: RiskLevel get() = MODERATE

        fun fromScore(score: Int): RiskLevel {
            return when {
                score <= 24 -> LOW
                score <= 49 -> MODERATE
                score <= 74 -> HIGH
                else -> SEVERE
            }
        }

        fun fromString(name: String?): RiskLevel {
            return when (name?.uppercase()?.trim()) {
                "LOW", "SAFE" -> LOW
                "MODERATE", "WATCH" -> MODERATE
                "HIGH" -> HIGH
                "SEVERE", "CRITICAL" -> SEVERE
                else -> MODERATE
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

enum class DataOrigin(val label: String) {
    LIVE("LIVE DATA"),
    VERIFIED_HISTORICAL("VERIFIED HISTORICAL"),
    PROTOTYPE("PROTOTYPE DATA"),
    DEMO("DEMO DATA"),
    PLANNED("PLANNED INTEGRATION"),
    HISTORICAL("HISTORICAL DATA"),
    STATIC_GIS("STATIC GIS DATA"),
    UNAVAILABLE("UNAVAILABLE")
}

enum class ValidationStatus(val label: String) {
    DEMO("DEMO ONLY — NOT REAL VALIDATION"),
    HISTORICAL_BENCHMARK("HISTORICAL BENCHMARK — NOT VERIFIED"),
    VERIFIED_VALIDATION("VERIFIED VALIDATION"),
    PENDING("PENDING")
}

data class FloodRiskResult(
    val score: Int,
    val level: RiskLevel,
    val rainfallContribution: Int,
    val runoffContribution: Int,
    val terrainContribution: Int,
    val flowAccumulationContribution: Int,
    val drainageContribution: Int,
    val historicalContribution: Int,
    val waterLevelContribution: Int,
    val confidence: Int,
    val primaryReason: String,
    val dataOrigin: DataOrigin
)

data class FactorBreakdown(
    val factorName: String,
    val score: Int, // 0-100
    val weight: Float,
    val contributingDesc: String
)

data class WhyAtRiskExplanation(
    val rainfallFactor: String,
    val runoffFactor: String,
    val terrainFactor: String,
    val flowAccFactor: String,
    val drainageStressFactor: String,
    val historicalFactor: String,
    val primarySummary: String
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
    val modelVersion: String = "v2.0-single-source-hydrological-engine",
    val baselineSusceptibilityPercent: Int = 35,
    val dynamicRiskPercent: Int = riskPercentage,
    val detailedWhyExplanation: String = "",
    val whyExplanation: WhyAtRiskExplanation? = null
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
    val lng2: Double,
    val drainageExposurePercent: Int = drainageStressPercent,
    val terrainExposurePercent: Int = if (elevationMeters <= 874.0) 80 else 25,
    val hazardPenaltyMin: Int = (riskPercentage * 0.25).toInt()
) {
    val roadId: String get() = id
    val startCoordinate: Pair<Double, Double> get() = Pair(lat1, lng1)
    val endCoordinate: Pair<Double, Double> get() = Pair(lat2, lng2)
    val length: Double get() = lengthKm
    val travelTime: Int get() = baseTravelTimeMin
    val floodRisk: Int get() = riskPercentage
    val drainageExposure: Int get() = drainageExposurePercent
    val terrainExposure: Int get() = terrainExposurePercent
    val riskPenalty: Int get() = hazardPenaltyMin
}

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
    val pathCoordinates: List<Pair<Double, Double>>,
    val routeScore: Double = travelTimeMinutes + (floodRiskPercentage * 0.4) + (riskySegmentsCount * 12.0),
    val bottlenecksAvoided: Int = 0
)

data class HistoricalEvent(
    val id: String,
    val date: String,
    val location: String,
    val rainfallRecordedMm: Double,
    val predictedRiskPercentage: Int,
    val observedCondition: String, // Inundation (1.2m), Waterlogged (0.4m), Minor pooling
    val predictionStatus: String, // "DEMO CORRECT-HIT SCENARIO", "DEMO OVER-PREDICTION SCENARIO"
    val notes: String,
    val dataOrigin: DataOrigin = DataOrigin.PROTOTYPE
)

data class ValidationMetrics(
    val accuracyPercent: Double,
    val precisionPercent: Double,
    val recallPercent: Double,
    val falseAlarmRatePercent: Double,
    val missedEventRatePercent: Double,
    val averageLeadTimeMinutes: Int,
    val verifiedEventsCount: Int = 0,
    val demoEventsCount: Int = 0,
    val isDemo: Boolean = true,
    val hasSufficientData: Boolean = false,
    val validationStatus: ValidationStatus = if (isDemo) ValidationStatus.DEMO else ValidationStatus.PENDING,
    val notice: String = if (isDemo) "DEMO VALIDATION WORKFLOW — NOT REAL MODEL VALIDATION (Synthetic benchmark scenario demonstration)." else "Verified model validation requires genuine observed flood-event and rainfall datasets. The current project contains demonstration scenarios only.",
    val f1ScorePercent: Double = if (precisionPercent + recallPercent > 0) {
        Math.round((2 * precisionPercent * recallPercent / (precisionPercent + recallPercent)) * 10.0) / 10.0
    } else 0.0,
    val statusLabel: String = if (isDemo) "DEMO VALIDATION WORKFLOW — NOT REAL MODEL VALIDATION" else "VALIDATION STATUS: PENDING VERIFIED GROUND TRUTH",
    val spatialValidationStatus: String = "Bellandur–Agara Basin pilot mesh (Ward 150 & 174)",
    val temporalValidationStatus: String = "15-min hyetograph resolution, 15–120m nowcast lead time",
    val groundTruthDataSource: String = "Planned external integration: Official KSNDMC AWS & BBMP Gauging Records"
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
