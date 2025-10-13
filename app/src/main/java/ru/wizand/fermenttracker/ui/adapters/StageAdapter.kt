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
import java.util.*
import java.util.concurrent.TimeUnit
import ru.wizand.fermenttracker.R

/**
 * Sealed class для payloads.
 */
sealed class StagePayload {
    data class DurationChanged(val newDuration: Long) : StagePayload()
    data class ActiveChanged(val isActive: Boolean, val weight: Double?) : StagePayload()
    data class WeightChanged(val weight: Double?) : StagePayload()
    data class TimeLeftUpdated(val timeLeft: String) : StagePayload()
    data class StageCompleted(val isCompleted: Boolean) : StagePayload()
    data class StageStarted(val isStarted: Boolean) : StagePayload()
}

/**
 * CompositePayload для передачи нескольких изменений за один раз,
 * чтобы избежать множественных notifyItemChanged.
 */
data class CompositeStagePayload(
    val durationChanged: StagePayload.DurationChanged? = null,
    val activeChanged: StagePayload.ActiveChanged? = null,
    val weightChanged: StagePayload.WeightChanged? = null,
    val timeLeftUpdated: StagePayload.TimeLeftUpdated? = null,
    val stageCompleted: StagePayload.StageCompleted? = null,
    val stageStarted: StagePayload.StageStarted? = null
)

class StageAdapter(
    private val onStartClicked: (Stage) -> Unit = {},
    private val onCompleteClicked: (Stage) -> Unit = {},
    private val onDurationChanged: (Stage) -> Unit = {}
) : ListAdapter<Stage, StageAdapter.VH>(StageDiffCallback()) {

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    var activeStageId: String? = null
        private set

    fun setActiveStage(id: String?) {
        val previousActiveId = activeStageId
        activeStageId = id

        // Используем CompositePayload для двойного обновления
        val composite = CompositeStagePayload(
            activeChanged = StagePayload.ActiveChanged(false, batchCurrentWeight),
            // Для нового активного — отдельно
        )

        previousActiveId?.let { prevId ->
            val position = currentList.indexOfFirst { it.id == prevId }
            if (position != -1) {
                notifyItemChanged(position, composite)
            }
        }

        id?.let { newId ->
            val position = currentList.indexOfFirst { it.id == newId }
            if (position != -1) {
                notifyItemChanged(position, CompositeStagePayload(
                    activeChanged = StagePayload.ActiveChanged(true, batchCurrentWeight)
                ))
            }
        }
    }

    var batchCurrentWeight: Double? = null

    fun updateBatchWeight(weight: Double?) {
        batchCurrentWeight = weight
        activeStageId?.let { id ->
            val position = currentList.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position, CompositeStagePayload(
                    weightChanged = StagePayload.WeightChanged(weight)
                ))
            }
        }
    }

    fun updateTimeLeft(timeLeft: String) {
        activeStageId?.let { id ->
            val position = currentList.indexOfFirst { it.id == id }
            if (position != -1) {
                notifyItemChanged(position, CompositeStagePayload(
                    timeLeftUpdated = StagePayload.TimeLeftUpdated(timeLeft)
                ))
            }
        }
    }

    fun updateStageCompleted(stageId: String, isCompleted: Boolean) {
        val position = currentList.indexOfFirst { it.id == stageId }
        if (position != -1) {
            notifyItemChanged(position, CompositeStagePayload(
                stageCompleted = StagePayload.StageCompleted(isCompleted)
            ))
        }
    }

    fun updateStageStarted(stageId: String, isStarted: Boolean) {
        val position = currentList.indexOfFirst { it.id == stageId }
        if (position != -1) {
            notifyItemChanged(position, CompositeStagePayload(
                stageStarted = StagePayload.StageStarted(isStarted)
            ))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    is CompositeStagePayload -> holder.bindCompositePayload(item, payload)
                    // Остальные для обратной совместимости
                    is StagePayload.DurationChanged -> holder.bindDurationChanged(item, payload.newDuration)
                    is StagePayload.ActiveChanged -> holder.bindActiveChanged(item, payload.isActive, payload.weight)
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
            binding.etDuration.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val stage = currentList[adapterPosition]
                        val newDuration = binding.etDuration.text.toString().toLongOrNull() ?: 0L
                        if (newDuration != stage.durationHours) {
                            val updatedStage = stage.copy(durationHours = newDuration)
                            submitList(currentList.map { if (it.id == stage.id) updatedStage else it })
                            onDurationChanged(updatedStage)
                        }
                    }
                }
            }
        }

        fun bind(stage: Stage) {
            // То же, что и прежде, с небольшими упрощениями
            binding.tvStageName.text = stage.name
            binding.tvDuration.text = binding.root.context.getString(R.string.duration_format, stage.durationHours.toDouble())
            binding.etDuration.setText(stage.durationHours.toString())

            val isThisActive = activeStageId != null && activeStageId == stage.id
            val inferredActive = (stage.startTime != null && stage.endTime == null)
            val isActive = isThisActive || inferredActive

            binding.tvCurrentWeight.text = if (isActive) {
                batchCurrentWeight?.let { binding.root.context.getString(R.string.weight_format, it) } ?: binding.root.context.getString(R.string.weight_na)
            } else {
                binding.root.context.getString(R.string.na)
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

            val anyRunning = activeStageId != null
            binding.btnStartStage.isVisible = !anyRunning && stage.startTime == null
            binding.btnCompleteStage.isVisible = isActive && stage.endTime == null

            binding.btnStartStage.setOnClickListener {
                val context = binding.root.context
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.start_stage_title))
                    .setMessage(context.getString(R.string.start_stage_message))
                    .setPositiveButton(context.getString(R.string.start_stage_confirm)) { _, _ ->
                        onStartClicked(stage)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            binding.btnCompleteStage.setOnClickListener {
                val context = binding.root.context
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.complete_stage_title))
                    .setMessage(context.getString(R.string.complete_stage_message))
                    .setPositiveButton(context.getString(R.string.complete_stage_confirm)) { _, _ ->
                        onCompleteClicked(stage)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        fun bindCompositePayload(stage: Stage, payload: CompositeStagePayload) {
            payload.durationChanged?.let { bindDurationChanged(stage, it.newDuration) }
            payload.activeChanged?.let { bindActiveChanged(stage, it.isActive, it.weight) }
            payload.weightChanged?.let { bindWeightChanged(stage, it.weight) }
            payload.timeLeftUpdated?.let { bindTimeLeftUpdated(stage, it.timeLeft) }
            payload.stageCompleted?.let { bindStageCompleted(stage, it.isCompleted) }
            payload.stageStarted?.let { bindStageStarted(stage, it.isStarted) }
        }

        fun bindDurationChanged(stage: Stage, newDuration: Long) {
            binding.tvDuration.text = binding.root.context.getString(R.string.duration_format, newDuration.toDouble())
            binding.etDuration.setText(newDuration.toString())
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, computeTimeLeftText(stage.copy(durationHours = newDuration)))
        }

        fun bindActiveChanged(stage: Stage, isActive: Boolean, weight: Double?) {
            binding.tvCurrentWeight.text = if (isActive) {
                weight?.let { binding.root.context.getString(R.string.weight_format, it) } ?: binding.root.context.getString(R.string.weight_na)
            } else {
                binding.root.context.getString(R.string.na)
            }

            val anyRunning = activeStageId != null
            binding.btnStartStage.isVisible = !anyRunning && stage.startTime == null
            binding.btnCompleteStage.isVisible = isActive && stage.endTime == null
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, computeTimeLeftText(stage))
        }

        fun bindWeightChanged(stage: Stage, weight: Double?) {
            binding.tvCurrentWeight.text = weight?.let {
                binding.root.context.getString(R.string.weight_format, it)
            } ?: binding.root.context.getString(R.string.weight_na)
        }

        fun bindTimeLeftUpdated(stage: Stage, timeLeft: String) {
            binding.tvTimeLeft.text = binding.root.context.getString(R.string.time_left_format_text, timeLeft)
        }

        fun bindStageCompleted(stage: Stage, isCompleted: Boolean) {
            binding.btnCompleteStage.isVisible = !isCompleted && activeStageId == stage.id
            binding.tvEndTime.text = if (isCompleted) {
                binding.root.context.getString(R.string.end_format, formatDate(System.currentTimeMillis()))
            } else {
                binding.root.context.getString(R.string.end_ongoing)
            }
        }

        fun bindStageStarted(stage: Stage, isStarted: Boolean) {
            binding.btnStartStage.isVisible = !isStarted && activeStageId == null
            binding.tvStartTime.text = if (isStarted) {
                binding.root.context.getString(R.string.start_format, formatDate(System.currentTimeMillis()))
            } else {
                binding.root.context.getString(R.string.start_not_started)
            }
        }

        fun clearListeners() {
            binding.etDuration.setOnFocusChangeListener(null)
            binding.btnStartStage.setOnClickListener(null)
            binding.btnCompleteStage.setOnClickListener(null)
        }

        private fun formatDate(timestamp: Long): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

        private fun computeTimeLeftText(stage: Stage): String {
            if (stage.endTime != null) return "Completed"
            val now = System.currentTimeMillis()
            val expectedEnd = when {
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
            // Собираем изменения как переменные (не val в data class)
            var durationChanged: StagePayload.DurationChanged? = null
            var stageStarted: StagePayload.StageStarted? = null
            var stageCompleted: StagePayload.StageCompleted? = null

            if (oldItem.durationHours != newItem.durationHours) {
                durationChanged = StagePayload.DurationChanged(newItem.durationHours)
            }
            if (oldItem.startTime != newItem.startTime) {
                stageStarted = StagePayload.StageStarted(newItem.startTime != null)
            }
            if (oldItem.endTime != newItem.endTime) {
                stageCompleted = StagePayload.StageCompleted(newItem.endTime != null)
            }

            // Создаём объект в конце
            val composite = CompositeStagePayload(
                durationChanged = durationChanged,
                stageStarted = stageStarted,
                stageCompleted = stageCompleted
            )

            // Возвращаем null, если изменений нет
            return composite.takeIf { it != CompositeStagePayload() }
        }
    }

    fun clearAllListeners() {
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                (rv.getChildViewHolder(rv.getChildAt(i)) as? VH)?.clearListeners()
            }
        }
    }
}