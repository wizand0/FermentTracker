package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentCreateBatchBinding
import ru.wizand.fermenttracker.ui.adapters.EditableStageAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateBatchFragment : Fragment() {
    private var _binding: FragmentCreateBatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private lateinit var stageAdapter: EditableStageAdapter
    private var stages: MutableList<Stage> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpinner()
        setupRecyclerView()
        setupButtons()
    }

    private fun setupSpinner() {
        // Changed: load from DB instead of array
        CoroutineScope(Dispatchers.IO).launch {
            val types = viewModel.repository.getAllRecipeTypes()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerProductType.adapter = adapter
                binding.spinnerProductType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedType = types[position]
                        loadStagesForType(selectedType)
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                })
            }
        }
    }

    private fun loadStagesForType(type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val templates = viewModel.repository.getStageTemplatesForType(type)
            stages = templates.mapIndexed { index, template ->
                Stage(
                    name = template.name,
                    durationHours = template.durationHours,
                    orderIndex = index
                )
            }.toMutableList()
            withContext(Dispatchers.Main) {
                stageAdapter.submitList(stages)
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
        binding.btnSave.setOnClickListener {
            val name = binding.etBatchName.text.toString()
            val initialWeightText = binding.etInitialWeight.text.toString()
            val initialWeight = initialWeightText.toDoubleOrNull()
            if (name.isEmpty()) {
                binding.etBatchName.error = "Batch name is required"
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            val batchId = UUID.randomUUID().toString()

            var lastPlannedEnd = now
            val calculatedStages = stages.mapIndexed { index, stage ->
                val plannedStartTime = if (index == 0) now else lastPlannedEnd
                val plannedEndTime = plannedStartTime + TimeUnit.HOURS.toMillis(stage.durationHours)
                lastPlannedEnd = plannedEndTime

                stage.copy(
                    batchId = batchId,
                    plannedStartTime = plannedStartTime,
                    plannedEndTime = plannedEndTime,
                    startTime = null,
                    orderIndex = index
                )
            }

            val totalDurationMs = calculatedStages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours) } // Added: compute total for plannedCompletionDate

            val batch = Batch(
                id = batchId,
                name = binding.etBatchName.text.toString(),
                type = binding.spinnerProductType.selectedItem.toString(),
                startDate = now,
                currentStage = calculatedStages.firstOrNull()?.name ?: "",
                notes = binding.etNotes.text.toString(),
                qrCode = binding.etQrCode.text.toString(),
                initialWeightGr = initialWeight,
                plannedCompletionDate = now + totalDurationMs // Added
            )

            // сохраняем в БД
            viewModel.createBatchWithStages(batch, calculatedStages)

            // планируем уведомления
            calculatedStages.forEach { stage ->
                viewModel.scheduleStageNotification(stage, batch)
            }

            findNavController().popBackStack()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}