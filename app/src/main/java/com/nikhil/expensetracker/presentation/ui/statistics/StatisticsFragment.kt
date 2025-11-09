package com.nikhil.expensetracker.presentation.ui.statistics

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentStatisticsBinding
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetSpendingStatisticsUseCase
import com.nikhil.expensetracker.presentation.adapters.CategoryStatsAdapter
import com.nikhil.expensetracker.presentation.utils.toCurrency

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StatisticsViewModel
    private lateinit var categoryStatsAdapter: CategoryStatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupPeriodTabs()
        setupObservers()

        // Load initial data for the default period (THIS_MONTH)
        viewModel.loadStatistics(TimePeriod.THIS_MONTH)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())

        val getSpendingStatisticsUseCase = GetSpendingStatisticsUseCase(expenseRepository)
        val getExpensesUseCase = GetExpensesUseCase(expenseRepository)

        viewModel = ViewModelProvider(
            this,
            StatisticsViewModelFactory(getSpendingStatisticsUseCase, getExpensesUseCase)
        ).get(StatisticsViewModel::class.java)
    }

    private fun setupRecyclerView() {
        categoryStatsAdapter = CategoryStatsAdapter()
        binding.rvCategoryStats.apply {
            adapter = categoryStatsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupPeriodTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val period = when (it.position) {
                        0 -> TimePeriod.THIS_WEEK
                        1 -> TimePeriod.THIS_MONTH
                        2 -> TimePeriod.THIS_YEAR
                        3 -> TimePeriod.LAST_30_DAYS
                        else -> TimePeriod.THIS_MONTH
                    }
                    viewModel.loadStatistics(period)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is StatisticsUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                }
                is StatisticsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                }
                is StatisticsUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.statisticsData.observe(viewLifecycleOwner) { data ->
            updateStatisticsUI(data)
        }

        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            updatePeriodSelection(period)
        }
    }

    private fun updateStatisticsUI(data: StatisticsData) {
        // Update summary cards
        binding.tvTotalSpent.text = data.totalSpent.toCurrency()
        binding.tvExpenseCount.text = "${data.expenseCount} expenses"
        binding.tvAveragePerDay.text = "${data.averagePerDay.toCurrency()}/day"

        // Update comparison
        updateComparisonUI(data.comparisonWithPreviousPeriod)

        // Update category breakdown with custom pie chart
        updateCategoryChart(data.categoryBreakdown)

        // Update top categories list
        categoryStatsAdapter.submitList(data.topCategories)

        // Update trend chart
        updateTrendChart(data.dailyTrend)
    }

    private fun updateComparisonUI(comparison: PeriodComparison) {
        val isIncrease = comparison.difference > 0
        val color = if (isIncrease) R.color.expense_red else R.color.primary

        binding.tvComparison.apply {
            text = if (isIncrease) {
                "↑ ${comparison.difference.toCurrency()} (${String.format("%.1f", comparison.percentageChange)}%)"
            } else {
                "↓ ${(-comparison.difference).toCurrency()} (${String.format("%.1f", -comparison.percentageChange)}%)"
            }
            setTextColor(ContextCompat.getColor(requireContext(), color))
        }
    }

    private fun updateCategoryChart(categoryBreakdown: Map<String, Double>) {
        // FIX: Always update the chart data, let the CustomPieChart handle empty data
        binding.customPieChart.setData(categoryBreakdown)

        if (categoryBreakdown.isEmpty() || categoryBreakdown.values.all { it <= 0 }) {
            // Show no data message in the layout if needed
            binding.tvNoDataMessage?.visibility = View.VISIBLE
            // Note: Don't hide the customPieChart as it will show its own "No data available" message
        } else {
            // Hide no data message if it exists
            binding.tvNoDataMessage?.visibility = View.GONE
        }
    }

    private fun updateTrendChart(dailyTrend: List<DailyExpense>) {
        // Simple text-based trend (you can replace with actual chart library)
        val trendText = if (dailyTrend.isNotEmpty()) {
            "Daily Spending Trend:\n" + dailyTrend.takeLast(7).joinToString("\n") { daily ->
                "${daily.date}: ${daily.amount.toCurrency()}"
            }
        } else {
            "No data available"
        }
        binding.tvTrendChart.text = trendText
    }

    private fun updatePeriodSelection(period: TimePeriod) {
        val position = when (period) {
            TimePeriod.THIS_WEEK -> 0
            TimePeriod.THIS_MONTH -> 1
            TimePeriod.THIS_YEAR -> 2
            TimePeriod.LAST_30_DAYS -> 3
        }

        binding.tabLayout.getTabAt(position)?.select()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}