package com.nikhil.expensetracker.presentation.ui.expense

sealed class AddEditExpenseUiState {
    object Loading : AddEditExpenseUiState()
    object Success : AddEditExpenseUiState()
    object LoadedForEdit : AddEditExpenseUiState()
    data class Error(val message: String) : AddEditExpenseUiState()
}