package com.nikhil.expensetracker.presentation.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.domain.repository.usecase.DeleteExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetSpendingStatisticsUseCase
import com.nikhil.expensetracker.presentation.utils.toUIModel
import kotlinx.coroutines.launch
import java.util.Calendar
import android.util.Log

class DashboardViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getSpendingStatisticsUseCase: GetSpendingStatisticsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _uiState = MutableLiveData<DashboardUiState>()
    val uiState: LiveData<DashboardUiState> = _uiState

    private val _recentExpenses = MutableLiveData<List<ExpenseUIModel>>()
    val recentExpenses: LiveData<List<ExpenseUIModel>> = _recentExpenses

    init {
        Log.d(TAG, "ViewModel initialized - loading dashboard data")
        loadDashboardData()
    }

    fun deleteExpense(expenseId: Long) {
        Log.d(TAG, "deleteExpense: Attempting to delete expense with ID: $expenseId")
        viewModelScope.launch {
            try {
                // You'll need to get the expense first to delete it
                // This is a simplified version - in real implementation you'd need GetExpenseByIdUseCase
                val expense = Expense(
                    id = expenseId,
                    amount = 0.0, // These values don't matter for deletion
                    categoryId = 0,
                    date = 0,
                    description = ""
                )

                val result = deleteExpenseUseCase(expense)
                if (result.isSuccess) {
                    Log.d(TAG, "deleteExpense: Expense deleted successfully, refreshing data")
                    // Don't call loadDashboardData() here as the Flow will automatically update
                    // loadDashboardData() // Remove this line
                } else {
                    Log.e(TAG, "deleteExpense: Failed to delete expense")
                    _uiState.value = DashboardUiState.Error("Failed to delete expense")
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteExpense: Exception occurred", e)
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun loadDashboardData() {
        Log.d(TAG, "loadDashboardData: Starting to load dashboard data")
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            Log.d(TAG, "loadDashboardData: Set UI state to Loading")

            try {
                val currentMonth = getCurrentMonthRange()
                Log.d(TAG, "loadDashboardData: Current month range: ${currentMonth.first} to ${currentMonth.second}")

                // Collect expenses and recalculate statistics each time expenses change
                getExpensesUseCase().collect { expenses ->
                    Log.d(TAG, "loadDashboardData: Received ${expenses.size} expenses from Flow")

                    // Update recent expenses
                    val recentExpenses = expenses.take(3).map { it.toUIModel() }
                    _recentExpenses.value = recentExpenses
                    Log.d(TAG, "loadDashboardData: Updated recent expenses with ${recentExpenses.size} items")

                    // Recalculate statistics with the current expenses
                    val statistics = getSpendingStatisticsUseCase(
                        currentMonth.first,
                        currentMonth.second
                    )
                    Log.d(TAG, "loadDashboardData: Recalculated statistics - Total: ${statistics.totalSpent}, Categories: ${statistics.categoryBreakdown.size}")

                    _uiState.value = DashboardUiState.Success(
                        totalSpent = statistics.totalSpent,
                        categoryBreakdown = statistics.categoryBreakdown,
                        expenseCount = statistics.expenseCount
                    )
                    Log.d(TAG, "loadDashboardData: Updated UI state to Success")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadDashboardData: Exception occurred", e)
                _uiState.value = DashboardUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshData() {
        Log.d(TAG, "refreshData: Manual refresh requested")
        loadDashboardData()
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endOfMonth = calendar.timeInMillis

        Log.d(TAG, "getCurrentMonthRange: Start: $startOfMonth, End: $endOfMonth")
        return Pair(startOfMonth, endOfMonth)
    }
}