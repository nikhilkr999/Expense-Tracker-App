package com.nikhil.expensetracker.data.repository

import com.nikhil.expensetracker.data.database.dao.CategoryDao
import com.nikhil.expensetracker.data.database.dao.ExpenseDao
import com.nikhil.expensetracker.data.database.entities.Expense
import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import com.nikhil.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<ExpenseWithCategory>> {
        return expenseDao.getAllExpenses().map { expenses ->
            expenses.map { expense ->
                val category = categoryDao.getCategoryById(expense.categoryId)
                ExpenseWithCategory(expense, category)
            }
        }
    }

    override fun getExpensesByDateRange(startDate: Long, endDate: Long): Flow<List<ExpenseWithCategory>> {
        return expenseDao.getExpensesByDateRange(startDate, endDate).map { expenses ->
            expenses.map { expense ->
                val category = categoryDao.getCategoryById(expense.categoryId)
                ExpenseWithCategory(expense, category)
            }
        }
    }

    override suspend fun getExpenseById(id: Long): ExpenseWithCategory? {
        return try {
            val expense = expenseDao.getExpenseById(id)
            if (expense != null) {
                val category = categoryDao.getCategoryById(expense.categoryId)
                ExpenseWithCategory(expense, category)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    override suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    override suspend fun getTotalSpentInRange(startDate: Long, endDate: Long): Double {
        return expenseDao.getTotalSpentInRange(startDate, endDate) ?: 0.0
    }
}