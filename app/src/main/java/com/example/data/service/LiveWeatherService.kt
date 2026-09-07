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
    val latitude: Double = 12.9270,
    val longitude: Double = 77.6765
)

class LiveWeatherService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    // Default Bellandur–Agara catchment coordinates
    private val defaultLat = 12.9270
    private val defaultLng = 77.6765

    private val timeFormat = SimpleDateFormat("HH:mm:ss (dd MMM)", Locale.ENGLISH)

    suspend fun fetchLiveWeather(
        latitude: Double = defaultLat,
        longitude: Double = defaultLng
    ): LiveWeatherResult = withContext(Dispatchers.IO) {
        val targetLat = if (latitude in 8.0..37.0) latitude else defaultLat
        val targetLng = if (longitude in 68.0..97.0) longitude else defaultLng

        try {
            // Real Open-Meteo API query with past 24 hours and 12 forecast hours
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$targetLat&longitude=$targetLng&current=temperature_2m,relative_humidity_2m,precipitation,rain,weather_code,wind_speed_10m&hourly=precipitation,rain&past_hours=24&forecast_hours=12&timezone=Asia%2FKolkata"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)

                val current = json.optJSONObject("current")
                val rainCurrent = current?.optDouble("rain", current.optDouble("precipitation", 0.0)) ?: 0.0
                val temp = current?.optDouble("temperature_2m", 26.5) ?: 26.5
                val humidity = current?.optInt("relative_humidity_2m", 78) ?: 78
                val wind = current?.optDouble("wind_speed_10m", 12.0) ?: 12.0
                val weatherCode = current?.optInt("weather_code", 0) ?: 0

                val desc = when (weatherCode) {
                    0 -> "Clear sky"
                    1, 2, 3 -> "Partly cloudy"
                    45, 48 -> "Foggy"
                    51, 53, 55 -> "Drizzle"
                    61, 63 -> "Moderate rain"
                    65 -> "Heavy rain"
                    80, 81 -> "Rain showers"
                    82 -> "Violent convective storm"
                    95, 96, 99 -> "Thunderstorm with heavy downpour"
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

                    // Sum antecedent 24 hours: indices 0 until 24
                    var sum24 = 0.0
                    for (i in 0 until currentIndex) {
                        sum24 += precipArray.optDouble(i, 0.0)
                    }
                    ante24h = sum24

                    // Last 1h
                    past1h = precipArray.optDouble(currentIndex, rainCurrent)

                    // Last 3h
                    var sum3 = 0.0
                    for (i in (currentIndex - 2).coerceAtLeast(0)..currentIndex) {
                        sum3 += precipArray.optDouble(i, 0.0)
                    }
                    past3h = sum3

                    // Last 6h
                    var sum6 = 0.0
                    for (i in (currentIndex - 5).coerceAtLeast(0)..currentIndex) {
                        sum6 += precipArray.optDouble(i, 0.0)
                    }
                    past6h = sum6

                    // Forecast 1h, 3h, 6h
                    fcast1h = if (currentIndex + 1 < precipArray.length()) precipArray.optDouble(currentIndex + 1, 0.0) else 0.0

                    var fsum3 = 0.0
                    for (i in (currentIndex + 1)..((currentIndex + 3).coerceAtMost(precipArray.length() - 1))) {
                        fsum3 += precipArray.optDouble(i, 0.0)
                    }
                    fcast3h = fsum3

                    var fsum6 = 0.0
                    for (i in (currentIndex + 1)..((currentIndex + 6).coerceAtMost(precipArray.length() - 1))) {
                        fsum6 += precipArray.optDouble(i, 0.0)
                    }
                    fcast6h = fsum6
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
                    sourceName = "Open-Meteo Weather API (ECMWF / DWD High-Resolution NWP)",
                    lastUpdated = timeFormat.format(Date())
                )
            } else {
                LiveWeatherResult(
                    rainfallMmHr = 0.0,
                    recentRainfall1hMm = 0.0,
                    temperatureC = 26.5,
                    humidityPercent = 75,
                    weatherDesc = "Live API unavailable (HTTP ${response.code})",
                    status = DataStatus.UNAVAILABLE,
                    sourceName = "Open-Meteo Weather API",
                    lastUpdated = "Unavailable"
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
                sourceName = "Open-Meteo Weather API",
                lastUpdated = "Unavailable"
            )
        }
    }
}
