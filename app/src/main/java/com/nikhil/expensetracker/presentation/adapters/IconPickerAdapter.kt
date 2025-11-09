package com.nikhil.expensetracker.presentation.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.databinding.ItemIconPickerBinding

// presentation/adapters/IconPickerAdapter.kt
class IconPickerAdapter(
    private val icons: List<String>,
    private val onIconSelected: (String) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {

    private var selectedIcon: String? = null

    fun setSelectedIcon(icon: String) {
        selectedIcon = icon
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemIconPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(icons[position])
    }

    override fun getItemCount(): Int = icons.size

    inner class IconViewHolder(
        private val binding: ItemIconPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(icon: String) {
            binding.apply {
                val isSelected = icon == selectedIcon

                // Set icon
                iconView.setImageResource(getIconResource(icon))
                iconView.imageTintList = ColorStateList.valueOf(Color.WHITE)

                // Show/hide selection indicator
                if (isSelected) {
                    selectionIndicator.visibility = View.VISIBLE
                    root.setBackgroundResource(R.drawable.selected_icon_background)
                } else {
                    selectionIndicator.visibility = View.GONE
                    root.setBackgroundResource(R.drawable.icon_picker_background)
                }

                // Handle click
                root.setOnClickListener {
                    selectedIcon = icon
                    onIconSelected(icon)
                    notifyDataSetChanged()
                }
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