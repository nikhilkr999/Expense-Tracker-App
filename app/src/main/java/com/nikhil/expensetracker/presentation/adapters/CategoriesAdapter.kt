package com.nikhil.expensetracker.presentation.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.databinding.ItemCategoryBinding

// Updated CategoriesAdapter.kt
class CategoriesAdapter(
    private val onEditClick: (Category) -> Unit,
    private val onDeleteClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoryViewHolder>() {

    private var categories = listOf<Category>()
    private var expenseCountMap = mapOf<Long, Int>()

    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    fun setExpenseCount(expenseCount: Map<Long, Int>) {
        expenseCountMap = expenseCount
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                tvCategoryName.text = category.name

                // Set category color
                val color = Color.parseColor(category.color)
                categoryColorView.setBackgroundColor(color)

                // Set category icon
                ivCategoryIcon.setImageResource(getIconResource(category.icon));
                ivCategoryIcon.imageTintList = ColorStateList.valueOf(Color.parseColor(category.color))

                // Set usage count
                val expenseCount = expenseCountMap[category.id] ?: 0
                tvCategoryUsage.text = "$expenseCount expenses"

                btnEdit.setOnClickListener { onEditClick(category) }
                btnDelete.setOnClickListener { onDeleteClick(category) }
            }
        }

        private fun getIconResource(iconName: String): Int {
            return when (iconName) {
                "ic_food" -> R.drawable.ic_food
                "ic_transport" -> R.drawable.ic_transport
                "ic_entertainment" -> R.drawable.ic_entertainment
                "ic_shopping" -> R.drawable.ic_shopping
                "ic_bills" -> R.drawable.ic_bills
                "ic_health" -> R.drawable.ic_health
                "ic_education" -> R.drawable.ic_education
                "ic_travel" -> R.drawable.ic_travel
                "ic_fitness" -> R.drawable.ic_fitness
                "ic_pets" -> R.drawable.ic_pets
                "ic_gifts" -> R.drawable.ic_gifts
                else -> R.drawable.ic_other
            }
        }
    }
}