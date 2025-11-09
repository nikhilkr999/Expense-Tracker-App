package com.nikhil.expensetracker.domain.repository.usecase

data class SpendingStatistics(
    val totalSpent: Double,
    val categoryBreakdown: Map<String, Double>,
    val expenseCount: Int
)