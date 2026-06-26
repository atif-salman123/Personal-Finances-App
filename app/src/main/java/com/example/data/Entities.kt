package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Bank", "Cash", "Credit Card", "Other"
    val balance: Double,
    val currency: String // "USD", "EUR", "INR", "AED", "GBP"
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subCategoryOfId: Int? = null, // ID of parent category
    val isIncome: Boolean,
    val isFixed: Boolean // Fixed vs Variable
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val amount: Double, // Original amount
    val currency: String, // Original currency
    val amountInBaseCurrency: Double, // Converted to Base Currency (e.g. USD)
    val categoryId: Int,
    val date: Long, // timestamp
    val description: String,
    val isIncome: Boolean,
    val isFixed: Boolean,
    // Amortization support (e.g. Q1, Semi, Annual payments split across months)
    val isAmortized: Boolean = false,
    val amortizationMonths: Int = 1, // 3 for quarterly, 6 for semi, 12 for annual
    val amortizationStartMonth: String? = null // YYYY-MM
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val month: String, // YYYY-MM
    val amountLimit: Double // Budget limit in Base Currency (e.g. USD)
)

@Entity(tableName = "debt_lending")
data class DebtLending(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val type: String, // "OWE_THEM" (Debt) or "OWE_ME" (Lending)
    val amount: Double,
    val currency: String,
    val amountInBaseCurrency: Double,
    val description: String,
    val date: Long,
    val isResolved: Boolean = false
)

@Entity(tableName = "planned_payments")
data class PlannedPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val currency: String,
    val categoryId: Int,
    val accountId: Int,
    val frequency: String, // "ONCE", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
    val nextDueDate: Long,
    val isSubscription: Boolean, // True for subscriptions, False for other planned bills
    val isActive: Boolean = true
)
