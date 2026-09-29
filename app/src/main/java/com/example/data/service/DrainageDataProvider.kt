package com.example.data.service

import kotlin.math.max
import kotlin.math.min

data class DrainageChannelInfo(
    val id: String,
    val name: String,
    val designCapacityM3Sec: Double,
    val catchmentAreaKm2: Double,
    val lengthKm: Double,
    val isPrimaryRajakaluve: Boolean,
    val isBottleneckSensitive: Boolean,
    val downstreamReceivingWater: String
)

/**
 * Configurable parameters for the prototype hydrological rainfall-runoff and drainage stress model.
 * Note: Clearly identified as a prototype rainfall-runoff/drainage model, NOT a full hydrodynamic simulation.
 */
data class HydrologicalParameters(
    var runoffCoefficient: Double = 0.82,     // Impervious urban surface coefficient (C: 0.0 - 1.0)
    var catchmentAreaKm2: Double = 2.5,        // Primary corridor sub-catchment area A in km²
    var drainageCapacityM3Sec: Double = 14.0,  // Box culvert design hydraulic discharge capacity Q_cap in m³/s
    var terrainModifier: Double = 1.15,       // Low-lying depression surcharge factor
    var flowAccumulationModifier: Double = 1.10, // Catchment convergence concentration factor
    var bottleneckModifier: Double = 1.25      // Culvert bottleneck throttling multiplier
)

/**
 * Prototype Rainfall-Runoff & Hydraulic Stress Engine.
 * Implements the physical pipeline:
 * Rainfall intensity (mm/hr)
 *   → Runoff coefficient (C)
 *   → Catchment area (A)
 *   → Estimated runoff inflow Q_in = (rainfall * C * A * 1000 / 3600) * terrainModifier
 *   → Drainage capacity Q_cap
 *   → Drainage hydraulic stress = (Q_in / Q_cap) * bottleneckModifier * 100
 */
class HydrologicalEngine(
    val params: HydrologicalParameters = HydrologicalParameters()
) {
    /**
     * Calculates estimated peak stormwater runoff inflow in m³/s using rational formulation
     */
    fun estimateRunoffM3Sec(
        rainfallMmHr: Double,
        customRunoffCoeff: Double? = null,
        customCatchmentAreaKm2: Double? = null,
        terrainModifier: Double = params.terrainModifier,
        flowAccModifier: Double = params.flowAccumulationModifier
    ): Double {
        if (rainfallMmHr <= 0.0) return 0.0
        val c = customRunoffCoeff ?: params.runoffCoefficient
        val a = customCatchmentAreaKm2 ?: params.catchmentAreaKm2

        // Formula: Q (m³/s) = (Rainfall mm/hr * 10^-3 m/mm * Area km² * 10^6 m²/km²) / (3600 s/hr) * C
        // Conversion factor: 1000 / 3600 = 0.27778
        val baseRunoff = rainfallMmHr * c * a * (1000.0 / 3600.0)
        val modifiedRunoff = baseRunoff * terrainModifier * flowAccModifier
        return Math.round(modifiedRunoff * 100.0) / 100.0
    }

    /**
     * Calculates drainage hydraulic stress percentage: estimatedRunoff / drainageCapacity
     * Normalized to 0–100%.
     */
    fun calculateDrainageStress(
        rainfallMmHr: Double,
        isBottleneck: Boolean = false,
        drainageCapacityM3Sec: Double = params.drainageCapacityM3Sec,
        customRunoffCoeff: Double? = null,
        customCatchmentAreaKm2: Double? = null
    ): Pair<Double, Int> {
        val runoff = estimateRunoffM3Sec(
            rainfallMmHr = rainfallMmHr,
            customRunoffCoeff = customRunoffCoeff,
            customCatchmentAreaKm2 = customCatchmentAreaKm2
        )
        if (drainageCapacityM3Sec <= 0.0) return Pair(runoff, 100)

        val bottleneckFactor = if (isBottleneck) params.bottleneckModifier else 1.0
        val rawStress = (runoff / drainageCapacityM3Sec) * bottleneckFactor * 100.0
        val normalizedStress = min(100, max(5, rawStress.toInt()))

        return Pair(runoff, normalizedStress)
    }
}

interface DrainageDataProvider {
    fun getDrainageChannels(): List<DrainageChannelInfo>
    fun calculateHydraulicStress(drainId: String, inflowM3Sec: Double): Int
    fun isBottleneckThrottling(drainId: String, inflowM3Sec: Double): Boolean
    fun getDataSourceName(): String
    val isDemo: Boolean
}

class BbmpRajakaluvePrototypeDataProvider : DrainageDataProvider {
    override val isDemo: Boolean = false
    override fun getDataSourceName(): String = "STATIC PROTOTYPE DATA: PROTOTYPE BBMP RAJAKALUVE NETWORK"

    private val channels = listOf(
        DrainageChannelInfo(
            id = "SWD-1",
            name = "Agara Overflow Trunk Canal",
            designCapacityM3Sec = 18.0,
            catchmentAreaKm2 = 14.5,
            lengthKm = 3.2,
            isPrimaryRajakaluve = true,
            isBottleneckSensitive = false,
            downstreamReceivingWater = "Agara Lake to Bellandur Lake link"
        ),
        DrainageChannelInfo(
            id = "SWD-3",
            name = "Outer Ring Road EcoSpace Box Culvert",
            designCapacityM3Sec = 14.0,
            catchmentAreaKm2 = 9.8,
            lengthKm = 1.6,
            isPrimaryRajakaluve = true,
            isBottleneckSensitive = true,
            downstreamReceivingWater = "Bellandur Central Inflow Weir"
        ),
        DrainageChannelInfo(
            id = "SWD-5",
            name = "Rainbow Drive / Sarjapur Secondary Feeder",
            designCapacityM3Sec = 8.5,
            catchmentAreaKm2 = 5.2,
            lengthKm = 2.1,
            isPrimaryRajakaluve = false,
            isBottleneckSensitive = true,
            downstreamReceivingWater = "Kaikondrahalli Lake Outfall"
        ),
        DrainageChannelInfo(
            id = "SWD-2",
            name = "Ibblur Junction Storm Drain Conduit",
            designCapacityM3Sec = 11.0,
            catchmentAreaKm2 = 6.4,
            lengthKm = 1.9,
            isPrimaryRajakaluve = true,
            isBottleneckSensitive = true,
            downstreamReceivingWater = "Ibblur Lake Retention Basin"
        )
    )

    override fun getDrainageChannels(): List<DrainageChannelInfo> = channels

    override fun calculateHydraulicStress(drainId: String, inflowM3Sec: Double): Int {
        val channel = channels.find { it.id == drainId } ?: channels[1]
        val stressRatio = (inflowM3Sec / channel.designCapacityM3Sec) * 100.0
        return stressRatio.coerceIn(5.0, 100.0).toInt()
    }

    override fun isBottleneckThrottling(drainId: String, inflowM3Sec: Double): Boolean {
        val channel = channels.find { it.id == drainId } ?: return false
        return channel.isBottleneckSensitive && (inflowM3Sec > channel.designCapacityM3Sec * 0.75)
    }
}

class DemoDrainageDataProvider : DrainageDataProvider {
    override val isDemo: Boolean = true
    override fun getDataSourceName(): String = "DEMO SCENARIO — Prototype Hydrodynamic Drainage"

    private val demoChannels = listOf(
        DrainageChannelInfo("SWD-1", "Agara Overflow Trunk Canal", 18.0, 14.5, 3.2, true, false, "Bellandur Lake"),
        DrainageChannelInfo("SWD-3", "EcoSpace Box Culvert (Bottleneck)", 14.0, 9.8, 1.6, true, true, "Bellandur Central"),
        DrainageChannelInfo("SWD-5", "Rainbow Drive Feeder", 8.5, 5.2, 2.1, false, true, "Kaikondrahalli Outfall")
    )

    override fun getDrainageChannels(): List<DrainageChannelInfo> = demoChannels

    override fun calculateHydraulicStress(drainId: String, inflowM3Sec: Double): Int {
        val channel = demoChannels.find { it.id == drainId } ?: demoChannels[1]
        return ((inflowM3Sec / channel.designCapacityM3Sec) * 100.0).coerceIn(10.0, 98.0).toInt()
    }

    override fun isBottleneckThrottling(drainId: String, inflowM3Sec: Double): Boolean {
        return drainId == "SWD-3" && inflowM3Sec > 10.5
    }
}
