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

    inner class BatchViewHolder(private val binding: ItemBatchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(batch: Batch) {
            binding.tvBatchName.text = batch.name // Changed: use binding directly
            binding.tvProductType.text = batch.type // Assuming binding has tvProductType
            binding.currentStageName.text = batch.currentStage // Assuming binding has currentStageName
            binding.tvBatchStartDate.text = formatDate(batch.startDate) // Assuming binding has tvBatchStartDate

            // Added: compute and set progress
            binding.tvProgress.text = computeProgress(batch)

            binding.root.setOnClickListener { onItemClick(batch) }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
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

    class BatchDiffCallback : DiffUtil.ItemCallback<Batch>() {
        override fun areItemsTheSame(oldItem: Batch, newItem: Batch): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Batch, newItem: Batch): Boolean {
            return oldItem == newItem
        }
    }
}