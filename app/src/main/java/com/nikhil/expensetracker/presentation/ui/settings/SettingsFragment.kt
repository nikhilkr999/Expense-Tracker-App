package com.nikhil.expensetracker.presentation.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.databinding.FragmentSettingsBinding

// presentation/ui/settings/SettingsFragment.kt
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.cardCategories.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_categories)
        }

        binding.cardExport.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_export)
        }

        binding.cardBackup.setOnClickListener {
            // TODO: Implement backup functionality
            //findNavController().navigate(R.id.action_settings_to_backup)
            Toast.makeText(requireContext(), "Backup feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardTheme.setOnClickListener {
            // TODO: Implement theme switching
            Toast.makeText(requireContext(), "Theme switching coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About Expense Tracker")
            .setMessage("Expense Tracker v1.0\n\nA simple and intuitive app to track your daily expenses and manage your budget effectively.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}