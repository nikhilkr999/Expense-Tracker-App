// Updated BackupFragment
package com.nikhil.expensetracker.presentation.ui.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.repository.ExpenseRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentBackupBinding
import com.nikhil.expensetracker.domain.repository.usecase.GetExpensesUseCase
import java.io.File

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BackupViewModel

    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(requireContext(), it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupClickListeners()
        setupObservers()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val expenseRepository = ExpenseRepositoryImpl(database.expenseDao(), database.categoryDao())
        val getExpensesUseCase = GetExpensesUseCase(expenseRepository)

        viewModel = ViewModelProvider(
            this,
            BackupViewModelFactory(getExpensesUseCase)
        )[BackupViewModel::class.java]
    }

    private fun setupClickListeners() {
        binding.btnCreateBackup.setOnClickListener {
            viewModel.createBackup(requireContext())
        }

        binding.btnRestoreBackup.setOnClickListener {
            showRestoreDialog()
        }

        binding.btnSync.setOnClickListener {
            // TODO: Implement sync with Firebase or other service
            Toast.makeText(requireContext(), "Sync feature coming soon - Integrate with Firebase", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.backupStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is BackupStatus.Success -> {
                    Toast.makeText(requireContext(), "Backup created: ${status.filePath}", Toast.LENGTH_LONG).show()
                    showShareDialog(status.filePath)
                }
                is BackupStatus.Error -> {
                    Toast.makeText(requireContext(), "Backup failed: ${status.message}", Toast.LENGTH_LONG).show()
                }
                is BackupStatus.Loading -> {
                    // Show progress if needed
                }
            }
        }

        viewModel.restoreStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is RestoreStatus.Success -> {
                    Toast.makeText(requireContext(), "Restore successful", Toast.LENGTH_SHORT).show()
                    // Optionally restart app or reload data
                }
                is RestoreStatus.Error -> {
                    Toast.makeText(requireContext(), "Restore failed: ${status.message}", Toast.LENGTH_LONG).show()
                }
                is RestoreStatus.Loading -> {
                    // Show progress
                }
            }
        }
    }

    private fun showRestoreDialog() {
        restoreLauncher.launch("*/*")
    }

    private fun showShareDialog(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Backup"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}