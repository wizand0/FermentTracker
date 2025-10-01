package ru.wizand.fermenttracker.ui.adapters

import android.app.AlertDialog
import android.app.Application
import android.text.Editable
import android.text.TextWatcher
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

class StageAdapter(
    private val onStartClicked: (Stage) -> Unit = {},
    private val onCompleteClicked: (Stage) -> Unit = {},
    private val onDurationChanged: (Stage) -> Unit = {}
) : ListAdapter<Stage, StageAdapter.StageViewHolder>(StageDiffCallback()) {

    // Хранит id активного этапа (тот, который сейчас выполняется). Устанавливается извне (фрагмент/VM).
    var activeStageId: String? = null
        private set

    fun setActiveStage(id: String?) {
        activeStageId = id
        notifyDataSetChanged()
    }

    var batchCurrentWeight: Double? = null // держим текущий вес батча для отображения в активном этапе

    fun updateBatchWeight(weight: Double?) { // метод для обновления веса и обновления UI
        batchCurrentWeight = weight
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StageViewHolder {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StageViewHolder(private val binding: ItemStageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stage: Stage) {
            binding.tvStageName.text = stage.name
            binding.tvDuration.text = binding.root.context.getString(R.string.duration_format, stage.durationHours.toDouble())

            // ========== Для duration ==========
            // Удаляем ВСЕ старые watchers из etDuration
            while (binding.etDuration.tag != null) {
                (binding.etDuration.tag as? TextWatcher)?.let {
                    binding.etDuration.removeTextChangedListener(it)
                }
                binding.etDuration.tag = null
            }

            binding.etDuration.setText(stage.durationHours.toString())

            // Создаём новый TextWatcher и сохраняем его в tag
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newDuration = s.toString().toLongOrNull()
                    if (newDuration != null && newDuration != stage.durationHours) {
                        val updatedStage = stage.copy(durationHours = newDuration)
                        onDurationChanged(updatedStage)
                    }
                }
            }
            binding.etDuration.tag = watcher
            binding.etDuration.addTextChangedListener(watcher)

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
    }
}