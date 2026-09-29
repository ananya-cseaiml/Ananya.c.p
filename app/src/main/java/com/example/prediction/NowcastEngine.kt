package com.example.prediction

import com.example.data.model.NowcastHorizon
import com.example.data.model.RiskLevel
import com.example.data.service.HydrologicalEngine
import kotlin.math.max

/**
 * Authoritative Nowcasting Engine.
 * Single authoritative calculation path for short-term urban flood risk projections:
 * Open-Meteo forecast → NowcastEngine (15m, 30m, 60m, 120m) → FloodRiskEngine (consistent risk scoring).
 *
 * Guarantees:
 * 1. Single source of truth for all nowcast horizons.
 * 2. No arbitrary hardcoded future rainfall arrays for live mode.
 * 3. Clear labeling of trend-based fallbacks as "Prototype forecast fallback".
 * 4. Distinct demo confidence labeling.
 */
class NowcastEngine(
    private val hydrologicalEngine: HydrologicalEngine = HydrologicalEngine(),
    private val riskEngine: FloodRiskEngine = FloodRiskEngine()
) {
    /**
     * Generates nowcast horizons from forecast inputs.
     *
     * @param currentRainfallMmHr Current surface rainfall rate in mm/h
     * @param forecast1hMm Forecast rainfall accumulation over the next 1 hour (from Open-Meteo or scenario)
     * @param forecast3hMm Forecast rainfall accumulation over the next 3 hours
     * @param forecast6hMm Forecast rainfall accumulation over the next 6 hours
     * @param baseDrainageCapacityM3Sec Design discharge capacity of primary SWD trunk
     * @param isDemo Whether the calculation is running under demo/synthetic scenario mode
     * @param isTrendFallback Whether this projection is using a mathematical trend fallback
     */
    fun generateNowcast(
        currentRainfallMmHr: Double,
        forecast1hMm: Double,
        forecast3hMm: Double,
        forecast6hMm: Double = forecast3hMm * 0.7,
        baseDrainageCapacityM3Sec: Double = 18.0,
        isDemo: Boolean = false,
        isTrendFallback: Boolean = false
    ): List<NowcastHorizon> {
        val horizons = mutableListOf<NowcastHorizon>()

        val fallbackPrefix = if (isTrendFallback) "[Prototype forecast fallback] " else ""
        val demoPrefix = if (isDemo) "[Demo Scenario] " else ""

        // Explicit rainfall intensity unit conversions:
        // Open-Meteo hourly precipitation is accumulation in millimetres (mm) for the forecast interval.
        // Formula: rainfallIntensity_mm_per_hour = precipitation_mm / intervalHours
        // 1-hour forecast interval: interval = 1.0 h -> intensity (mm/h) = forecast1hMm / 1.0
        val forecast1hIntensityMmHr = max(0.0, forecast1hMm / 1.0)

        // 1. NOW (0 min): current rainfall intensity in mm/h
        val (_, nowStress) = hydrologicalEngine.calculateDrainageStress(
            rainfallMmHr = currentRainfallMmHr,
            isBottleneck = true,
            drainageCapacityM3Sec = baseDrainageCapacityM3Sec
        )
        val nowRisk = riskEngine.calculateNowcastHorizonRisk(currentRainfallMmHr, nowStress, 874.0)
        horizons.add(
            NowcastHorizon(
                horizonLabel = "NOW",
                riskPercentage = nowRisk,
                riskLevel = riskEngine.getRiskLevel(nowRisk),
                confidence = if (isDemo) 52 else 85,
                expectedRainfallMmHr = Math.round(currentRainfallMmHr * 10.0) / 10.0, // Unit: mm/h
                predictedDrainageStressPercent = nowStress,
                recommendedReadinessLevel = if (nowRisk >= 50) "Decision support: Standby mobile pumps at EcoSpace" else "Routine monitoring",
                mainReasons = listOf(
                    if (isDemo) "Demo Scenario: Initial convective cloudburst phase"
                    else "Real-time meteorological observation & primary drainage state"
                )
            )
        )

        // 2. +15 Minutes: 15-minute short-term lag (25% progress toward 1h forecast intensity)
        // Unit: rainfall intensity in mm/h
        val rain15m = max(0.0, currentRainfallMmHr + (forecast1hIntensityMmHr - currentRainfallMmHr) * 0.25)
        val (_, stress15m) = hydrologicalEngine.calculateDrainageStress(
            rainfallMmHr = rain15m,
            isBottleneck = true,
            drainageCapacityM3Sec = baseDrainageCapacityM3Sec
        )
        val risk15m = riskEngine.calculateNowcastHorizonRisk(rain15m, stress15m, 874.0)
        horizons.add(
            NowcastHorizon(
                horizonLabel = "+15m",
                riskPercentage = risk15m,
                riskLevel = riskEngine.getRiskLevel(risk15m),
                confidence = if (isDemo) 50 else 82,
                expectedRainfallMmHr = Math.round(rain15m * 10.0) / 10.0, // Unit: mm/h
                predictedDrainageStressPercent = stress15m,
                recommendedReadinessLevel = if (risk15m >= 50) "Decision support: Pre-position traffic diversion signage" else "Monitor culvert intakes",
                mainReasons = listOf(
                    "${fallbackPrefix}${demoPrefix}Overland runoff arrival in primary SWD trunk conduits"
                )
            )
        )

        // 3. +30 Minutes: mid-hour projection (50% progress toward 1h forecast intensity)
        // Unit: rainfall intensity in mm/h
        val rain30m = max(0.0, currentRainfallMmHr + (forecast1hIntensityMmHr - currentRainfallMmHr) * 0.50)
        val (_, stress30m) = hydrologicalEngine.calculateDrainageStress(
            rainfallMmHr = rain30m,
            isBottleneck = true,
            drainageCapacityM3Sec = baseDrainageCapacityM3Sec
        )
        val risk30m = riskEngine.calculateNowcastHorizonRisk(rain30m, stress30m, 874.0)
        horizons.add(
            NowcastHorizon(
                horizonLabel = "+30m",
                riskPercentage = risk30m,
                riskLevel = riskEngine.getRiskLevel(risk30m),
                confidence = if (isDemo) 48 else 78,
                expectedRainfallMmHr = Math.round(rain30m * 10.0) / 10.0, // Unit: mm/h
                predictedDrainageStressPercent = stress30m,
                recommendedReadinessLevel = if (risk30m >= 50) "Decision support: Recommend diversion away from EcoSpace low point" else "Maintain standby pumps",
                mainReasons = listOf(
                    "${fallbackPrefix}${demoPrefix}Conduit surcharge and arterial depression back-ponding"
                )
            )
        )

        // 4. +60 Minutes: 1-hour forecast horizon intensity
        // Interval: 1 hour -> precipitation_mm / 1.0 h = intensity in mm/h
        val rain60m = forecast1hIntensityMmHr
        val (_, stress60m) = hydrologicalEngine.calculateDrainageStress(
            rainfallMmHr = rain60m,
            isBottleneck = true,
            drainageCapacityM3Sec = baseDrainageCapacityM3Sec
        )
        val risk60m = riskEngine.calculateNowcastHorizonRisk(rain60m, stress60m, 874.0)
        horizons.add(
            NowcastHorizon(
                horizonLabel = "+60m",
                riskPercentage = risk60m,
                riskLevel = riskEngine.getRiskLevel(risk60m),
                confidence = if (isDemo) 45 else 72,
                expectedRainfallMmHr = Math.round(rain60m * 10.0) / 10.0, // Unit: mm/h
                predictedDrainageStressPercent = stress60m,
                recommendedReadinessLevel = if (risk60m >= 50) "Decision support: Emergency dewatering deployment at bottlenecks" else "Heightened monitoring",
                mainReasons = listOf(
                    "${fallbackPrefix}${demoPrefix}Peak drainage surcharge and arterial road inundation risk"
                )
            )
        )

        // 5. +120 Minutes: 2-hour forecast projection derived from accumulated 3-hour precipitation
        // Interval between hour 1 and hour 3 is 2.0 hours.
        // rainfallIntensity_mm_per_hour = (forecast3hMm - forecast1hMm) / 2.0 hours
        val deltaHours1To3Mm = max(0.0, forecast3hMm - forecast1hMm)
        val rain120m = Math.round((deltaHours1To3Mm / 2.0) * 10.0) / 10.0 // Unit: mm/h
        val (_, stress120m) = hydrologicalEngine.calculateDrainageStress(
            rainfallMmHr = rain120m,
            isBottleneck = true,
            drainageCapacityM3Sec = baseDrainageCapacityM3Sec
        )
        val risk120m = riskEngine.calculateNowcastHorizonRisk(rain120m, stress120m, 874.0)
        horizons.add(
            NowcastHorizon(
                horizonLabel = "+120m",
                riskPercentage = risk120m,
                riskLevel = riskEngine.getRiskLevel(risk120m),
                confidence = if (isDemo) 40 else 64,
                expectedRainfallMmHr = rain120m, // Unit: mm/h
                predictedDrainageStressPercent = stress120m,
                recommendedReadinessLevel = if (risk120m >= 50) "Decision support: Inspect lake weir buffer retention" else "Gradual recession expected",
                mainReasons = listOf(
                    "${fallbackPrefix}${demoPrefix}Lake feeder backwater surcharge and depression pooling"
                )
            )
        )

        return horizons
    }
}
