package com.nikhil.expensetracker.presentation.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private val onItemClick: (ExpenseUIModel) -> Unit,
    private val onDeleteClick: (ExpenseUIModel) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private var expenses = listOf<ExpenseUIModel>()

    fun submitList(newExpenses: List<ExpenseUIModel>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(
        private val binding: ItemExpenseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: ExpenseUIModel) {
            binding.apply {
                tvAmount.text = expense.amount
                tvCategory.text = expense.categoryName
                tvDate.text = expense.date
                tvDescription.text = expense.description

                // Set category color
                val color = Color.parseColor(expense.categoryColor)
                categoryColorView.setBackgroundColor(color)

                root.setOnClickListener { onItemClick(expense) }
                btnDelete.setOnClickListener { onDeleteClick(expense) }
            }
        }
    }
}