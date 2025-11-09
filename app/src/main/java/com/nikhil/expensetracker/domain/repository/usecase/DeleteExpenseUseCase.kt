package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.domain.repository.ExpenseRepository

// domain/usecase/DeleteExpenseUseCase.kt
class DeleteExpenseUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        return try {
            repository.deleteExpense(expense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}