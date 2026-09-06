package com.example.data.service

import com.example.data.model.*

enum class ScenarioStep(
    val stepNumber: Int,
    val title: String,
    val description: String
) {
    NORMAL(1, "1. Normal Baseline", "Dry conditions, baseline urban drainage at 20% capacity, roads safe."),
    RAINFALL_INCREASES(2, "2. Rainfall Increases", "Convective cloudburst strikes Bellandur basin: 15 mm/hr -> 48 mm/hr."),
    RUNOFF_INCREASES(3, "3. Runoff Increases", "Impervious asphalt and urban surfaces convert 85% of rainfall to rapid overland runoff."),
    DRAINAGE_LOAD_INCREASES(4, "4. Drainage Load Increases", "Rajakaluve trunk channels SWD-1 and SWD-3 receive surging stormwater inflows."),
    DRAINAGE_STRESS_INCREASES(5, "5. Drainage Stress Increases", "Culvert bottlenecks at Outer Ring Road near EcoSpace reach 92% hydraulic surcharge."),
    FLOOD_RISK_INCREASES(6, "6. Flood Risk Increases", "Hydraulic model projects water accumulation in low-lying depressions (872m ASL)."),
    ROAD_BECOMES_HIGH_RISK(7, "7. Road Becomes HIGH Risk", "Outer Ring Road (EcoSpace low point) predicted 78% HIGH flood risk in 30-60 min."),
    PUBLIC_ALERT(8, "8. Public Alert Issued", "Citizen alert dispatched: High flood risk on ORR EcoSpace, avoid corridor."),
    SAFER_ROUTE(9, "9. Safer Route Prioritized", "Navigation routes update: HSR Ridge bypass recommended over flooded ORR."),
    AUTHORITY_RESPONSE(10, "10. Authority Response", "BBMP & Traffic Police notified: Deploy dewatering pumps and initiate traffic diversion.");

    fun next(): ScenarioStep {
        val nextOrdinal = (ordinal + 1).coerceAtMost(values().size - 1)
        return values()[nextOrdinal]
    }

    fun previous(): ScenarioStep {
        val prevOrdinal = (ordinal - 1).coerceAtLeast(0)
        return values()[prevOrdinal]
    }
}

data class ScenarioState(
    val currentStep: ScenarioStep = ScenarioStep.NORMAL,
    val rainfallMmHr: Double = 4.0,
    val antecedentRainfallMm: Double = 12.0,
    val runoffStatus: String = "Low",
    val drainageLoadM3Sec: Double = 6.2,
    val drainageCapacityM3Sec: Double = 32.0,
    val drainageStressPercent: Int = 19,
    val flowAccumulationIndex: Int = 240,
    val overallFloodRiskPercent: Int = 18,
    val overallRiskLevel: RiskLevel = RiskLevel.SAFE,
    val highRiskRoadsCount: Int = 0,
    val activeAlerts: List<FloodAlert> = emptyList(),
    val locations: List<LocationInfo> = emptyList(),
    val roads: List<RoadSegment> = emptyList(),
    val routes: List<RouteOption> = emptyList(),
    val governmentActions: List<GovernmentActionItem> = emptyList()
)

class DemoScenarioEngine {

    fun getStateForStep(step: ScenarioStep): ScenarioState {
        return when (step) {
            ScenarioStep.NORMAL -> createNormalState()
            ScenarioStep.RAINFALL_INCREASES -> createRainfallState()
            ScenarioStep.RUNOFF_INCREASES -> createRunoffState()
            ScenarioStep.DRAINAGE_LOAD_INCREASES -> createDrainageLoadState()
            ScenarioStep.DRAINAGE_STRESS_INCREASES -> createDrainageStressState()
            ScenarioStep.FLOOD_RISK_INCREASES -> createFloodRiskState()
            ScenarioStep.ROAD_BECOMES_HIGH_RISK -> createRoadHighRiskState()
            ScenarioStep.PUBLIC_ALERT -> createPublicAlertState()
            ScenarioStep.SAFER_ROUTE -> createSaferRouteState()
            ScenarioStep.AUTHORITY_RESPONSE -> createAuthorityResponseState()
        }
    }

    private fun createNormalState(): ScenarioState {
        val locations = listOf(
            LocationInfo("loc_ecospace", "Outer Ring Road - EcoSpace", "Ward 150 Bellandur", 12.9278, 77.6820, 872.4, 0.8, 850, 88, 22, RiskLevel.SAFE, "Dry", 86, "Normal dry weather baseline", "No action needed"),
            LocationInfo("loc_agara", "Agara Junction Underpass", "Ward 174 HSR Layout", 12.9248, 77.6515, 878.2, 1.4, 480, 80, 18, RiskLevel.SAFE, "Dry", 89, "Free flowing drainage", "Routine check"),
            LocationInfo("loc_rainbow", "Rainbow Drive / Sarjapur Rd", "Ward 150 Bellandur", 12.9160, 77.6890, 871.0, 0.5, 910, 92, 28, RiskLevel.SAFE, "Dry", 82, "Encroached channel currently clear", "Routine check"),
            LocationInfo("loc_ibblur", "Ibblur Junction Sump", "Ward 174 HSR Layout", 12.9220, 77.6690, 874.5, 1.1, 620, 85, 20, RiskLevel.SAFE, "Dry", 85, "Retention sump dry", "Routine check")
        )

        val roads = listOf(
            RoadSegment("r_orr_1", "Outer Ring Road (Ibblur-EcoSpace)", "Ibblur", "EcoSpace", 2.2, 5, 872.4, 0.8, 850, 22, 22, RiskLevel.SAFE, "Normal", 88, 4.0, listOf("Dry road camber"), 12.9230, 77.6705, 12.9280, 77.6820),
            RoadSegment("r_sarjapur", "Sarjapur Main Road", "Agara", "Ibblur", 2.8, 7, 878.0, 1.2, 420, 18, 16, RiskLevel.SAFE, "Normal", 85, 4.0, listOf("Clear side drains"), 12.9248, 77.6515, 12.9225, 77.6690),
            RoadSegment("r_hsr_ridge", "HSR 14th Main Ridge Bypass", "Agara", "Bellandur Gate", 3.4, 9, 895.0, 3.2, 120, 15, 12, RiskLevel.SAFE, "Normal", 92, 4.0, listOf("Ridge line elevated profile"), 12.9210, 77.6480, 12.9140, 77.6780)
        )

        return ScenarioState(
            currentStep = ScenarioStep.NORMAL,
            rainfallMmHr = 4.0,
            antecedentRainfallMm = 12.0,
            runoffStatus = "Low (0.8 m³/s)",
            drainageLoadM3Sec = 6.2,
            drainageCapacityM3Sec = 32.0,
            drainageStressPercent = 19,
            flowAccumulationIndex = 240,
            overallFloodRiskPercent = 18,
            overallRiskLevel = RiskLevel.SAFE,
            highRiskRoadsCount = 0,
            activeAlerts = emptyList(),
            locations = locations,
            roads = roads,
            routes = createRoutes(orrRisk = 22, hsrRisk = 12, orrSever = RiskLevel.SAFE),
            governmentActions = emptyList()
        )
    }

    private fun createRainfallState(): ScenarioState {
        val base = createNormalState()
        return base.copy(
            currentStep = ScenarioStep.RAINFALL_INCREASES,
            rainfallMmHr = 48.5,
            antecedentRainfallMm = 38.0,
            runoffStatus = "Moderate (6.4 m³/s)",
            drainageLoadM3Sec = 14.5,
            drainageStressPercent = 45,
            overallFloodRiskPercent = 44,
            overallRiskLevel = RiskLevel.WATCH
        )
    }

    private fun createRunoffState(): ScenarioState {
        val base = createRainfallState()
        return base.copy(
            currentStep = ScenarioStep.RUNOFF_INCREASES,
            rainfallMmHr = 62.0,
            runoffStatus = "High (18.2 m³/s - Impervious surge)",
            drainageLoadM3Sec = 22.0,
            drainageStressPercent = 68,
            flowAccumulationIndex = 580,
            overallFloodRiskPercent = 58,
            overallRiskLevel = RiskLevel.WATCH
        )
    }

    private fun createDrainageLoadState(): ScenarioState {
        val base = createRunoffState()
        return base.copy(
            currentStep = ScenarioStep.DRAINAGE_LOAD_INCREASES,
            drainageLoadM3Sec = 27.8,
            drainageStressPercent = 86,
            overallFloodRiskPercent = 68,
            overallRiskLevel = RiskLevel.HIGH
        )
    }

    private fun createDrainageStressState(): ScenarioState {
        val base = createDrainageLoadState()
        return base.copy(
            currentStep = ScenarioStep.DRAINAGE_STRESS_INCREASES,
            drainageLoadM3Sec = 30.5,
            drainageStressPercent = 95,
            flowAccumulationIndex = 820,
            overallFloodRiskPercent = 75,
            overallRiskLevel = RiskLevel.HIGH
        )
    }

    private fun createFloodRiskState(): ScenarioState {
        val base = createDrainageStressState()
        return base.copy(
            currentStep = ScenarioStep.FLOOD_RISK_INCREASES,
            overallFloodRiskPercent = 79,
            overallRiskLevel = RiskLevel.HIGH
        )
    }

    private fun createRoadHighRiskState(): ScenarioState {
        val locations = listOf(
            LocationInfo("loc_ecospace", "Outer Ring Road - EcoSpace", "Ward 150 Bellandur", 12.9278, 77.6820, 872.4, 0.8, 850, 88, 78, RiskLevel.HIGH, "30-60 min", 84, "Severe culvert throttling + intense surface runoff from Sarjapur slope", "Deploy 50HP dewatering pumps; issue traffic caution", true),
            LocationInfo("loc_agara", "Agara Junction Underpass", "Ward 174 HSR Layout", 12.9248, 77.6515, 878.2, 1.4, 480, 80, 52, RiskLevel.WATCH, "45-60 min", 79, "Depression pooling at underpass ingress", "Activate sump motor #2", true),
            LocationInfo("loc_rainbow", "Rainbow Drive / Sarjapur Rd", "Ward 150 Bellandur", 12.9160, 77.6890, 871.0, 0.5, 910, 92, 85, RiskLevel.SEVERE, "15-30 min", 91, "Channel encroachment bottleneck with 98% surcharge", "Dispatch quick response personnel; close entry ramp", true),
            LocationInfo("loc_ibblur", "Ibblur Junction Sump", "Ward 174 HSR Layout", 12.9220, 77.6690, 874.5, 1.1, 620, 85, 64, RiskLevel.HIGH, "30-45 min", 81, "Trunk SWD backflow into secondary storm drain", "Inspect barrier gates", true)
        )

        val roads = listOf(
            RoadSegment("r_orr_1", "Outer Ring Road (Ibblur-EcoSpace)", "Ibblur", "EcoSpace", 2.2, 18, 872.4, 0.8, 850, 95, 78, RiskLevel.HIGH, "30-60 min", 85, 68.0, listOf("Heavy rainfall (68 mm/hr)", "High flow accumulation in depression", "Drainage stress 95% near culvert"), 12.9230, 77.6705, 12.9280, 77.6820),
            RoadSegment("r_sarjapur", "Sarjapur Main Road", "Agara", "Ibblur", 2.8, 12, 878.0, 1.2, 420, 68, 54, RiskLevel.WATCH, "45-60 min", 80, 68.0, listOf("Moderate pooling at roadside culverts"), 12.9248, 77.6515, 12.9225, 77.6690),
            RoadSegment("r_hsr_ridge", "HSR 14th Main Ridge Bypass", "Agara", "Bellandur Gate", 3.4, 11, 895.0, 3.2, 120, 24, 18, RiskLevel.SAFE, "Free Flow", 94, 68.0, listOf("Elevated ridge topography; gravity drainage unimpeded"), 12.9210, 77.6480, 12.9140, 77.6780)
        )

        return ScenarioState(
            currentStep = ScenarioStep.ROAD_BECOMES_HIGH_RISK,
            rainfallMmHr = 68.0,
            antecedentRainfallMm = 45.0,
            runoffStatus = "Severe (26.5 m³/s)",
            drainageLoadM3Sec = 31.8,
            drainageCapacityM3Sec = 32.0,
            drainageStressPercent = 98,
            flowAccumulationIndex = 880,
            overallFloodRiskPercent = 82,
            overallRiskLevel = RiskLevel.SEVERE,
            highRiskRoadsCount = 2,
            activeAlerts = emptyList(),
            locations = locations,
            roads = roads,
            routes = createRoutes(orrRisk = 78, hsrRisk = 18, orrSever = RiskLevel.HIGH),
            governmentActions = emptyList()
        )
    }

    private fun createPublicAlertState(): ScenarioState {
        val base = createRoadHighRiskState()
        val alerts = listOf(
            FloodAlert(
                id = "alt_001",
                location = "Bellandur–Agara / ORR EcoSpace",
                severity = RiskLevel.HIGH,
                riskPercentage = 78,
                issuedAt = "Just now",
                expectedTime = "30–60 minutes",
                reason = "Heavy rainfall (68 mm/hr) + drainage stress (98%) at EcoSpace culvert bottleneck.",
                recommendedAction = "Avoid Outer Ring Road between Ibblur and EcoSpace. Use HSR Ridge diversion."
            ),
            FloodAlert(
                id = "alt_002",
                location = "Rainbow Drive / Sarjapur Road",
                severity = RiskLevel.SEVERE,
                riskPercentage = 85,
                issuedAt = "5 min ago",
                expectedTime = "15–30 minutes",
                reason = "Severe drain encroachment + historical low depression waterlogging.",
                recommendedAction = "Do not attempt to drive through underpasses or ground-level basement entrances."
            )
        )
        return base.copy(
            currentStep = ScenarioStep.PUBLIC_ALERT,
            activeAlerts = alerts
        )
    }

    private fun createSaferRouteState(): ScenarioState {
        val base = createPublicAlertState()
        return base.copy(
            currentStep = ScenarioStep.SAFER_ROUTE,
            routes = createRoutes(orrRisk = 78, hsrRisk = 18, orrSever = RiskLevel.HIGH)
        )
    }

    private fun createAuthorityResponseState(): ScenarioState {
        val base = createSaferRouteState()
        val actions = listOf(
            GovernmentActionItem("ORR EcoSpace Culvert (SWD-3)", RiskLevel.HIGH, "Inspect culvert inlet for debris clogging & start mobile dewatering pump", "BBMP Stormwater Drain Dept", "CRITICAL"),
            GovernmentActionItem("Agara Junction Underpass", RiskLevel.WATCH, "Verify sump automatic pump float sensors & clear roadside silt traps", "BBMP Engineering", "HIGH"),
            GovernmentActionItem("Rainbow Drive Ingress", RiskLevel.SEVERE, "Deploy barricades & NDRF/Civil Defence quick team for residential protection", "Bengaluru Traffic Police / SDMA", "IMMEDIATE"),
            GovernmentActionItem("Ibblur Junction Traffic Sump", RiskLevel.HIGH, "Monitor water level in Agara lake feeder link channel", "BWSSB & Lake Development", "HIGH")
        )
        return base.copy(
            currentStep = ScenarioStep.AUTHORITY_RESPONSE,
            governmentActions = actions
        )
    }

    fun createRoutes(orrRisk: Int, hsrRisk: Int, orrSever: RiskLevel): List<RouteOption> {
        val orrSegments = listOf(
            RoadSegment("r_orr_1", "Outer Ring Road (Ibblur-EcoSpace)", "Ibblur", "EcoSpace", 2.2, 18, 872.4, 0.8, 850, 95, orrRisk, orrSever, "30-60 min", 85, 68.0, listOf("Culvert bottleneck"), 12.9230, 77.6705, 12.9280, 77.6820)
        )
        val hsrSegments = listOf(
            RoadSegment("r_hsr_ridge", "HSR Ridge Road", "Agara", "Bellandur Gate", 3.4, 11, 895.0, 3.2, 120, 24, hsrRisk, RiskLevel.SAFE, "Free Flow", 94, 68.0, listOf("Elevated ridge topography"), 12.9210, 77.6480, 12.9140, 77.6780)
        )

        val fastestRoute = RouteOption(
            routeType = "FASTEST",
            routeTitle = "Via Outer Ring Road (Direct)",
            travelTimeMinutes = if (orrRisk > 50) 24 else 18,
            distanceKm = 7.2,
            floodRiskPercentage = orrRisk,
            riskySegmentsCount = if (orrRisk > 50) 4 else 0,
            recommendationNote = if (orrRisk > 50) "High flood risk predicted on ORR EcoSpace depression. Significant waterlogging delay expected." else "Standard fastest transit route.",
            segments = orrSegments,
            pathCoordinates = listOf(
                Pair(12.9248, 77.6515), // Agara
                Pair(12.9230, 77.6705), // Ibblur
                Pair(12.9278, 77.6820), // EcoSpace Low Point
                Pair(12.9340, 77.6920)  // Bellandur EcoWorld
            )
        )

        val saferRoute = RouteOption(
            routeType = "SAFER",
            routeTitle = "Via HSR Ridge & Sarjapur Diversion",
            travelTimeMinutes = 21,
            distanceKm = 7.8,
            floodRiskPercentage = hsrRisk,
            riskySegmentsCount = 0,
            recommendationNote = "Safer route recommended because predicted flood risk is substantially lower (18% vs ${orrRisk}%) by avoiding Outer Ring Road bottleneck.",
            segments = hsrSegments,
            pathCoordinates = listOf(
                Pair(12.9248, 77.6515), // Agara
                Pair(12.9150, 77.6520), // HSR Sector 2
                Pair(12.9110, 77.6620), // 14th Main Ridge (Elevated 895m)
                Pair(12.9140, 77.6780), // Kaikondrahalli Link
                Pair(12.9340, 77.6920)  // Bellandur EcoWorld
            )
        )

        return listOf(fastestRoute, saferRoute)
    }
}
