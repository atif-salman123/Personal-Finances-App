package com.example.data

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
}
