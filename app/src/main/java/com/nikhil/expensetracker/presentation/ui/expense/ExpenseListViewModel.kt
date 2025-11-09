package com.nikhil.expensetracker.presentation.ui.expense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.domain.repository.usecase.DeleteExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.presentation.utils.toUIModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// presentation/ui/expense/ExpenseListViewModel.kt
class ExpenseListViewModel(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<ExpenseListUiState>()
    val uiState: LiveData<ExpenseListUiState> = _uiState

    private val _expenses = MutableLiveData<List<ExpenseUIModel>>()
    val expenses: LiveData<List<ExpenseUIModel>> = _expenses

    private val _filteredExpenses = MutableLiveData<List<ExpenseUIModel>>()
    val filteredExpenses: LiveData<List<ExpenseUIModel>> = _filteredExpenses

    private var allExpenses = listOf<ExpenseUIModel>()
    private var expensesWithTimestamps = listOf<ExpenseWithTimestamp>()
    private var currentFilter = ExpenseFilter()

    init {
        loadExpenses()
    }

    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.value = ExpenseListUiState.Loading

            try {
                getExpensesUseCase().collect { expensesWithCategory ->
                    allExpenses = expensesWithCategory.map { it.toUIModel() }

                    // Create expenses with timestamps for filtering
                    expensesWithTimestamps = expensesWithCategory.map { expenseWithCategory ->
                        ExpenseWithTimestamp(
                            uiModel = expenseWithCategory.toUIModel(),
                            timestamp = expenseWithCategory.expense.date,
                            categoryId = expenseWithCategory.expense.categoryId
                        )
                    }

                    _expenses.value = allExpenses
                    applyFilter()
                    _uiState.value = ExpenseListUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = ExpenseListUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                val expenseWithTimestamp = expensesWithTimestamps.find { it.uiModel.id == expenseId }
                if (expenseWithTimestamp != null) {
                    val expenseEntity = Expense(
                        id = expenseWithTimestamp.uiModel.id,
                        amount = expenseWithTimestamp.uiModel.amount.replace("₹", "").replace(",", "").toDouble(),
                        categoryId = expenseWithTimestamp.categoryId,
                        date = expenseWithTimestamp.timestamp,
                        description = expenseWithTimestamp.uiModel.description
                    )
                    deleteExpenseUseCase(expenseEntity)
                }
            } catch (e: Exception) {
                _uiState.value = ExpenseListUiState.Error("Failed to delete expense")
            }
        }
    }

    fun searchExpenses(query: String) {
        currentFilter = currentFilter.copy(searchQuery = query)
        applyFilter()
    }

    fun filterByCategory(categoryName: String?) {
        currentFilter = currentFilter.copy(categoryName = categoryName)
        applyFilter()
    }

    fun filterByDateRange(startDate: Long?, endDate: Long?) {
        currentFilter = currentFilter.copy(startDate = startDate, endDate = endDate)
        applyFilter()
    }

    fun sortExpenses(sortBy: SortBy) {
        currentFilter = currentFilter.copy(sortBy = sortBy)
        applyFilter()
    }

    private fun applyFilter() {
        var filtered = expensesWithTimestamps

        // Apply search filter
        if (currentFilter.searchQuery.isNotBlank()) {
            filtered = filtered.filter { expenseWithTimestamp ->
                expenseWithTimestamp.uiModel.description.contains(currentFilter.searchQuery, ignoreCase = true) ||
                        expenseWithTimestamp.uiModel.categoryName.contains(currentFilter.searchQuery, ignoreCase = true)
            }
        }

        // Apply category filter
        currentFilter.categoryName?.let { category ->
            filtered = filtered.filter { it.uiModel.categoryName == category }
        }

        // FIXED: Apply date range filter
        if (currentFilter.startDate != null && currentFilter.endDate != null) {
            val startOfDay = getStartOfDay(currentFilter.startDate!!)
            val endOfDay = getEndOfDay(currentFilter.endDate!!)

            filtered = filtered.filter { expenseWithTimestamp ->
                expenseWithTimestamp.timestamp >= startOfDay && expenseWithTimestamp.timestamp <= endOfDay
            }
        }

        // Apply sorting
        filtered = when (currentFilter.sortBy) {
            SortBy.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            SortBy.DATE_ASC -> filtered.sortedBy { it.timestamp }
            SortBy.AMOUNT_DESC -> filtered.sortedByDescending {
                it.uiModel.amount.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0
            }
            SortBy.AMOUNT_ASC -> filtered.sortedBy {
                it.uiModel.amount.replace("₹", "").replace(",", "").toDoubleOrNull() ?: 0.0
            }
            SortBy.CATEGORY -> filtered.sortedBy { it.uiModel.categoryName }
        }

        // Convert back to UI models
        _filteredExpenses.value = filtered.map { it.uiModel }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    fun clearFilters() {
        currentFilter = ExpenseFilter()
        applyFilter()
    }

    fun getCurrentFilter(): ExpenseFilter = currentFilter

    // Helper function to check if date range filter is active
    fun hasDateRangeFilter(): Boolean {
        return currentFilter.startDate != null && currentFilter.endDate != null
    }

    // Helper function to get formatted date range for UI display
    fun getDateRangeText(): String? {
        return if (hasDateRangeFilter()) {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val startText = sdf.format(Date(currentFilter.startDate!!))
            val endText = sdf.format(Date(currentFilter.endDate!!))
            "$startText - $endText"
        } else {
            null
        }
    }
}

// Helper data class to store UI model with timestamp
data class ExpenseWithTimestamp(
    val uiModel: ExpenseUIModel,
    val timestamp: Long,
    val categoryId: Long
)

// Data classes for filtering
data class ExpenseFilter(
    val searchQuery: String = "",
    val categoryName: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val sortBy: SortBy = SortBy.DATE_DESC
)

enum class SortBy {
    DATE_DESC, DATE_ASC, AMOUNT_DESC, AMOUNT_ASC, CATEGORY
}

sealed class ExpenseListUiState {
    object Loading : ExpenseListUiState()
    object Success : ExpenseListUiState()
    data class Error(val message: String) : ExpenseListUiState()
}

// ViewModel Factory
class ExpenseListViewModelFactory(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseListViewModel(getExpensesUseCase, deleteExpenseUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}