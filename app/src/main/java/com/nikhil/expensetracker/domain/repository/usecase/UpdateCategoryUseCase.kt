package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.domain.repository.CategoryRepository

class UpdateCategoryUseCase(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        return try {
            repository.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}