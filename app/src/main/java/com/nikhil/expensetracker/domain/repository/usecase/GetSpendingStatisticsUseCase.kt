package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.first

class GetSpendingStatisticsUseCase(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(startDate: Long, endDate: Long): SpendingStatistics {
        val totalSpent = repository.getTotalSpentInRange(startDate, endDate)
        val expenses = repository.getExpensesByDateRange(startDate, endDate).first()

        val categorySpending = expenses.groupBy { it.category?.name ?: "Unknown" }
            .mapValues { (_, expenses) ->
                expenses.sumOf { it.expense.amount }
            }

        return SpendingStatistics(
            totalSpent = totalSpent,
            categoryBreakdown = categorySpending,
            expenseCount = expenses.size
        )
    }
}