package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

object CurrencyRates {
    // Standard base currency is USD. All rates are relative to 1 USD.
    var rates = mutableMapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "AED" to 3.67,
        "INR" to 83.50
    )

    val symbols = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "AED" to "د.إ",
        "INR" to "₹"
    )

    data class HistoricalRate(val month: String, val rate: Double)

    // Historical rates trends relative to USD
    val historicalRates = mapOf(
        "EUR" to listOf(
            HistoricalRate("2026-01", 0.91),
            HistoricalRate("2026-02", 0.92),
            HistoricalRate("2026-03", 0.93),
            HistoricalRate("2026-04", 0.91),
            HistoricalRate("2026-05", 0.92),
            HistoricalRate("2026-06", 0.92)
        ),
        "GBP" to listOf(
            HistoricalRate("2026-01", 0.78),
            HistoricalRate("2026-02", 0.79),
            HistoricalRate("2026-03", 0.79),
            HistoricalRate("2026-04", 0.78),
            HistoricalRate("2026-05", 0.77),
            HistoricalRate("2026-06", 0.79)
        ),
        "AED" to listOf(
            HistoricalRate("2026-01", 3.67),
            HistoricalRate("2026-02", 3.67),
            HistoricalRate("2026-03", 3.67),
            HistoricalRate("2026-04", 3.67),
            HistoricalRate("2026-05", 3.67),
            HistoricalRate("2026-06", 3.67)
        ),
        "INR" to listOf(
            HistoricalRate("2026-01", 82.90),
            HistoricalRate("2026-02", 83.10),
            HistoricalRate("2026-03", 83.40),
            HistoricalRate("2026-04", 83.30),
            HistoricalRate("2026-05", 83.50),
            HistoricalRate("2026-06", 83.50)
        )
    )

    fun getSymbol(currency: String): String = symbols[currency] ?: "$"

    /**
     * Convert an amount from one currency to another using current rates.
     */
    fun convert(amount: Double, from: String, to: String): Double {
        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0
        val amountInUSD = amount / fromRate
        return amountInUSD * toRate
    }

    /**
     * Format currency amount with symbol.
     */
    fun format(amount: Double, currency: String): String {
        val symbol = getSymbol(currency)
        return String.format("%s %.2f", symbol, amount)
    }

    /**
     * Simulate fetching real-time exchange rates or allow custom overrides.
     */
    fun updateRates(newRates: Map<String, Double>) {
        rates.putAll(newRates)
    }

    /**
     * Fetch real-time exchange rates from API
     */
    suspend fun fetchLatestRates(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://open.er-api.com/v6/latest/USD")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(responseText)
            if (jsonObject.getString("result") == "success") {
                val ratesJson = jsonObject.getJSONObject("rates")
                val newRates = mutableMapOf<String, Double>()
                val keys = ratesJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key in listOf("USD", "EUR", "GBP", "AED", "INR")) {
                        newRates[key] = ratesJson.getDouble(key)
                    }
                }
                updateRates(newRates)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
