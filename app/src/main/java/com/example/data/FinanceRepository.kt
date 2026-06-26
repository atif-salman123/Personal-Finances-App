package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(private val financeDao: FinanceDao) {

    val allAccounts: Flow<List<Account>> = financeDao.getAllAccounts()
    val allCategories: Flow<List<Category>> = financeDao.getAllCategories()
    val allTransactions: Flow<List<Transaction>> = financeDao.getAllTransactions()
    val allBudgets: Flow<List<Budget>> = financeDao.getAllBudgets()
    val allDebtLending: Flow<List<DebtLending>> = financeDao.getAllDebtLending()
    val allPlannedPayments: Flow<List<PlannedPayment>> = financeDao.getAllPlannedPayments()

    suspend fun insertAccount(account: Account) = financeDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = financeDao.updateAccount(account)
    suspend fun deleteAccount(id: Int) = financeDao.deleteAccountById(id)

    suspend fun insertCategory(category: Category) = financeDao.insertCategory(category)

    suspend fun insertTransaction(transaction: Transaction) = financeDao.insertTransaction(transaction)
    suspend fun updateTransaction(transaction: Transaction) = financeDao.updateTransaction(transaction)
    suspend fun deleteTransaction(id: Int) = financeDao.deleteTransactionById(id)

    suspend fun insertBudget(budget: Budget) = financeDao.insertBudget(budget)
    suspend fun updateBudget(budget: Budget) = financeDao.updateBudget(budget)
    suspend fun deleteBudget(id: Int) = financeDao.deleteBudgetById(id)

    suspend fun insertDebtLending(debtLending: DebtLending) = financeDao.insertDebtLending(debtLending)
    suspend fun updateDebtLending(debtLending: DebtLending) = financeDao.updateDebtLending(debtLending)
    suspend fun deleteDebtLending(id: Int) = financeDao.deleteDebtLendingById(id)

    suspend fun insertPlannedPayment(plannedPayment: PlannedPayment) = financeDao.insertPlannedPayment(plannedPayment)
    suspend fun updatePlannedPayment(plannedPayment: PlannedPayment) = financeDao.updatePlannedPayment(plannedPayment)
    suspend fun deletePlannedPayment(id: Int) = financeDao.deletePlannedPaymentById(id)

    suspend fun initializeData() {
        val count = financeDao.getCategoryCount()
        if (count == 0) {
            // Pre-populate core parent categories and their subcategories
            // Income parent & subcategories
            val incomeId = financeDao.insertCategory(Category(name = "Income Sources", isIncome = true, isFixed = true))
            financeDao.insertCategory(Category(name = "Salary", subCategoryOfId = incomeId.toInt(), isIncome = true, isFixed = true))
            financeDao.insertCategory(Category(name = "Freelance & Consulting", subCategoryOfId = incomeId.toInt(), isIncome = true, isFixed = false))
            financeDao.insertCategory(Category(name = "Dividends & Interest", subCategoryOfId = incomeId.toInt(), isIncome = true, isFixed = false))
            financeDao.insertCategory(Category(name = "Gifts & Side Hustles", subCategoryOfId = incomeId.toInt(), isIncome = true, isFixed = false))

            // Housing (Fixed)
            val housingId = financeDao.insertCategory(Category(name = "Housing", isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Rent / Mortgage", subCategoryOfId = housingId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Property Taxes", subCategoryOfId = housingId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Home Insurance", subCategoryOfId = housingId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Repairs & Maintenance", subCategoryOfId = housingId.toInt(), isIncome = false, isFixed = false))

            // Food & Dining (Variable)
            val foodId = financeDao.insertCategory(Category(name = "Food & Dining", isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Groceries", subCategoryOfId = foodId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Restaurants & Cafes", subCategoryOfId = foodId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Delivery & Fast Food", subCategoryOfId = foodId.toInt(), isIncome = false, isFixed = false))

            // Transportation (Variable)
            val transportId = financeDao.insertCategory(Category(name = "Transportation", isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Fuel / Gas", subCategoryOfId = transportId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Public Transit", subCategoryOfId = transportId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Ridesharing", subCategoryOfId = transportId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Car Insurance", subCategoryOfId = transportId.toInt(), isIncome = false, isFixed = true))

            // Utilities (Fixed)
            val utilitiesId = financeDao.insertCategory(Category(name = "Utilities", isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Electricity", subCategoryOfId = utilitiesId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Water & Gas", subCategoryOfId = utilitiesId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Internet & Cable", subCategoryOfId = utilitiesId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Mobile Plan", subCategoryOfId = utilitiesId.toInt(), isIncome = false, isFixed = true))

            // Entertainment & Subscriptions
            val leisureId = financeDao.insertCategory(Category(name = "Entertainment & Leisure", isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Streaming Services", subCategoryOfId = leisureId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Movies & Concerts", subCategoryOfId = leisureId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Sports & Gym", subCategoryOfId = leisureId.toInt(), isIncome = false, isFixed = true))
            financeDao.insertCategory(Category(name = "Travel & Hobbies", subCategoryOfId = leisureId.toInt(), isIncome = false, isFixed = false))

            // Health & Insurance
            val healthId = financeDao.insertCategory(Category(name = "Health & Wellness", isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Medical & Pharmacy", subCategoryOfId = healthId.toInt(), isIncome = false, isFixed = false))
            financeDao.insertCategory(Category(name = "Health Insurance", subCategoryOfId = healthId.toInt(), isIncome = false, isFixed = true))
        }

        // Add default accounts if none exist
        val accounts = financeDao.getAllAccounts().first()
        if (accounts.isEmpty()) {
            financeDao.insertAccount(Account(name = "Main Bank Account", type = "Bank", balance = 5000.0, currency = "USD"))
            financeDao.insertAccount(Account(name = "Cash Wallet", type = "Cash", balance = 350.0, currency = "USD"))
            financeDao.insertAccount(Account(name = "Credit Card", type = "Credit Card", balance = -120.0, currency = "USD"))
        }
    }
}
