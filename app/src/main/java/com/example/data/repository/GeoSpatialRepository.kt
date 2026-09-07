package com.example.data.repository

import com.example.data.service.*

/**
 * GeoSpatialRepository
 * Manages spatial terrain and drainage data providers, isolating static GIS datasets
 * from simulated demo scenario pipelines, adhering to the physical coupling architecture.
 */
class GeoSpatialRepository(
    val staticTerrainProvider: TerrainDataProvider = StaticGisTerrainDataProvider(),
    val demoTerrainProvider: TerrainDataProvider = DemoTerrainDataProvider(),
    val realDrainageProvider: DrainageDataProvider = BbmpRajakaluveDrainageDataProvider(),
    val demoDrainageProvider: DrainageDataProvider = DemoDrainageDataProvider()
) {
    fun getTerrainProvider(isDemo: Boolean): TerrainDataProvider {
        return if (isDemo) demoTerrainProvider else staticTerrainProvider
    }

    fun getDrainageProvider(isDemo: Boolean): DrainageDataProvider {
        return if (isDemo) demoDrainageProvider else realDrainageProvider
    }

    /**
     * Computes overland runoff and hydraulic loading using the coupled Rational equation:
     * Q = C * I * A / 360
     * where:
     * C = Urban runoff coefficient (~0.85 for impervious Bengaluru corridor)
     * I = Rainfall intensity in mm/hr
     * A = Catchment area in hectares (1 km² = 100 ha)
     */
    fun computeCoupledDrainageLoading(
        rainfallMmHr: Double,
        drainId: String = "SWD-3",
        isDemo: Boolean = false
    ): Triple<Double, Double, Int> {
        val drainageProvider = getDrainageProvider(isDemo)
        val channel = drainageProvider.getDrainageChannels().find { it.id == drainId }
            ?: drainageProvider.getDrainageChannels().first()

        // Rational runoff calculation
        val urbanRunoffCoefficient = 0.85
        val catchmentAreaHectares = channel.catchmentAreaKm2 * 100.0
        val peakInflowM3Sec = (urbanRunoffCoefficient * rainfallMmHr * catchmentAreaHectares) / 360.0
        val roundedInflow = Math.round(peakInflowM3Sec * 10.0) / 10.0
        val stressPercent = drainageProvider.calculateHydraulicStress(drainId, roundedInflow)

        return Triple(roundedInflow, channel.designCapacityM3Sec, stressPercent)
    }
}
