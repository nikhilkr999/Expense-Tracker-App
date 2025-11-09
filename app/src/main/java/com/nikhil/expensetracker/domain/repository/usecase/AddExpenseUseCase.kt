package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.domain.repository.ExpenseRepository

class AddExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Long> {
        return try {
            val id = repository.insertExpense(expense)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}