package com.example.prediction

import com.example.data.model.RiskLevel
import com.example.data.model.RoadSegment
import com.example.data.model.RouteOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Road Network Data Provider abstraction.
 * Allows switching between static prototype road graphs and future live OSRM/GIS vector graphs.
 */
interface RoadNetworkProvider {
    fun getCorridorRoadSegments(rainfallMmHr: Double, drainageStressPercent: Int): List<RoadSegment>
    val isPrototype: Boolean
    val providerName: String
}

class PrototypeRoadNetworkProvider(
    private val riskEngine: FloodRiskEngine
) : RoadNetworkProvider {
    override val isPrototype: Boolean = true
    override val providerName: String = "Prototype road network"

    override fun getCorridorRoadSegments(rainfallMmHr: Double, drainageStressPercent: Int): List<RoadSegment> {
        val baseSegments = listOf(
            RoadSegment(
                id = "seg_orr_ecospace",
                roadName = "Outer Ring Road (Ibblur to EcoSpace Low Point)",
                fromNode = "Ibblur Junction",
                toNode = "EcoSpace SEZ Entry",
                lengthKm = 1.4,
                baseTravelTimeMin = 4,
                elevationMeters = 872.4, // Low depression
                slopePercent = 0.8,
                flowAccumulation = 880,
                drainageStressPercent = drainageStressPercent,
                riskPercentage = 25,
                severity = RiskLevel.LOW,
                predictionTime = "+15 to +45 min",
                confidence = 85,
                currentRainfallMmHr = rainfallMmHr,
                reasons = listOf("Low depression bottleneck adjacent to SWD culvert"),
                lat1 = 12.9230,
                lng1 = 77.6705,
                lat2 = 12.9280,
                lng2 = 77.6820,
                drainageExposurePercent = drainageStressPercent,
                terrainExposurePercent = 88,
                hazardPenaltyMin = (drainageStressPercent * 0.25).toInt()
            ),
            RoadSegment(
                id = "seg_sarjapur_rd",
                roadName = "Sarjapur Road (Agara to Bellandur Gate)",
                fromNode = "Agara Circle",
                toNode = "Bellandur Gate",
                lengthKm = 2.1,
                baseTravelTimeMin = 6,
                elevationMeters = 878.0,
                slopePercent = 1.2,
                flowAccumulation = 620,
                drainageStressPercent = (drainageStressPercent * 0.85).toInt(),
                riskPercentage = 20,
                severity = RiskLevel.LOW,
                predictionTime = "+30 min",
                confidence = 82,
                currentRainfallMmHr = rainfallMmHr,
                reasons = listOf("Moderate cross-slope; intermediate drainage discharge"),
                lat1 = 12.9248,
                lng1 = 77.6515,
                lat2 = 12.9225,
                lng2 = 77.6690,
                drainageExposurePercent = (drainageStressPercent * 0.85).toInt(),
                terrainExposurePercent = 45,
                hazardPenaltyMin = 2
            ),
            RoadSegment(
                id = "seg_hsr_ridge_bypass",
                roadName = "HSR 14th Main Ridge Bypass",
                fromNode = "Agara Flyover North",
                toNode = "Bellandur Outer Perimeter",
                lengthKm = 3.2,
                baseTravelTimeMin = 8,
                elevationMeters = 898.5, // High ridge ground
                slopePercent = 3.4,
                flowAccumulation = 180,
                drainageStressPercent = (drainageStressPercent * 0.35).toInt(),
                riskPercentage = 10,
                severity = RiskLevel.LOW,
                predictionTime = "+60 min",
                confidence = 88,
                currentRainfallMmHr = rainfallMmHr,
                reasons = listOf("Natural ridgeline elevation; rapid gravity storm runoff"),
                lat1 = 12.9210,
                lng1 = 77.6480,
                lat2 = 12.9140,
                lng2 = 77.6780,
                drainageExposurePercent = (drainageStressPercent * 0.35).toInt(),
                terrainExposurePercent = 10,
                hazardPenaltyMin = 0
            )
        )

        // Evaluate physical road risk dynamically using FloodRiskEngine as single source of truth
        return baseSegments.map { road ->
            riskEngine.calculateRoadRisk(road, rainfallMmHr, road.drainageStressPercent)
        }
    }
}

/**
 * Flood-Aware Safe Routing Engine.
 * Evaluates alternative travel routes using:
 * routeScore = travelTimeWeight * travelTime
 *            + floodExposureWeight * avgFloodRisk
 *            + maxRoadRiskWeight * maxRoadRisk
 *            + riskySegmentPenalty * countOfRiskySegments
 *
 * Guarantees that safest route != shortest route.
 * Transparently explains route decisions to citizen users.
 */
class SafeRoutingEngine(
    private val riskEngine: FloodRiskEngine = FloodRiskEngine(),
    private val roadNetworkProvider: RoadNetworkProvider = PrototypeRoadNetworkProvider(riskEngine)
) {
    // Configurable routing scoring weights and penalties
    var travelTimeWeight: Double = 1.0
    var floodExposureWeight: Double = 0.40
    var maximumRoadRiskWeight: Double = 0.30
    var riskySegmentPenalty: Double = 12.0
    var riskPenalty: Double = 0.25
    var drainagePenalty: Double = 0.15
    var terrainPenalty: Double = 0.10

    fun getBaselineSegments(
        rainfallMmHr: Double,
        drainageStressPercent: Int
    ): List<RoadSegment> = roadNetworkProvider.getCorridorRoadSegments(rainfallMmHr, drainageStressPercent)

    /**
     * Calculates route cost using:
     * routeCost = travelTime + (floodRisk * riskPenalty) + (drainageExposure * drainagePenalty) + (terrainExposure * terrainPenalty)
     */
    fun calculateSegmentCost(segment: RoadSegment): Double {
        return segment.travelTime +
                (segment.floodRisk * riskPenalty) +
                (segment.drainageExposure * drainagePenalty) +
                (segment.terrainExposure * terrainPenalty)
    }

    /**
     * Computes route options (FASTEST vs SAFER) with full explainability
     */
    suspend fun computeRouteOptions(
        originLat: Double = 12.9248,
        originLng: Double = 77.6515, // Agara Junction
        destLat: Double = 12.9280,
        destLng: Double = 77.6820,   // EcoSpace ORR
        rainfallMmHr: Double,
        drainageStressPercent: Int
    ): Pair<RouteOption, RouteOption> = withContext(Dispatchers.IO) {
        val evaluatedSegments = getBaselineSegments(rainfallMmHr, drainageStressPercent)

        val ecospaceSeg = evaluatedSegments.first { it.id == "seg_orr_ecospace" }
        val sarjapurSeg = evaluatedSegments.first { it.id == "seg_sarjapur_rd" }
        val ridgeSeg = evaluatedSegments.first { it.id == "seg_hsr_ridge_bypass" }

        // Route 1: Fastest Route (Direct arterial via Outer Ring Road through EcoSpace depression)
        val fastestSegments = listOf(sarjapurSeg, ecospaceSeg)
        val fastestDistance = 3.5
        val fastestTravelTime = 12 // minutes in normal traffic
        val fastestMaxRisk = max(sarjapurSeg.riskPercentage, ecospaceSeg.riskPercentage)
        val fastestAvgRisk = (sarjapurSeg.riskPercentage + ecospaceSeg.riskPercentage) / 2
        val fastestAvgDrainageExposure = (sarjapurSeg.drainageExposure + ecospaceSeg.drainageExposure) / 2
        val fastestAvgTerrainExposure = (sarjapurSeg.terrainExposure + ecospaceSeg.terrainExposure) / 2
        val fastestRiskyCount = fastestSegments.count { it.riskPercentage >= 50 }
        val fastestHazardPenalty = (fastestMaxRisk * 0.35 + fastestRiskyCount * 12).toInt()

        // Formula: routeCost = travelTime + (floodRisk * riskPenalty) + (drainageExposure * drainagePenalty) + (terrainExposure * terrainPenalty)
        val fastestScore = fastestTravelTime +
                (fastestMaxRisk * riskPenalty * 2.0) +
                (fastestAvgDrainageExposure * drainagePenalty) +
                (fastestAvgTerrainExposure * terrainPenalty) +
                (fastestRiskyCount * riskySegmentPenalty)

        val fastestCoordinates = listOf(
            Pair(12.9248, 77.6515), // Agara
            Pair(12.9230, 77.6705), // Ibblur
            Pair(12.9255, 77.6765), // ORR mid
            Pair(12.9278, 77.6820)  // EcoSpace low point
        )

        val fastestOption = RouteOption(
            routeType = "FASTEST",
            routeTitle = "Direct via Outer Ring Road (EcoSpace Low Point)",
            travelTimeMinutes = fastestTravelTime,
            distanceKm = fastestDistance,
            floodRiskPercentage = fastestMaxRisk,
            riskySegmentsCount = fastestRiskyCount,
            maximumSegmentRisk = fastestMaxRisk,
            averageSegmentRisk = fastestAvgRisk,
            floodRiskPenaltyMinutes = fastestHazardPenalty,
            recommendationNote = if (fastestMaxRisk >= 50) {
                "Fastest direct distance, but passes through the Outer Ring Road culvert bottleneck at 872m ASL which faces severe culvert surcharge ($fastestMaxRisk% risk)."
            } else {
                "Shortest direct route under current mild weather conditions."
            },
            segments = fastestSegments,
            pathCoordinates = fastestCoordinates,
            routeScore = fastestScore,
            bottlenecksAvoided = 0
        )

        // Route 2: Safer Route (High-Elevation HSR Ridge Bypass)
        val saferSegments = listOf(ridgeSeg)
        val saferDistance = 4.8
        val saferTravelTime = 17 // adds 5 minutes
        val saferMaxRisk = ridgeSeg.riskPercentage
        val saferAvgRisk = ridgeSeg.riskPercentage
        val saferDrainageExposure = ridgeSeg.drainageExposure
        val saferTerrainExposure = ridgeSeg.terrainExposure
        val saferRiskyCount = saferSegments.count { it.riskPercentage >= 50 }
        val saferHazardPenalty = (saferMaxRisk * 0.20).toInt()

        // Formula: routeCost = travelTime + (floodRisk * riskPenalty) + (drainageExposure * drainagePenalty) + (terrainExposure * terrainPenalty)
        val saferScore = saferTravelTime +
                (saferMaxRisk * riskPenalty) +
                (saferDrainageExposure * drainagePenalty) +
                (saferTerrainExposure * terrainPenalty) +
                (saferRiskyCount * riskySegmentPenalty)

        val saferCoordinates = listOf(
            Pair(12.9248, 77.6515), // Agara
            Pair(12.9210, 77.6480), // HSR 14th Main
            Pair(12.9150, 77.6620), // High Ridge Sector 2
            Pair(12.9140, 77.6780), // Bellandur Ridge Upper
            Pair(12.9280, 77.6820)  // EcoSpace elevated flyover entry
        )

        val timeDiff = saferTravelTime - fastestTravelTime
        val bottlenecksAvoided = if (fastestRiskyCount > 0) fastestRiskyCount else 1
        val saferExplanation = if (fastestMaxRisk >= 50) {
            "Selected Route B (Safer Route) because Route A (Fastest Route) has severe culvert surcharge at EcoSpace ($fastestMaxRisk% flood risk). Adds +$timeDiff min (+1.3 km) along high-elevation ridge (898m ASL), keeping risk to only ${saferMaxRisk}%."
        } else {
            "Alternative ridge corridor via HSR Layout elevated terrain (+${timeDiff} min, 898m ASL) avoiding intermediate roadside collection sumps."
        }

        val saferOption = RouteOption(
            routeType = "SAFER",
            routeTitle = "Flood-Safe via HSR Ridge Elevated Bypass",
            travelTimeMinutes = saferTravelTime,
            distanceKm = saferDistance,
            floodRiskPercentage = saferMaxRisk,
            riskySegmentsCount = saferRiskyCount,
            maximumSegmentRisk = saferMaxRisk,
            averageSegmentRisk = saferAvgRisk,
            floodRiskPenaltyMinutes = saferHazardPenalty,
            recommendationNote = saferExplanation,
            segments = saferSegments,
            pathCoordinates = saferCoordinates,
            routeScore = saferScore,
            bottlenecksAvoided = bottlenecksAvoided
        )

        Pair(fastestOption, saferOption)
    }
}
