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

class CurrencyRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates = _exchangeRates.asStateFlow()

    init {
        loadExchangeRatesFromCache()
    }

    suspend fun fetchExchangeRates(isNetworkAccessEnabled: Boolean) {
        if (!isNetworkAccessEnabled) {
            loadExchangeRatesFromCache()
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.exchangerate-api.com/v4/latest/USD")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val ratesJson = json.getJSONObject("rates")
                val ratesMap = mutableMapOf<String, Double>()

                ratesJson.keys().forEach { key ->
                    ratesMap[key.lowercase()] = ratesJson.getDouble(key)
                }

                _exchangeRates.value = ratesMap
                prefs.edit().putString("cached_exchange_rates", ratesJson.toString()).apply()
                prefs.edit().putLong("last_rates_update", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
                loadExchangeRatesFromCache()
            }
        }
    }

    private fun loadExchangeRatesFromCache() {
        val cached = prefs.getString("cached_exchange_rates", null)
        if (cached != null) {
            try {
                val ratesJson = JSONObject(cached)
                val ratesMap = mutableMapOf<String, Double>()
                ratesJson.keys().forEach { key ->
                    ratesMap[key.lowercase()] = ratesJson.getDouble(key)
                }
                _exchangeRates.value = ratesMap
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // 提供基礎離線匯率
            _exchangeRates.value = mapOf(
                "usd" to 1.0, "twd" to 32.5, "jpy" to 155.0,
                "eur" to 0.92, "cny" to 7.25, "hkd" to 7.8, "gbp" to 0.79
            )
        }
    }
}
