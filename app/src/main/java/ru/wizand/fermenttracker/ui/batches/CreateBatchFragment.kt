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
import androidx.navigation.fragment.findNavController
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
import java.util.UUID
import java.util.concurrent.TimeUnit

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
                        // Load recipe first to get ingredients and notes
                        CoroutineScope(Dispatchers.IO).launch {
                            val recipe = viewModel.repository.getRecipeByType(selectedType) // Теперь вызов корректен
                            if (recipe != null) {
                                withContext(Dispatchers.Main) {
                                    showRecipeDetailsDialog(recipe)
                                }
                            } else {
                                // Handle case where recipe is not found
                                // Показать сообщение пользователю или очистить поля
                                withContext(Dispatchers.Main) {
                                    // Например, можно показать короткое сообщение
                                    // Toast.makeText(context, "Recipe for '$selectedType' not found!", Toast.LENGTH_SHORT).show()
                                    // Или просто очистить поля
                                    binding.etIngredientsDisplay.setText("")
                                    binding.etNoteDisplay.setText("")
                                }
                            }
                        }
                        // Load stages separately
                        loadStagesForType(selectedType)
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
                })
            }
        }
    }

    private fun showRecipeDetailsDialog(recipe: Recipe) {
        val context = context ?: return // Проверка на null контекста

        // Создаем ScrollView вручную или из XML (предпочтительно XML)
        // Для простоты примера создадим в коде
        val scrollView = NestedScrollView(context).apply {
            // Используем NestedScrollView, если он внутри другого ScrollView/RecyclerView
            // Иначе подойдёт обычный ScrollView
            // val scrollView = ScrollView(context).apply {
            isFillViewport = true // Позволяет содержимому заполнять вью
        }

        val contentLayout = androidx.appcompat.widget.LinearLayoutCompat(context).apply {
            orientation = androidx.appcompat.widget.LinearLayoutCompat.VERTICAL
            // Добавим отступы
            val padding = resources.getDimensionPixelSize(R.dimen.dialog_content_padding) // Определите размер в res/values/dimens.xml
            setPadding(padding, padding, padding, padding)
        }

        val ingredientsTitle = TextView(context).apply {
            text = getString(R.string.ingredients_title) // Определите строку в res/values/strings.xml, например, "Ingredients:"
            textSize = 16f
            // Добавьте стиль при необходимости
        }
        val ingredientsText = TextView(context).apply {
            text = recipe.ingredients
            textSize = 14f
            // Добавьте стиль при необходимости
        }

        val noteTitle = TextView(context).apply {
            text = getString(R.string.note_title) // Определите строку в res/values/strings.xml, например, "Instructions/Note:"
            textSize = 16f
            // Добавьте стиль при необходимости
        }
        val noteText = TextView(context).apply {
            text = recipe.note
            textSize = 14f
            // Добавьте стиль при необходимости
        }

        contentLayout.addView(ingredientsTitle)
        contentLayout.addView(ingredientsText)
        contentLayout.addView(noteTitle)
        contentLayout.addView(noteText)

        scrollView.addView(contentLayout)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.recipe_details_title, recipe.type)) // Определите строку, например, "Recipe for %s"
            .setView(scrollView) // Устанавливаем ScrollView как содержимое
            .setPositiveButton(R.string.ok) { _, _ ->
                // После нажатия OK - заполняем поля в основном UI и разрешаем редактирование
                binding.etIngredientsDisplay.setText(recipe.ingredients)
                binding.etNoteDisplay.setText(recipe.note)
            }
            .setNegativeButton(R.string.cancel, null) // Действие по умолчанию (ничего) для Cancel
            .create()

        dialog.show()
    }


    private fun loadStagesForType(type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val templates = viewModel.repository.getStageTemplatesForType(type)
            stages = templates.mapIndexed { index, template ->
                Stage( // Теперь указываем все обязательные поля
                    id = UUID.randomUUID().toString(), // Генерируем UUID
                    batchId = "", // batchId пока пустой, будет установлен позже
                    name = template.name,
                    durationHours = template.durationHours, // Убедимся, что это Double
                    orderIndex = index
                )
            }.toMutableList()
            withContext(Dispatchers.Main) {
                stageAdapter.submitList(stages.toList()) // Передаём копию списка
            }
        }
    }

    private fun setupRecyclerView() {
        stageAdapter = EditableStageAdapter( // Изменено: передаем onRemoveStage
            onRemoveStage = { position ->
                stages.removeAt(position)
                // Обновляем orderIndex для оставшихся
                stages.forEachIndexed { index, stage ->
                    stages[index] = stage.copy(orderIndex = index) // Изменяем сам список
                }
                stageAdapter.submitList(stages.toList())
            }
        )
        binding.rvStages.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStages.adapter = stageAdapter
    }

    private fun setupButtons() {
        binding.btnAddStage.setOnClickListener {
            stages.add(Stage( // Указываем все обязательные поля
                id = UUID.randomUUID().toString(), // Генерируем UUID
                batchId = "", // batchId пока пустой, будет установлен позже
                name = getString(R.string.new_stage_name),
                durationHours = 24, // Указываем Double
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
            val batchId = UUID.randomUUID().toString()

            var lastPlannedEnd = now
            val calculatedStages = stages.mapIndexed { index, stage ->
                val plannedStartTime = if (index == 0) now else lastPlannedEnd
                // Используем TimeUnit.HOURS.toMillis, который возвращает Long
                val plannedEndTime = plannedStartTime + TimeUnit.HOURS.toMillis(stage.durationHours.toLong()) // Исправлено: toLong()
                lastPlannedEnd = plannedEndTime

                stage.copy( // Копируем и устанавливаем batchId и другие поля
                    batchId = batchId, // Устанавливаем batchId
                    plannedStartTime = plannedStartTime,
                    plannedEndTime = plannedEndTime,
                    startTime = null,
                    orderIndex = index
                    // id остаётся тем же (UUID), что был сгенерирован при создании stage
                )
            }

            val totalDurationMs = calculatedStages.sumOf { TimeUnit.HOURS.toMillis(it.durationHours.toLong()) } // Исправлено: toLong()

            val batch = Batch(
                id = batchId,
                name = binding.etBatchName.text.toString(),
                type = binding.spinnerProductType.selectedItem.toString(),
                startDate = now,
                currentStage = calculatedStages.firstOrNull()?.name ?: "",
                notes = binding.etNotes.text.toString(),
//                qrCode = binding.etQrCode.text.toString(),
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