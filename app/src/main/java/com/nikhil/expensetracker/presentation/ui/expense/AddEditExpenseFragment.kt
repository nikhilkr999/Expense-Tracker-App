package com.nikhil.expensetracker.presentation.ui.expense

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.data.repository.CategoryRepositoryImpl
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentAddEditExpenseBinding
import com.nikhil.expensetracker.domain.repository.usecase.AddExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpenseByIdUseCase
import com.nikhil.expensetracker.domain.repository.usecase.UpdateExpenseUseCase
import com.nikhil.expensetracker.presentation.adapters.CategorySpinnerAdapter
import com.nikhil.expensetracker.presentation.utils.toDateString
import java.util.Calendar

class AddEditExpenseFragment : Fragment() {

    private var _binding: FragmentAddEditExpenseBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddEditExpenseViewModel
    private lateinit var categoryAdapter: CategorySpinnerAdapter
    private val args: AddEditExpenseFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        setupObservers()
        setupClickListeners()


        // Load existing expense if editing
        if (args.expenseId != -1L) {
             viewModel.loadExpense(args.expenseId)
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())

        val addExpenseUseCase = AddExpenseUseCase(expenseRepository)
        val updateExpenseUseCase = UpdateExpenseUseCase(expenseRepository)
        val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
        val getExpenseByIdUseCase = GetExpenseByIdUseCase(expenseRepository)

        viewModel = ViewModelProvider(
            this,
            AddEditExpenseViewModelFactory(addExpenseUseCase, updateExpenseUseCase, getCategoriesUseCase, getExpenseByIdUseCase)
        ).get(AddEditExpenseViewModel::class.java)
    }

    private fun setupUI() {
        // Setup category spinner
        categoryAdapter = CategorySpinnerAdapter(requireContext())
        binding.spinnerCategory.adapter = categoryAdapter

        // Setup date picker
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        // Set current date as default
        updateDateDisplay(System.currentTimeMillis())
    }

    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.submitList(categories)
        }

        viewModel.currentExpense.observe(viewLifecycleOwner) { expense ->
            expense?.let { populateExpenseData(it) }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddEditExpenseUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                }
                is AddEditExpenseUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val message = if (viewModel.isEditMode()) "Expense updated successfully" else "Expense saved successfully"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is AddEditExpenseUiState.LoadedForEdit -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Update"
                    requireActivity().title = "Edit Expense"
                }
                is AddEditExpenseUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun populateExpenseData(expense: ExpenseUIModel) {
        // Populate amount (remove currency symbol)
        val amount = expense.amount.replace("₹", "").replace(",", "")
        binding.etAmount.setText(amount)

        // Populate description
        binding.etDescription.setText(expense.description)

        // Update date display
        updateDateDisplay(viewModel.selectedDate)

        // Select category in spinner
        val categories = viewModel.categories.value ?: emptyList()
        val categoryIndex = categories.indexOfFirst { it.id == viewModel.selectedCategory?.id }
        if (categoryIndex != -1) {
            binding.spinnerCategory.setSelection(categoryIndex)
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveExpense()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryAdapter.getItem(position)
                viewModel.selectedCategory = selectedCategory
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        viewModel.saveExpense(amount, description)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = viewModel.selectedDate

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)
                viewModel.selectedDate = selectedCalendar.timeInMillis
                updateDateDisplay(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateDisplay(timestamp: Long) {
        binding.btnSelectDate.text = timestamp.toDateString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// presentation/ui/expense/AddEditExpenseViewModelFactory.kt
class AddEditExpenseViewModelFactory(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditExpenseViewModel(addExpenseUseCase, updateExpenseUseCase, getCategoriesUseCase, getExpenseByIdUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}