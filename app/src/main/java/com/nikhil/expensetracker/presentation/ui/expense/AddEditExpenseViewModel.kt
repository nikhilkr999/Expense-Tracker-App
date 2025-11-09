package com.nikhil.expensetracker.presentation.ui.expense

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.domain.repository.usecase.AddExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpenseByIdUseCase
import com.nikhil.expensetracker.domain.repository.usecase.UpdateExpenseUseCase
import com.nikhil.expensetracker.presentation.utils.toUIModel
import kotlinx.coroutines.launch

class AddEditExpenseViewModel(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AddEditExpenseUiState>()
    val uiState: LiveData<AddEditExpenseUiState> = _uiState

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _currentExpense = MutableLiveData<ExpenseUIModel?>()
    val currentExpense: LiveData<ExpenseUIModel?> = _currentExpense

    var selectedCategory: Category? = null
    var selectedDate: Long = System.currentTimeMillis()
    private var isEditMode = false
    private var expenseId: Long = -1L

    init {
        loadCategories()
    }

    fun loadExpense(expenseId: Long) {
        if (expenseId == -1L) return

        viewModelScope.launch {
            _uiState.value = AddEditExpenseUiState.Loading

            try {
                val expense = getExpenseByIdUseCase(expenseId)
                if (expense != null) {
                    this@AddEditExpenseViewModel.expenseId = expenseId
                    isEditMode = true

                    // Convert to UI model
                    val expenseUIModel = expense.toUIModel()
                    _currentExpense.value = expenseUIModel

                    // Set selected date and category
                    selectedDate = expense.expense.date
                    selectedCategory = expense.category

                    _uiState.value = AddEditExpenseUiState.LoadedForEdit
                } else {
                    _uiState.value = AddEditExpenseUiState.Error("Expense not found")
                }
            } catch (e: Exception) {
                _uiState.value = AddEditExpenseUiState.Error(e.message ?: "Failed to load expense")
            }
        }
    }

    fun saveExpense(amount: String, description: String) {
        viewModelScope.launch {
            _uiState.value = AddEditExpenseUiState.Loading

            val amountDouble = amount.toDoubleOrNull()
            if (amountDouble == null || amountDouble <= 0) {
                _uiState.value = AddEditExpenseUiState.Error("Please enter a valid amount")
                return@launch
            }

            if (selectedCategory == null) {
                _uiState.value = AddEditExpenseUiState.Error("Please select a category")
                return@launch
            }

            try {
                if (isEditMode) {
                    // Update existing expense
                    val updatedExpense = Expense(
                        id = expenseId,
                        amount = amountDouble,
                        categoryId = selectedCategory!!.id,
                        date = selectedDate,
                        description = description
                    )
                    val result = updateExpenseUseCase(updatedExpense)
                    if (result.isSuccess) {
                        _uiState.value = AddEditExpenseUiState.Success
                    } else {
                        _uiState.value = AddEditExpenseUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to update expense"
                        )
                    }
                } else {
                    // Add new expense
                    val expense = Expense(
                        amount = amountDouble,
                        categoryId = selectedCategory!!.id,
                        date = selectedDate,
                        description = description
                    )
                    val result = addExpenseUseCase(expense)
                    if (result.isSuccess) {
                        _uiState.value = AddEditExpenseUiState.Success
                    } else {
                        _uiState.value = AddEditExpenseUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to save expense"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddEditExpenseUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                _categories.value = categories
            }
        }
    }

    fun isEditMode(): Boolean = isEditMode
}


