package com.nikhil.expensetracker.presentation.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nikhil.expensetracker.data.models.ExpenseWithCategory
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetSpendingStatisticsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// presentation/ui/statistics/StatisticsViewModel.kt
class StatisticsViewModel(
    private val getSpendingStatisticsUseCase: GetSpendingStatisticsUseCase,
    private val getExpensesUseCase: GetExpensesUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<StatisticsUiState>()
    val uiState: LiveData<StatisticsUiState> = _uiState

    private val _statisticsData = MutableLiveData<StatisticsData>()
    val statisticsData: LiveData<StatisticsData> = _statisticsData

    private val _selectedPeriod = MutableLiveData<TimePeriod>()
    val selectedPeriod: LiveData<TimePeriod> = _selectedPeriod

    init {
        _selectedPeriod.value = TimePeriod.THIS_MONTH
        loadStatistics(TimePeriod.THIS_MONTH)
    }

    fun loadStatistics(period: TimePeriod) {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading
            _selectedPeriod.value = period

            try {
                val dateRange = getDateRangeForPeriod(period)
                val statistics = getSpendingStatisticsUseCase(dateRange.first, dateRange.second)

                // Get expenses for trend analysis
                val expenses = getExpensesUseCase().first()
                val expensesInPeriod = expenses.filter { expense ->
                    expense.expense.date >= dateRange.first && expense.expense.date <= dateRange.second
                }

                val statisticsData = StatisticsData(
                    totalSpent = statistics.totalSpent,
                    expenseCount = statistics.expenseCount,
                    averagePerDay = calculateAveragePerDay(statistics.totalSpent, period),
                    categoryBreakdown = statistics.categoryBreakdown,
                    dailyTrend = calculateDailyTrend(expensesInPeriod, dateRange),
                    topCategories = getTopCategories(statistics.categoryBreakdown, 5),
                    comparisonWithPreviousPeriod = calculatePeriodComparison(period, statistics.totalSpent)
                )

                _statisticsData.value = statisticsData
                _uiState.value = StatisticsUiState.Success

            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getDateRangeForPeriod(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        return when (period) {
            TimePeriod.THIS_WEEK -> {
                // FIXED: Proper week calculation
                calendar.firstDayOfWeek = Calendar.MONDAY

                // Calculate days to subtract to get to Monday
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = when (currentDayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }

                // Set to start of current week (Monday)
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfWeek = calendar.timeInMillis

                // Set to end of current week (Sunday)
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfWeek = calendar.timeInMillis

                Pair(startOfWeek, endOfWeek)
            }
            TimePeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfMonth = calendar.timeInMillis

                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfMonth = calendar.timeInMillis

                Pair(startOfMonth, endOfMonth)
            }
            TimePeriod.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfYear = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfYear = calendar.timeInMillis

                Pair(startOfYear, endOfYear)
            }
            TimePeriod.LAST_30_DAYS -> {
                // FIXED: Set end to end of today, start to beginning of 30 days ago
                val todayCalendar = Calendar.getInstance()
                todayCalendar.set(Calendar.HOUR_OF_DAY, 23)
                todayCalendar.set(Calendar.MINUTE, 59)
                todayCalendar.set(Calendar.SECOND, 59)
                todayCalendar.set(Calendar.MILLISECOND, 999)
                val endDate = todayCalendar.timeInMillis

                val startCalendar = Calendar.getInstance()
                startCalendar.add(Calendar.DAY_OF_MONTH, -29) // -29 because today is day 1 of the 30-day period
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val startDate = startCalendar.timeInMillis

                Pair(startDate, endDate)
            }
        }
    }

    private fun calculateAveragePerDay(totalSpent: Double, period: TimePeriod): Double {
        val days = when (period) {
            TimePeriod.THIS_WEEK -> 7
            TimePeriod.THIS_MONTH -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
            TimePeriod.THIS_YEAR -> if (isLeapYear()) 366 else 365
            TimePeriod.LAST_30_DAYS -> 30
        }
        return totalSpent / days
    }

    private fun isLeapYear(): Boolean {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun calculateDailyTrend(
        expenses: List<ExpenseWithCategory>,
        dateRange: Pair<Long, Long>
    ): List<DailyExpense> {
        val dailyExpenses = mutableMapOf<String, Double>()

        expenses.forEach { expense ->
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(expense.expense.date))
            dailyExpenses[dateKey] = (dailyExpenses[dateKey] ?: 0.0) + expense.expense.amount
        }

        return dailyExpenses.entries.map { (date, amount) ->
            DailyExpense(date, amount)
        }.sortedBy { it.date }
    }

    private fun getTopCategories(
        categoryBreakdown: Map<String, Double>,
        limit: Int
    ): List<CategoryExpense> {
        return categoryBreakdown.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (category, amount) ->
                CategoryExpense(category, amount)
            }
    }

    private suspend fun calculatePeriodComparison(
        currentPeriod: TimePeriod,
        currentSpent: Double
    ): PeriodComparison {
        // Get previous period range
        val previousRange = getPreviousPeriodRange(currentPeriod)
        val previousStats = getSpendingStatisticsUseCase(previousRange.first, previousRange.second)

        val difference = currentSpent - previousStats.totalSpent
        val percentageChange = if (previousStats.totalSpent > 0) {
            (difference / previousStats.totalSpent) * 100
        } else {
            0.0
        }

        return PeriodComparison(
            previousAmount = previousStats.totalSpent,
            currentAmount = currentSpent,
            difference = difference,
            percentageChange = percentageChange
        )
    }

    private fun getPreviousPeriodRange(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        return when (period) {
            TimePeriod.THIS_WEEK -> {
                // FIXED: Proper previous week calculation
                calendar.firstDayOfWeek = Calendar.MONDAY

                // First get to the current week's Monday
                val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = when (currentDayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }

                // Go to current Monday, then subtract 7 days for previous week
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday - 7)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfPrevWeek = calendar.timeInMillis

                // Add 6 days to get to Sunday
                calendar.add(Calendar.DAY_OF_MONTH, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfPrevWeek = calendar.timeInMillis

                Pair(startOfPrevWeek, endOfPrevWeek)
            }
            TimePeriod.THIS_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfPrevMonth = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfPrevMonth = calendar.timeInMillis

                Pair(startOfPrevMonth, endOfPrevMonth)
            }
            TimePeriod.THIS_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfPrevYear = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_YEAR, calendar.getActualMaximum(Calendar.DAY_OF_YEAR))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val endOfPrevYear = calendar.timeInMillis

                Pair(startOfPrevYear, endOfPrevYear)
            }
            TimePeriod.LAST_30_DAYS -> {
                val endDate = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                val startDate = endDate - (30 * 24 * 60 * 60 * 1000L)
                Pair(startDate, endDate)
            }
        }
    }
}

// Data classes
data class StatisticsData(
    val totalSpent: Double,
    val expenseCount: Int,
    val averagePerDay: Double,
    val categoryBreakdown: Map<String, Double>,
    val dailyTrend: List<DailyExpense>,
    val topCategories: List<CategoryExpense>,
    val comparisonWithPreviousPeriod: PeriodComparison
)

data class DailyExpense(val date: String, val amount: Double)
data class CategoryExpense(val name: String, val amount: Double)
data class PeriodComparison(
    val previousAmount: Double,
    val currentAmount: Double,
    val difference: Double,
    val percentageChange: Double
)

enum class TimePeriod {
    THIS_WEEK, THIS_MONTH, THIS_YEAR, LAST_30_DAYS
}

sealed class StatisticsUiState {
    object Loading : StatisticsUiState()
    object Success : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}

// ViewModel Factory
class StatisticsViewModelFactory(
    private val getSpendingStatisticsUseCase: GetSpendingStatisticsUseCase,
    private val getExpensesUseCase: GetExpensesUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(getSpendingStatisticsUseCase, getExpensesUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}