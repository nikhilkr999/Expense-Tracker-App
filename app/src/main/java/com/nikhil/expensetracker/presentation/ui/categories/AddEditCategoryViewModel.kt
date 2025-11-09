package com.nikhil.expensetracker.presentation.ui.categories

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.domain.repository.usecase.AddCategoryUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoryByIdUseCase
import com.nikhil.expensetracker.domain.repository.usecase.UpdateCategoryUseCase
import kotlinx.coroutines.launch

// presentation/ui/categories/AddEditCategoryViewModel.kt
class AddEditCategoryViewModel(
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryByIdUseCase: GetCategoryByIdUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AddEditCategoryUiState>()
    val uiState: LiveData<AddEditCategoryUiState> = _uiState

    private val _currentCategory = MutableLiveData<Category?>()
    val currentCategory: LiveData<Category?> = _currentCategory

    private val _selectedColor = MutableLiveData<String>()
    val selectedColor: LiveData<String> = _selectedColor

    private val _selectedIcon = MutableLiveData<String>()
    val selectedIcon: LiveData<String> = _selectedIcon

    private var isEditMode = false
    private var categoryId: Long = -1L

    init {
        // Set default values
        _selectedColor.value = "#6200EE"
        _selectedIcon.value = "ic_category_placeholder"
    }

    fun loadCategory(categoryId: Long) {
        if (categoryId == -1L) return

        viewModelScope.launch {
            _uiState.value = AddEditCategoryUiState.Loading

            try {
                val category = getCategoryByIdUseCase(categoryId)
                if (category != null) {
                    this@AddEditCategoryViewModel.categoryId = categoryId
                    isEditMode = true

                    _currentCategory.value = category
                    _selectedColor.value = category.color
                    _selectedIcon.value = category.icon

                    _uiState.value = AddEditCategoryUiState.LoadedForEdit
                } else {
                    _uiState.value = AddEditCategoryUiState.Error("Category not found")
                }
            } catch (e: Exception) {
                _uiState.value = AddEditCategoryUiState.Error(e.message ?: "Failed to load category")
            }
        }
    }

    fun saveCategory(name: String) {
        viewModelScope.launch {
            _uiState.value = AddEditCategoryUiState.Loading

            if (name.trim().isEmpty()) {
                _uiState.value = AddEditCategoryUiState.Error("Please enter a category name")
                return@launch
            }

            try {
                if (isEditMode) {
                    // Update existing category
                    val updatedCategory = Category(
                        id = categoryId,
                        name = name.trim(),
                        color = _selectedColor.value ?: "#6200EE",
                        icon = _selectedIcon.value ?: "ic_category_placeholder"
                    )
                    val result = updateCategoryUseCase(updatedCategory)
                    if (result.isSuccess) {
                        _uiState.value = AddEditCategoryUiState.Success
                    } else {
                        _uiState.value = AddEditCategoryUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to update category"
                        )
                    }
                } else {
                    // Add new category
                    val category = Category(
                        name = name.trim(),
                        color = _selectedColor.value ?: "#6200EE",
                        icon = _selectedIcon.value ?: "ic_category_placeholder"
                    )
                    val result = addCategoryUseCase(category)
                    if (result.isSuccess) {
                        _uiState.value = AddEditCategoryUiState.Success
                    } else {
                        _uiState.value = AddEditCategoryUiState.Error(
                            result.exceptionOrNull()?.message ?: "Failed to save category"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = AddEditCategoryUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun setSelectedColor(color: String) {
        _selectedColor.value = color
    }

    fun setSelectedIcon(icon: String) {
        _selectedIcon.value = icon
    }

    fun isEditMode(): Boolean = isEditMode

    fun getAvailableColors(): List<String> {
        return listOf(
            "#FF5722", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
            "#795548", "#9E9E9E", "#607D8B", "#000000"
        )
    }

    fun getAvailableIcons(): List<String> {
        return listOf(
            "ic_food", "ic_transport", "ic_entertainment", "ic_shopping",
            "ic_bills", "ic_health", "ic_education", "ic_travel",
            "ic_fitness", "ic_pets", "ic_gifts", "ic_other"
        )
    }
}

sealed class AddEditCategoryUiState {
    object Loading : AddEditCategoryUiState()
    object Success : AddEditCategoryUiState()
    object LoadedForEdit : AddEditCategoryUiState()
    data class Error(val message: String) : AddEditCategoryUiState()
}

// ViewModel Factory
class AddEditCategoryViewModelFactory(
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryByIdUseCase: GetCategoryByIdUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditCategoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditCategoryViewModel(addCategoryUseCase, updateCategoryUseCase, getCategoriesUseCase, getCategoryByIdUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}