package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = FinanceRepository(database.financeDao())

    // --- Core StateFlows from DB ---
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val budgets = repository.allBudgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val debtLending = repository.allDebtLending.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val plannedPayments = repository.allPlannedPayments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI Configuration States ---
    val displayCurrency = MutableStateFlow("USD")
    val selectedMonth = MutableStateFlow("") // format: "YYYY-MM"
    
    // Auth & Security State
    val isAuthenticated = MutableStateFlow(false)
    val isBiometricsEnabled = MutableStateFlow(true)
    val userPin = MutableStateFlow("1234") // default pin for testing
    val isPrivateMode = MutableStateFlow(false)

    // Sync State
    val isSyncing = MutableStateFlow(false)
    val lastSyncTime = MutableStateFlow("Never")

    init {
        // Set current month as default (e.g. "2026-06")
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        selectedMonth.value = sdf.format(Date())

        viewModelScope.launch {
            repository.initializeData()
        }
    }

    // --- Amortization & Reporting Logic ---

    /**
     * Converts calendar year-month string "YYYY-MM" to a Date object.
     */
    private fun parseYearMonth(ym: String): Date? {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return try { sdf.parse(ym) } catch (e: Exception) { null }
    }

    /**
     * Check if a specific target month is within the amortization period.
     */
    private fun isMonthInAmortization(startMonth: String, durationMonths: Int, targetMonth: String): Boolean {
        val start = parseYearMonth(startMonth) ?: return false
        val target = parseYearMonth(targetMonth) ?: return false

        val cal = Calendar.getInstance()
        cal.time = start

        for (i in 0 until durationMonths) {
            val checkSdf = SimpleDateFormat("yyyy-MM", Locale.US)
            if (checkSdf.format(cal.time) == targetMonth) {
                return true
            }
            cal.add(Calendar.MONTH, 1)
        }
        return false
    }

    /**
     * Computes the final list of transactions for a specific month,
     * applying amortization logic (e.g. splitting quarterly/semi-annual/annual bills into months).
     */
    fun getProcessedTransactionsForMonth(targetMonth: String): Flow<List<Transaction>> {
        return transactions.map { allTx ->
            val processed = mutableListOf<Transaction>()

            for (tx in allTx) {
                val txMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(tx.date))

                if (!tx.isAmortized) {
                    // Standard transaction: must match target month exactly
                    if (txMonth == targetMonth) {
                        processed.add(tx)
                    }
                } else {
                    // Amortized transaction: check if target month falls in period
                    val startMonth = tx.amortizationStartMonth ?: txMonth
                    if (isMonthInAmortization(startMonth, tx.amortizationMonths, targetMonth)) {
                        // Create a virtual transaction split for this month
                        val splitAmount = tx.amount / tx.amortizationMonths
                        val splitAmountBase = tx.amountInBaseCurrency / tx.amortizationMonths
                        processed.add(
                            tx.copy(
                                amount = splitAmount,
                                amountInBaseCurrency = splitAmountBase,
                                description = "${tx.description} (Amortized ${tx.amortizationMonths}m split)"
                            )
                        )
                    }
                }
            }
            processed
        }
    }

    /**
     * Get aggregated monthly stats (Income, Expenses, Fixed vs Variable)
     */
    fun getMonthlyReports(targetMonth: String): Flow<MonthlyReportData> {
        return getProcessedTransactionsForMonth(targetMonth).map { monthTxs ->
            var totalIncome = 0.0
            var totalExpense = 0.0
            var fixedExpense = 0.0
            var variableExpense = 0.0

            val categoryBreakdown = mutableMapOf<Int, Double>() // categoryId to total spend (base currency)

            for (tx in monthTxs) {
                val amountBase = tx.amountInBaseCurrency
                if (tx.isIncome) {
                    totalIncome += amountBase
                } else {
                    totalExpense += amountBase
                    if (tx.isFixed) {
                        fixedExpense += amountBase
                    } else {
                        variableExpense += amountBase
                    }
                    categoryBreakdown[tx.categoryId] = (categoryBreakdown[tx.categoryId] ?: 0.0) + amountBase
                }
            }

            MonthlyReportData(
                month = targetMonth,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                fixedExpense = fixedExpense,
                variableExpense = variableExpense,
                categoryBreakdown = categoryBreakdown
            )
        }
    }

    // --- Core Operations ---

    fun setDisplayCurrency(currency: String) {
        displayCurrency.value = currency
    }

    fun setSelectedMonth(month: String) {
        selectedMonth.value = month
    }

    fun changeMonth(offset: Int) {
        val current = parseYearMonth(selectedMonth.value) ?: Date()
        val cal = Calendar.getInstance()
        cal.time = current
        cal.add(Calendar.MONTH, offset)
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        selectedMonth.value = sdf.format(cal.time)
    }

    fun addTransaction(
        accountId: Int,
        amount: Double,
        currency: String,
        categoryId: Int,
        description: String,
        isIncome: Boolean,
        isFixed: Boolean,
        isAmortized: Boolean = false,
        amortizationMonths: Int = 1,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            // Convert to base currency (USD)
            val baseAmount = CurrencyRates.convert(amount, currency, "USD")
            
            val startMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(date))

            val tx = Transaction(
                accountId = accountId,
                amount = amount,
                currency = currency,
                amountInBaseCurrency = baseAmount,
                categoryId = categoryId,
                date = date,
                description = description,
                isIncome = isIncome,
                isFixed = isFixed,
                isAmortized = isAmortized,
                amortizationMonths = amortizationMonths,
                amortizationStartMonth = if (isAmortized) startMonth else null
            )

            repository.insertTransaction(tx)

            // Adjust account balance
            val currentAccounts = accounts.value
            val targetAccount = currentAccounts.find { it.id == accountId }
            if (targetAccount != null) {
                // Adjust balance in account's original currency
                val finalAmountInAccountCurrency = CurrencyRates.convert(amount, currency, targetAccount.currency)
                val newBalance = if (isIncome) {
                    targetAccount.balance + finalAmountInAccountCurrency
                } else {
                    targetAccount.balance - finalAmountInAccountCurrency
                }
                repository.updateAccount(targetAccount.copy(balance = newBalance))
            }
        }
    }

    fun deleteTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx.id)

            // Reverse account balance adjustment
            val currentAccounts = accounts.value
            val targetAccount = currentAccounts.find { it.id == tx.accountId }
            if (targetAccount != null) {
                val finalAmountInAccountCurrency = CurrencyRates.convert(tx.amount, tx.currency, targetAccount.currency)
                val newBalance = if (tx.isIncome) {
                    targetAccount.balance - finalAmountInAccountCurrency
                } else {
                    targetAccount.balance + finalAmountInAccountCurrency
                }
                repository.updateAccount(targetAccount.copy(balance = newBalance))
            }
        }
    }

    fun addAccount(name: String, type: String, balance: Double, currency: String) {
        viewModelScope.launch {
            repository.insertAccount(Account(name = name, type = type, balance = balance, currency = currency))
        }
    }

    fun updateAccountBalance(accountId: Int, newBalance: Double) {
        viewModelScope.launch {
            val acc = accounts.value.find { it.id == accountId }
            if (acc != null) {
                repository.updateAccount(acc.copy(balance = newBalance))
            }
        }
    }

    fun addDebtLending(personName: String, type: String, amount: Double, currency: String, description: String) {
        viewModelScope.launch {
            val amountBase = CurrencyRates.convert(amount, currency, "USD")
            val dl = DebtLending(
                personName = personName,
                type = type,
                amount = amount,
                currency = currency,
                amountInBaseCurrency = amountBase,
                description = description,
                date = System.currentTimeMillis()
            )
            repository.insertDebtLending(dl)
        }
    }

    fun resolveDebtLending(dl: DebtLending) {
        viewModelScope.launch {
            repository.updateDebtLending(dl.copy(isResolved = true))
        }
    }

    fun deleteDebtLending(id: Int) {
        viewModelScope.launch {
            repository.deleteDebtLending(id)
        }
    }

    fun addPlannedPayment(name: String, amount: Double, currency: String, categoryId: Int, accountId: Int, frequency: String, isSubscription: Boolean, nextDueDate: Long) {
        viewModelScope.launch {
            val pp = PlannedPayment(
                name = name,
                amount = amount,
                currency = currency,
                categoryId = categoryId,
                accountId = accountId,
                frequency = frequency,
                isSubscription = isSubscription,
                nextDueDate = nextDueDate
            )
            repository.insertPlannedPayment(pp)
        }
    }

    fun triggerPlannedPaymentPaid(pp: PlannedPayment) {
        viewModelScope.launch {
            // Add a real transaction based on the planned payment
            addTransaction(
                accountId = pp.accountId,
                amount = pp.amount,
                currency = pp.currency,
                categoryId = pp.categoryId,
                description = pp.name,
                isIncome = false,
                isFixed = true,
                date = System.currentTimeMillis()
            )

            // Calculate next due date
            val cal = Calendar.getInstance()
            cal.timeInMillis = pp.nextDueDate
            when (pp.frequency) {
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "QUARTERLY" -> cal.add(Calendar.MONTH, 3)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
                else -> {
                    // For "ONCE", mark active as false
                    repository.updatePlannedPayment(pp.copy(isActive = false))
                    return@launch
                }
            }
            repository.updatePlannedPayment(pp.copy(nextDueDate = cal.timeInMillis))
        }
    }

    fun deletePlannedPayment(id: Int) {
        viewModelScope.launch {
            repository.deletePlannedPayment(id)
        }
    }

    fun addOrUpdateBudget(categoryId: Int, amountLimit: Double) {
        viewModelScope.launch {
            val currentMonth = selectedMonth.value
            val existing = budgets.value.find { it.categoryId == categoryId && it.month == currentMonth }
            if (existing != null) {
                repository.updateBudget(existing.copy(amountLimit = amountLimit))
            } else {
                repository.insertBudget(Budget(categoryId = categoryId, month = currentMonth, amountLimit = amountLimit))
            }
        }
    }

    // --- CSV Importing Operations ---

    fun importCsvContent(csvContent: String, accountId: Int) {
        viewModelScope.launch {
            val allCats = categories.value
            val parsed = CsvParser.parseCsv(csvContent, allCats)

            for (row in parsed) {
                // Find matching category ID
                val cat = allCats.find { it.name.lowercase() == row.suggestedCategoryName.lowercase() }
                    ?: allCats.firstOrNull()
                val catId = cat?.id ?: 1

                // Is fixed? Check category setting or default
                val isFixed = cat?.isFixed ?: false

                addTransaction(
                    accountId = accountId,
                    amount = row.amount,
                    currency = row.currency,
                    categoryId = catId,
                    description = row.description,
                    isIncome = row.isIncome,
                    isFixed = isFixed,
                    date = row.date
                )
            }
        }
    }

    // --- Security & Biometrics ---

    fun verifyPin(pin: String): Boolean {
        if (pin == userPin.value) {
            isAuthenticated.value = true
            return true
        }
        return false
    }

    fun simulateBiometricLogin() {
        if (isBiometricsEnabled.value) {
            isAuthenticated.value = true
        }
    }

    fun logout() {
        isAuthenticated.value = false
    }

    // --- Cloud Backup & Sync Simulation ---

    fun triggerCloudSync() {
        viewModelScope.launch {
            isSyncing.value = true
            // Simulate brief network delay
            kotlinx.coroutines.delay(2000)
            isSyncing.value = false
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            lastSyncTime.value = sdf.format(Date())
        }
    }
}

data class MonthlyReportData(
    val month: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val fixedExpense: Double,
    val variableExpense: Double,
    val categoryBreakdown: Map<Int, Double>
)
