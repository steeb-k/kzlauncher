package app.olauncher.data

data class WeatherSnapshot(
    val currentTemp: Double,
    val highTemp: Double,
    val lowTemp: Double,
    val weatherCode: Int,
    val fetchedAt: Long,
    val fahrenheit: Boolean
)

data class GeocodeResult(
    val name: String,
    val country: String,
    val countryCode: String,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double
) {
    fun displayLabel(): String {
        val parts = mutableListOf(name)
        if (!admin1.isNullOrBlank() && admin1 != name) parts.add(admin1)
        if (countryCode.isNotBlank()) parts.add(countryCode) else if (country.isNotBlank()) parts.add(country)
        return parts.joinToString(", ")
    }
}
