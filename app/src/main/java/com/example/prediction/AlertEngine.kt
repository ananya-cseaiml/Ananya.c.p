package com.example.prediction

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Alert Engine for FloodSafe Bengaluru
 * - Evaluates meaningful risk transitions (LOW -> MODERATE, MODERATE -> HIGH, HIGH -> SEVERE)
 * - Detects rapidly increasing rainfall & culvert surcharge bottlenecks
 * - Considers user proximity to hazard
 * - Enforces alert cooldown & de-duplication per corridor to prevent alert fatigue
 * - Generates clear citizen-facing alerts & municipal government decision-support actions
 */
class AlertEngine(
    var moderateRiskThreshold: Int = 25,
    var highRiskThreshold: Int = 50,
    var severeRiskThreshold: Int = 75,
    var alertCooldownMs: Long = 300_000L // 5-minute cooldown per corridor
) {
    // Tracks the last alert time per location ID to prevent duplicates
    private val lastAlertTimeMap = ConcurrentHashMap<String, Long>()

    // Tracks previously observed risk level per location/road to detect state transitions
    private val previousRiskLevelMap = ConcurrentHashMap<String, RiskLevel>()

    private val timeFormat = SimpleDateFormat("HH:mm, dd MMM", Locale.ENGLISH)

    /**
     * Checks if a risk transition has occurred (e.g., LOW -> MODERATE, MODERATE -> HIGH, HIGH -> SEVERE)
     */
    fun isMeaningfulTransition(id: String, currentLevel: RiskLevel): Boolean {
        val prevLevel = previousRiskLevelMap[id]
        previousRiskLevelMap[id] = currentLevel
        if (prevLevel == null) {
            // First time evaluation: alert if already MODERATE, HIGH or SEVERE
            return currentLevel != RiskLevel.LOW
        }
        return currentLevel.ordinal > prevLevel.ordinal
    }

    /**
     * Checks if cooldown period has elapsed for an entity
     */
    fun isCooldownExpired(id: String): Boolean {
        val now = System.currentTimeMillis()
        val lastAlert = lastAlertTimeMap[id] ?: 0L
        return (now - lastAlert) >= alertCooldownMs
    }

    /**
     * Records dispatch to enforce cooldown
     */
    fun recordAlertDispatched(id: String) {
        lastAlertTimeMap[id] = System.currentTimeMillis()
    }

    /**
     * Clears cooldown & transition memory (e.g. on scenario reset)
     */
    fun resetCooldowns() {
        lastAlertTimeMap.clear()
        previousRiskLevelMap.clear()
    }

    /**
     * Determines whether an alert should be generated for a location
     */
    fun shouldGenerateAlert(
        locationId: String,
        currentRisk: Int,
        userLat: Double? = null,
        userLng: Double? = null,
        targetLat: Double? = null,
        targetLng: Double? = null,
        maxDistanceKm: Double = 10.0,
        forceImmediate: Boolean = false
    ): Boolean {
        val level = RiskLevel.fromScore(currentRisk)

        // Only alert for MODERATE, HIGH, or SEVERE risk
        if (level == RiskLevel.LOW && !forceImmediate) {
            return false
        }

        // Distance check if user location is available
        if (userLat != null && userLng != null && targetLat != null && targetLng != null) {
            val distKm = calculateDistanceKm(userLat, userLng, targetLat, targetLng)
            if (distKm > maxDistanceKm) {
                return false
            }
        }

        // Must be a meaningful transition or forced, AND cooldown expired
        val transitionOccurred = isMeaningfulTransition(locationId, level)
        val cooldownPassed = isCooldownExpired(locationId)

        return (transitionOccurred || forceImmediate) && cooldownPassed
    }

    /**
     * Generates Citizen-Facing Flood Alert with exact required structure
     */
    fun createCitizenAlert(
        id: String,
        location: String,
        riskPercentage: Int,
        expectedTime: String,
        reason: String,
        recommendedAction: String,
        isVerifiedObservation: Boolean = false
    ): FloodAlert {
        val severity = RiskLevel.fromScore(riskPercentage)

        return FloodAlert(
            id = id,
            location = location,
            severity = severity,
            riskPercentage = riskPercentage,
            issuedAt = timeFormat.format(Date()),
            expectedTime = expectedTime,
            reason = reason,
            recommendedAction = recommendedAction,
            status = "ACTIVE",
            isModelGenerated = !isVerifiedObservation
        )
    }

    /**
     * Generates Government Decision-Support Action item
     */
    fun createGovernmentAction(
        location: String,
        riskPercentage: Int,
        rainfallMmHr: Double,
        drainageStressPercent: Int,
        isUnderpassOrDepression: Boolean
    ): GovernmentActionItem {
        val severity = RiskLevel.fromScore(riskPercentage)

        val (action, agency, priority) = when {
            severity == RiskLevel.SEVERE || (rainfallMmHr >= 50.0 && drainageStressPercent >= 80) -> Triple(
                "Recommended action: Position 50HP mobile dewatering pumps & prepare emergency traffic diversion",
                "BBMP Stormwater Drain (SWD) Dept & Traffic Police",
                "CRITICAL"
            )
            severity == RiskLevel.HIGH -> Triple(
                "Recommended action: Inspect culvert inlet screens for debris clogging & prepare diversion signage via ridge bypass",
                "BBMP Road Infrastructure & Traffic Police",
                "HIGH"
            )
            isUnderpassOrDepression && riskPercentage >= 35 -> Triple(
                "Recommended action: Verify automatic sump pump float sensors and monitor road camber drainage",
                "BBMP Ward Engineering",
                "MEDIUM"
            )
            else -> Triple(
                "Recommended action: Routine surveillance of culvert inlet screens and silt traps",
                "BBMP Ward Engineering",
                "ROUTINE"
            )
        }

        return GovernmentActionItem(
            location = location,
            severity = severity,
            suggestedCheck = action,
            agency = agency,
            priority = priority,
            isAcknowledged = false
        )
    }

    /**
     * Evaluates catchment state and generates de-duplicated, transition-based alerts
     */
    fun evaluateCatchmentAlerts(
        locations: List<LocationInfo>,
        roads: List<RoadSegment>,
        rainfallMmHr: Double,
        drainageStressPercent: Int
    ): Pair<List<FloodAlert>, List<GovernmentActionItem>> {
        val alerts = mutableListOf<FloodAlert>()
        val actions = mutableListOf<GovernmentActionItem>()

        locations.forEach { loc ->
            val level = RiskLevel.fromScore(loc.currentRisk)
            if (level != RiskLevel.LOW) {
                val shouldAlert = isCooldownExpired(loc.id)
                if (shouldAlert) {
                    val alert = createCitizenAlert(
                        id = "alt_${loc.id}_${System.currentTimeMillis() % 10000}",
                        location = "${loc.name} (${loc.ward})",
                        riskPercentage = loc.currentRisk,
                        expectedTime = loc.predictedTimeWindow,
                        reason = "${level.label} flood risk projected near ${loc.name} within next hour due to increasing rainfall (${rainfallMmHr.toInt()} mm/hr) and elevated drainage stress ($drainageStressPercent%).",
                        recommendedAction = loc.recommendedAction,
                        isVerifiedObservation = false
                    )
                    alerts.add(alert)
                    recordAlertDispatched(loc.id)
                }

                val action = createGovernmentAction(
                    location = loc.name,
                    riskPercentage = loc.currentRisk,
                    rainfallMmHr = rainfallMmHr,
                    drainageStressPercent = drainageStressPercent,
                    isUnderpassOrDepression = loc.elevationMeters <= 874.0
                )
                actions.add(action)
            }
        }

        roads.forEach { road ->
            val roadLevel = RiskLevel.fromScore(road.riskPercentage)
            if (roadLevel != RiskLevel.LOW && isCooldownExpired(road.id) && alerts.none { it.location.contains(road.roadName) }) {
                val alert = createCitizenAlert(
                    id = "alt_${road.id}_${System.currentTimeMillis() % 10000}",
                    location = road.roadName,
                    riskPercentage = road.riskPercentage,
                    expectedTime = road.predictionTime,
                    reason = road.reasons.firstOrNull() ?: "Culvert bottleneck hydraulic surcharge (${road.drainageStressPercent}%)",
                    recommendedAction = "Avoid corridor. Use designated high-elevation bypass route.",
                    isVerifiedObservation = false
                )
                alerts.add(alert)
                recordAlertDispatched(road.id)
            }
        }

        return Pair(alerts, actions)
    }

    /**
     * Haversine formula for distance in km
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
