package com.nikhil.expensetracker.presentation.ui.expense

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.data.repository.CategoryRepositoryImpl
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentExpenseListBinding
import com.nikhil.expensetracker.domain.repository.usecase.DeleteExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.presentation.adapters.CategoryFilterAdapter
import com.nikhil.expensetracker.presentation.adapters.ExpenseAdapter
import kotlinx.coroutines.launch

// presentation/ui/expense/ExpenseListFragment.kt
class ExpenseListFragment : Fragment() {

    private var _binding: FragmentExpenseListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExpenseListViewModel
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var categoryFilterAdapter: CategoryFilterAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExpenseListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupSearchView()
        setupFilterChips()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())

        val getExpensesUseCase = GetExpensesUseCase(expenseRepository)
        val deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)

        viewModel = ViewModelProvider (
            this,
            ExpenseListViewModelFactory(getExpensesUseCase, deleteExpenseUseCase)
        ).get(ExpenseListViewModel::class.java)
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(
            onItemClick = { expense ->
                val action = ExpenseListFragmentDirections.actionExpensesToEditExpense(expense.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { expense ->
                showDeleteConfirmation(expense)
            }
        )

        binding.rvExpenses.apply {
            adapter = expenseAdapter
            layoutManager = LinearLayoutManager(requireContext())

            // Add item decoration for better spacing
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSearchView() {
        styleSearchViewWhite()
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchExpenses(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchExpenses(it) }
                return true
            }
        })
    }

    private fun styleSearchViewWhite() {
        try {
            val context = binding.searchView.context

            // Get the EditText inside SearchView
            val searchEditTextId = context.resources.getIdentifier("search_src_text", "id", "android")
            val searchEditText = binding.searchView.findViewById<EditText>(searchEditTextId)
            searchEditText?.apply {
                setTextColor(Color.WHITE)
                setHintTextColor(Color.WHITE)
            }

            // Style the search icon
            val searchIconId = context.resources.getIdentifier("search_mag_icon", "id", "android")
            val searchIcon = binding.searchView.findViewById<ImageView>(searchIconId)
            searchIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

            // Style the close/clear icon
            val closeIconId = context.resources.getIdentifier("search_close_btn", "id", "android")
            val closeIcon = binding.searchView.findViewById<ImageView>(closeIconId)
            closeIcon?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

            // Set white underline
            val searchPlateId = context.resources.getIdentifier("search_plate", "id", "android")
            val searchPlate = binding.searchView.findViewById<View>(searchPlateId)
            searchPlate?.setBackgroundResource(R.drawable.white_underline_background)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFilterChips() {
        // Category filter
        categoryFilterAdapter = CategoryFilterAdapter { category ->
            viewModel.filterByCategory(category)
        }

        binding.rvCategoryFilter.apply {
            adapter = categoryFilterAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Load categories for filter
        loadCategoriesForFilter()

        // Sort options
        binding.chipSortDate.setOnClickListener {
            val currentSort = viewModel.getCurrentFilter().sortBy
            val newSort = if (currentSort == SortBy.DATE_DESC) SortBy.DATE_ASC else SortBy.DATE_DESC
            viewModel.sortExpenses(newSort)
            updateSortChips(newSort)
        }

        binding.chipSortAmount.setOnClickListener {
            val currentSort = viewModel.getCurrentFilter().sortBy
            val newSort = if (currentSort == SortBy.AMOUNT_DESC) SortBy.AMOUNT_ASC else SortBy.AMOUNT_DESC
            viewModel.sortExpenses(newSort)
            updateSortChips(newSort)
        }

        binding.chipSortCategory.setOnClickListener {
            viewModel.sortExpenses(SortBy.CATEGORY)
            updateSortChips(SortBy.CATEGORY)
        }
    }

    private fun loadCategoriesForFilter() {
        val database = AppDatabase.getDatabase(requireContext())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())

        lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                categoryFilterAdapter.submitList(categories)
            }
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExpenseListUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvExpenses.visibility = View.GONE
                }
                is ExpenseListUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvExpenses.visibility = View.VISIBLE
                }
                is ExpenseListUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.filteredExpenses.observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.submitList(expenses)

            // Show/hide empty state
            if (expenses.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvExpenses.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvExpenses.visibility = View.VISIBLE
            }

            // Update expense count with filter info
            updateExpenseCountText(expenses.size)
        }
    }

    private fun updateExpenseCountText(count: Int) {
        val baseText = "$count expenses"
        val dateRangeText = viewModel.getDateRangeText()

        binding.tvExpenseCount.text = if (dateRangeText != null) {
            "$baseText (${dateRangeText})"
        } else {
            baseText
        }
    }

    private fun setupClickListeners() {
        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_expenses_to_addExpense)
        }

        binding.btnClearFilters.setOnClickListener {
            viewModel.clearFilters()
            binding.searchView.setQuery("", false)
            binding.searchView.clearFocus()
            updateDateFilterButton() // Update button appearance
        }

        binding.btnDateFilter.setOnClickListener {
            showDateRangePickerDialog()
        }
    }

    private fun updateSortChips(sortBy: SortBy) {
        // Reset all chips
        binding.chipSortDate.isChecked = false
        binding.chipSortAmount.isChecked = false
        binding.chipSortCategory.isChecked = false

        // Set active chip
        when (sortBy) {
            SortBy.DATE_DESC, SortBy.DATE_ASC -> {
                binding.chipSortDate.isChecked = true
                binding.chipSortDate.text = if (sortBy == SortBy.DATE_DESC) "Date ↓" else "Date ↑"
            }
            SortBy.AMOUNT_DESC, SortBy.AMOUNT_ASC -> {
                binding.chipSortAmount.isChecked = true
                binding.chipSortAmount.text = if (sortBy == SortBy.AMOUNT_DESC) "Amount ↓" else "Amount ↑"
            }
            SortBy.CATEGORY -> {
                binding.chipSortCategory.isChecked = true
            }
        }
    }

    private fun showDateRangePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")

        // Set existing selection if any
        val currentFilter = viewModel.getCurrentFilter()
        if (currentFilter.startDate != null && currentFilter.endDate != null) {
            builder.setSelection(androidx.core.util.Pair(currentFilter.startDate, currentFilter.endDate))
        }

        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection.first
            val endDate = selection.second
            viewModel.filterByDateRange(startDate, endDate)
            updateDateFilterButton()
        }

        picker.addOnNegativeButtonClickListener {
            // User canceled - no action needed
        }

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateDateFilterButton() {
        // Update button appearance based on whether date filter is active
        val hasDateFilter = viewModel.hasDateRangeFilter()
        binding.btnDateFilter.text = if (hasDateFilter) {
            "Date Range ✓"
        } else {
            "Date Range"
        }

        // You can also change button color/style here if needed
        // binding.btnDateFilter.setBackgroundColor(if (hasDateFilter) activeColor else defaultColor)
    }

    private fun showDeleteConfirmation(expense: ExpenseUIModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteExpense(expense.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Update date filter button appearance when returning to fragment
        updateDateFilterButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}