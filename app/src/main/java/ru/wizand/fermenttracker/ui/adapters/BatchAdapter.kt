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

class BatchAdapter(private val onItemClick: (Batch) -> Unit) :
    ListAdapter<Batch, BatchAdapter.BatchViewHolder>(BatchDiffCallback()) {

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

            binding.root.setOnClickListener { onItemClick(batch) }
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
    }
}