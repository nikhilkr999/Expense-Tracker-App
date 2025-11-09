package com.nikhil.expensetracker.domain.repository.usecase

import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import com.nikhil.expensetracker.domain.repository.CategoryRepository
import com.nikhil.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

class GetCategoriesUseCase(
     val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return categoryRepository.getAllCategories()
    }
}