package com.example.data.service

import com.example.data.model.HistoricalEvent

/**
 * Terrain data abstraction providing elevation, slope, flow accumulation, and catchment context.
 * Clearly isolates static prototype GIS layers from future live spatial APIs.
 */
interface TerrainDataProvider {
    fun getElevationMeters(lat: Double, lng: Double): Double
    fun getSlopePercent(lat: Double, lng: Double): Double
    fun getFlowAccumulationIndex(lat: Double, lng: Double): Int
    fun getCatchmentBasinName(lat: Double, lng: Double): String
    fun isNaturalDepressionSink(lat: Double, lng: Double): Boolean
    fun getDataSourceName(): String
    val isDemo: Boolean
}

class StaticGisTerrainDataProvider : TerrainDataProvider {
    override val isDemo: Boolean = false
    override fun getDataSourceName(): String = "Prototype static GIS-derived terrain layer"

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

    override fun getCatchmentBasinName(lat: Double, lng: Double): String {
        return when {
            lat in 12.924..12.932 && lng in 77.670..77.690 -> "Bellandur East Inflow Sub-catchment"
            lat in 12.912..12.920 && lng in 77.680..77.698 -> "Rainbow Drive / Sarjapur Basin"
            lat in 12.910..12.924 && lng in 77.640..77.665 -> "Agara Lake Upstream Ridge Basin"
            else -> "Bellandur–Agara Inter-lake Corridor"
        }
    }

    override fun isNaturalDepressionSink(lat: Double, lng: Double): Boolean {
        return getElevationMeters(lat, lng) <= 874.0
    }
}

class DemoTerrainDataProvider : TerrainDataProvider {
    override val isDemo: Boolean = true
    override fun getDataSourceName(): String = "DEMO SCENARIO — Synthetic Terrain Profile"

    override fun getElevationMeters(lat: Double, lng: Double): Double = 872.4
    override fun getSlopePercent(lat: Double, lng: Double): Double = 0.8
    override fun getFlowAccumulationIndex(lat: Double, lng: Double): Int = 850
    override fun getCatchmentBasinName(lat: Double, lng: Double): String = "EcoSpace SEZ Low-Lying Depression Bowl"
    override fun isNaturalDepressionSink(lat: Double, lng: Double): Boolean = true
}

/**
 * Historical flood ground truth abstraction
 */
interface HistoricalFloodDataProvider {
    fun getHistoricalEvents(): List<HistoricalEvent>
    fun getHotspotVulnerabilityScore(lat: Double, lng: Double): Int
    fun getDataSourceName(): String
    val isDemo: Boolean
}

class PrototypeHistoricalFloodDataProvider : HistoricalFloodDataProvider {
    override val isDemo: Boolean = true
    val dataOrigin: com.example.data.model.DataOrigin = com.example.data.model.DataOrigin.PROTOTYPE
    override fun getDataSourceName(): String = "Prototype historical benchmark scenarios (External KSNDMC/BBMP datasets planned for future validation)"

    override fun getHistoricalEvents(): List<HistoricalEvent> = listOf(
        HistoricalEvent("h_01", "05 Sep 2022", "Rainbow Drive, Sarjapur Road", 131.6, 92, "Inundation (1.4m depth) - Residential evacuation", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Heavy cloudburst; Rajakaluve breach confirmed.", com.example.data.model.DataOrigin.PROTOTYPE),
        HistoricalEvent("h_02", "19 Oct 2023", "ORR EcoSpace Low Point", 78.4, 76, "Severe Waterlogging (0.6m) - Traffic halted", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Culvert bottleneck throttled discharge to Bellandur lake.", com.example.data.model.DataOrigin.PROTOTYPE),
        HistoricalEvent("h_03", "21 May 2024", "Agara Junction Underpass", 52.0, 64, "Waterlogged (0.4m) - Sump motor overwhelmed", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Short-duration extreme intensity (42 mm in 35 min).", com.example.data.model.DataOrigin.PROTOTYPE),
        HistoricalEvent("h_04", "12 Aug 2023", "Ibblur Junction Sump", 38.0, 58, "Minor ponding (0.15m) - Traffic slowed", "DEMO OVER-PREDICTION SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Emergency desilting 2 days prior had improved capacity.", com.example.data.model.DataOrigin.PROTOTYPE),
        HistoricalEvent("h_05", "03 Jul 2024", "Bellandur Gate", 45.0, 32, "Dry - Camber drainage functioned properly", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Elevated section; gravity drainage safely discharged runoff.", com.example.data.model.DataOrigin.PROTOTYPE)
    )

    override fun getHotspotVulnerabilityScore(lat: Double, lng: Double): Int {
        return when {
            lat in 12.926..12.930 && lng in 77.678..77.685 -> 88 // EcoSpace recurring hotspot
            lat in 12.914..12.919 && lng in 77.686..77.693 -> 92 // Rainbow drive
            lat in 12.920..12.924 && lng in 77.665..77.673 -> 55 // Ibblur
            lat in 12.923..12.927 && lng in 77.648..77.655 -> 60 // Agara underpass
            else -> 20
        }
    }
}
