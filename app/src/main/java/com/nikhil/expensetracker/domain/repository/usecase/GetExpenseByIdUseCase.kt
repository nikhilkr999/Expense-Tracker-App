package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import com.nikhil.expensetracker.domain.repository.ExpenseRepository

class GetExpenseByIdUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expenseId: Long): ExpenseWithCategory? {
        return try {
            repository.getExpenseById(expenseId)
        } catch (e: Exception) {
            null
        }
    }
}