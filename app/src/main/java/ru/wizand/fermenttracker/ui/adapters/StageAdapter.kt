package ru.wizand.fermenttracker.ui.adapters

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import ru.wizand.fermenttracker.R

/**
 * Sealed class для payloads, используемых при частичном обновлении элементов списка.
 * Это позволяет избежать полного перерисовки элемента и улучшает производительность.
 */
sealed class StagePayload {
    data class DurationChanged(val newDuration: Long) : StagePayload()
    data class ActiveChanged(val isActive: Boolean) : StagePayload()
    data class WeightChanged(val weight: Double?) : StagePayload()
    data class TimeLeftUpdated(val timeLeft: String) : StagePayload()
    data class StageCompleted(val isCompleted: Boolean) : StagePayload()
    data class StageStarted(val isStarted: Boolean) : StagePayload()
}

class StageAdapter(
    private val onStartClicked: (Stage) -> Unit = {},
    private val onCompleteClicked: (Stage) -> Unit = {},
    private val onDurationChanged: (Stage) -> Unit = {}
) : ListAdapter<Stage, StageAdapter.VH>(StageDiffCallback()) {

    // Хранит id активного этапа (тот, который сейчас выполняется). Устанавливается извне (фрагмент/VM).
    var activeStageId: String? = null
        private set

    /**
     * Устанавливает активный этап и обновляет только соответствующие элементы списка
     * вместо полного обновления через notifyDataSetChanged()
     */
    fun setActiveStage(id: String?) {
        val previousActiveId = activeStageId
        activeStageId = id

        // Обновляем предыдущий активный этап
        previousActiveId?.let { prevId ->
            val position = currentList.indexOfFirst { it.id == prevId }
            if (position != -1) {
                notifyItemChanged(position, StagePayload.ActiveChanged(false))
            }
        }

        // Обновляем новый активный этап
        id?.let { newId ->
            val position = currentList.indexOfFirst { it.id == newId }
            if (position != -1) {
                notifyItemChanged(position, StagePayload.ActiveChanged(true))
            }
        }
    }

    var batchCurrentWeight: Double? = null // держим текущий вес батча для отображения в активном этапе

    /**
     * Обновляет вес и обновляет UI только для активного этапа
     */
    fun updateBatchWeight(weight: Double?) {
        batchCurrentWeight = weight
        activeStageId?.let { id ->
            val position = currentList.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position, StagePayload.WeightChanged(weight))
            }
        }
    }

    /**
     * Обновляет оставшееся время для активного этапа
     */
    fun updateTimeLeft(timeLeft: String) {
        activeStageId?.let { id ->
            val position = currentList.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position, StagePayload.TimeLeftUpdated(timeLeft))
            }
        }
    }

    /**
     * Обновляет статус завершения этапа
     */
    fun updateStageCompleted(stageId: String, isCompleted: Boolean) {
        val position = currentList.indexOfFirst { it.id == stageId }
        if (position != -1) {
            notifyItemChanged(position, StagePayload.StageCompleted(isCompleted))
        }
    }

    /**
     * Обновляет статус начала этапа
     */
    fun updateStageStarted(stageId: String, isStarted: Boolean) {
        val position = currentList.indexOfFirst { it.id == stageId }
        if (position != -1) {
            notifyItemChanged(position, StagePayload.StageStarted(isStarted))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Обрабатывает частичные обновления элементов списка через payloads
     */
    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    is StagePayload.DurationChanged -> holder.bindDurationChanged(item, payload.newDuration)
                    is StagePayload.ActiveChanged -> holder.bindActiveChanged(item, payload.isActive)
                    is StagePayload.WeightChanged -> holder.bindWeightChanged(item, payload.weight)
                    is StagePayload.TimeLeftUpdated -> holder.bindTimeLeftUpdated(item, payload.timeLeft)
                    is StagePayload.StageCompleted -> holder.bindStageCompleted(item, payload.isCompleted)
                    is StagePayload.StageStarted -> holder.bindStageStarted(item, payload.isStarted)
                }
            }
        }
    }

    inner class VH(private val binding: ItemStageBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Устанавливаем слушатели один раз в init блоке, а не при каждом bind()
            binding.etDuration.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Когда поле теряет фокус, обрабатываем изменение длительности
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val stage = currentList[adapterPosition]
                        val newDuration = binding.etDuration.text.toString().toLongOrNull() ?: 0L

                        if (newDuration != stage.durationHours) {
                            // Обновляем модель
                            val updatedStage = stage.copy(durationHours = newDuration)
                            val newList = currentList.toMutableList()
                            newList[adapterPosition] = updatedStage
                            submitList(newList)   // триггерит перерисовку
                            onDurationChanged(updatedStage)
                        }
                    }
                }
            }
        }

        fun bind(stage: Stage) {
            binding.tvStageName.text = stage.name
            binding.tvDuration.text = binding.root.context.getString(R.string.duration_format, stage.durationHours.toDouble())
            binding.etDuration.setText(stage.durationHours.toString())

            // Определяем состояние "выполняется" по activeStageId (приоритет) либо по полям startTime/endTime
            val isThisActive = activeStageId != null && activeStageId == stage.id
            val inferredActive = (stage.startTime != null && stage.endTime == null)
            val isActive = isThisActive || inferredActive

            // Показываем вес только для активного этапа (если известен)
            binding.tvCurrentWeight.text = if (isActive) {
                batchCurrentWeight?.let { binding.root.context.getString(R.string.weight_format, it) } ?: binding.root.context.getString(R.string.weight_na)
            } else {
                binding.root.context.getString(R.string.na) // используем существующую строку
            }

            binding.tvPlannedStartTime.text = stage.plannedStartTime?.let {
                binding.root.context.getString(R.string.planned_start_format, formatDate(it))
            } ?: binding.root.context.getString(R.string.planned_start_na)
            binding.tvPlannedEndTime.text = stage.plannedEndTime?.let {
                binding.root.context.getString(R.string.planned_end_format, formatDate(it))
            } ?: binding.root.context.getString(R.string.planned_end_na)
            binding.tvStartTime.text = stage.startTime?.let {
                binding.root.context.getString(R.string.start_format, formatDate(it))
            } ?: binding.root.context.getString(R.string.start_not_started)
            binding.tvEndTime.text = stage.endTime?.let {
                binding.root.context.getString(R.string.end_format, formatDate(it))
            } ?: binding.root.context.getString(R.string.end_ongoing)

            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, computeTimeLeftText(stage))

            val btnStartStage = binding.btnStartStage
            val btnCompleteStage = binding.btnCompleteStage

            // Если есть активный этап (activeStageId != null), то Start доступен только если нет активного,
            // а Complete доступен только у активного этапа
            val anyRunning = activeStageId != null
            btnStartStage.isVisible = !anyRunning && stage.startTime == null
            btnCompleteStage.isVisible = isActive && stage.endTime == null

            btnStartStage.setOnClickListener {
                val context = binding.root.context
                AlertDialog.Builder(context)
                    .setTitle(binding.root.context.getString(R.string.start_stage_title))
                    .setMessage(binding.root.context.getString(R.string.start_stage_message))
                    .setPositiveButton(binding.root.context.getString(R.string.start_stage_confirm)) { _, _ ->
                        onStartClicked(stage)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            btnCompleteStage.setOnClickListener {
                val context = binding.root.context
                AlertDialog.Builder(context)
                    .setTitle(binding.root.context.getString(R.string.complete_stage_title))
                    .setMessage(binding.root.context.getString(R.string.complete_stage_message))
                    .setPositiveButton(binding.root.context.getString(R.string.complete_stage_confirm)) { _, _ ->
                        onCompleteClicked(stage)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        /**
         * Обрабатывает изменение длительности через payload
         */
        fun bindDurationChanged(stage: Stage, newDuration: Long) {
            binding.tvDuration.text = binding.root.context.getString(R.string.duration_format, newDuration.toDouble())
            binding.etDuration.setText(newDuration.toString())

            // Обновляем оставшееся время
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, computeTimeLeftText(stage.copy(durationHours = newDuration)))
        }

        /**
         * Обрабатывает изменение активности через payload
         */
        fun bindActiveChanged(stage: Stage, isActive: Boolean) {
            val btnStartStage = binding.btnStartStage
            val btnCompleteStage = binding.btnCompleteStage

            // Показываем вес только для активного этапа
            binding.tvCurrentWeight.text = if (isActive) {
                batchCurrentWeight?.let { binding.root.context.getString(R.string.weight_format, it) } ?: binding.root.context.getString(R.string.weight_na)
            } else {
                binding.root.context.getString(R.string.na)
            }

            // Обновляем видимость кнопок
            val anyRunning = activeStageId != null
            btnStartStage.isVisible = !anyRunning && stage.startTime == null
            btnCompleteStage.isVisible = isActive && stage.endTime == null

            // Обновляем оставшееся время
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, computeTimeLeftText(stage))
        }

        /**
         * Обрабатывает изменение веса через payload
         */
        fun bindWeightChanged(stage: Stage, weight: Double?) {
            // Показываем вес только для активного этапа
            binding.tvCurrentWeight.text = weight?.let {
                binding.root.context.getString(R.string.weight_format, it)
            } ?: binding.root.context.getString(R.string.weight_na)
        }

        /**
         * Обрабатывает обновление оставшегося времени через payload
         */
        fun bindTimeLeftUpdated(stage: Stage, timeLeft: String) {
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, timeLeft)
        }

        /**
         * Обрабатывает изменение статуса завершения через payload
         */
        fun bindStageCompleted(stage: Stage, isCompleted: Boolean) {
            val btnCompleteStage = binding.btnCompleteStage
            btnCompleteStage.isVisible = !isCompleted && activeStageId == stage.id

            // Обновляем текст времени окончания
            binding.tvEndTime.text = if (isCompleted) {
                binding.root.context.getString(R.string.end_format, formatDate(System.currentTimeMillis()))
            } else {
                binding.root.context.getString(R.string.end_ongoing)
            }
        }

        /**
         * Обрабатывает изменение статуса начала через payload
         */
        fun bindStageStarted(stage: Stage, isStarted: Boolean) {
            val btnStartStage = binding.btnStartStage
            btnStartStage.isVisible = !isStarted && activeStageId == null

            // Обновляем текст времени начала
            binding.tvStartTime.text = if (isStarted) {
                binding.root.context.getString(R.string.start_format, formatDate(System.currentTimeMillis()))
            } else {
                binding.root.context.getString(R.string.start_not_started)
            }
        }

        /**
         * Очищает слушатели для предотвращения утечек памяти
         */
        fun clearListeners() {
            binding.etDuration.setOnFocusChangeListener(null)
            binding.btnStartStage.setOnClickListener(null)
            binding.btnCompleteStage.setOnClickListener(null)
        }

        private fun formatDate(timestamp: Long): String {
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(timestamp))
        }

        private fun computeTimeLeftText(stage: Stage): String {
            val now = System.currentTimeMillis()
            val expectedEnd = when {
                stage.endTime != null -> stage.endTime
                stage.startTime != null -> stage.startTime!! + TimeUnit.HOURS.toMillis(stage.durationHours)
                stage.plannedEndTime != null -> stage.plannedEndTime
                else -> null
            }
            return if (expectedEnd == null) {
                "N/A"
            } else {
                val diff = expectedEnd - now
                if (diff <= 0) "Overdue" else formatDuration(diff)
            }
        }

        private fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(hours)
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    }

    class StageDiffCallback : DiffUtil.ItemCallback<Stage>() {
        override fun areItemsTheSame(oldItem: Stage, newItem: Stage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Stage, newItem: Stage): Boolean = oldItem == newItem

        override fun getChangePayload(oldItem: Stage, newItem: Stage): Any? {
            // Определяем, что именно изменилось, чтобы передать соответствующий payload
            return when {
                oldItem.durationHours != newItem.durationHours -> StagePayload.DurationChanged(newItem.durationHours)
                oldItem.startTime != newItem.startTime -> StagePayload.StageStarted(newItem.startTime != null)
                oldItem.endTime != newItem.endTime -> StagePayload.StageCompleted(newItem.endTime != null)
                else -> null
            }
        }
    }
}