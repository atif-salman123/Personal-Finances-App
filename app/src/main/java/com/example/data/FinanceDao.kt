package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Int)

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // --- Transactions ---
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // --- Budgets ---
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    // --- Debt & Lending ---
    @Query("SELECT * FROM debt_lending ORDER BY date DESC")
    fun getAllDebtLending(): Flow<List<DebtLending>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtLending(debtLending: DebtLending): Long

    @Update
    suspend fun updateDebtLending(debtLending: DebtLending)

    @Query("DELETE FROM debt_lending WHERE id = :id")
    suspend fun deleteDebtLendingById(id: Int)

    // --- Planned Payments & Subscriptions ---
    @Query("SELECT * FROM planned_payments ORDER BY nextDueDate ASC")
    fun getAllPlannedPayments(): Flow<List<PlannedPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedPayment(plannedPayment: PlannedPayment): Long

    @Update
    suspend fun updatePlannedPayment(plannedPayment: PlannedPayment)

    @Query("DELETE FROM planned_payments WHERE id = :id")
    suspend fun deletePlannedPaymentById(id: Int)
}
