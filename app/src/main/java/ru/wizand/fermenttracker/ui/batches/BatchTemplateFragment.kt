package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentBatchTemplateBinding
import ru.wizand.fermenttracker.ui.adapters.EditableStageAdapter

class BatchTemplateFragment : Fragment() {
    private var _binding: FragmentBatchTemplateBinding? = null
    private val binding get() = _binding!!
    private val args: BatchTemplateFragmentArgs by navArgs()
    private val stagesList = mutableListOf<Stage>()
    private lateinit var stagesAdapter: EditableStageAdapter

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
        setupStagesRecycler()
        loadTemplateStages()
        setupButtons()
    }

    private fun setupStagesRecycler() {
        stagesAdapter = EditableStageAdapter(
            onAddStage = { addCustomStage() },
            onRemoveStage = { pos -> removeStage(pos) }
        )
        binding.rvStages.adapter = stagesAdapter
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadTemplateStages() {
        val selectedType = args.selectedType
        val templates = RecipeTemplates.getTemplateStages(requireContext())
        stagesList.clear()
        stagesList.addAll(templates[selectedType] ?: emptyList())
        stagesAdapter.submitList(stagesList.toList())
    }

    private fun setupButtons() {
        binding.btnSaveStages.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "requestKey_stages",
                Bundle().apply {
                    putParcelableArrayList("editedStages", ArrayList(stagesList))
                }
            )
            findNavController().popBackStack()
        }
        binding.btnAddStage?.setOnClickListener { addCustomStage() }
    }

    private fun addCustomStage() {
        val newStage = Stage(
            id = java.util.UUID.randomUUID().toString(),
            name = "Custom Stage",
            durationHours = 24,
            orderIndex = stagesList.size
        )
        stagesList.add(newStage)
        stagesAdapter.submitList(stagesList.toList())
    }

    private fun removeStage(pos: Int) {
        stagesList.removeAt(pos)
        stagesAdapter.submitList(stagesList.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}