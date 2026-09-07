package com.example.data.service

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

interface DrainageDataProvider {
    fun getDrainageChannels(): List<DrainageChannelInfo>
    fun calculateHydraulicStress(drainId: String, inflowM3Sec: Double): Int
    fun isBottleneckThrottling(drainId: String, inflowM3Sec: Double): Boolean
    fun getDataSourceName(): String
    val isDemo: Boolean
}

class BbmpRajakaluveDrainageDataProvider : DrainageDataProvider {
    override val isDemo: Boolean = false
    override fun getDataSourceName(): String = "BBMP Master Drainage Plan & Survey GIS"

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
    override fun getDataSourceName(): String = "DEMO SCENARIO — Synthetic Hydrodynamic Drainage"

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
