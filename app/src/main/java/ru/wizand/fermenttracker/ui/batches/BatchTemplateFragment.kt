package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import androidx.navigation.fragment.findNavController
import ru.wizand.fermenttracker.ui.adapters.EditableStageAdapter
import java.util.UUID

class BatchTemplateFragment : Fragment() {
    private var _binding: FragmentBatchTemplateBinding? = null
    private val binding get() = _binding!!
    private lateinit var stageAdapter: EditableStageAdapter
    private var stages: MutableList<Stage> = mutableListOf()

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
        val types = resources.getStringArray(R.array.fermentation_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProductType.adapter = adapter
        binding.spinnerProductType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedType = types[position]
                stages = RecipeTemplates.getTemplateStages(requireContext())[selectedType]
                    ?.mapIndexed { index, stage ->
                        stage.copy(orderIndex = index)
                    }?.toMutableList() ?: mutableListOf()
                stageAdapter.submitList(stages)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })
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
            // Save template logic (e.g., update RecipeTemplates or navigate back)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}