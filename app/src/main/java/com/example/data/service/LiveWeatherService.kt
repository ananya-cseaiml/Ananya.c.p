package com.example.data.service

import com.example.data.model.DataStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Live weather and forecast result model.
 *
 * EXPLICIT UNIT DEFINITIONS:
 * - rainfallMmHr: Current surface rainfall intensity in millimetres per hour (mm/h).
 *   Derived from Open-Meteo current precipitation interval:
 *   rainfallIntensity_mm_per_hour = precipitation_mm / intervalHours (with intervalHours = 1.0)
 * - recentRainfall1hMm: Accumulated rainfall in millimetres (mm) over preceding 1-hour interval.
 * - recentRainfall3hMm: Accumulated rainfall in millimetres (mm) over preceding 3-hour interval.
 * - recentRainfall6hMm: Accumulated rainfall in millimetres (mm) over preceding 6-hour interval.
 * - antecedent24hRainfallMm: Accumulated rainfall in millimetres (mm) over antecedent 24-hour interval.
 * - forecast1hMm: Forecasted accumulated rainfall in millimetres (mm) over next 1-hour interval.
 *   (Corresponding 1-hour forecast intensity = forecast1hMm / 1.0 h = mm/h)
 * - forecast3hMm: Forecasted accumulated rainfall in millimetres (mm) over next 3-hour interval.
 *   (Corresponding 3-hour average forecast intensity = forecast3hMm / 3.0 h = mm/h)
 * - forecast6hMm: Forecasted accumulated rainfall in millimetres (mm) over next 6-hour interval.
 *   (Corresponding 6-hour average forecast intensity = forecast6hMm / 6.0 h = mm/h)
 */
data class LiveWeatherResult(
    val rainfallMmHr: Double,
    val recentRainfall1hMm: Double,
    val recentRainfall3hMm: Double = 0.0,
    val recentRainfall6hMm: Double = 0.0,
    val antecedent24hRainfallMm: Double = 0.0,
    val forecast1hMm: Double = 0.0,
    val forecast3hMm: Double = 0.0,
    val forecast6hMm: Double = 0.0,
    val temperatureC: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double = 12.0,
    val weatherDesc: String,
    val status: DataStatus,
    val sourceName: String,
    val lastUpdated: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val latitude: Double = 12.9270,
    val longitude: Double = 77.6765
) {
    /**
     * Checks if cached weather data has expired (> 30 minutes old)
     */
    val isStale: Boolean
        get() = (System.currentTimeMillis() - timestampMs) > (30 * 60 * 1000L)

    companion object {
        /**
         * Converts accumulated precipitation (mm) over an interval (hours) to average intensity (mm/h).
         * Formula: rainfallIntensity_mm_per_hour = precipitation_mm / intervalHours
         */
        fun toIntensityMmHr(precipitationMm: Double, intervalHours: Double): Double {
            if (intervalHours <= 0.0) return 0.0
            return Math.round((precipitationMm / intervalHours) * 10.0) / 10.0
        }
    }
}

class LiveWeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Default Bellandur–Agara catchment coordinates (Ward 150/174)
    private val defaultLat = 12.9270
    private val defaultLng = 77.6765

    private val timeFormat = SimpleDateFormat("HH:mm:ss (dd MMM)", Locale.ENGLISH)

    /**
     * Fetches real live weather & numerical rainfall forecast from Open-Meteo API.
     * Validates inputs, handles timeouts, and guarantees deterministic data status.
     */
    suspend fun fetchLiveWeather(
        latitude: Double = defaultLat,
        longitude: Double = defaultLng
    ): LiveWeatherResult = withContext(Dispatchers.IO) {
        val targetLat = if (latitude in 8.0..37.0) latitude else defaultLat
        val targetLng = if (longitude in 68.0..97.0) longitude else defaultLng

        try {
            // Real Open-Meteo High-Resolution NWP API query with past 24 hours and 12 forecast hours
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$targetLat&longitude=$targetLng&current=temperature_2m,relative_humidity_2m,precipitation,rain,weather_code,wind_speed_10m&hourly=precipitation,rain&past_hours=24&forecast_hours=12&timezone=Asia%2FKolkata"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                    val current = json.optJSONObject("current")
                    val rawRain = current?.optDouble("rain", current.optDouble("precipitation", 0.0)) ?: 0.0
                    // Sanitize & validate rainfall value
                    val rainCurrent = max(0.0, if (rawRain.isNaN()) 0.0 else rawRain)

                    val temp = current?.optDouble("temperature_2m", 26.5) ?: 26.5
                    val humidity = (current?.optInt("relative_humidity_2m", 78) ?: 78).coerceIn(0, 100)
                    val wind = current?.optDouble("wind_speed_10m", 12.0) ?: 12.0
                    val weatherCode = current?.optInt("weather_code", 0) ?: 0

                    val desc = when (weatherCode) {
                        0 -> "Clear sky"
                        1, 2, 3 -> "Partly cloudy"
                        45, 48 -> "Foggy"
                        51, 53, 55 -> "Drizzle"
                        61, 63 -> "Moderate rain"
                        65 -> "Heavy rain downpour"
                        80, 81 -> "Rain showers"
                        82 -> "Violent convective storm"
                        95, 96, 99 -> "Thunderstorm with heavy convective rain"
                        else -> if (rainCurrent > 0.1) "Rain (${String.format(Locale.ENGLISH, "%.1f", rainCurrent)} mm/hr)" else "Dry conditions"
                    }

                    // Parse real past hours and forecast hours from Open-Meteo hourly array
                    val hourly = json.optJSONObject("hourly")
                    val precipArray = hourly?.optJSONArray("precipitation")

                    var past1h = rainCurrent
                    var past3h = rainCurrent
                    var past6h = rainCurrent
                    var ante24h = 0.0
                    var fcast1h = 0.0
                    var fcast3h = 0.0
                    var fcast6h = 0.0

                    if (precipArray != null && precipArray.length() >= 24) {
                        // Index 24 corresponds to the current hour (since past_hours=24)
                        val currentIndex = 24.coerceAtMost(precipArray.length() - 1)

                        // Sum antecedent 24 hours: indices 0 until currentIndex
                        var sum24 = 0.0
                        for (i in 0 until currentIndex) {
                            sum24 += max(0.0, precipArray.optDouble(i, 0.0))
                        }
                        ante24h = Math.round(sum24 * 10.0) / 10.0

                        // Last 1h
                        past1h = max(0.0, precipArray.optDouble(currentIndex, rainCurrent))

                        // Last 3h
                        var sum3 = 0.0
                        for (i in (currentIndex - 2).coerceAtLeast(0)..currentIndex) {
                            sum3 += max(0.0, precipArray.optDouble(i, 0.0))
                        }
                        past3h = Math.round(sum3 * 10.0) / 10.0

                        // Last 6h
                        var sum6 = 0.0
                        for (i in (currentIndex - 5).coerceAtLeast(0)..currentIndex) {
                            sum6 += max(0.0, precipArray.optDouble(i, 0.0))
                        }
                        past6h = Math.round(sum6 * 10.0) / 10.0

                        // Forecast 1h, 3h, 6h
                        fcast1h = if (currentIndex + 1 < precipArray.length()) {
                            max(0.0, precipArray.optDouble(currentIndex + 1, 0.0))
                        } else 0.0

                        var fsum3 = 0.0
                        for (i in (currentIndex + 1)..((currentIndex + 3).coerceAtMost(precipArray.length() - 1))) {
                            fsum3 += max(0.0, precipArray.optDouble(i, 0.0))
                        }
                        fcast3h = Math.round(fsum3 * 10.0) / 10.0

                        var fsum6 = 0.0
                        for (i in (currentIndex + 1)..((currentIndex + 6).coerceAtMost(precipArray.length() - 1))) {
                            fsum6 += max(0.0, precipArray.optDouble(i, 0.0))
                        }
                        fcast6h = Math.round(fsum6 * 10.0) / 10.0
                    }

                    LiveWeatherResult(
                        rainfallMmHr = rainCurrent,
                        recentRainfall1hMm = past1h,
                        recentRainfall3hMm = past3h,
                        recentRainfall6hMm = past6h,
                        antecedent24hRainfallMm = ante24h,
                        forecast1hMm = fcast1h,
                        forecast3hMm = fcast3h,
                        forecast6hMm = fcast6h,
                        temperatureC = temp,
                        humidityPercent = humidity,
                        windSpeedKmh = wind,
                        weatherDesc = desc,
                        status = DataStatus.LIVE,
                        sourceName = "Open-Meteo High-Resolution Weather API (ECMWF / DWD NWP)",
                        lastUpdated = timeFormat.format(Date()),
                        timestampMs = System.currentTimeMillis(),
                        latitude = targetLat,
                        longitude = targetLng
                    )
                } else {
                    LiveWeatherResult(
                        rainfallMmHr = 0.0,
                        recentRainfall1hMm = 0.0,
                        temperatureC = 26.5,
                        humidityPercent = 75,
                        weatherDesc = "Live API unavailable (HTTP ${response.code})",
                        status = DataStatus.UNAVAILABLE,
                        sourceName = "Open-Meteo High-Resolution Weather API",
                        lastUpdated = "Connection Failed",
                        timestampMs = System.currentTimeMillis()
                    )
                }
            }
        } catch (e: Exception) {
            LiveWeatherResult(
                rainfallMmHr = 0.0,
                recentRainfall1hMm = 0.0,
                temperatureC = 26.0,
                humidityPercent = 70,
                weatherDesc = "Offline / Connection timeout",
                status = DataStatus.UNAVAILABLE,
                sourceName = "Open-Meteo High-Resolution Weather API",
                lastUpdated = "Offline",
                timestampMs = System.currentTimeMillis()
            )
        }
    }
}
