package com.nikhil.expensetracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val period: String, // "MONTHLY", "WEEKLY", "YEARLY"
    val startDate: Long
)
