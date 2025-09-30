package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import ru.wizand.fermenttracker.ui.adapters.EditableStageTemplateAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel
import java.util.UUID

class BatchTemplateFragment : Fragment() {
    private var _binding: FragmentBatchTemplateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private lateinit var stageAdapter: EditableStageTemplateAdapter
    private var stages: MutableList<StageTemplate> = mutableListOf()
    private var selectedType: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupButtons()

        val initialType = arguments?.getString("recipe_type")
        if (initialType != null) {
            // Editing existing: Pre-load type and stages
            binding.etNewType.setText(initialType)
            loadStagesForType(initialType)
        } else {
            // Creating new: Clear stages, show etNewType
            binding.etNewType.setText("")
            stages.clear()
            stageAdapter.submitList(stages)
        }
    }

    private fun loadStagesForType(type: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val templates = withContext(Dispatchers.IO) {
                    viewModel.repository.getStageTemplatesForType(type).distinct()
                }
                val recipe = withContext(Dispatchers.IO) {
                    viewModel.repository.getAllRecipes().find { it.type == type }
                }

                // Проверяем, что фрагмент еще активен перед обновлением UI
                if (isAdded && !isDetached) {
                    stageAdapter.submitList(templates)
                    binding.etIngredients.setText(recipe?.ingredients ?: "")
                    binding.etNote.setText(recipe?.note ?: "")
                }
            } catch (e: CancellationException) {
                // Корутина была отменена - это нормально, не обрабатываем
                throw e
            } catch (e: Exception) {
                // Обрабатываем другие ошибки
                if (isAdded && !isDetached) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading stages: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        stageAdapter = EditableStageTemplateAdapter(
            onRemoveStage = { position ->
                val current = stageAdapter.currentList.toMutableList()
                if (position >= 0 && position < current.size) {
                    current.removeAt(position)
                    // Обновляем orderIndex для всех оставшихся этапов
                    current.forEachIndexed { index, template ->
                        template.orderIndex = index
                    }
                    stageAdapter.submitList(current)
                }
            }
        )
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStages.adapter = stageAdapter
    }

    private fun setupButtons() {
        binding.btnAddStage.setOnClickListener {
            val current = stageAdapter.currentList.toMutableList()
            val newTemplate = StageTemplate(
                id = UUID.randomUUID().toString(),
                recipeType = "", // будет перезаписан при сохранении
                name = "New Stage",
                durationHours = 24,
                orderIndex = current.size
            )
            current.add(newTemplate)
            stageAdapter.submitList(current)
        }

        binding.btnSaveTemplate.setOnClickListener {
            val type = binding.etNewType.text.toString()
            if (type.isEmpty()) {
                Toast.makeText(requireContext(), "Type is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ingredients = binding.etIngredients.text.toString()
            val note = binding.etNote.text.toString()
            val recipe = Recipe(type = type, ingredients = ingredients, note = note)
            val templates = stageAdapter.currentList.map { template ->
                template.copy(recipeType = type)
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        // Удаляем старые шаблоны для этого типа
                        val oldTemplates = viewModel.repository.getStageTemplatesForType(type)
                        oldTemplates.forEach { viewModel.repository.deleteStageTemplate(it.id) }

                        // Вставляем новые
                        viewModel.repository.insertRecipe(recipe)
                        templates.forEach { viewModel.repository.insertStageTemplate(it) }
                    }

                    // Проверяем, что фрагмент еще активен перед навигацией
                    if (isAdded && !isDetached) {
                        goBack()
                    }
                } catch (e: CancellationException) {
                    // Корутина была отменена - пробрасываем дальше
                    throw e
                } catch (e: Exception) {
                    // Обрабатываем другие ошибки
                    if (isAdded && !isDetached) {
                        Toast.makeText(
                            requireContext(),
                            "Error saving template: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun goBack() {
        try {
            // Пытаемся найти NavController безопасно
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.batchTemplateFragment) {
                navController.popBackStack()
            } else {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        } catch (e: IllegalStateException) {
            // NavController не найден - мы в RecipeEditorActivity
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}