package ru.wizand.fermenttracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.databinding.ActivityRecipeEditorBinding
import ru.wizand.fermenttracker.ui.adapters.EditableStageTemplateAdapter  // Новый адаптер для StageTemplate
import ru.wizand.fermenttracker.ui.adapters.RecipeListAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel
import java.util.UUID

class RecipeEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeEditorBinding
    private lateinit var viewModel: BatchListViewModel
    private lateinit var recipeAdapter: RecipeListAdapter
    private lateinit var stageAdapter: EditableStageTemplateAdapter  // Изменено на новый адаптер
    private var currentRecipe: Recipe? = null
    private var stages: MutableList<StageTemplate> = mutableListOf()  // Оставляем var

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BatchListViewModel::class.java]

        setupRecipeList()
        setupStageEditor()
        setupButtons()
        loadRecipes()
    }

    private fun setupRecipeList() {
        recipeAdapter = RecipeListAdapter(
            onClick = { recipe -> loadRecipeForEdit(recipe) },
            onDelete = { recipe -> deleteRecipe(recipe) }
        )
        binding.rvRecipes.layoutManager = LinearLayoutManager(this)
        binding.rvRecipes.adapter = recipeAdapter
    }

    private fun setupStageEditor() {
        stageAdapter = EditableStageTemplateAdapter(  // Новый адаптер
            onAddStage = {
                stages.add(StageTemplate(
                    id = UUID.randomUUID().toString(),
                    recipeType = "", // Будет установлен при save
                    name = "New Stage",
                    durationHours = 24,
                    orderIndex = stages.size
                ))
                updateStages()
            },
            onRemoveStage = { position ->
                stages.removeAt(position)
                reindexStages()
                updateStages()
            }
        )
        binding.rvStages.layoutManager = LinearLayoutManager(this)
        binding.rvStages.adapter = stageAdapter

        // Drag-drop для переупорядочивания
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                stages.swap(from, to)
                reindexStages()
                stageAdapter.submitList(stages.toList())
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.rvStages)
    }

    private fun setupButtons() {
        binding.btnAddNewRecipe.setOnClickListener {
            currentRecipe = null
            binding.etRecipeType.setText("")
            binding.etIngredients.setText("")
            binding.etNote.setText("")
            stages.clear()
            updateStages()
            toggleEditorVisibility(true)
        }

        binding.btnSaveRecipe.setOnClickListener {
            saveRecipe()
        }

        binding.btnCancelEdit.setOnClickListener {
            toggleEditorVisibility(false)
        }
    }

    private fun loadRecipes() {
        CoroutineScope(Dispatchers.IO).launch {
            val recipes = viewModel.repository.getAllRecipes()
            withContext(Dispatchers.Main) {
                recipeAdapter.submitList(recipes)
            }
        }
    }

    private fun loadRecipeForEdit(recipe: Recipe) {
        currentRecipe = recipe
        binding.etRecipeType.setText(recipe.type)
        binding.etIngredients.setText(recipe.ingredients)
        binding.etNote.setText(recipe.note)
        CoroutineScope(Dispatchers.IO).launch {
            stages = viewModel.repository.getStageTemplatesForType(recipe.type).toMutableList()
            withContext(Dispatchers.Main) {
                updateStages()
                toggleEditorVisibility(true)
            }
        }
    }

    private fun saveRecipe() {
        val type = binding.etRecipeType.text.toString().trim()
        if (type.isEmpty()) {
            Snackbar.make(binding.root, "Recipe type is required", Snackbar.LENGTH_SHORT).show()
            return
        }
        val ingredients = binding.etIngredients.text.toString()
        val note = binding.etNote.text.toString()
        val recipe = Recipe(type = type, ingredients = ingredients, note = note)

        CoroutineScope(Dispatchers.IO).launch {
            if (currentRecipe != null) {
                viewModel.repository.updateRecipe(recipe)
            } else {
                viewModel.repository.insertRecipe(recipe)
            }
            // Удаляем старые шаблоны и вставляем новые (для update)
            val existingTemplates = viewModel.repository.getStageTemplatesForType(type)
            existingTemplates.forEach { viewModel.repository.deleteStageTemplate(it.id) }
            stages.forEach { template ->
                viewModel.repository.insertStageTemplate(template.copy(recipeType = type))
            }
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, "Recipe saved", Snackbar.LENGTH_SHORT).show()
                toggleEditorVisibility(false)
                loadRecipes()
            }
        }
    }

    private fun deleteRecipe(recipe: Recipe) {
        CoroutineScope(Dispatchers.IO).launch {
            viewModel.repository.deleteRecipe(recipe.type)  // Шаблоны удалятся по CASCADE
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, "Recipe deleted", Snackbar.LENGTH_SHORT).show()
                loadRecipes()
            }
        }
    }

    private fun updateStages() {
        stageAdapter.submitList(stages.toList())
    }

    private fun reindexStages() {
        stages.forEachIndexed { index, template ->
            template.orderIndex = index
        }
    }

    private fun toggleEditorVisibility(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.etRecipeType.visibility = visibility
        binding.etIngredients.visibility = visibility
        binding.etNote.visibility = visibility
        binding.rvStages.visibility = visibility
        binding.btnSaveRecipe.visibility = visibility
        binding.btnCancelEdit.visibility = visibility
    }

    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}