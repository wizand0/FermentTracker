package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding

class StageAdapter : ListAdapter<Stage, StageAdapter.StageViewHolder>(StageDiffCallback()) {

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
            binding.tvDuration.text = "${stage.durationHours} h"
            binding.tvCurrentWeight.text = stage.currentWeightGr?.toString() ?: "N/A"
            binding.tvStartTime.text = stage.startTime?.let { "Start: ${formatDate(it)}" } ?: "Not started"
            binding.tvEndTime.text = stage.endTime?.let { "End: ${formatDate(it)}" } ?: "Ongoing"
            binding.tvTimeLeft.text = "Time Left: ${calculateTimeLeft(stage)}"
        }

        private fun formatDate(timestamp: Long): String {
            return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        }

        private fun calculateTimeLeft(stage: Stage): String {
            val start = stage.startTime ?: return "N/A"
            val endPlanned = start + java.util.concurrent.TimeUnit.HOURS.toMillis(stage.durationHours)
            val leftMs = endPlanned - System.currentTimeMillis()
            return if (leftMs > 0) {
                val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(leftMs)
                "$hours h"
            } else "Overdue"
        }
    }

    class StageDiffCallback : DiffUtil.ItemCallback<Stage>() {
        override fun areItemsTheSame(oldItem: Stage, newItem: Stage): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Stage, newItem: Stage): Boolean = oldItem == newItem
    }
}