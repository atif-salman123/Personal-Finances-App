package com.example.data

import java.text.SimpleDateFormat
import java.util.Locale

object CsvParser {

    data class ParsedRow(
        val date: Long,
        val description: String,
        val amount: Double,
        val currency: String,
        val isIncome: Boolean,
        val suggestedCategoryName: String
    )

    /**
     * Parses CSV lines of bank statements.
     * Expected format: Date, Description, Amount, Currency
     * Example: 2026-06-15, Netflix Subscription, -15.49, USD
     */
    fun parseCsv(csvText: String, availableCategories: List<Category>): List<ParsedRow> {
        val rows = mutableListOf<ParsedRow>()
        val lines = csvText.lines()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (line in lines) {
            val parts = line.split(",").map { it.trim() }
            if (parts.size < 3) continue
            // Skip header if matches
            if (parts[0].lowercase().contains("date") || parts[1].lowercase().contains("description")) continue

            try {
                val dateStr = parts[0]
                val date = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                val description = parts[1]
                val amountStr = parts[2]
                val amount = amountStr.toDouble()
                val currency = if (parts.size >= 4) parts[3].uppercase() else "USD"

                val isIncome = amount > 0
                val absoluteAmount = Math.abs(amount)

                // Smart auto-categorizer based on keywords
                val suggestedCategory = autoCategorize(description, isIncome, availableCategories)

                rows.add(
                    ParsedRow(
                        date = date,
                        description = description,
                        amount = absoluteAmount,
                        currency = currency,
                        isIncome = isIncome,
                        suggestedCategoryName = suggestedCategory
                    )
                )
            } catch (e: Exception) {
                // Skip malformed rows silently
            }
        }
        return rows
    }

    private fun autoCategorize(description: String, isIncome: Boolean, categories: List<Category>): String {
        val descLower = description.lowercase()

        if (isIncome) {
            return when {
                descLower.contains("salary") || descLower.contains("paycheck") || descLower.contains("payroll") -> "Salary"
                descLower.contains("freelance") || descLower.contains("consulting") || descLower.contains("contract") -> "Freelance & Consulting"
                descLower.contains("dividend") || descLower.contains("interest") || descLower.contains("stock") -> "Dividends & Interest"
                else -> "Gifts & Side Hustles"
            }
        } else {
            return when {
                descLower.contains("netflix") || descLower.contains("spotify") || descLower.contains("disney") || descLower.contains("hulu") || descLower.contains("hbo") -> "Streaming Services"
                descLower.contains("rent") || descLower.contains("mortgage") || descLower.contains("landlord") || descLower.contains("lease") -> "Rent / Mortgage"
                descLower.contains("tax") || descLower.contains("irs") || descLower.contains("property tax") -> "Property Taxes"
                descLower.contains("groceries") || descLower.contains("supermarket") || descLower.contains("walmart") || descLower.contains("safeway") || descLower.contains("target") -> "Groceries"
                descLower.contains("restaurant") || descLower.contains("cafe") || descLower.contains("starbucks") || descLower.contains("mcdonald") || descLower.contains("burger") || descLower.contains("pizza") || descLower.contains("dining") -> "Restaurants & Cafes"
                descLower.contains("delivery") || descLower.contains("ubereats") || descLower.contains("doordash") || descLower.contains("grubhub") || descLower.contains("deliveroo") || descLower.contains("foodpanda") -> "Delivery & Fast Food"
                descLower.contains("uber") || descLower.contains("lyft") || descLower.contains("cab") || descLower.contains("rideshare") -> "Ridesharing"
                descLower.contains("fuel") || descLower.contains("gas") || descLower.contains("chevron") || descLower.contains("shell") || descLower.contains("petrol") -> "Fuel / Gas"
                descLower.contains("subway") || descLower.contains("metro") || descLower.contains("bus") || descLower.contains("transit") -> "Public Transit"
                descLower.contains("insurance") || descLower.contains("geico") || descLower.contains("allstate") || descLower.contains("metlife") -> "Home Insurance"
                descLower.contains("electric") || descLower.contains("power") || descLower.contains("utility") || descLower.contains("electricity") -> "Electricity"
                descLower.contains("water") || descLower.contains("sewer") || descLower.contains("gas bill") -> "Water & Gas"
                descLower.contains("internet") || descLower.contains("comcast") || descLower.contains("wifi") || descLower.contains("broadband") -> "Internet & Cable"
                descLower.contains("phone") || descLower.contains("mobile") || descLower.contains("verizon") || descLower.contains("att") || descLower.contains("tmobile") -> "Mobile Plan"
                descLower.contains("gym") || descLower.contains("fitness") || descLower.contains("crossfit") || descLower.contains("workout") -> "Sports & Gym"
                descLower.contains("pharmacy") || descLower.contains("cvs") || descLower.contains("walgreens") || descLower.contains("medical") || descLower.contains("doctor") || descLower.contains("clinic") || descLower.contains("hospital") -> "Medical & Pharmacy"
                else -> {
                    // Fallback to finding first expense subcategory or any general category
                    val subcats = categories.filter { !it.isIncome && it.subCategoryOfId != null }
                    if (subcats.isNotEmpty()) subcats.first().name else "Groceries"
                }
            }
        }
    }

    /**
     * Generates a realistic CSV mock template that the user can copy/paste.
     */
    fun getMockCsvString(): String {
        return """
            Date, Description, Amount, Currency
            2026-06-01, Monthly Employer Salary, 4500.00, USD
            2026-06-02, landlord Rent Payment, -1200.00, USD
            2026-06-04, Safeway Groceries, -156.40, USD
            2026-06-05, Netflix Subscription, -15.49, USD
            2026-06-08, Starbucks Cafe Coffee, -6.50, USD
            2026-06-10, Shell Fuel Petrol, -45.00, USD
            2026-06-12, Uber Rideshare, -22.30, USD
            2026-06-15, Comcast Internet, -79.99, USD
            2026-06-18, Gym Membership, -50.00, USD
            2026-06-20, CVS Pharmacy Medicine, -12.50, USD
            2026-06-22, Freelance Design Work, 350.00, USD
        """.trimIndent()
    }
}
