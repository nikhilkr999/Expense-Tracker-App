package com.nikhil.expensetracker.presentation.ui.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.domain.repository.usecase.DeleteCategoryUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import kotlinx.coroutines.launch

// presentation/ui/categories/CategoriesViewModel.kt
class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<CategoriesUiState>()
    val uiState: LiveData<CategoriesUiState> = _uiState

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading

            try {
                getCategoriesUseCase().collect { categories ->
                    _categories.value = categories
                    _uiState.value = CategoriesUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = CategoriesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                val result = deleteCategoryUseCase(category)
                if (result.isSuccess) {
                    // Categories will be automatically updated through the Flow
                } else {
                    _uiState.value = CategoriesUiState.Error("Failed to delete category")
                }
            } catch (e: Exception) {
                _uiState.value = CategoriesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class CategoriesUiState {
    object Loading : CategoriesUiState()
    object Success : CategoriesUiState()
    data class Error(val message: String) : CategoriesUiState()
}

// ViewModel Factory
class CategoriesViewModelFactory(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(getCategoriesUseCase, deleteCategoryUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}