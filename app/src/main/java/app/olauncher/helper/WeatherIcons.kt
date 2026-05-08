package app.olauncher.helper

object WeatherIcons {

    private const val SUNNY = ""
    private const val PARTLY_CLOUDY = ""
    private const val CLOUDY = ""
    private const val FOGGY = ""
    private const val RAINY = ""
    private const val SNOWING = ""
    private const val THUNDERSTORM = ""

    fun iconFor(weatherCode: Int): String = when (weatherCode) {
        0 -> SUNNY
        1, 2 -> PARTLY_CLOUDY
        3 -> CLOUDY
        45, 48 -> FOGGY
        in 51..67 -> RAINY
        in 71..77 -> SNOWING
        in 80..82 -> RAINY
        85, 86 -> SNOWING
        in 95..99 -> THUNDERSTORM
        else -> CLOUDY
    }
}
