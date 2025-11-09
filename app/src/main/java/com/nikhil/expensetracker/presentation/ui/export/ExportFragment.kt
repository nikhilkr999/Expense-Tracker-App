// Fixed ExportFragment
package com.nikhil.expensetracker.presentation.ui.export

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.repository.CategoryRepositoryImpl
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentExportBinding
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log

class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExportViewModel
    private var selectedDateRange: Pair<Long, Long>? = null

    private val TAG = "ExportFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView: Inflating layout")
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Setting up view components")

        setupViewModel()
        setupObservers()
        setupClickListeners()
        updateDateRangeDisplay()
    }

    private fun setupViewModel() {
        Log.d(TAG, "setupViewModel: Initializing repositories and ViewModel")
        val database = AppDatabase.getDatabase(requireContext())
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())

        val getExpensesUseCase = GetExpensesUseCase(expenseRepository)
        val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)

        viewModel = ViewModelProvider(
            this,
            ExportViewModelFactory(getExpensesUseCase, getCategoriesUseCase)
        )[ExportViewModel::class.java]
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExportUiState.Loading -> {
                    Log.d(TAG, "uiState: Loading state started")
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvProgress.visibility = View.VISIBLE
                    binding.btnExportCsv.isEnabled = false
                    binding.btnExportPdf.isEnabled = false
                }
                is ExportUiState.Success -> {
                    Log.d(TAG, "uiState: Success - Exported ${state.expenseCount} expenses to ${state.filePath}")
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    binding.btnExportCsv.isEnabled = true
                    binding.btnExportPdf.isEnabled = true

                    showExportSuccessDialog(state.filePath, state.expenseCount)
                }
                is ExportUiState.Error -> {
                    Log.e(TAG, "uiState: Error - ${state.message}")
                    binding.progressBar.visibility = View.GONE
                    binding.tvProgress.visibility = View.GONE
                    binding.btnExportCsv.isEnabled = true
                    binding.btnExportPdf.isEnabled = true

                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.exportProgress.observe(viewLifecycleOwner) { progress ->
            Log.d(TAG, "exportProgress: Progress updated to $progress%")
            binding.progressBar.progress = progress
            binding.tvProgress.text = "Exporting... $progress%"
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectDateRange.setOnClickListener {
            Log.d(TAG, "Click: Select Date Range button clicked")
            showDateRangePickerDialog()
        }

        binding.btnClearDateRange.setOnClickListener {
            Log.d(TAG, "Click: Clear Date Range button clicked")
            selectedDateRange = null
            updateDateRangeDisplay()
        }

        binding.btnExportCsv.setOnClickListener {
            Log.d(TAG, "Click: Export CSV button clicked")
            checkPermissionsAndExport { viewModel.exportToCSV(requireContext(), selectedDateRange) }
        }

        binding.btnExportPdf.setOnClickListener {
            Log.d(TAG, "Click: Export PDF button clicked")
            checkPermissionsAndExport { viewModel.exportToPDF(requireContext(), selectedDateRange) }
        }
    }

    private fun showDateRangePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        val picker = builder.build()

        picker.addOnPositiveButtonClickListener { selection ->
            // Adjust to full day range
            val start = selection.first // start of day in UTC
            val end = selection.second + 86_399_999 // end of day (23:59:59.999)
            selectedDateRange = Pair(start, end)
            Log.d(TAG, "DateRangePicker: Selected range from $start to $end")
            updateDateRangeDisplay()
        }

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateDateRangeDisplay() {
        if (selectedDateRange != null) {
            val startDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateRange!!.first))
            val endDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDateRange!!.second))
            binding.tvDateRange.text = "$startDate - $endDate"
            binding.btnClearDateRange.visibility = View.VISIBLE
            Log.d(TAG, "updateDateRangeDisplay: Updated to $startDate - $endDate")
        } else {
            binding.tvDateRange.text = "All expenses"
            binding.btnClearDateRange.visibility = View.GONE
            Log.d(TAG, "updateDateRangeDisplay: Reset to All expenses")
        }
    }

    private fun checkPermissionsAndExport(exportAction: () -> Unit) {
        Log.d(TAG, "checkPermissionsAndExport: Starting export")
        // No need for WRITE_EXTERNAL_STORAGE permission when using getExternalFilesDir()
        exportAction()
    }

    private fun showExportSuccessDialog(filePath: String, expenseCount: Int) {
        Log.d(TAG, "showExportSuccessDialog: Showing success dialog for $filePath")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Successful")
            .setMessage("Successfully exported $expenseCount expenses.\n\nFile saved to: $filePath")
            .setPositiveButton("Share") { _, _ ->
                shareFile(filePath)
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun shareFile(filePath: String) {
        Log.d(TAG, "shareFile: Sharing file at $filePath")
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (filePath.endsWith(".csv")) "text/csv" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Share Export File"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: Cleaning up")
        _binding = null
    }
}