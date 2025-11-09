package com.nikhil.expensetracker

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.nikhil.expensetracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                statusBarInsets.top, // add padding equal to status bar height
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        setupToolbar()        // First, set up the toolbar
        setupNavigation()     // Then, set up navigation (this will use the toolbar)
        setupBottomNavigation()
    }
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup ActionBar with NavController - now the ActionBar exists
        setupActionBarWithNavController(navController)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        // Handle bottom navigation item reselection
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            Log.d(TAG, "Bottom navigation item reselected: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Scroll to top or refresh dashboard
                    navController.popBackStack(R.id.nav_dashboard, false)
                }

                R.id.nav_expenses -> {
                    navController.popBackStack(R.id.nav_expenses, false)
                }

                R.id.nav_statistics -> {
                    navController.popBackStack(R.id.nav_statistics, false)
                }

                R.id.nav_settings -> {
                    navController.popBackStack(R.id.nav_settings, false)
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {
                R.id.nav_dashboard, R.id.nav_expenses, R.id.nav_statistics, R.id.nav_settings -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }

            val expenseId = navController.currentBackStackEntry?.arguments?.getLong("expenseId") ?: -1L

            supportActionBar?.title = when (destination.id) {
                R.id.nav_dashboard -> "Dashboard"
                R.id.nav_expenses -> "Filter Expense"
                R.id.nav_statistics -> "Expense Insights"
                R.id.nav_settings -> "Settings"
                R.id.addEditExpenseFragment -> if (expenseId != -1L) "Edit Expense" else "Add Expense"
                else -> destination.label
            }
        }

    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (!navController.navigateUp()) {
            super.onBackPressed()
        }
    }

}