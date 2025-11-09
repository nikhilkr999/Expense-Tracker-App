package com.nikhil.expensetracker.data.models

data class ExpenseUIModel(
    val id: Long,
    val amount: String,
    val categoryName: String,
    val categoryColor: String,
    val date: String,
    val description: String
)