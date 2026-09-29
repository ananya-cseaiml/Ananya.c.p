package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.service.*
import com.example.prediction.AlertEngine
import com.example.prediction.FloodRiskEngine
import com.example.prediction.SafeRoutingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloodSafeRepository(
    private val database: FloodSafeDatabase,
    val riskEngine: FloodRiskEngine = FloodRiskEngine(),
    val demoScenarioEngine: DemoScenarioEngine = DemoScenarioEngine(),
    val liveWeatherService: LiveWeatherService = LiveWeatherService(),
    val alertEngine: AlertEngine = AlertEngine(),
    val routingEngine: SafeRoutingEngine = SafeRoutingEngine(riskEngine),
    val geminiService: GeminiExplanationService = GeminiExplanationService()
) {
    // Mode management: LIVE, DEMO, HISTORICAL (Default to LIVE for real-time working app)
    private val _currentAppMode = MutableStateFlow("LIVE") // "LIVE", "DEMO", "HISTORICAL"
    val currentAppMode: StateFlow<String> = _currentAppMode.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _scenarioState = MutableStateFlow(demoScenarioEngine.getStateForStep(ScenarioStep.NORMAL))
    val scenarioState: StateFlow<ScenarioState> = _scenarioState.asStateFlow()

    private val _liveWeather = MutableStateFlow<LiveWeatherResult?>(null)
    val liveWeather: StateFlow<LiveWeatherResult?> = _liveWeather.asStateFlow()

    private val _nowcastHorizons = MutableStateFlow<List<NowcastHorizon>>(emptyList())
    val nowcastHorizons: StateFlow<List<NowcastHorizon>> = _nowcastHorizons.asStateFlow()

    private val _historicalEvents = MutableStateFlow<List<HistoricalEvent>>(emptyList())
    val historicalEvents: StateFlow<List<HistoricalEvent>> = _historicalEvents.asStateFlow()

    val geoSpatialRepository: GeoSpatialRepository = GeoSpatialRepository()

    private val _demoValidationMetrics = MutableStateFlow(
        ValidationMetrics(
            accuracyPercent = 0.0,
            precisionPercent = 0.0,
            recallPercent = 0.0,
            falseAlarmRatePercent = 0.0,
            missedEventRatePercent = 0.0,
            averageLeadTimeMinutes = 0,
            verifiedEventsCount = 0,
            isDemo = true,
            hasSufficientData = false,
            notice = "Validation pending verified ground-truth data.",
            statusLabel = "DEMO SCENARIO — NOT VALIDATION"
        )
    )
    val demoValidationMetrics: StateFlow<ValidationMetrics> = _demoValidationMetrics.asStateFlow()

    private val _realValidationMetrics = MutableStateFlow(
        ValidationMetrics(
            accuracyPercent = 0.0,
            precisionPercent = 0.0,
            recallPercent = 0.0,
            falseAlarmRatePercent = 0.0,
            missedEventRatePercent = 0.0,
            averageLeadTimeMinutes = 0,
            verifiedEventsCount = 0,
            isDemo = false,
            hasSufficientData = false,
            notice = "Validation pending verified ground-truth data.",
            statusLabel = "Validation pending verified ground-truth data."
        )
    )
    val realValidationMetrics: StateFlow<ValidationMetrics> = _realValidationMetrics.asStateFlow()

    private val _dataSources = MutableStateFlow<List<DataSourceStatus>>(emptyList())
    val dataSources: StateFlow<List<DataSourceStatus>> = _dataSources.asStateFlow()

    private val _routeOptions = MutableStateFlow<Pair<RouteOption, RouteOption>?>(null)
    val routeOptions: StateFlow<Pair<RouteOption, RouteOption>?> = _routeOptions.asStateFlow()

    init {
        updateNowcastForCurrentRisk(0.0, 1.0, 20)
        seedHistoricalEvents()
        updateDataSourcesList()

        CoroutineScope(Dispatchers.IO).launch {
            seedRoomDatabase()
            calculateRealValidationFromDatabase()
            refreshRoutes()
            // Real-time live boot: immediately fetch live Open-Meteo telemetry
            refreshLiveWeather()

            // Continuous background real-time polling every 30 seconds
            while (true) {
                kotlinx.coroutines.delay(30000L)
                if (_currentAppMode.value == "LIVE") {
                    refreshLiveWeather()
                }
            }
        }
    }

    fun setAppMode(mode: String) {
        _currentAppMode.value = mode
        val isDemo = mode == "DEMO"
        _isDemoMode.value = isDemo

        if (mode == "LIVE") {
            refreshLiveWeather()
        } else if (mode == "HISTORICAL") {
            loadHistoricalPlayback()
        } else {
            resetScenario()
        }
        updateDataSourcesList()
    }

    fun setDemoMode(isDemo: Boolean) {
        setAppMode(if (isDemo) "DEMO" else "LIVE")
    }

    fun refreshRoutes() {
        CoroutineScope(Dispatchers.IO).launch {
            val rainfall = _scenarioState.value.rainfallMmHr
            val stress = _scenarioState.value.drainageStressPercent
            val routes = routingEngine.computeRouteOptions(
                rainfallMmHr = rainfall,
                drainageStressPercent = stress
            )
            _routeOptions.value = routes
        }
    }

    fun refreshLiveWeather(userLat: Double? = null, userLng: Double? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = if (userLat != null && userLng != null && userLat != 0.0) {
                liveWeatherService.fetchLiveWeather(latitude = userLat, longitude = userLng)
            } else {
                liveWeatherService.fetchLiveWeather()
            }
            _liveWeather.value = result

            if (_currentAppMode.value == "LIVE" && result.status == DataStatus.LIVE) {
                val current = _scenarioState.value

                val rain = result.rainfallMmHr
                // 1. Prototype hydrological pipeline: compute runoff and drainage stress
                val (liveRunoffM3Sec, liveStress) = riskEngine.hydrologicalEngine.calculateDrainageStress(
                    rainfallMmHr = rain,
                    isBottleneck = true,
                    drainageCapacityM3Sec = 18.0
                )

                // 2. Evaluate physical risk for each location using FloodRiskEngine as single source of truth
                val updatedLocations = current.locations.map { loc ->
                    val eval = riskEngine.evaluateLocationRisk(
                        location = loc,
                        currentRainfallMmHr = rain,
                        recentRainfall1hMm = result.recentRainfall1hMm,
                        rainfallDurationMin = 30,
                        antecedent24hMm = result.antecedent24hRainfallMm,
                        forecast1hMm = result.forecast1hMm,
                        drainageStressPercent = liveStress,
                        isDrainBottleneck = liveStress > 50,
                        hasGenuineWaterTelemetry = false,
                        dataStatus = DataStatus.LIVE
                    )
                    loc.copy(
                        currentRisk = eval.riskPercentage,
                        riskLevel = eval.riskLevel,
                        confidence = eval.confidence,
                        whyAtRisk = eval.whyExplanation?.primarySummary ?: loc.whyAtRisk
                    )
                }

                val overallRisk = updatedLocations.maxOfOrNull { it.currentRisk } ?: 18
                val overallLevel = riskEngine.getRiskLevel(overallRisk)

                val updatedRoads = current.roads.map { road ->
                    riskEngine.calculateRoadRisk(road, rain, liveStress)
                }

                val (liveAlerts, liveActions) = alertEngine.evaluateCatchmentAlerts(
                    updatedLocations,
                    updatedRoads,
                    rain,
                    liveStress
                )

                _scenarioState.value = current.copy(
                    rainfallMmHr = rain,
                    overallFloodRiskPercent = overallRisk,
                    overallRiskLevel = overallLevel,
                    drainageStressPercent = liveStress,
                    drainageLoadM3Sec = liveRunoffM3Sec,
                    locations = updatedLocations,
                    roads = updatedRoads,
                    activeAlerts = liveAlerts,
                    governmentActions = liveActions
                )

                // 3. Compute short-term nowcast directly from Open-Meteo forecast data (no hardcoded future rain)
                _nowcastHorizons.value = riskEngine.computeNowcastFromForecast(
                    currentRainfallMmHr = rain,
                    forecast1hMm = result.forecast1hMm,
                    forecast3hMm = result.forecast3hMm,
                    forecast6hMm = result.forecast6hMm,
                    baseDrainageCapacityM3Sec = 18.0,
                    isDemo = false
                )
                refreshRoutes()
            }
            updateDataSourcesList()
        }
    }

    fun advanceScenarioStep() {
        val nextStep = _scenarioState.value.currentStep.next()
        setScenarioStep(nextStep)
    }

    fun setScenarioStep(step: ScenarioStep) {
        val newState = demoScenarioEngine.getStateForStep(step)
        _scenarioState.value = newState
        val rain = newState.rainfallMmHr
        val forecast1h = rain * (if (step.ordinal >= 5) 1.15 else 0.85)
        val forecast3h = forecast1h * 0.75
        _nowcastHorizons.value = riskEngine.computeNowcastFromForecast(
            currentRainfallMmHr = rain,
            forecast1hMm = forecast1h,
            forecast3hMm = forecast3h,
            forecast6hMm = forecast3h * 0.6,
            baseDrainageCapacityM3Sec = 18.0,
            isDemo = true
        )

        // Sync alerts to Room DB
        CoroutineScope(Dispatchers.IO).launch {
            if (newState.activeAlerts.isNotEmpty()) {
                val entities = newState.activeAlerts.map {
                    AlertEntity(
                        id = it.id,
                        location = it.location,
                        severity = it.severity.name,
                        riskPercentage = it.riskPercentage,
                        issuedAt = it.issuedAt,
                        expectedTime = it.expectedTime,
                        reason = it.reason,
                        recommendedAction = it.recommendedAction,
                        status = it.status,
                        isModelGenerated = it.isModelGenerated
                    )
                }
                database.alertDao().insertAlerts(entities)
            }
            refreshRoutes()
        }
        updateDataSourcesList()
    }

    fun resetScenario() {
        setScenarioStep(ScenarioStep.NORMAL)
    }

    private fun loadHistoricalPlayback() {
        // Load severe September 2022 historic flood scenario
        setScenarioStep(ScenarioStep.FLOOD_RISK_INCREASES)
    }

    private fun updateNowcastForCurrentRisk(rainfallMmHr: Double, trend: Double, drainageStress: Int) {
        _nowcastHorizons.value = riskEngine.calculateNowcast(
            currentRainfallMmHr = rainfallMmHr,
            rainfallTrendFactor = trend,
            currentDrainageStress = drainageStress,
            isDemo = (_currentAppMode.value == "DEMO")
        )
    }

    private fun updateDataSourcesList() {
        val mode = _currentAppMode.value
        val weather = _liveWeather.value
        val weatherStatus = if (mode == "DEMO") "DEMO DATA" else if (weather?.status == DataStatus.LIVE) "LIVE" else "UNAVAILABLE"

        _dataSources.value = listOf(
            DataSourceStatus(
                name = "Open-Meteo Weather Forecast API",
                category = "Rainfall & Atmospheric Telemetry",
                type = weatherStatus,
                statusText = if (mode == "DEMO") "Demo Scenario: Convective Cloudburst Progression" else if (weather?.status == DataStatus.LIVE) (weather.weatherDesc) else "Live weather unavailable",
                lastUpdatedText = if (mode == "DEMO") "Scenario Clock" else (weather?.lastUpdated ?: "Pending sync"),
                freshness = if (mode == "DEMO") "Demo Scenario" else if (weather?.status == DataStatus.LIVE) "Live (Open-Meteo)" else "Unavailable",
                honestyNote = "ECMWF / DWD numerical weather prediction mesh. Live weather integration with forecast mapping."
            ),
            DataSourceStatus(
                name = "PROTOTYPE BBMP RAJAKALUVE NETWORK",
                category = "Drainage Hydraulic Capacity",
                type = "STATIC PROTOTYPE DATA",
                statusText = "Bellandur–Agara Master Drain Baseline (SWD-1, 2, 3, 5)",
                lastUpdatedText = "Static Prototype",
                freshness = "STATIC PROTOTYPE DATA",
                honestyNote = "STATIC PROTOTYPE DATA: Predefined baseline of primary SWD trunk conduits. Live municipal BBMP sensor telemetry or API feed is a planned integration."
            ),
            DataSourceStatus(
                name = "Prototype static GIS-derived terrain layer",
                category = "Terrain & Depression Bathymetry",
                type = "STATIC GIS DATA",
                statusText = "Catchment Slope & Flow Accumulation Sinks (870m–910m)",
                lastUpdatedText = "Static GIS Layer",
                freshness = "Static Prototype",
                honestyNote = "Prototype static GIS-derived terrain layer based on digital elevation model. Real-time dynamic DEM processing is a planned integration."
            ),
            DataSourceStatus(
                name = "Water Level Telemetry (Agara & Bellandur)",
                category = "Water Level Telemetry",
                type = if (mode == "DEMO") "DEMO DATA" else "PLANNED INTEGRATION",
                statusText = if (mode == "DEMO") "Demo Scenario: Sump Level (2.4m / 3.0m danger mark)" else "No live water-level telemetry connected (Planned integration)",
                lastUpdatedText = if (mode == "DEMO") "Active" else "Unavailable",
                freshness = if (mode == "DEMO") "Demo Scenario" else "Unavailable",
                honestyNote = "No live water-level telemetry connected. IoT ultrasonic water-level telemetry is a planned integration."
            ),
            DataSourceStatus(
                name = "Prototype Historical Benchmark Scenarios",
                category = "Historical Flood Scenarios",
                type = "PROTOTYPE DATA",
                statusText = "Prototype Historical Scenarios — Not Verified (Bellandur–Agara)",
                lastUpdatedText = "Static Benchmark Data",
                freshness = "Prototype Scenarios",
                honestyNote = "Prototype historical benchmark scenarios for the Bellandur–Agara catchment. External KSNDMC/BBMP datasets are planned for future validation."
            ),
            DataSourceStatus(
                name = "Prototype Road Network",
                category = "Routing & Corridor Graph",
                type = "PROTOTYPE DATA",
                statusText = "Predefined Bellandur–Agara Arterial & Ridge Segments",
                lastUpdatedText = "Static Prototype Graph",
                freshness = "Prototype Data",
                honestyNote = "Prototype road network with elevation attributes. Dynamic real-time OSRM/Google live routing is a planned integration."
            )
        )
    }

    private fun seedHistoricalEvents() {
        _historicalEvents.value = listOf(
            HistoricalEvent("h_01", "05 Sep 2022", "Rainbow Drive, Sarjapur Road", 131.6, 92, "Inundation (1.4m depth) - Residential evacuation", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Heavy cloudburst; Rajakaluve breach confirmed.", DataOrigin.PROTOTYPE),
            HistoricalEvent("h_02", "19 Oct 2023", "ORR EcoSpace Low Point", 78.4, 76, "Severe Waterlogging (0.6m) - Traffic halted", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Culvert bottleneck throttled discharge to Bellandur lake.", DataOrigin.PROTOTYPE),
            HistoricalEvent("h_03", "21 May 2024", "Agara Junction Underpass", 52.0, 64, "Waterlogged (0.4m) - Sump motor overwhelmed", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Short-duration extreme intensity (42 mm in 35 min).", DataOrigin.PROTOTYPE),
            HistoricalEvent("h_04", "12 Aug 2023", "Ibblur Junction Sump", 38.0, 58, "Minor ponding (0.15m) - Traffic slowed", "DEMO OVER-PREDICTION SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Emergency desilting 2 days prior had improved capacity.", DataOrigin.PROTOTYPE),
            HistoricalEvent("h_05", "03 Jul 2024", "Bellandur Gate", 45.0, 32, "Dry - Camber drainage functioned properly", "DEMO CORRECT-HIT SCENARIO", "HISTORICAL BENCHMARK — NOT VERIFIED. Elevated section; gravity drainage safely discharged runoff.", DataOrigin.PROTOTYPE)
        )
    }

    private suspend fun calculateRealValidationFromDatabase() {
        _realValidationMetrics.value = ValidationMetrics(
            accuracyPercent = 0.0,
            precisionPercent = 0.0,
            recallPercent = 0.0,
            falseAlarmRatePercent = 0.0,
            missedEventRatePercent = 0.0,
            averageLeadTimeMinutes = 0,
            verifiedEventsCount = 0,
            demoEventsCount = 0,
            isDemo = false,
            hasSufficientData = false,
            validationStatus = ValidationStatus.PENDING,
            notice = "Verified model validation requires genuine observed flood-event and rainfall datasets. The current project contains demonstration scenarios only.",
            statusLabel = "VALIDATION STATUS: PENDING VERIFIED GROUND TRUTH",
            spatialValidationStatus = "Bellandur–Agara Basin pilot mesh (Ward 150 & 174)",
            temporalValidationStatus = "15-min hyetograph resolution, 15–120m nowcast lead time",
            groundTruthDataSource = "Planned external integration: KSNDMC AWS & BBMP Gauging Records"
        )

        // Synthetic benchmark demonstration workflow
        _demoValidationMetrics.value = ValidationMetrics(
            accuracyPercent = 80.0,
            precisionPercent = 75.0,
            recallPercent = 85.0,
            falseAlarmRatePercent = 20.0,
            missedEventRatePercent = 15.0,
            averageLeadTimeMinutes = 42,
            verifiedEventsCount = 0,
            demoEventsCount = 5,
            isDemo = true,
            hasSufficientData = true,
            validationStatus = ValidationStatus.DEMO,
            notice = "DEMO VALIDATION WORKFLOW — NOT REAL MODEL VALIDATION (Synthetic benchmark scenario demonstration).",
            statusLabel = "DEMO VALIDATION WORKFLOW — NOT REAL MODEL VALIDATION",
            spatialValidationStatus = "Bellandur–Agara Basin pilot mesh (Ward 150 & 174)",
            temporalValidationStatus = "15-min hyetograph resolution, 15–120m nowcast lead time",
            groundTruthDataSource = "Synthetic prototype scenario benchmark (For validation workflow demonstration only)"
        )
    }

    private suspend fun seedRoomDatabase() {
        val locations = listOf(
            LocationEntity("loc_ecospace", "Outer Ring Road - EcoSpace", "Ward 150 Bellandur", 12.9278, 77.6820, 872.4, 0.8, 850, 88, 85),
            LocationEntity("loc_agara", "Agara Junction Underpass", "Ward 174 HSR Layout", 12.9248, 77.6515, 878.2, 1.4, 480, 80, 60),
            LocationEntity("loc_rainbow", "Rainbow Drive / Sarjapur Rd", "Ward 150 Bellandur", 12.9160, 77.6890, 871.0, 0.5, 910, 92, 90),
            LocationEntity("loc_ibblur", "Ibblur Junction Sump", "Ward 174 HSR Layout", 12.9220, 77.6690, 874.5, 1.1, 620, 85, 55)
        )
        database.locationDao().insertLocations(locations)

        val roads = listOf(
            RoadEntity("r_orr_1", "Outer Ring Road (Ibblur-EcoSpace)", "Ibblur", "EcoSpace", 2.2, 5, 872.4, 0.8, 12.0, 12.9230, 77.6705, 12.9280, 77.6820),
            RoadEntity("r_sarjapur", "Sarjapur Main Road", "Agara", "Ibblur", 2.8, 7, 878.0, 1.2, 16.0, 12.9248, 77.6515, 12.9225, 77.6690),
            RoadEntity("r_hsr_ridge", "HSR 14th Main Ridge Bypass", "Agara", "Bellandur Gate", 3.4, 9, 895.0, 3.2, 28.0, 12.9210, 77.6480, 12.9140, 77.6780)
        )
        database.roadDao().insertRoads(roads)

        val histEntities = _historicalEvents.value.map {
            HistoricalEventEntity(
                id = it.id,
                eventDate = it.date,
                location = it.location,
                rainfallMm = it.rainfallRecordedMm,
                predictedRiskPercentage = it.predictedRiskPercentage,
                observedCondition = it.observedCondition,
                predictionStatus = it.predictionStatus,
                notes = it.notes
            )
        }
        database.historicalDao().insertHistoricalEvents(histEntities)
    }
}
