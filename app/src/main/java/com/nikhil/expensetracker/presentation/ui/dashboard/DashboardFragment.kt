package com.nikhil.expensetracker.presentation.ui.dashboard

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineHeightSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.models.ExpenseUIModel
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentDashboardBinding
import com.nikhil.expensetracker.domain.repository.usecase.DeleteExpenseUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetSpendingStatisticsUseCase
import com.nikhil.expensetracker.presentation.adapters.ExpenseAdapter
import com.nikhil.expensetracker.presentation.utils.toCurrency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    companion object {
        private const val TAG = "DashboardFragment"
    }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var expenseAdapter: ExpenseAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView: Creating dashboard fragment view")
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up dashboard fragment")

        setupViewModel()
        setupRecyclerView()
        setupCurrentTime();
        setupObservers()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Ensure full screen is set up when fragment starts
        setupFullScreen()
    }

    private fun setupCurrentTime(){
        val calendar = Calendar.getInstance()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val currentMonthYear = dateFormat.format(calendar.time)

        val greeting = when (hour) {
            in 0..11 -> "Good Morning!"
            in 12..16 -> "Good Afternoon!"
            else -> "Good Evening!"
        }

        binding.tvGreeting.text = greeting + " track your spending daily"
        binding.currentMonth.text = currentMonthYear
    }

    private fun setupFullScreen() {
        try {
            // Hide the ActionBar/Toolbar
            (requireActivity() as? AppCompatActivity)?.supportActionBar?.hide()

            // For a more immersive experience, you can also hide status bar and navigation bar
            // Uncomment the lines below if you want true full screen

            val window = requireActivity().window
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            // Hide status bar and navigation bar (optional)
            // windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Hide only the status bar (alternative option)
            // windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

            // Set the behavior for showing system bars
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            Log.d(TAG, "setupFullScreen: Full screen mode configured successfully")
        } catch (e: Exception) {
            Log.e(TAG,""+e);
        }
    }

    private fun restoreSystemUI() {

            // Show the ActionBar/Toolbar when leaving this fragment
            (requireActivity() as? AppCompatActivity)?.supportActionBar?.show()
            // Restore system bars if they were hidden
            val window = requireActivity().window
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            // Show system bars (uncomment if you hid them in setupFullScreen)
            // windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            // windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    }

    private fun setupViewModel() {
        try {
            // In a real app, you'd use dependency injection (Hilt/Dagger)
            val database = AppDatabase.getDatabase(requireContext())
            Log.d(TAG, "setupViewModel: Database instance obtained")

            val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())
            Log.d(TAG, "setupViewModel: Repositories created")

            val getExpensesUseCase = GetExpensesUseCase(expenseRepository)
            val getSpendingStatisticsUseCase = GetSpendingStatisticsUseCase(expenseRepository)
            val deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository)
            Log.d(TAG, "setupViewModel: Use cases created")

            viewModel = ViewModelProvider(
                this,
                DashboardViewModelFactory(getExpensesUseCase, getSpendingStatisticsUseCase, deleteExpenseUseCase)
            ).get(DashboardViewModel::class.java)

            Log.d(TAG, "setupViewModel: ViewModel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "setupViewModel: Error setting up ViewModel", e)
        }
    }

    private fun setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Setting up RecyclerView and adapter")
        try {
            expenseAdapter = ExpenseAdapter(
                onItemClick = { expense ->
                    Log.d(TAG, "setupRecyclerView: Expense item clicked - ID: ${expense.id}")
                    // Navigate to edit expense
                    val action = DashboardFragmentDirections.actionDashboardToEditExpense(expense.id)
                    findNavController().navigate(action)
                    Log.d(TAG, "setupRecyclerView: Navigated to edit expense")
                },
                onDeleteClick = { expense ->
                    Log.d(TAG, "setupRecyclerView: Delete clicked for expense - ID: ${expense.id}")
                    showDeleteConfirmation(expense)
                }
            )

            binding.rvRecentExpenses.apply {
                adapter = expenseAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }

            Log.d(TAG, "setupRecyclerView: RecyclerView setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "setupRecyclerView: Error setting up RecyclerView", e)
        }
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up ViewModel observers")
        try {
            viewModel.uiState.observe(viewLifecycleOwner) { state ->
                Log.d(TAG, "setupObservers: UI state changed to ${state::class.simpleName}")
                when (state) {
                    is DashboardUiState.Loading -> {
                        Log.d(TAG, "setupObservers: Showing loading state")
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is DashboardUiState.Success -> {
                        Log.d(TAG, "setupObservers: Success state - Total spent: ${state.totalSpent}")
                        binding.progressBar.visibility = View.GONE
                        binding.tvTotalSpent.text = state.totalSpent.toCurrency()

                        // Update category breakdown (you can add a chart here)
                        updateCategoryBreakdown(state.categoryBreakdown)
                        Log.d(TAG, "setupObservers: Category breakdown updated with ${state.categoryBreakdown.size} categories")
                    }
                    is DashboardUiState.Error -> {
                        Log.e(TAG, "setupObservers: Error state - ${state.message}")
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }

            viewModel.recentExpenses.observe(viewLifecycleOwner) { expenses ->
                Log.d(TAG, "setupObservers: Recent expenses updated - Count: ${expenses.size}")
                expenseAdapter.submitList(expenses)

                // Show/hide empty state
                if (expenses.isEmpty()) {
                    Log.d(TAG, "setupObservers: Showing empty state")
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvRecentExpenses.visibility = View.GONE
                } else {
                    Log.d(TAG, "setupObservers: Showing expenses list")
                    binding.tvEmptyState.visibility = View.GONE
                    binding.rvRecentExpenses.visibility = View.VISIBLE
                }
            }

            Log.d(TAG, "setupObservers: All observers set up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "setupObservers: Error setting up observers", e)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_expenses_to_addExpense)
        }

        binding.btnStatistics.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_statistics
        }

        binding.btnViewAllExpenses.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_expenses
        }

        binding.fabAddExpense.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addExpense)
        }
    }

    private fun updateCategoryBreakdown(categoryBreakdown: Map<String, Double>) {
        Log.d(TAG, "updateCategoryBreakdown: Updating category breakdown with ${categoryBreakdown.size} categories")
        try {
            // You can implement a simple chart or list here
            // For now, let's show top 3 categories
            val topCategories = categoryBreakdown.entries
                .sortedByDescending { it.value }
                .take(4)

            val catCount = topCategories.size
            Log.d(TAG, "updateCategoryBreakdown: Top 4 categories count: $catCount")

            when(catCount){
                0 ->{
                    binding.llForOne.visibility = View.GONE
                    binding.llForTwo.visibility = View.GONE
                    binding.llForAllfour.visibility = View.GONE
                }
                1 ->{
                    binding.llForOne.visibility = View.VISIBLE
                    binding.llForTwo.visibility = View.GONE
                    binding.llForAllfour.visibility = View.GONE

                    binding.tvCatNameOne.text = getStringInSize(topCategories,0)
                }

                2 ->{
                    binding.llForOne.visibility = View.GONE
                    binding.llForTwo.visibility = View.VISIBLE
                    binding.llForAllfour.visibility = View.GONE

                    binding.tvCatName20.text = getStringInSize(topCategories,0)
                    binding.tvCatName21.text = getStringInSize(topCategories,1)

                }
                3 ->{
                    binding.llForOne.visibility = View.VISIBLE
                    binding.llForTwo.visibility = View.VISIBLE
                    binding.llForAllfour.visibility = View.GONE

                    binding.tvCatNameOne.text = getStringInSize(topCategories,0)
                    binding.tvCatName20.text = getStringInSize(topCategories,1)
                    binding.tvCatName21.text = getStringInSize(topCategories,2)
                }
                else -> {
                    binding.llForOne.visibility = View.GONE
                    binding.llForTwo.visibility = View.GONE
                    binding.llForAllfour.visibility = View.VISIBLE

                    binding.tvCatName40.text = getStringInSize(topCategories,0)
                    binding.tvCatName41.text = getStringInSize(topCategories,1)
                    binding.tvCatName42.text = getStringInSize(topCategories,2)
                    binding.tvCatName43.text = getStringInSize(topCategories,3)
                }

            }

            binding.tvTopCategories.text = if (topCategories.isNotEmpty()) {
                val categoriesText = "Top Categories:\n" + topCategories.joinToString("\n") { (category, amount) ->
                    "$category: ${amount.toCurrency()}"
                }
                Log.d(TAG, "updateCategoryBreakdown: Categories text updated")
                categoriesText
            } else {
                Log.d(TAG, "updateCategoryBreakdown: No expenses - showing empty message")
                "No expenses yet"
            }

            Log.d(TAG, "updateCategoryBreakdown: Category breakdown update completed")
        } catch (e: Exception) {
            Log.e(TAG, "updateCategoryBreakdown: Error updating category breakdown", e)
        }
    }

    private fun getStringInSize(topCategories: List<Map.Entry<String, Double>>, i: Int): SpannableStringBuilder  {
        val key = topCategories[i].key.toString()
        val value = topCategories[i].value.toString()

        val spannable = SpannableStringBuilder()
        spannable.append(key)
        spannable.setSpan(
            AbsoluteSizeSpan(14, true), // size in sp
            0,
            key.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannable.append("\n")
        spannable.setSpan(
            object : LineHeightSpan {
                override fun chooseHeight(
                    text: CharSequence?,
                    start: Int,
                    end: Int,
                    spanstartv: Int,
                    v: Int,
                    fm: Paint.FontMetricsInt?
                ) {
                    fm?.let {
                        it.descent += 12 // Add vertical space (in pixels)
                    }
                }
            },
            key.length,
            key.length + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#E3FFFFFF")), // Dim white color
            0,
            key.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )


        spannable.append("₹"+value)
        spannable.setSpan(
            AbsoluteSizeSpan(22, true), // different size for value
            key.length + 1,
            key.length + 1 + value.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun showDeleteConfirmation(expense: ExpenseUIModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { _, _ ->
                Log.d(TAG, "showDeleteConfirmation: User confirmed deletion for expense ID: ${expense.id}")
                viewModel.deleteExpense(expense.id)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Log.d(TAG, "showDeleteConfirmation: User cancelled deletion")
            }
            .show()
        Log.d(TAG, "showDeleteConfirmation: Showing delete confirmation for expense ID: ${expense.id}")
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: Destroying dashboard fragment view")
        super.onDestroyView()
        // Restore system UI when the fragment is destroyed
        restoreSystemUI()
        _binding = null
    }
}

// presentation/ui/dashboard/DashboardViewModelFactory.kt
class DashboardViewModelFactory(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getSpendingStatisticsUseCase: GetSpendingStatisticsUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase
) : ViewModelProvider.Factory {

    companion object {
        private const val TAG = "DashboardViewModelFactory"
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "create: Creating ViewModel for class: ${modelClass.simpleName}")
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val viewModel = DashboardViewModel(getExpensesUseCase, getSpendingStatisticsUseCase, deleteExpenseUseCase) as T
            Log.d(TAG, "create: DashboardViewModel created successfully")
            return viewModel
        }
        Log.e(TAG, "create: Unknown ViewModel class: ${modelClass.simpleName}")
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}