package ru.wizand.fermenttracker.ui.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
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
        loadRecipes()
    }

    private fun setupRecyclerView() {
        adapter = RecipeListAdapter(
            { selectionCount ->
                // Обработка выбора элементов, если нужно
            },
            { recipe ->
                // Обработка клика на рецепт
                // Например, открытие деталей рецепта
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            val recipes = viewModel.repository.getAllRecipes()
            adapter.submitList(recipes)

            // Показываем или скрываем заглушку в зависимости от наличия рецептов
            if (recipes.isEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyStateLayout.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}