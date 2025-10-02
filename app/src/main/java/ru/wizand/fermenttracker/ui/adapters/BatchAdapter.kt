package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.databinding.ItemBatchBinding
import ru.wizand.fermenttracker.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * Sealed class для payloads, используемых при частичном обновлении элементов списка.
 */
sealed class BatchPayload {
    data class WeightChanged(val newWeight: Double?) : BatchPayload()
    data class StageChanged(val newStage: String) : BatchPayload()
    data class ProgressChanged(val newProgress: String) : BatchPayload()
}

class BatchAdapter(private val onItemClick: (Batch) -> Unit) :
    ListAdapter<Batch, BatchAdapter.BatchViewHolder>(BatchDiffCallback()) {

    private val weightBasedTypes = listOf("Dry-cured meat", "Dry-cured sausage") // Added: hardcoded weight-based types

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatchViewHolder {
        val binding = ItemBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int) {
        val batch = getItem(position)
        holder.bind(batch)
    }

    override fun onBindViewHolder(holder: BatchViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val batch = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    is BatchPayload.WeightChanged -> holder.updateWeight(batch, payload.newWeight)
                    is BatchPayload.StageChanged -> holder.updateStage(batch, payload.newStage)
                    is BatchPayload.ProgressChanged -> holder.updateProgress(batch, payload.newProgress)
                }
            }
        }
    }

    /**
     * Обновляет вес конкретной партии
     */
    fun updateBatchWeight(batchId: String, weight: Double?) {
        val position = currentList.indexOfFirst { it.id == batchId }
        if (position != -1) {
            // Обновляем данные в списке
            val updatedBatch = currentList[position].copy(currentWeightGr = weight)
            (currentList as MutableList)[position] = updatedBatch

            // Уведомляем об изменении через payload
            val progress = computeProgress(updatedBatch)
            notifyItemChanged(position, BatchPayload.WeightChanged(weight))
            notifyItemChanged(position, BatchPayload.ProgressChanged(progress))
        }
    }

    /**
     * Обновляет текущий этап конкретной партии
     */
    fun updateBatchStage(batchId: String, stage: String?) {
        val position = currentList.indexOfFirst { it.id == batchId }
        if (position != -1) {
            val updatedBatch = currentList[position].copy(currentStage = stage)
            (currentList as MutableList)[position] = updatedBatch

            // передаём строку, даже если null
            notifyItemChanged(position, BatchPayload.StageChanged(stage ?: "N/A"))
        }
    }

    inner class BatchViewHolder(private val binding: ItemBatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(batch: Batch) {
            binding.tvBatchName.text = batch.name
            binding.tvProductType.text = batch.type
            binding.currentStageName.text = batch.currentStage   // <-- без ?: "N/A"
            binding.tvBatchStartDate.text = formatDate(batch.startDate)
            binding.tvProgress.text = computeProgress(batch)
            binding.root.setOnClickListener { onItemClick(batch) }
        }

        fun updateWeight(batch: Batch, weight: Double?) {
            // Обновляем прогресс, так как он зависит от веса
            val updatedBatch = batch.copy(currentWeightGr = weight)
            binding.tvProgress.text = computeProgress(updatedBatch)
        }

        fun updateStage(batch: Batch, stage: String?) {
            binding.currentStageName.text = stage ?: "N/A"
        }

        fun updateProgress(batch: Batch, progress: String) {
            binding.tvProgress.text = progress
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class BatchDiffCallback : DiffUtil.ItemCallback<Batch>() {
        override fun areItemsTheSame(oldItem: Batch, newItem: Batch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Batch, newItem: Batch): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Batch, newItem: Batch): Any? {
            return when {
                oldItem.currentWeightGr != newItem.currentWeightGr ->
                    BatchPayload.WeightChanged(newItem.currentWeightGr)
                oldItem.currentStage != newItem.currentStage ->
                    BatchPayload.StageChanged(newItem.currentStage ?: "N/A")   // <-- без ?: "N/A"
                else -> null
            }
        }
    }

    private fun computeProgress(batch: Batch): String {
        val now = System.currentTimeMillis()
        if (batch.type in weightBasedTypes && batch.initialWeightGr != null && batch.currentWeightGr != null && batch.initialWeightGr > 0) {
            val lossPercent = ((batch.initialWeightGr - batch.currentWeightGr) / batch.initialWeightGr * 100) // Updated: keep as Double for precision
            return "%.2f%% loss".format(lossPercent) // Updated: format to 2 decimal places, like in Python
        } else if (batch.plannedCompletionDate != null && batch.plannedCompletionDate > batch.startDate) {
            val totalMs = batch.plannedCompletionDate - batch.startDate
            val passedMs = now - batch.startDate
            val percent = min(100, max(0, (passedMs.toDouble() / totalMs * 100).toInt()))
            return "$percent%"
        }
        return "N/A"
    }
}