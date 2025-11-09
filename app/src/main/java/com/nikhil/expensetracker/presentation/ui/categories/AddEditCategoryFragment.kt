package com.nikhil.expensetracker.presentation.ui.categories

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.nikhil.expensetracker.R
import com.nikhil.expensetracker.data.database.AppDatabase
import com.nikhil.expensetracker.data.database.entities.Category
import com.nikhil.expensetracker.data.repository.CategoryRepositoryImpl
import com.nikhil.expensetracker.databinding.FragmentAddEditCategoryBinding
import com.nikhil.expensetracker.domain.repository.usecase.AddCategoryUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoriesUseCase
import com.nikhil.expensetracker.domain.repository.usecase.GetCategoryByIdUseCase
import com.nikhil.expensetracker.domain.repository.usecase.UpdateCategoryUseCase
import com.nikhil.expensetracker.presentation.adapters.ColorPickerAdapter
import com.nikhil.expensetracker.presentation.adapters.IconPickerAdapter

class AddEditCategoryFragment : Fragment() {

    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddEditCategoryViewModel
    private lateinit var colorAdapter: ColorPickerAdapter
    private lateinit var iconAdapter: IconPickerAdapter
    private val args: AddEditCategoryFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupColorPicker()
        setupIconPicker()
        setupObservers()
        setupClickListeners()

        // Load existing category if editing
        if (args.categoryId != -1L) {
            viewModel.loadCategory(args.categoryId)
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val categoryRepository = CategoryRepositoryImpl(database.categoryDao())

        val addCategoryUseCase = AddCategoryUseCase(categoryRepository)
        val updateCategoryUseCase = UpdateCategoryUseCase(categoryRepository)
        val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
        val getCategoryByIdUseCase = GetCategoryByIdUseCase(categoryRepository)

        viewModel = ViewModelProvider(
            this,
            AddEditCategoryViewModelFactory(addCategoryUseCase, updateCategoryUseCase, getCategoriesUseCase, getCategoryByIdUseCase)
        ).get(AddEditCategoryViewModel::class.java)
    }

    private fun setupColorPicker() {
        colorAdapter = ColorPickerAdapter(
            colors = viewModel.getAvailableColors(),
            onColorSelected = { color ->
                viewModel.setSelectedColor(color)
            }
        )

        binding.rvColors.apply {
            adapter = colorAdapter
            layoutManager = GridLayoutManager(requireContext(), 5)
        }
    }

    private fun setupIconPicker() {
        iconAdapter = IconPickerAdapter(
            icons = viewModel.getAvailableIcons(),
            onIconSelected = { icon ->
                viewModel.setSelectedIcon(icon)
            }
        )

        binding.rvIcons.apply {
            adapter = iconAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
        }
    }

    private fun setupObservers() {
        viewModel.currentCategory.observe(viewLifecycleOwner) { category ->
            category?.let { populateCategoryData(it) }
        }

        viewModel.selectedColor.observe(viewLifecycleOwner) { color ->
            colorAdapter.setSelectedColor(color)
            binding.viewSelectedColor.setBackgroundColor(Color.parseColor(color))
        }

        viewModel.selectedIcon.observe(viewLifecycleOwner) { icon ->
            iconAdapter.setSelectedIcon(icon)
            // Update preview icon
            binding.ivSelectedIcon.setImageResource(getIconResource(icon))
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddEditCategoryUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSave.isEnabled = false
                }
                is AddEditCategoryUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val message = if (viewModel.isEditMode()) "Category updated successfully" else "Category saved successfully"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is AddEditCategoryUiState.LoadedForEdit -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Update"
                    requireActivity().title = "Edit Category"
                }
                is AddEditCategoryUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            val name = binding.etCategoryName.text.toString()
            viewModel.saveCategory(name)
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun populateCategoryData(category: Category) {
        binding.etCategoryName.setText(category.name)
        binding.viewSelectedColor.setBackgroundColor(Color.parseColor(category.color))
        binding.ivSelectedIcon.setImageResource(getIconResource(category.icon))
    }

    private fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "ic_food" -> R.drawable.ic_food
            "ic_transport" -> R.drawable.ic_transport
            "ic_entertainment" -> R.drawable.ic_entertainment
            "ic_shopping" -> R.drawable.ic_shopping
            "ic_bills" -> R.drawable.ic_bills
            "ic_health" -> R.drawable.ic_health
            "ic_education" -> R.drawable.ic_education
            "ic_travel" -> R.drawable.ic_travel
            "ic_fitness" -> R.drawable.ic_fitness
            "ic_pets" -> R.drawable.ic_pets
            "ic_gifts" -> R.drawable.ic_gifts
            else -> R.drawable.ic_other
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}