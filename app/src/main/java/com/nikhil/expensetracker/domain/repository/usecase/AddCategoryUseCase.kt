package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.domain.repository.CategoryRepository

class AddCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Long> {
        return try {
            val id = categoryRepository.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}