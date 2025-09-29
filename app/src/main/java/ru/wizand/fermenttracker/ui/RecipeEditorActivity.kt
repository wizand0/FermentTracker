package ru.wizand.fermenttracker.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.databinding.ActivityRecipeEditorBinding
import ru.wizand.fermenttracker.ui.adapters.RecipeListAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel
import androidx.activity.viewModels
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.ui.batches.BatchTemplateFragment
import ru.wizand.fermenttracker.R

class RecipeEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeEditorBinding
    private lateinit var adapter: RecipeListAdapter
    private val viewModel: BatchListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecipeListAdapter(
            { selectionCount ->
                binding.btnDeleteSelectedRecipes.visibility =
                    if (selectionCount > 0) View.VISIBLE else View.GONE
            },
            { recipe ->
                openTemplateEditor(recipe.type)
            }
        )

        binding.rvRecipes.layoutManager = LinearLayoutManager(this)
        binding.rvRecipes.adapter = adapter

        loadRecipes()

        binding.btnDeleteSelectedRecipes.setOnClickListener {
            val selected = adapter.getSelectedItems()
            lifecycleScope.launch(Dispatchers.IO) {
                selected.forEach { recipe ->
                    viewModel.repository.deleteRecipe(recipe.type)
                }
                val recipes = viewModel.repository.getAllRecipes()
                withContext(Dispatchers.Main) {
                    adapter.submitList(recipes)
                    adapter.clearSelection()
                }
            }
        }

        binding.btnAddNewRecipe.setOnClickListener {
            openTemplateEditor(null)
        }
    }

    private fun loadRecipes() {
        lifecycleScope.launch {
            val recipes = withContext(Dispatchers.IO) {
                viewModel.repository.getAllRecipes()
            }
            adapter.submitList(recipes)
        }
    }

    private fun openTemplateEditor(recipeType: String?) {
        val fragment = BatchTemplateFragment()
        val bundle = Bundle().apply {
            putString("recipe_type", recipeType)
        }
        fragment.arguments = bundle

        // Скрываем список и кнопки
        binding.rvRecipes.visibility = View.GONE
        binding.btnAddNewRecipe.visibility = View.GONE
        binding.btnDeleteSelectedRecipes.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (supportFragmentManager.backStackEntryCount == 0) {
            // Восстанавливаем вид списка
            binding.rvRecipes.visibility = View.VISIBLE
            binding.btnAddNewRecipe.visibility = View.VISIBLE
            binding.btnDeleteSelectedRecipes.visibility = if (adapter.getSelectedItems().isNotEmpty()) View.VISIBLE else View.GONE
            binding.fragmentContainer.visibility = View.GONE
        }
    }
}