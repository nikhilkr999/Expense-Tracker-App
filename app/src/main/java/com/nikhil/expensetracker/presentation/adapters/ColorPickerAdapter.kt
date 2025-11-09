package com.nikhil.expensetracker.presentation.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.databinding.ItemColorPickerBinding

// presentation/adapters/ColorPickerAdapter.kt
class ColorPickerAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit
) : RecyclerView.Adapter<ColorPickerAdapter.ColorViewHolder>() {

    private var selectedColor: String? = null

    fun setSelectedColor(color: String) {
        selectedColor = color
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorPickerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position])
    }

    override fun getItemCount(): Int = colors.size

    inner class ColorViewHolder(
        private val binding: ItemColorPickerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(color: String) {
            binding.apply {
                val isSelected = color == selectedColor

                // Set color
                colorView.setBackgroundColor(Color.parseColor(color))

                // Show/hide selection indicator
                if (isSelected) {
                    selectionIndicator.visibility = View.VISIBLE
                    root.setBackgroundResource(R.drawable.selected_color_background)
                } else {
                    selectionIndicator.visibility = View.GONE
                    root.setBackgroundResource(R.drawable.color_picker_background)
                }

                // Handle click
                root.setOnClickListener {
                    selectedColor = color
                    onColorSelected(color)
                    notifyDataSetChanged()
                }
            }
        }
    }
}