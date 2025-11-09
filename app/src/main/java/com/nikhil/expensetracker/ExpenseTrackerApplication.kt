package com.nikhil.expensetracker

import android.app.Application
import android.util.Log
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.database.entities.Category
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// ExpenseTrackerApplication.kt
class ExpenseTrackerApplication : Application() {
    companion object {
        private const val TAG = "ExpenseTrackerApp"
    }
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate started")
        initializeDefaultCategories()
    }

    private fun initializeDefaultCategories() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Initializing default categories in background")
            val database = AppDatabase.getDatabase(this@ExpenseTrackerApplication)
            val categoryDao = database.categoryDao()

            // Check if categories already exist
            val existingCategories = categoryDao.getAllCategories().first()
            if (existingCategories.isEmpty()) {
                // Insert default categories
                val defaultCategories = listOf(
                    Category(name = "Food", color = "#FF5722", icon = "ic_food"),
                    Category(name = "Transport", color = "#2196F3", icon = "ic_transport"),
                    Category(name = "Entertainment", color = "#9C27B0", icon = "ic_entertainment"),
                    Category(name = "Shopping", color = "#FF9800", icon = "ic_shopping"),
                    Category(name = "Bills", color = "#F44336", icon = "ic_bills"),
                    Category(name = "Health", color = "#4CAF50", icon = "ic_health"),
                    Category(name = "Education", color = "#3F51B5", icon = "ic_education"),
                    Category(name = "Other", color = "#607D8B", icon = "ic_other")
                )

                defaultCategories.forEach { category ->
                    categoryDao.insertCategory(category)
                }
            }
        }
    }
}