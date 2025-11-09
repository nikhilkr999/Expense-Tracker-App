package com.nikhil.expensetracker.presentation.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.databinding.ItemCategoryFilterBinding

// presentation/adapters/CategoryFilterAdapter.kt
class CategoryFilterAdapter(
    private val onCategorySelected: (String?) -> Unit
) : RecyclerView.Adapter<CategoryFilterAdapter.CategoryFilterViewHolder>() {

    private var categories = listOf<Category>()
    private var selectedCategory: String? = null

    fun submitList(newCategories: List<Category>) {
        // Add "All" as first item
        categories = listOf(
            Category(id = -1, name = "All", color = "#757575", icon = "")
        ) + newCategories
        notifyDataSetChanged()
    }

    fun setSelectedCategory(categoryName: String?) {
        selectedCategory = categoryName
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryFilterViewHolder {
        val binding = ItemCategoryFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryFilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryFilterViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size

    inner class CategoryFilterViewHolder(
        private val binding: ItemCategoryFilterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            binding.apply {
                val isSelected = if (category.name == "All") {
                    selectedCategory == null
                } else {
                    selectedCategory == category.name
                }

                // Set category name
                tvCategoryName.text = category.name

                // Set background color based on selection
                if (isSelected) {

                    root.background = ContextCompat.getDrawable(root.context, R.drawable.rounded_selectable_background)

                    tvCategoryName.setTextColor(
                        ContextCompat.getColor(root.context, R.color.black)
                    )
                } else {
                    root.setBackgroundColor(
                        ContextCompat.getColor(root.context, android.R.color.transparent)
                    )
                    tvCategoryName.setTextColor(
                        ContextCompat.getColor(root.context, R.color.white)
                    )
                }

                // Set category color indicator (skip for "All")
                if (category.name != "All") {
                    categoryColorView.visibility = View.VISIBLE
                    val ovalDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(category.color))
                    }
                    categoryColorView.background = ovalDrawable
                } else {
                    categoryColorView.visibility = View.GONE
                }

                // Handle click
                root.setOnClickListener {
                    val newSelection = if (category.name == "All") null else category.name
                    selectedCategory = newSelection
                    onCategorySelected(newSelection)
                    setSelectedCategory(newSelection)
                    notifyDataSetChanged()
                }
            }
        }
    }
}