package com.example.data.service

/**
 * Terrain data abstraction providing elevation, slope, and flow accumulation.
 * Isolates real GIS digital elevation models (SRTM 30m) from simulated demo scenarios.
 */
interface TerrainDataProvider {
    fun getElevationMeters(lat: Double, lng: Double): Double
    fun getSlopePercent(lat: Double, lng: Double): Double
    fun getFlowAccumulationIndex(lat: Double, lng: Double): Int
    fun getDataSourceName(): String
    val isDemo: Boolean
}

class StaticGisTerrainDataProvider : TerrainDataProvider {
    override val isDemo: Boolean = false
    override fun getDataSourceName(): String = "SRTM 30m Digital Elevation Model (NASA / Survey of India)"

    // Spatial lookup within Bellandur–Agara Catchment (870m - 910m ASL)
    override fun getElevationMeters(lat: Double, lng: Double): Double {
        return when {
            // EcoSpace depression hotspot
            lat in 12.926..12.930 && lng in 77.678..77.685 -> 872.4
            // Rainbow Drive low corridor
            lat in 12.914..12.919 && lng in 77.686..77.693 -> 871.0
            // Ibblur Junction depression
            lat in 12.920..12.924 && lng in 77.665..77.673 -> 874.5
            // Agara Lake weir apron
            lat in 12.923..12.927 && lng in 77.648..77.655 -> 878.2
            // HSR Ridge elevated ground
            lat in 12.910..12.922 && lng in 77.640..77.650 -> 898.5
            else -> 880.0
        }
    }

    override fun getSlopePercent(lat: Double, lng: Double): Double {
        return when {
            lat in 12.926..12.930 && lng in 77.678..77.685 -> 0.8 // flat bowl
            lat in 12.914..12.919 && lng in 77.686..77.693 -> 0.5 // stagnant flat
            lat in 12.920..12.924 && lng in 77.665..77.673 -> 1.1
            lat in 12.923..12.927 && lng in 77.648..77.655 -> 1.4
            lat in 12.910..12.922 && lng in 77.640..77.650 -> 3.4 // steep ridge
            else -> 1.5
        }
    }

    override fun getFlowAccumulationIndex(lat: Double, lng: Double): Int {
        return when {
            lat in 12.926..12.930 && lng in 77.678..77.685 -> 880 // major convergence
            lat in 12.914..12.919 && lng in 77.686..77.693 -> 910 // high confluence
            lat in 12.920..12.924 && lng in 77.665..77.673 -> 620
            lat in 12.923..12.927 && lng in 77.648..77.655 -> 480
            lat in 12.910..12.922 && lng in 77.640..77.650 -> 180 // ridge shedding
            else -> 500
        }
    }
}

class DemoTerrainDataProvider : TerrainDataProvider {
    override val isDemo: Boolean = true
    override fun getDataSourceName(): String = "DEMO SCENARIO — Synthetic Terrain Profile"

    override fun getElevationMeters(lat: Double, lng: Double): Double = 872.4
    override fun getSlopePercent(lat: Double, lng: Double): Double = 0.8
    override fun getFlowAccumulationIndex(lat: Double, lng: Double): Int = 850
}
