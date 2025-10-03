// ru/wizand/fermenttracker/ui/batches/CreateBatchFragment.kt
package ru.wizand.fermenttracker.ui.batches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.FragmentCreateBatchBinding
import ru.wizand.fermenttracker.ui.adapters.EditableStageAdapter
import ru.wizand.fermenttracker.vm.BatchListViewModel
import java.util.*
import java.util.concurrent.TimeUnit

class CreateBatchFragment : Fragment() {
    private var _binding: FragmentCreateBatchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BatchListViewModel by activityViewModels()
    private lateinit var stageAdapter: EditableStageAdapter
    private var stages: MutableList<Stage> = mutableListOf()

    // Добавляем переменную для хранения batchId
    private var batchId: String? = null
    private var isEditMode = false

    // Добавляем аргументы для навигации
    private val args: CreateBatchFragmentArgs by navArgs()

    // Добавляем переменную для хранения исходной даты начала
    private var originalStartDate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Получаем batchId из аргументов
        batchId = args.batchId
        isEditMode = batchId != null
    }

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

        // Устанавливаем заголовок в зависимости от режима
        binding.toolbar.title = if (isEditMode) {
            getString(R.string.edit_batch)
        } else {
            getString(R.string.create_batch)
        }

        // Устанавливаем обработчик нажатия на кнопку "назад"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupSpinner()
        setupRecyclerView()
        setupButtons()

        // Если в режиме редактирования, загружаем данные партии
        if (isEditMode) {
            loadBatchData()
        }
    }

    private fun loadBatchData() {
        batchId?.let { id ->
            CoroutineScope(Dispatchers.IO).launch {
                val batch = viewModel.repository.getBatchByIdOnce(id)
                if (batch != null) {
                    originalStartDate = batch.startDate

                    withContext(Dispatchers.Main) {
                        // Заполняем поля формы данными из партии
                        binding.etBatchName.setText(batch.name)

                        // Загружаем типы рецептов и устанавливаем выбранный тип продукта
                        CoroutineScope(Dispatchers.IO).launch {
                            val types = viewModel.repository.getAllRecipeTypes()
                            withContext(Dispatchers.Main) {
                                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.spinnerProductType.adapter = adapter

                                // Устанавливаем выбранный тип продукта
                                val position = types.indexOf(batch.type)
                                if (position >= 0) {
                                    binding.spinnerProductType.setSelection(position)
                                }

                                // Загружаем рецепт для заполнения ингредиентов и заметок
                                CoroutineScope(Dispatchers.IO).launch {
                                    val recipe = viewModel.repository.getRecipeByType(batch.type)
                                    if (recipe != null) {
                                        withContext(Dispatchers.Main) {
                                            binding.etIngredientsDisplay.setText(recipe.ingredients)
                                            binding.etNoteDisplay.setText(recipe.note)
                                        }
                                    }
                                }

                                binding.etInitialWeight.setText(batch.initialWeightGr?.toString() ?: "")
                                binding.etNotes.setText(batch.notes)
                            }
                        }

                        // Загружаем этапы для этой партии
                        val batchStages = viewModel.getStagesForBatchLive(id)

                        // Наблюдаем за изменениями в списке этапов
                        batchStages.observe(viewLifecycleOwner) { stageList ->
                            stages = stageList.toMutableList()
                            stageAdapter.submitList(stages.toList())
                        }
                    }
                }
            }
        }
    }

    private fun setupSpinner() {
        CoroutineScope(Dispatchers.IO).launch {
            val types = viewModel.repository.getAllRecipeTypes()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerProductType.adapter = adapter
                binding.spinnerProductType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedType = types[position]
                        // Load recipe first to get ingredients and notes
                        CoroutineScope(Dispatchers.IO).launch {
                            val recipe = viewModel.repository.getRecipeByType(selectedType)
                            if (recipe != null) {
                                withContext(Dispatchers.Main) {
                                    showRecipeDetailsDialog(recipe)
                                }
                            } else {
                                // Handle case where recipe is not found
                                withContext(Dispatchers.Main) {
                                    binding.etIngredientsDisplay.setText("")
                                    binding.etNoteDisplay.setText("")
                                }
                            }
                        }
                        // Load stages separately, только если не в режиме редактирования
                        if (!isEditMode) {
                            loadStagesForType(selectedType)
                        }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                })
            }
        }
    }

    private fun showRecipeDetailsDialog(recipe: Recipe) {
        val context = context ?: return

        val scrollView = NestedScrollView(context).apply {
            isFillViewport = true
        }

        val contentLayout = androidx.appcompat.widget.LinearLayoutCompat(context).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_content_padding)
            setPadding(padding, padding, padding, padding)
        }

        val ingredientsTitle = TextView(context).apply {
            text = getString(R.string.ingredients_title)
            textSize = 16f
        }
        val ingredientsText = TextView(context).apply {
            text = recipe.ingredients
            textSize = 14f
        }

        val noteTitle = TextView(context).apply {
            text = getString(R.string.note_title)
            textSize = 16f
        }
        val noteText = TextView(context).apply {
            text = recipe.note
            textSize = 14f
        }

        contentLayout.addView(ingredientsTitle)
        contentLayout.addView(ingredientsText)
        contentLayout.addView(noteTitle)
        contentLayout.addView(noteText)

        scrollView.addView(contentLayout)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.recipe_details_title, recipe.type))
            .setView(scrollView)
            .setPositiveButton(R.string.ok) { _, _ ->
                binding.etIngredientsDisplay.setText(recipe.ingredients)
                binding.etNoteDisplay.setText(recipe.note)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun loadStagesForType(type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val templates = viewModel.repository.getStageTemplatesForType(type)
            stages = templates.mapIndexed { index, template ->
                Stage(
                    id = UUID.randomUUID().toString(),
                    batchId = "",
                    name = template.name,
                    durationHours = template.durationHours,
                    orderIndex = index
                )
            }.toMutableList()
            withContext(Dispatchers.Main) {
                stageAdapter.submitList(stages.toList())
            }
        }
    }

    private fun setupRecyclerView() {
        stageAdapter = EditableStageAdapter(
            onRemoveStage = { position ->
                stages.removeAt(position)
                stages.forEachIndexed { index, stage ->
                    stages[index] = stage.copy(orderIndex = index)
                }
                stageAdapter.submitList(stages.toList())
            }
        )
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStages.adapter = stageAdapter
    }

    private fun setupButtons() {
        binding.btnAddStage.setOnClickListener {
            stages.add(Stage(
                id = UUID.randomUUID().toString(),
                batchId = batchId ?: "",
                name = getString(R.string.new_stage_name),
                durationHours = 24,
                orderIndex = stages.size
            ))
            stageAdapter.submitList(stages.toList())
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etBatchName.text.toString()
            val initialWeightText = binding.etInitialWeight.text.toString()
            val initialWeight = initialWeightText.toDoubleOrNull()
            if (name.isEmpty()) {
                binding.etBatchName.error = getString(R.string.batch_name_required)
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            val batchIdToUse = batchId ?: UUID.randomUUID().toString()

            var lastPlannedEnd = now
            val calculatedStages = stages.mapIndexed { index, stage ->
                val plannedStartTime = if (index == 0) now else lastPlannedEnd
                val plannedEndTime = plannedStartTime + TimeUnit.HOURS.toMillis(stage.durationHours.toLong())
                lastPlannedEnd = plannedEndTime

                stage.copy(
                    batchId = batchIdToUse,
                    plannedStartTime = plannedStartTime,
                    plannedEndTime = plannedEndTime,
                    startTime = null,
                    orderIndex = index
                )
            }

            val totalDurationMs = calculatedStages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours.toLong()) }

            val batch = Batch(
                id = batchIdToUse,
                name = binding.etBatchName.text.toString(),
                type = binding.spinnerProductType.selectedItem.toString(),
                startDate = if (isEditMode) originalStartDate else now,
                currentStage = calculatedStages.firstOrNull()?.name ?: "",
                notes = binding.etNotes.text.toString(),
                initialWeightGr = initialWeight,
                plannedCompletionDate = now + totalDurationMs
            )

            if (isEditMode) {
                // В режиме редактирования обновляем существующую партию
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.repository.updateBatch(batch)

                    // Получаем старые этапы
                    val oldStages = viewModel.getStagesForBatchLive(batchIdToUse)

                    // Используем lifecycleScope для наблюдения за LiveData
                    lifecycleScope.launch {
                        oldStages.observe(viewLifecycleOwner) { stageList ->
                            // Удаляем старые этапы
                            CoroutineScope(Dispatchers.IO).launch {
                                stageList.forEach { stage ->
                                    viewModel.repository.deleteStage(stage.id)
                                }

                                // Добавляем новые этапы
                                calculatedStages.forEach { stage ->
                                    viewModel.repository.addStage(stage)
                                }

                                // Планируем уведомления
                                calculatedStages.forEach { stage ->
                                    viewModel.scheduleStageNotification(stage, batch)
                                }
                            }
                        }
                    }
                }
            } else {
                // В режиме создания создаем новую партию
                viewModel.createBatchWithStages(batch, calculatedStages)

                // Планируем уведомления
                calculatedStages.forEach { stage ->
                    viewModel.scheduleStageNotification(stage, batch)
                }
            }

            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}