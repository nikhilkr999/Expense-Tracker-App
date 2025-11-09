package com.nikhil.expensetracker.domain.repository

import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<ExpenseWithCategory>>
    fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategory>>
    suspend fun getExpenseById(id: Long): ExpenseWithCategory?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    suspend fun getTotalSpentInRange(startDate: Long, endDate: Long): Double
}