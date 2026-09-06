package com.example.ui

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FloodSafeDatabase
import com.example.data.model.*
import com.example.data.repository.FloodSafeRepository
import com.example.data.service.LocationTracker
import com.example.data.service.ScenarioStep
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class AppScreen(val label: String) {
    HOME("Home"),
    FLOOD_MAP("Flood Map"),
    SAFE_NAV("Safe Nav"),
    LIVE_NAV("Live Nav"),
    ALERTS("Alerts"),
    NOWCAST("Nowcast"),
    DRAINAGE("Drainage + Rain"),
    AUTHORITY("Authority"),
    HISTORY("History"),
    VALIDATION("Validation"),
    DATA_STATUS("Data Status"),
    SETTINGS("Settings")
}

class FloodSafeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FloodSafeDatabase.getDatabase(application)
    val repository = FloodSafeRepository(db)
    val locationTracker = LocationTracker(application)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val currentAppMode = repository.currentAppMode
    val isDemoMode = repository.isDemoMode
    val scenarioState = repository.scenarioState
    val liveWeather = repository.liveWeather
    val nowcastHorizons = repository.nowcastHorizons
    val historicalEvents = repository.historicalEvents
    val demoValidationMetrics = repository.demoValidationMetrics
    val realValidationMetrics = repository.realValidationMetrics
    val dataSources = repository.dataSources
    val routeOptions = repository.routeOptions

    // GPS & Location tracking
    val gpsStatus = locationTracker.gpsStatus
    val currentLocation = locationTracker.currentLocation

    // Selected items for modal/drawer details
    private val _selectedLocation = MutableStateFlow<LocationInfo?>(null)
    val selectedLocation: StateFlow<LocationInfo?> = _selectedLocation.asStateFlow()

    private val _selectedRoad = MutableStateFlow<RoadSegment?>(null)
    val selectedRoad: StateFlow<RoadSegment?> = _selectedRoad.asStateFlow()

    // Safe Navigation state
    private val _originInput = MutableStateFlow("Agara Junction (Pilot Ingress)")
    val originInput: StateFlow<String> = _originInput.asStateFlow()

    private val _destinationInput = MutableStateFlow("Bellandur EcoSpace (Outer Ring Rd)")
    val destinationInput: StateFlow<String> = _destinationInput.asStateFlow()

    private val _selectedRouteType = MutableStateFlow("SAFER")
    val selectedRouteType: StateFlow<String> = _selectedRouteType.asStateFlow()

    // Navigation state
    private val _isLiveNavigating = MutableStateFlow(false)
    val isLiveNavigating: StateFlow<Boolean> = _isLiveNavigating.asStateFlow()

    private val _navStepIndex = MutableStateFlow(0)
    val navStepIndex: StateFlow<Int> = _navStepIndex.asStateFlow()

    private val _showFloodRiskAheadAlert = MutableStateFlow(false)
    val showFloodRiskAheadAlert: StateFlow<Boolean> = _showFloodRiskAheadAlert.asStateFlow()

    private val _hazardDistanceMeters = MutableStateFlow(450)
    val hazardDistanceMeters: StateFlow<Int> = _hazardDistanceMeters.asStateFlow()

    private val _alertCooldownActive = MutableStateFlow(false)
    val alertCooldownActive: StateFlow<Boolean> = _alertCooldownActive.asStateFlow()

    // Gemini AI natural-language explanation
    private val _aiExplanationText = MutableStateFlow<String?>(null)
    val aiExplanationText: StateFlow<String?> = _aiExplanationText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Real-time live status and ticker
    private val _isRefreshingLiveWeather = MutableStateFlow(false)
    val isRefreshingLiveWeather: StateFlow<Boolean> = _isRefreshingLiveWeather.asStateFlow()

    private val _realTimeClock = MutableStateFlow("")
    val realTimeClock: StateFlow<String> = _realTimeClock.asStateFlow()

    private var navJob: Job? = null

    init {
        // Real-time second-by-second IST clock
        val clockFormat = SimpleDateFormat("hh:mm:ss a 'IST'", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        viewModelScope.launch {
            while (true) {
                _realTimeClock.value = clockFormat.format(Date())
                delay(1000L)
            }
        }

        // Start GPS listening if permission is already granted
        if (locationTracker.checkPermission()) {
            locationTracker.startListening()
        }
    }

    fun refreshLiveWeather() {
        viewModelScope.launch {
            _isRefreshingLiveWeather.value = true
            val curLoc = locationTracker.currentLocation.value
            repository.refreshLiveWeather(curLoc?.latitude, curLoc?.longitude)
            delay(500)
            _isRefreshingLiveWeather.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopListening()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            locationTracker.startListening()
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setAppMode(mode: String) {
        repository.setAppMode(mode)
    }

    fun setDemoMode(isDemo: Boolean) {
        repository.setDemoMode(isDemo)
    }

    fun advanceScenario() {
        repository.advanceScenarioStep()
    }

    fun setScenarioStep(step: ScenarioStep) {
        repository.setScenarioStep(step)
    }

    fun resetScenario() {
        repository.resetScenario()
        _showFloodRiskAheadAlert.value = false
        _alertCooldownActive.value = false
        stopLiveNavigation()
    }

    fun selectLocation(location: LocationInfo?) {
        _selectedLocation.value = location
        if (location != null) {
            requestAiExplanation(location)
        }
    }

    fun selectRoad(road: RoadSegment?) {
        _selectedRoad.value = road
    }

    fun setOrigin(origin: String) {
        _originInput.value = origin
    }

    fun setDestination(destination: String) {
        _destinationInput.value = destination
    }

    fun selectRoute(routeType: String) {
        _selectedRouteType.value = routeType
    }

    fun startLiveNavigation() {
        _isLiveNavigating.value = true
        _navStepIndex.value = 0
        _currentScreen.value = AppScreen.LIVE_NAV

        if (locationTracker.checkPermission()) {
            locationTracker.startListening()
        }

        navJob?.cancel()
        navJob = viewModelScope.launch {
            val routesPair = repository.routeOptions.value
            val selectedRoute = if (_selectedRouteType.value == "SAFER") routesPair?.second else routesPair?.first
            val waypointsCount = selectedRoute?.pathCoordinates?.size ?: 4

            for (i in 0 until waypointsCount) {
                _navStepIndex.value = i

                // Check distance to hazard bottleneck (EcoSpace depression: 12.9278, 77.6820)
                val isFastest = _selectedRouteType.value == "FASTEST"
                val isHighRisk = repository.scenarioState.value.overallFloodRiskPercent >= 60

                if (isFastest && isHighRisk && i in 1..2 && !_alertCooldownActive.value) {
                    _hazardDistanceMeters.value = if (i == 1) 650 else 320
                    _showFloodRiskAheadAlert.value = true
                }
                delay(3000)
            }
        }
    }

    fun stopLiveNavigation() {
        navJob?.cancel()
        _isLiveNavigating.value = false
        _navStepIndex.value = 0
    }

    fun dismissFloodAheadAlert() {
        _showFloodRiskAheadAlert.value = false
        _alertCooldownActive.value = true
        viewModelScope.launch {
            delay(15000) // 15-second alert cooldown
            _alertCooldownActive.value = false
        }
    }

    fun rerouteToSafer() {
        _selectedRouteType.value = "SAFER"
        _showFloodRiskAheadAlert.value = false
        _navStepIndex.value = 0
        startLiveNavigation()
    }

    fun requestAiExplanation(location: LocationInfo) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val (text, _) = repository.geminiService.getRiskExplanation(
                location = location,
                rainfallMmHr = repository.scenarioState.value.rainfallMmHr,
                drainageStressPercent = repository.scenarioState.value.drainageStressPercent
            )
            _aiExplanationText.value = text
            _isAiLoading.value = false
        }
    }

    // Configurable thresholds in Settings
    fun updateThresholds(safe: Int, watch: Int, high: Int) {
        repository.riskEngine.safeMaxThreshold = safe
        repository.riskEngine.watchMaxThreshold = watch
        repository.riskEngine.highMaxThreshold = high
        repository.setScenarioStep(repository.scenarioState.value.currentStep)
    }

    // Configurable weights in Settings
    fun updateWeights(
        rainfall: Float,
        antecedent: Float,
        terrain: Float,
        flowAcc: Float,
        drainage: Float,
        waterLevel: Float,
        historical: Float
    ) {
        repository.riskEngine.rainfallWeight = rainfall
        repository.riskEngine.antecedentWeight = antecedent
        repository.riskEngine.terrainWeight = terrain
        repository.riskEngine.flowAccumulationWeight = flowAcc
        repository.riskEngine.drainageWeight = drainage
        repository.riskEngine.waterLevelWeight = waterLevel
        repository.riskEngine.historicalWeight = historical
        repository.setScenarioStep(repository.scenarioState.value.currentStep)
    }
}
