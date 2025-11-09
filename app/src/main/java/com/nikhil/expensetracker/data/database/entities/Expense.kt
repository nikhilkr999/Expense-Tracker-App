package com.nikhil.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long,
    val date: Long, // Unix timestamp
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
