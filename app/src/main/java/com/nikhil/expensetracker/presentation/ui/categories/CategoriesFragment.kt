package com.nikhil.expensetracker.presentation.ui.categories

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.data.repository.CategoryRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentCategoriesBinding
import com.nikhil.expensetracker.domain.repository.usecase.DeleteCategoryUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.presentation.adapters.CategoriesAdapter
class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CategoriesViewModel
    private lateinit var categoriesAdapter: CategoriesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())

        val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
        val deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)

        viewModel = ViewModelProvider (
            this,
            CategoriesViewModelFactory(getCategoriesUseCase, deleteCategoryUseCase)
        ).get(CategoriesViewModel::class.java)
    }

    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter(
            onEditClick = { category ->
                val action = CategoriesFragmentDirections.actionCategoriesToEditCategory(category.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { category ->
                showDeleteConfirmation(category)
            }
        )

        binding.rvCategories.apply {
            adapter = categoriesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CategoriesUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvCategories.visibility = View.GONE
                }
                is CategoriesUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvCategories.visibility = View.VISIBLE
                }
                is CategoriesUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoriesAdapter.submitList(categories)

            // Show/hide empty state
            if (categories.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvCategories.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvCategories.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddCategory.setOnClickListener {
            findNavController().navigate(R.id.action_categories_to_addCategory)
        }
    }

    private fun showDeleteConfirmation(category: Category) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}