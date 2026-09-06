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
 * Evaluates risk threshold, road relevance, user distance, prediction horizon, and alert cooldown.
 * Generates both citizen-facing flood alerts and government operational actions.
 * Prevents duplicate alerts and spamming.
 */
class AlertEngine(
    var highRiskThreshold: Int = 60,
    var severeRiskThreshold: Int = 80,
    var alertCooldownMs: Long = 300_000L // 5-minute cooldown per corridor
) {
    // Tracks the last alert time per location ID to prevent duplicates
    private val lastAlertTimeMap = ConcurrentHashMap<String, Long>()
    private val timeFormat = SimpleDateFormat("HH:mm, dd MMM", Locale.ENGLISH)

    /**
     * Determines whether an alert should be generated for a location
     */
    fun shouldGenerateAlert(
        locationId: String,
        riskPercentage: Int,
        userLat: Double? = null,
        userLng: Double? = null,
        targetLat: Double? = null,
        targetLng: Double? = null,
        maxDistanceKm: Double = 10.0
    ): Boolean {
        // 1. Check risk threshold
        if (riskPercentage < highRiskThreshold) {
            return false
        }

        // 2. Check distance relevance if user coordinates are available
        if (userLat != null && userLng != null && targetLat != null && targetLng != null) {
            val distKm = calculateDistanceKm(userLat, userLng, targetLat, targetLng)
            if (distKm > maxDistanceKm) {
                return false
            }
        }

        // 3. Check alert cooldown to prevent duplicate alerts
        val now = System.currentTimeMillis()
        val lastAlert = lastAlertTimeMap[locationId] ?: 0L
        if (now - lastAlert < alertCooldownMs) {
            return false
        }

        return true
    }

    /**
     * Records that an alert was dispatched to enforce cooldown
     */
    fun recordAlertDispatched(locationId: String) {
        lastAlertTimeMap[locationId] = System.currentTimeMillis()
    }

    /**
     * Clears cooldown memory (e.g. on scenario reset)
     */
    fun resetCooldowns() {
        lastAlertTimeMap.clear()
    }

    /**
     * Generates a Citizen-Facing Flood Alert
     * Strictly fulfills Requirement 18:
     * - Location
     * - Risk %
     * - Severity
     * - Expected time
     * - Reason
     * - Recommended action
     * - Issued timestamp
     * - Clearly labeled: MODEL-GENERATED PREDICTION
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
        val severity = when {
            riskPercentage >= severeRiskThreshold -> RiskLevel.SEVERE
            riskPercentage >= highRiskThreshold -> RiskLevel.HIGH
            else -> RiskLevel.WATCH
        }

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
     * Generates an actionable Government Decision-Support recommendation
     * strictly fulfilling Requirement 20
     */
    fun createGovernmentAction(
        location: String,
        riskPercentage: Int,
        rainfallMmHr: Double,
        drainageStressPercent: Int,
        isUnderpassOrDepression: Boolean
    ): GovernmentActionItem {
        val severity = when {
            riskPercentage >= severeRiskThreshold -> RiskLevel.SEVERE
            riskPercentage >= highRiskThreshold -> RiskLevel.HIGH
            else -> RiskLevel.WATCH
        }

        val (action, agency, priority) = when {
            rainfallMmHr >= 50.0 && drainageStressPercent >= 80 -> Triple(
                "Inspect culvert inlet for debris clogging, open barrier screens & position 50HP mobile dewatering pump",
                "BBMP Stormwater Drain (SWD) Dept",
                "CRITICAL"
            )
            severity == RiskLevel.SEVERE -> Triple(
                "Deploy emergency barricades, close flooded underpass ingress & dispatch NDRF / Civil Defence unit",
                "Bengaluru Traffic Police & KSDMA",
                "IMMEDIATE"
            )
            isUnderpassOrDepression && riskPercentage >= 60 -> Triple(
                "Verify automatic sump pump float sensors and initiate traffic diversion signage via ridge bypass",
                "BBMP Road Infrastructure & Traffic Police",
                "HIGH"
            )
            drainageStressPercent >= 70 -> Triple(
                "Clear roadside silt traps and monitor downstream lake weir gates",
                "BBMP Engineering & BWSSB",
                "HIGH"
            )
            else -> Triple(
                "Routine pre-monsoon culvert surveillance and camber drainage clearance",
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
     * Evaluates all locations and roads in current catchment state and produces active alerts
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
            if (loc.currentRisk >= highRiskThreshold) {
                val alert = createCitizenAlert(
                    id = "alt_${loc.id}_${System.currentTimeMillis() % 10000}",
                    location = "${loc.name} (${loc.ward})",
                    riskPercentage = loc.currentRisk,
                    expectedTime = loc.predictedTimeWindow,
                    reason = loc.whyAtRisk,
                    recommendedAction = loc.recommendedAction,
                    isVerifiedObservation = false
                )
                alerts.add(alert)
                recordAlertDispatched(loc.id)

                val action = createGovernmentAction(
                    location = loc.name,
                    riskPercentage = loc.currentRisk,
                    rainfallMmHr = rainfallMmHr,
                    drainageStressPercent = drainageStressPercent,
                    isUnderpassOrDepression = loc.elevationMeters <= 875.0
                )
                actions.add(action)
            }
        }

        roads.forEach { road ->
            if (road.riskPercentage >= highRiskThreshold && alerts.none { it.location.contains(road.roadName) }) {
                val alert = createCitizenAlert(
                    id = "alt_${road.id}_${System.currentTimeMillis() % 10000}",
                    location = road.roadName,
                    riskPercentage = road.riskPercentage,
                    expectedTime = road.predictionTime,
                    reason = road.reasons.firstOrNull() ?: "Culvert hydraulic surcharge",
                    recommendedAction = "Avoid corridor. Use designated high-elevation bypass.",
                    isVerifiedObservation = false
                )
                alerts.add(alert)
            }
        }

        return Pair(alerts, actions)
    }

    /**
     * Haversine formula for distance in km
     */
    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
