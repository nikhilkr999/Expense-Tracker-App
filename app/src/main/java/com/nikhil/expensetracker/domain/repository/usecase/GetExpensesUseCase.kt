package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import com.nikhil.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetExpensesUseCase(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<ExpenseWithCategory>> {
        return repository.getAllExpenses()
    }
}