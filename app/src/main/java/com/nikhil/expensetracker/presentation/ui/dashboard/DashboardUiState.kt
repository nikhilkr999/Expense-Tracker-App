package com.nikhil.expensetracker.presentation.ui.dashboard

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val totalSpent: Double,
        val categoryBreakdown: Map<String, Double>,
        val expenseCount: Int
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}