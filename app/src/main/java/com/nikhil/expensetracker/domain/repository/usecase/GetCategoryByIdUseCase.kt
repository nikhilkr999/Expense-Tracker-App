package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.domain.repository.CategoryRepository

class GetCategoryByIdUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(categoryId: Long): Category? {
        return try {
            repository.getCategoryById(categoryId)
        } catch (e: Exception) {
            null
        }
    }
}