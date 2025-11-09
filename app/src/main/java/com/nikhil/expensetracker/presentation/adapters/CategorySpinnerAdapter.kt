package com.nikhil.expensetracker.presentation.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.entities.Category

// presentation/adapters/CategorySpinnerAdapter.kt
class CategorySpinnerAdapter(
    private val context: Context
) : BaseAdapter() {

    private var categories = listOf<Category>()

    fun submitList(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun getCount(): Int = categories.size

    override fun getItem(position: Int): Category = categories[position]

    override fun getItemId(position: Int): Long = categories[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_category_spinner, parent, false)

        val category = categories[position]

        val colorView = view.findViewById<View>(R.id.category_color_view)
        val nameText = view.findViewById<TextView>(R.id.tv_category_name)

        colorView.setBackgroundColor(Color.parseColor(category.color))
        nameText.text = category.name
        nameText.setTextColor(Color.parseColor(category.color))

        return view
    }
}