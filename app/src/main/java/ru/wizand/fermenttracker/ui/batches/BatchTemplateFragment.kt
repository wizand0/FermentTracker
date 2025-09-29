package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.ui.adapters.EditableStageTemplateAdapter  // Изменено: импортируем правильный адаптер для StageTemplate
import ru.wizand.fermenttracker.vm.BatchListViewModel
import androidx.fragment.app.activityViewModels
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchTemplateFragment : Fragment() {
    private var _binding: FragmentBatchTemplateBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private lateinit var stageAdapter: EditableStageTemplateAdapter  // Изменено: EditableStageTemplateAdapter вместо EditableStageAdapter
    private var stages: MutableList<StageTemplate> = mutableListOf()
    private var selectedType: String? = null  // Изменено: добавили обратно объявление переменной

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBatchTemplateBinding.inflate(inflater, container, false)
        return binding.root
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        setupSpinner()
//        setupRecyclerView()
//        setupButtons()
//
//        val initialType = arguments?.getString("recipe_type")
//        if (initialType != null) {
//            selectedType = initialType
//            loadStagesForType(initialType)
//        } else {
//            // Для нового: Очистите stages, покажите etNewType если нужно
//            stages.clear()
//            stageAdapter.submitList(stages)
//            // binding.tilNewType.visibility = View.VISIBLE  // Если хотите показывать для нового
//        }
//
//    }
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
        // Creating new: Clear stages, show etNewType (assume it's already visible in XML; if not, add visibility here)
        binding.etNewType.setText("")
        stages.clear()
        stageAdapter.submitList(stages)
    }
}



//    private fun setupSpinner() {
//        // Changed: load types from DB instead of array
//        CoroutineScope(Dispatchers.IO).launch {
//            val types = viewModel.repository.getAllRecipeTypes()
//            withContext(Dispatchers.Main) {
//                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
//                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//                binding.spinnerProductType.adapter = adapter
//                binding.spinnerProductType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
//                        selectedType = types[position]
//                        loadStagesForType(selectedType!!)
//                    }
//                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
//                })
//            }
//        }
//    }

//    private fun loadStagesForType(type: String) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val templates = viewModel.repository.getStageTemplatesForType(type)
//            val recipe = viewModel.repository.getAllRecipes().find { it.type == type }
//            stages = templates.toMutableList()
//            withContext(Dispatchers.Main) {
//                stageAdapter.submitList(stages)
//                binding.etIngredients.setText(recipe?.ingredients ?: "") // Added: load to new field (add to XML)
//                binding.etNote.setText(recipe?.note ?: "") // Added: load to new field (add to XML)
//            }
//        }
//    }

    private fun loadStagesForType(type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val templates = viewModel.repository.getStageTemplatesForType(type).distinct()
            val recipe = viewModel.repository.getAllRecipes().find { it.type == type }
            withContext(Dispatchers.Main) {
                stageAdapter.submitList(templates)
                binding.etIngredients.setText(recipe?.ingredients ?: "")
                binding.etNote.setText(recipe?.note ?: "")
            }
        }
    }


    private fun setupRecyclerView() {
        stageAdapter = EditableStageTemplateAdapter(
            onRemoveStage = { position ->
                val current = stageAdapter.currentList.toMutableList()
                if (position < 0 || position >= current.size) return@EditableStageTemplateAdapter
                current.removeAt(position)
                // Обновляем orderIndex
                current.forEachIndexed { index, template ->
                    template.orderIndex = index
                }
                stageAdapter.submitList(current)
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
//        binding.btnSaveTemplate.setOnClickListener {
//            val type = selectedType ?: binding.etNewType.text.toString() // Added: support new type (add etNewType to XML if needed)
//            if (type.isEmpty()) {
//                Toast.makeText(requireContext(), "Type is required", Toast.LENGTH_SHORT).show() // Added: basic error handling
//                return@setOnClickListener
//            }
//            val ingredients = binding.etIngredients.text.toString() // Added: new field in XML
//            val note = binding.etNote.text.toString() // Added: new field in XML
//            val recipe = Recipe(type = type, ingredients = ingredients, note = note)
//            val templates = stages.map { template ->
//                template.copy(recipeType = type) // Изменено: обновляем recipeType для всех (на случай новых)
//            }
//            CoroutineScope(Dispatchers.IO).launch {
//                viewModel.repository.insertRecipe(recipe)
//                templates.forEach { viewModel.repository.insertStageTemplate(it) }
//            }
//            findNavController().popBackStack()
//        }

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
            CoroutineScope(Dispatchers.IO).launch {
                viewModel.repository.insertRecipe(recipe)
                templates.forEach { viewModel.repository.insertStageTemplate(it) }
            }
            findNavController().popBackStack()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}