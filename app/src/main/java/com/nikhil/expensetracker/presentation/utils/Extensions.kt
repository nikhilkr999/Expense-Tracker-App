package com.nikhil.expensetracker.presentation.utils

import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ExpenseWithCategory.toUIModel(): ExpenseUIModel {
    return ExpenseUIModel(
        id = expense.id,
        amount = "₹${String.format("%.2f", expense.amount)}",
        categoryName = category?.name ?: "Unknown",
        categoryColor = category?.color ?: "#757575",
        date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.date)),
        description = expense.description
    )
}

fun Double.toCurrency(): String {
    return "₹${String.format("%.2f", this)}"
}

fun Long.toDateString(): String {
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(this))
}