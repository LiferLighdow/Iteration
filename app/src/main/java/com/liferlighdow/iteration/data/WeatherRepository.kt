package com.liferlighdow.iteration.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo = _weatherInfo.asStateFlow()

    private val _weatherError = MutableStateFlow<String?>(null)
    val weatherError = _weatherError.asStateFlow()

    private val _weatherProvider = MutableStateFlow(
        try {
            WeatherProvider.valueOf(prefs.getString("weather_provider", "MET_NORWAY") ?: "MET_NORWAY")
        } catch (e: Exception) {
            WeatherProvider.MET_NORWAY
        }
    )
    val weatherProvider = _weatherProvider.asStateFlow()

    private val _customLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    private val _customCityName = MutableStateFlow<String?>(null)

    init {
        loadWeatherFromCache()
    }

    suspend fun fetchWeather(isNetworkAccessEnabled: Boolean) {
        if (!isNetworkAccessEnabled) {
            loadWeatherFromCache()
            return
        }

        _weatherError.value = "Updating..."
        withContext(Dispatchers.IO) {
            try {
                var lat = _customLocation.value?.first
                var lon = _customLocation.value?.second
                var cityName = _customCityName.value

                if (lat == null || lon == null) {
                    try {
                        val ipUrl = URL("https://free.freeipapi.com/api/json")
                        val ipConn = ipUrl.openConnection() as HttpURLConnection
                        ipConn.connectTimeout = 5000
                        val ipRes = ipConn.inputStream.bufferedReader().use { it.readText() }
                        val ipJson = JSONObject(ipRes)
                        lat = ipJson.getDouble("latitude")
                        lon = ipJson.getDouble("longitude")
                        cityName = ipJson.getString("cityName")
                        _customLocation.value = lat to lon
                        _customCityName.value = cityName
                    } catch (e: Exception) {
                        lat = 25.03; lon = 121.56; cityName = "Taipei"
                    }
                }

                val errorMsg = when (_weatherProvider.value) {
                    WeatherProvider.MET_NORWAY -> fetchFromMetNorway(lat!!, lon!!, cityName ?: "Unknown")
                    WeatherProvider.OPEN_METEO -> fetchFromOpenMeteo(lat!!, lon!!, cityName ?: "Unknown")
                }
                _weatherError.value = errorMsg
            } catch (e: Exception) {
                val msg = e.message ?: e.toString()
                _weatherError.value = if (msg.contains("resolve") || msg.contains("address")) "Network Error" else msg
                loadWeatherFromCache()
            }
        }
    }

    private fun fetchFromMetNorway(lat: Double, lon: Double, cityName: String): String? {
        val url = URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Iteration/1.0")
        return if (connection.responseCode == 200) {
            val res = connection.inputStream.bufferedReader().use { it.readText() }
            prefs.edit().putString("cached_weather_json", res).apply()
            parseAndSetMetWeather(JSONObject(res), cityName)
            null
        } else "Service Unavailable (${connection.responseCode})"
    }

    private fun fetchFromOpenMeteo(lat: Double, lon: Double, cityName: String): String? {
        val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=weather_code,temperature_2m_max,temperature_2m_min&current_weather=true&timezone=auto")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return if (connection.responseCode == 200) {
            val res = connection.inputStream.bufferedReader().use { it.readText() }
            prefs.edit().putString("cached_weather_json", res).apply()
            parseAndSetWeather(JSONObject(res), cityName)
            null
        } else "Service Unavailable (${connection.responseCode})"
    }

    fun setWeatherProvider(provider: WeatherProvider) {
        _weatherProvider.value = provider
        prefs.edit().putString("weather_provider", provider.name).apply()
    }

    fun updateLocation(lat: Double, lon: Double, name: String) {
        _customLocation.value = lat to lon
        _customCityName.value = name
    }

    fun resetToIpLocation() {
        _customLocation.value = null
        _customCityName.value = null
    }

    private fun loadWeatherFromCache() {
        val cached = prefs.getString("cached_weather_json", null) ?: return
        try {
            if (cached.contains("timeseries")) {
                parseAndSetMetWeather(JSONObject(cached))
            } else {
                parseAndSetWeather(JSONObject(cached))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun parseAndSetMetWeather(json: JSONObject, cityName: String = "Taipei") {
        val properties = json.getJSONObject("properties")
        val timeseries = properties.getJSONArray("timeseries")
        val currentData = timeseries.getJSONObject(0).getJSONObject("data").getJSONObject("instant").getJSONObject("details")
        val currentTemp = currentData.getDouble("air_temperature")

        val dailyList = mutableListOf<DailyWeather>()
        val seenDates = mutableSetOf<String>()

        for (i in 0 until timeseries.length()) {
            val entry = timeseries.getJSONObject(i)
            val fullTime = entry.getString("time")
            val date = fullTime.split("T")[0]

            if (!seenDates.contains(date) && (fullTime.contains("T12:00:00Z") || fullTime.contains("T11:00:00Z"))) {
                val data = entry.getJSONObject("data")
                val details = data.getJSONObject("instant").getJSONObject("details")
                val weatherSymbol = try {
                    data.getJSONObject("next_12_hours").getJSONObject("summary").getString("symbol_code")
                } catch (e: Exception) {
                    try {
                        data.getJSONObject("next_6_hours").getJSONObject("summary").getString("symbol_code")
                    } catch (e2: Exception) { "clearsky_day" }
                }

                dailyList.add(
                    DailyWeather(
                        date = date,
                        maxTemp = details.getDouble("air_temperature"),
                        minTemp = details.getDouble("air_temperature") - 2.0,
                        weatherCode = convertMetSymbolToCode(weatherSymbol)
                    )
                )
                seenDates.add(date)
            }
            if (dailyList.size >= 6) break
        }
        _weatherInfo.value = WeatherInfo(currentTemp, dailyList, cityName)
    }

    private fun convertMetSymbolToCode(symbol: String): Int {
        return when {
            symbol.contains("clearsky") || symbol.contains("fair") -> 0
            symbol.contains("cloud") -> 3
            symbol.contains("fog") -> 45
            symbol.contains("rain") || symbol.contains("drizzle") -> 61
            symbol.contains("snow") -> 71
            symbol.contains("sleet") || symbol.contains("showers") -> 95
            else -> 1
        }
    }

    private fun parseAndSetWeather(json: JSONObject, cityName: String = "Taipei") {
        val current = json.getJSONObject("current_weather")
        val daily = json.getJSONObject("daily")
        val timeArray = daily.getJSONArray("time")
        val codeArray = daily.getJSONArray("weather_code")
        val maxArray = daily.getJSONArray("temperature_2m_max")
        val minArray = daily.getJSONArray("temperature_2m_min")

        val dailyList = mutableListOf<DailyWeather>()
        for (i in 0 until timeArray.length()) {
            dailyList.add(
                DailyWeather(
                    date = timeArray.getString(i),
                    maxTemp = maxArray.getDouble(i),
                    minTemp = minArray.getDouble(i),
                    weatherCode = codeArray.getInt(i)
                )
            )
        }
        _weatherInfo.value = WeatherInfo(current.getDouble("temperature"), dailyList, cityName)
    }
}
