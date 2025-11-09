package com.nikhil.expensetracker.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.databinding.ItemCategoryStatsBinding
import com.nikhil.expensetracker.presentation.ui.statistics.CategoryExpense
import com.nikhil.expensetracker.presentation.utils.toCurrency

/// presentation/adapters/CategoryStatsAdapter.kt
class CategoryStatsAdapter : RecyclerView.Adapter<CategoryStatsAdapter.CategoryStatsViewHolder>() {

    private var categories = listOf<CategoryExpense>()
    private var totalAmount = 0.0

    fun submitList(newCategories: List<CategoryExpense>) {
        categories = newCategories
        totalAmount = categories.sumOf { it.amount }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryStatsViewHolder {
        val binding = ItemCategoryStatsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryStatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryStatsViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryStatsViewHolder(
        private val binding: ItemCategoryStatsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryExpense) {
            binding.apply {
                tvCategoryName.text = category.name
                tvCategoryAmount.text = category.amount.toCurrency()

                // Calculate percentage
                val percentage = if (totalAmount > 0) {
                    (category.amount / totalAmount) * 100
                } else {
                    0.0
                }
                tvCategoryPercentage.text = "${String.format("%.1f", percentage)}%"

                // Update progress bar
                progressBar.progress = percentage.toInt()
            }
        }
    }
}