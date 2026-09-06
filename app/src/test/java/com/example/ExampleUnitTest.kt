package com.example

import com.example.data.model.LocationInfo
import com.example.data.model.RiskLevel
import com.example.data.model.RoadSegment
import com.example.prediction.FloodRiskEngine
import org.junit.Assert.*
import org.junit.Test

class FloodRiskEngineUnitTest {

    private val engine = FloodRiskEngine()

    @Test
    fun testRiskLevelMapping() {
        assertEquals(RiskLevel.SAFE, engine.getRiskLevel(20))
        assertEquals(RiskLevel.WATCH, engine.getRiskLevel(45))
        assertEquals(RiskLevel.HIGH, engine.getRiskLevel(75))
        assertEquals(RiskLevel.SEVERE, engine.getRiskLevel(90))
    }

    @Test
    fun testPhysicalRainfallAndDrainageCoupling() {
        val loc = LocationInfo(
            id = "loc_ecospace",
            name = "Outer Ring Road - EcoSpace",
            ward = "Ward 150 Bellandur",
            lat = 12.9278,
            lng = 77.6820,
            elevationMeters = 872.4,
            slopePercent = 0.8,
            flowAccumulationIndex = 850,
            imperviousnessPercent = 88,
            currentRisk = 20,
            riskLevel = RiskLevel.SAFE,
            predictedTimeWindow = "30-60 min",
            confidence = 85,
            whyAtRisk = "Culvert throttling",
            recommendedAction = "Deploy pumps",
            isVulnerableHotspot = true
        )

        // Baseline dry test
        val dryResult = engine.evaluateLocationRisk(
            location = loc,
            currentRainfallMmHr = 4.0,
            recentRainfall1hMm = 2.0,
            rainfallDurationMin = 15,
            antecedent24hMm = 10.0,
            drainageStressPercent = 20,
            isDrainBottleneck = false
        )
        assertTrue("Dry conditions must be moderate or low baseline", dryResult.riskPercentage <= 45)
        assertTrue("Dry conditions must be lower than flood conditions", dryResult.riskPercentage < 50)

        // Extreme cloudburst + drainage surcharge test
        val floodResult = engine.evaluateLocationRisk(
            location = loc,
            currentRainfallMmHr = 68.0,
            recentRainfall1hMm = 45.0,
            rainfallDurationMin = 60,
            antecedent24hMm = 55.0,
            drainageStressPercent = 95,
            isDrainBottleneck = true
        )
        assertTrue("Severe cloudburst + drainage stress must trigger HIGH or SEVERE risk", floodResult.riskPercentage >= 70)
        assertTrue(floodResult.riskLevel == RiskLevel.HIGH || floodResult.riskLevel == RiskLevel.SEVERE)
        assertTrue("Must include reasons for failure", floodResult.reasons.isNotEmpty())
    }

    @Test
    fun testRouteRiskScoring() {
        val safeSegment = RoadSegment(
            "r_hsr", "HSR Ridge", "A", "B", 3.0, 10, 895.0, 2.0, 100, 20, 15, RiskLevel.SAFE, "Free Flow", 90, 10.0, emptyList(), 0.0, 0.0, 0.0, 0.0
        )
        val riskySegment = RoadSegment(
            "r_orr", "ORR EcoSpace", "A", "B", 2.0, 15, 872.0, 0.5, 900, 95, 80, RiskLevel.HIGH, "Surcharged", 85, 60.0, emptyList(), 0.0, 0.0, 0.0, 0.0
        )

        val (safeComposite, _) = engine.scoreRoute(travelTimeMin = 12, segments = listOf(safeSegment))
        val (riskyComposite, _) = engine.scoreRoute(travelTimeMin = 15, segments = listOf(riskySegment))

        assertTrue("Flooded route score penalty must exceed safe elevated route", riskyComposite > safeComposite)
    }
}
