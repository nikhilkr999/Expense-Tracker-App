package com.nikhil.expensetracker.data.models

import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.data.database.entities.Expense

data class ExpenseWithCategory(
    val expense: Expense,
    val category: Category?
)