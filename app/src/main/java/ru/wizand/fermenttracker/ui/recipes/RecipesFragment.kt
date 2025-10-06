package ru.wizand.fermenttracker.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.FragmentRecipesBinding
import ru.wizand.fermenttracker.ui.adapters.RecipeListAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel

class RecipesFragment : Fragment() {
    private var _binding: FragmentRecipesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by viewModels()
    private lateinit var adapter: RecipeListAdapter
    private var selectedRecipesCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        loadRecipes()
    }

    private fun setupRecyclerView() {
        adapter = RecipeListAdapter(
            { selectionCount ->
                selectedRecipesCount = selectionCount
                binding.btnDeleteSelectedRecipes.visibility =
                    if (selectionCount > 0) View.VISIBLE else View.GONE
            },
            { recipe ->
                openTemplateEditor(recipe.type)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnDeleteSelectedRecipes.setOnClickListener {
            val selected = adapter.getSelectedItems()
            lifecycleScope.launch {
                selected.forEach { recipe ->
                    viewModel.repository.deleteRecipe(recipe.type)
                }
                val recipes = viewModel.repository.getAllRecipes()
                adapter.submitList(recipes)
                adapter.clearSelection()
                binding.btnDeleteSelectedRecipes.visibility = View.GONE
            }
        }

        binding.btnAddNewRecipe.setOnClickListener {
            openTemplateEditor(null)
        }
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            val recipes = viewModel.repository.getAllRecipes()
            adapter.submitList(recipes)

            // Показываем или скрываем заглушку в зависимости от наличия рецептов
            if (recipes.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                binding.btnDeleteSelectedRecipes.visibility = View.GONE
                binding.btnAddNewRecipe.visibility = View.VISIBLE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                binding.btnDeleteSelectedRecipes.visibility = if (selectedRecipesCount > 0) View.VISIBLE else View.GONE
                binding.btnAddNewRecipe.visibility = View.VISIBLE
            }
        }
    }

    private fun openTemplateEditor(recipeType: String?) {
        val bundle = Bundle().apply {
            putString("recipe_type", recipeType)
        }

        findNavController().navigate(R.id.action_recipes_to_batchTemplate, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}