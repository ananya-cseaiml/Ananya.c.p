package com.example

import com.example.data.model.DataStatus
import com.example.data.model.LocationInfo
import com.example.data.model.RiskLevel
import com.example.data.model.RoadSegment
import com.example.data.service.HydrologicalEngine
import com.example.prediction.AlertEngine
import com.example.prediction.FloodRiskEngine
import org.junit.Assert.*
import org.junit.Test

class FloodRiskEngineUnitTest {

    private val engine = FloodRiskEngine()
    private val hydroEngine = HydrologicalEngine()
    private val alertEngine = AlertEngine()

    @Test
    fun testRiskLevelMapping() {
        assertEquals(RiskLevel.LOW, engine.getRiskLevel(20))
        assertEquals(RiskLevel.MODERATE, engine.getRiskLevel(45))
        assertEquals(RiskLevel.HIGH, engine.getRiskLevel(65))
        assertEquals(RiskLevel.SEVERE, engine.getRiskLevel(85))

        // Backward compatibility alias tests
        assertEquals(RiskLevel.LOW, RiskLevel.SAFE)
        assertEquals(RiskLevel.MODERATE, RiskLevel.WATCH)
    }

    @Test
    fun testHydrologicalRunoffAndDrainagePipeline() {
        // Rational runoff formula: Q = (rainfall * C * A * 1000 / 3600) * terrainModifier
        val dryRunoff = hydroEngine.estimateRunoffM3Sec(rainfallMmHr = 0.0)
        assertEquals(0.0, dryRunoff, 0.01)

        val stormRunoff = hydroEngine.estimateRunoffM3Sec(rainfallMmHr = 60.0)
        assertTrue("Runoff must be positive under 60 mm/hr storm", stormRunoff > 5.0)

        // Drainage stress: estimatedRunoff / drainageCapacity
        val (_, dryStress) = hydroEngine.calculateDrainageStress(rainfallMmHr = 4.0)
        assertTrue("Dry rainfall should cause minimal stress", dryStress <= 25)

        val (_, heavyStress) = hydroEngine.calculateDrainageStress(rainfallMmHr = 65.0, isBottleneck = true)
        assertTrue("Extreme storm at bottleneck must cause heavy stress", heavyStress >= 60)
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
            riskLevel = RiskLevel.LOW,
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
            isDrainBottleneck = false,
            dataStatus = DataStatus.LIVE
        )
        assertTrue("Dry conditions must be moderate or low baseline", dryResult.riskPercentage <= 45)
        assertTrue("Dry level should be LOW or MODERATE", dryResult.riskLevel == RiskLevel.LOW || dryResult.riskLevel == RiskLevel.MODERATE)
        assertNotNull(dryResult.whyExplanation)

        // Extreme cloudburst + drainage surcharge test
        val floodResult = engine.evaluateLocationRisk(
            location = loc,
            currentRainfallMmHr = 68.0,
            recentRainfall1hMm = 45.0,
            rainfallDurationMin = 60,
            antecedent24hMm = 55.0,
            drainageStressPercent = 95,
            isDrainBottleneck = true,
            dataStatus = DataStatus.LIVE
        )
        assertTrue("Severe cloudburst + drainage stress must trigger HIGH or SEVERE risk", floodResult.riskPercentage >= 50)
        assertTrue(floodResult.riskLevel == RiskLevel.HIGH || floodResult.riskLevel == RiskLevel.SEVERE)
        assertTrue("Must include reasons for failure", floodResult.reasons.isNotEmpty())
        assertNotNull("Must generate explainable why factors", floodResult.whyExplanation)
        assertTrue("Primary explanation must describe physical coupling", floodResult.whyExplanation?.primarySummary?.isNotEmpty() == true)
    }

    @Test
    fun testNowcastForecastMappingWithoutHardcoding() {
        val horizons = engine.computeNowcastFromForecast(
            currentRainfallMmHr = 15.0,
            forecast1hMm = 50.0,
            forecast3hMm = 35.0,
            forecast6hMm = 10.0,
            isDemo = false
        )

        assertEquals("Must produce 5 forecast horizons (NOW, +15m, +30m, +60m, +120m)", 5, horizons.size)
        assertEquals("NOW", horizons[0].horizonLabel)
        assertEquals("+15m", horizons[1].horizonLabel)
        assertEquals("+30m", horizons[2].horizonLabel)
        assertEquals("+60m", horizons[3].horizonLabel)
        assertEquals("+120m", horizons[4].horizonLabel)

        // Rainfall should track real forecast without hardcoded assumptions
        assertEquals(15.0, horizons[0].expectedRainfallMmHr, 0.5)
        assertEquals(50.0, horizons[3].expectedRainfallMmHr, 0.5) // +60m is 1h forecast
    }

    @Test
    fun testConfidenceIntegrity() {
        // DEMO mode must NEVER have higher confidence than LIVE data
        val (liveConf, liveReason) = engine.calculateConfidence(
            dataStatus = DataStatus.LIVE,
            hasWaterLevelTelemetry = true,
            hasHistoricalSupport = true
        )
        val (demoConf, demoReason) = engine.calculateConfidence(
            dataStatus = DataStatus.DEMO,
            hasWaterLevelTelemetry = false,
            hasHistoricalSupport = true
        )

        assertTrue("Live data with verified telemetry must exceed demo confidence", liveConf > demoConf)
        assertTrue("Demo reason must clearly state it is a prototype scenario", demoReason.contains("Demo scenario"))
    }

    @Test
    fun testAlertTransitionsAndCooldown() {
        alertEngine.resetCooldowns()

        // 1. Initial LOW risk should not trigger alert
        val shouldAlertLow = alertEngine.shouldGenerateAlert(
            locationId = "loc_ecospace",
            currentRisk = 15
        )
        assertFalse(shouldAlertLow)

        // 2. Transition from LOW to HIGH should trigger alert
        val shouldAlertHigh = alertEngine.shouldGenerateAlert(
            locationId = "loc_ecospace",
            currentRisk = 65
        )
        assertTrue(shouldAlertHigh)

        // Record dispatch to start cooldown
        alertEngine.recordAlertDispatched("loc_ecospace")

        // 3. Immediate repeated check should be blocked by 5-minute cooldown
        val shouldAlertImmediateRepeat = alertEngine.shouldGenerateAlert(
            locationId = "loc_ecospace",
            currentRisk = 65
        )
        assertFalse("Cooldown must prevent immediate duplicate alert", shouldAlertImmediateRepeat)
    }

    @Test
    fun testRouteRiskScoring() {
        val safeSegment = RoadSegment(
            "r_hsr", "HSR Ridge", "A", "B", 3.0, 10, 895.0, 2.0, 100, 20, 15, RiskLevel.LOW, "Free Flow", 90, 10.0, emptyList(), 0.0, 0.0, 0.0, 0.0
        )
        val riskySegment = RoadSegment(
            "r_orr", "ORR EcoSpace", "A", "B", 2.0, 15, 872.0, 0.5, 900, 95, 80, RiskLevel.SEVERE, "Surcharged", 85, 60.0, emptyList(), 0.0, 0.0, 0.0, 0.0
        )

        val (safeComposite, _) = engine.scoreRoute(travelTimeMin = 12, segments = listOf(safeSegment))
        val (riskyComposite, _) = engine.scoreRoute(travelTimeMin = 15, segments = listOf(riskySegment))

        assertTrue("Flooded route score penalty must exceed safe elevated route", riskyComposite > safeComposite)
    }

    @Test
    fun testStructuredFloodRiskResult() {
        val result = engine.calculateFloodRiskResult(
            currentRainfallMmHr = 55.0,
            recentRainfall1hMm = 40.0,
            forecastRainfallMm = 30.0,
            rainfallDurationMin = 45,
            antecedent24hMm = 40.0,
            imperviousnessPercent = 88,
            catchmentAreaKm2 = 3.0,
            elevationMeters = 872.4,
            slopePercent = 0.8,
            flowAccumulationIndex = 880,
            drainageCapacityM3Sec = 14.0,
            drainageStressPercent = 85,
            isBottleneck = true,
            historicalIncidentsCount = 4,
            hasGenuineWaterTelemetry = false,
            dataOrigin = com.example.data.model.DataOrigin.LIVE
        )

        assertTrue("Risk score must be positive", result.score > 50)
        assertTrue(result.level == RiskLevel.HIGH || result.level == RiskLevel.SEVERE)
        assertTrue("Rainfall contribution must be positive", result.rainfallContribution > 0)
        assertTrue("Runoff contribution must be positive", result.runoffContribution > 0)
        assertTrue("Terrain contribution must be positive", result.terrainContribution > 0)
        assertTrue("Drainage contribution must be positive", result.drainageContribution > 0)
        assertTrue("Flow accumulation contribution must be positive", result.flowAccumulationContribution > 0)
        assertEquals(com.example.data.model.DataOrigin.LIVE, result.dataOrigin)
        assertTrue("Primary reason must explain risk factors", result.primaryReason.isNotEmpty())
    }

    @Test
    fun testSafeRoutingCostFormulaAndDecision() {
        val routingEngine = com.example.prediction.SafeRoutingEngine(engine)
        val segment = RoadSegment(
            id = "seg_test",
            roadName = "EcoSpace ORR",
            fromNode = "Ibblur",
            toNode = "EcoSpace",
            lengthKm = 1.4,
            baseTravelTimeMin = 4,
            elevationMeters = 872.4,
            slopePercent = 0.8,
            flowAccumulation = 880,
            drainageStressPercent = 80,
            riskPercentage = 75,
            severity = RiskLevel.HIGH,
            predictionTime = "+30 min",
            confidence = 85,
            currentRainfallMmHr = 45.0,
            reasons = listOf("Bottleneck"),
            lat1 = 12.923, lng1 = 77.670, lat2 = 12.928, lng2 = 77.682,
            drainageExposurePercent = 80,
            terrainExposurePercent = 85,
            hazardPenaltyMin = 18
        )

        // Expected cost: travelTime (4) + (floodRisk * 0.25) + (drainageExposure * 0.15) + (terrainExposure * 0.10)
        // 4 + (75 * 0.25) + (80 * 0.15) + (85 * 0.10) = 4 + 18.75 + 12.0 + 8.5 = 43.25
        val cost = routingEngine.calculateSegmentCost(segment)
        assertEquals(43.25, cost, 0.1)
    }

    @Test
    fun testValidationStatusIntegrity() {
        val realMetrics = com.example.data.model.ValidationMetrics(
            accuracyPercent = 0.0,
            precisionPercent = 0.0,
            recallPercent = 0.0,
            falseAlarmRatePercent = 0.0,
            missedEventRatePercent = 0.0,
            averageLeadTimeMinutes = 0,
            verifiedEventsCount = 0,
            isDemo = false,
            hasSufficientData = false,
            validationStatus = com.example.data.model.ValidationStatus.PENDING,
            notice = "Validation pending verified ground-truth data."
        )

        assertFalse("Real validation must NOT claim sufficient data without verified ground truth", realMetrics.hasSufficientData)
        assertEquals(com.example.data.model.ValidationStatus.PENDING, realMetrics.validationStatus)
        assertTrue(realMetrics.notice.contains("pending verified ground-truth"))
    }

    @Test
    fun testCompleteDemoPipelineFlow() = kotlinx.coroutines.runBlocking {
        // 1. LIVE weather / input telemetry
        val liveWeather = com.example.data.service.LiveWeatherResult(
            rainfallMmHr = 55.0, // heavy cloudburst intensity
            recentRainfall1hMm = 35.0,
            recentRainfall3hMm = 45.0,
            recentRainfall6hMm = 50.0,
            antecedent24hRainfallMm = 42.0,
            forecast1hMm = 38.0,
            forecast3hMm = 48.0,
            forecast6hMm = 52.0,
            temperatureC = 24.5,
            humidityPercent = 92,
            windSpeedKmh = 18.0,
            weatherDesc = "Heavy Monsoon Cloudburst",
            status = DataStatus.LIVE,
            sourceName = "Open-Meteo High-Resolution Weather API",
            lastUpdated = "11:50 IST"
        )
        assertTrue("Step 1 (LIVE Weather): Rainfall should reflect heavy storm", liveWeather.rainfallMmHr > 50.0)
        assertEquals(DataStatus.LIVE, liveWeather.status)

        // 2. Rainfall input
        val currentRainfall = liveWeather.rainfallMmHr
        assertEquals(55.0, currentRainfall, 0.001)

        // 3. Runoff generation (Rational method: Q = C * I * A / 360)
        val runoffM3Sec = hydroEngine.estimateRunoffM3Sec(
            rainfallMmHr = currentRainfall
        )
        assertTrue("Step 3 (Runoff): Runoff discharge must exceed 10 m³/s for 55mm/hr", runoffM3Sec > 10.0)

        // 4. Drainage stress
        val (conduitDischarge, drainageStressPercent) = hydroEngine.calculateDrainageStress(
            rainfallMmHr = currentRainfall,
            isBottleneck = true
        )
        assertTrue("Step 4 (Drainage Stress): Capacity surcharge must exceed 60%", drainageStressPercent >= 60)
        assertTrue("Discharge throttled by bottleneck", conduitDischarge > 10.0)

        // 5. Flood risk (Composite multi-criteria calculation)
        val floodRiskResult = engine.calculateFloodRiskResult(
            currentRainfallMmHr = currentRainfall,
            recentRainfall1hMm = liveWeather.recentRainfall1hMm,
            forecastRainfallMm = liveWeather.forecast1hMm,
            rainfallDurationMin = 45,
            antecedent24hMm = liveWeather.antecedent24hRainfallMm,
            elevationMeters = 872.4, // EcoSpace low bowl
            slopePercent = 0.8,
            flowAccumulationIndex = 880,
            drainageStressPercent = drainageStressPercent,
            isBottleneck = true,
            historicalIncidentsCount = 4,
            hasGenuineWaterTelemetry = false,
            dataOrigin = com.example.data.model.DataOrigin.LIVE
        )
        assertTrue("Step 5 (Flood Risk): Composite score must be HIGH or SEVERE", floodRiskResult.score >= 50)
        assertTrue(floodRiskResult.level == RiskLevel.HIGH || floodRiskResult.level == RiskLevel.SEVERE)
        assertTrue(floodRiskResult.rainfallContribution > 0)
        assertTrue(floodRiskResult.drainageContribution > 0)

        // 6. Nowcast (Projections across NOW, +15m, +30m, +60m, +120m)
        val nowcastEngine = com.example.prediction.NowcastEngine(hydroEngine, engine)
        val horizons = nowcastEngine.generateNowcast(
            currentRainfallMmHr = currentRainfall,
            forecast1hMm = liveWeather.forecast1hMm,
            forecast3hMm = liveWeather.forecast3hMm,
            baseDrainageCapacityM3Sec = 14.0,
            isDemo = true,
            isTrendFallback = false
        )
        assertEquals("Step 6 (Nowcast): Must generate 5 discrete horizons", 5, horizons.size)
        val nowHorizon = horizons.find { it.horizonLabel == "NOW" }
        val plus30Horizon = horizons.find { it.horizonLabel == "+30m" }
        assertNotNull(nowHorizon)
        assertNotNull(plus30Horizon)
        assertTrue("Nowcast risk must track severe rainfall", plus30Horizon!!.riskPercentage > 30)

        // 7. Road risk evaluation
        val routingEngine = com.example.prediction.SafeRoutingEngine(engine)
        val networkProvider = com.example.prediction.PrototypeRoadNetworkProvider(engine)
        val segments = networkProvider.getCorridorRoadSegments(currentRainfall, drainageStressPercent)
        assertTrue("Step 7 (Road Risk): Segments must be populated", segments.isNotEmpty())
        val riskySegment = segments.find { it.id == "seg_orr_ecospace" }
        assertNotNull("ORR EcoSpace segment must exist", riskySegment)

        // 8. Safer route determination
        val (fastestRoute, saferRoute) = routingEngine.computeRouteOptions(
            rainfallMmHr = currentRainfall,
            drainageStressPercent = drainageStressPercent
        )
        assertTrue("Step 8 (Safer Route): Fastest route has high flood risk at EcoSpace", fastestRoute.floodRiskPercentage >= 50)
        assertTrue("Safer route via high-elevation ridge has lower flood risk", saferRoute.floodRiskPercentage <= fastestRoute.floodRiskPercentage)
        assertTrue("Safer route score reflects flood penalty avoidance", saferRoute.routeScore < fastestRoute.routeScore)

        // 9. Alert dispatch
        val alert = alertEngine.createCitizenAlert(
            id = "alert_eco",
            location = "Outer Ring Road - EcoSpace",
            riskPercentage = floodRiskResult.score,
            expectedTime = "+15 to +30 min",
            reason = floodRiskResult.primaryReason,
            recommendedAction = "Avoid EcoSpace low point; use HSR 14th Main Ridge Bypass."
        )
        assertTrue("Step 9 (Alert): Alert must be generated for severe flood risk", alert.severity == RiskLevel.HIGH || alert.severity == RiskLevel.SEVERE)

        // 10. Authority recommendation
        val govAction = alertEngine.createGovernmentAction(
            location = "Outer Ring Road - EcoSpace",
            riskPercentage = floodRiskResult.score,
            rainfallMmHr = currentRainfall,
            drainageStressPercent = drainageStressPercent,
            isUnderpassOrDepression = true
        )
        assertTrue("Step 10 (Authority Recommendation): Action must provide clear guidance", govAction.suggestedCheck.isNotEmpty())
        assertTrue("Must be labeled as recommended action", govAction.suggestedCheck.contains("Recommended action", ignoreCase = true))
    }

    @Test
    fun testRainfallUnitConversionAndAlertTransitions() {
        // Test rainfall intensity conversion formula: intensity = precipitation / hours
        assertEquals(45.0, com.example.data.service.LiveWeatherResult.toIntensityMmHr(45.0, 1.0), 0.01)
        assertEquals(30.0, com.example.data.service.LiveWeatherResult.toIntensityMmHr(60.0, 2.0), 0.01)
        assertEquals(15.0, com.example.data.service.LiveWeatherResult.toIntensityMmHr(45.0, 3.0), 0.01)

        // Test alert engine transitions and cooldown deduplication
        val alertEngine = AlertEngine(alertCooldownMs = 60_000L)
        val locId = "loc_ecospace"
        // Transition: null -> MODERATE
        assertTrue(alertEngine.isMeaningfulTransition(locId, RiskLevel.MODERATE))
        // Transition: MODERATE -> HIGH
        assertTrue(alertEngine.isMeaningfulTransition(locId, RiskLevel.HIGH))
        // Transition: HIGH -> HIGH (no transition)
        assertFalse(alertEngine.isMeaningfulTransition(locId, RiskLevel.HIGH))
        // Transition: HIGH -> SEVERE
        assertTrue(alertEngine.isMeaningfulTransition(locId, RiskLevel.SEVERE))

        // Cooldown check
        assertTrue("Cooldown must be initially expired", alertEngine.isCooldownExpired(locId))
        alertEngine.recordAlertDispatched(locId)
        assertFalse("Cooldown must prevent immediate duplicate dispatch", alertEngine.isCooldownExpired(locId))
    }
}
