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
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.ui.adapters.EditableStageAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel // Added: to access repository via VM
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
    private val viewModel: BatchListViewModel by activityViewModels() // Added: for repository access
    private lateinit var stageAdapter: EditableStageAdapter
    private var stages: MutableList<Stage> = mutableListOf() // Note: using Stage for editing, but will map to StageTemplate on save
    private var selectedType: String? = null // Added: track selected or new type

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
        setupSpinner()
        setupRecyclerView()
        setupButtons()
    }

    private fun setupSpinner() {
        // Changed: load types from DB instead of array
        CoroutineScope(Dispatchers.IO).launch {
            val types = viewModel.repository.getAllRecipeTypes()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerProductType.adapter = adapter
                binding.spinnerProductType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedType = types[position]
                        loadStagesForType(selectedType!!)
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                })
            }
        }
    }

    private fun loadStagesForType(type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val templates = viewModel.repository.getStageTemplatesForType(type)
            val recipe = viewModel.repository.getAllRecipes().find { it.type == type }
            stages = templates.map {
                Stage(
                    id = it.id,
                    name = it.name,
                    durationHours = it.durationHours,
                    orderIndex = it.orderIndex
                )
            }.toMutableList()
            withContext(Dispatchers.Main) {
                stageAdapter.submitList(stages)
                binding.etIngredients.setText(recipe?.ingredients ?: "") // Added: load to new field (add to XML)
                binding.etNote.setText(recipe?.note ?: "") // Added: load to new field (add to XML)
            }
        }
    }

    private fun setupRecyclerView() {
        stageAdapter = EditableStageAdapter(
            onAddStage = {
                stages.add(Stage(
                    id = UUID.randomUUID().toString(),
                    name = "New Stage",
                    durationHours = 24,
                    orderIndex = stages.size
                ))
                stageAdapter.submitList(stages.toList())
            },
            onRemoveStage = { position ->
                stages.removeAt(position)
                stages.forEachIndexed { index, stage -> stage.copy(orderIndex = index) }
                stageAdapter.submitList(stages.toList())
            }
        )
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStages.adapter = stageAdapter
    }

    private fun setupButtons() {
//        binding.btnAddStage.setOnClickListener {
//            stageAdapter.onAddStage()
//        }
        binding.btnSaveTemplate.setOnClickListener {
            val type = selectedType ?: binding.etNewType.text.toString() // Added: support new type (add etNewType to XML if needed)
            if (type.isEmpty()) {
                Toast.makeText(requireContext(), "Type is required", Toast.LENGTH_SHORT).show() // Added: basic error handling
                return@setOnClickListener
            }
            val ingredients = binding.etIngredients.text.toString() // Added: new field in XML
            val note = binding.etNote.text.toString() // Added: new field in XML
            val recipe = Recipe(type = type, ingredients = ingredients, note = note)
            val templates = stages.map {
                StageTemplate(
                    recipeType = type,
                    name = it.name,
                    durationHours = it.durationHours,
                    orderIndex = it.orderIndex
                )
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