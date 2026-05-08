package app.olauncher.helper

import app.olauncher.data.Constants
import app.olauncher.data.GeocodeResult
import app.olauncher.data.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WeatherClient {

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000

    suspend fun searchCity(query: String): List<GeocodeResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val url = "${Constants.URL_OPEN_METEO_GEOCODE}?name=${encode(trimmed)}&count=10&language=en&format=json"
        val body = httpGet(url)
        val root = JSONObject(body)
        val results = root.optJSONArray("results") ?: return@withContext emptyList()
        val out = ArrayList<GeocodeResult>(results.length())
        for (i in 0 until results.length()) {
            val r = results.getJSONObject(i)
            out.add(
                GeocodeResult(
                    name = r.optString("name"),
                    country = r.optString("country"),
                    countryCode = r.optString("country_code"),
                    admin1 = r.optString("admin1").takeIf { it.isNotBlank() },
                    latitude = r.optDouble("latitude", 0.0),
                    longitude = r.optDouble("longitude", 0.0)
                )
            )
        }
        out
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double, fahrenheit: Boolean): WeatherSnapshot = withContext(Dispatchers.IO) {
        val unit = if (fahrenheit) "fahrenheit" else "celsius"
        val url = buildString {
            append(Constants.URL_OPEN_METEO_FORECAST)
            append("?latitude=").append(latitude)
            append("&longitude=").append(longitude)
            append("&current=temperature_2m,weather_code")
            append("&daily=temperature_2m_max,temperature_2m_min")
            append("&temperature_unit=").append(unit)
            append("&timezone=auto")
            append("&forecast_days=1")
        }
        val body = httpGet(url)
        parseSnapshot(body, fahrenheit)
    }

    fun parseSnapshot(body: String, fahrenheit: Boolean): WeatherSnapshot {
        val root = JSONObject(body)
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        return WeatherSnapshot(
            currentTemp = current.getDouble("temperature_2m"),
            highTemp = highs.getDouble(0),
            lowTemp = lows.getDouble(0),
            weatherCode = current.getInt("weather_code"),
            fetchedAt = System.currentTimeMillis(),
            fahrenheit = fahrenheit
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun httpGet(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw java.io.IOException("HTTP $code from $urlString")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
