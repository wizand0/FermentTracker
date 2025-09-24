package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class StageAdapter(
    private val onAction: (stageId: String, action: String) -> Unit,
    private var photosByStage: Map<String, List<Photo>> = emptyMap()
) : ListAdapter<Stage, StageAdapter.VH>(DIFF) {

    fun updatePhotos(photos: Map<String, List<Photo>>) {
        photosByStage = photos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemStageBinding) : RecyclerView.ViewHolder(b.root) {
        private val photosAdapter = PhotosAdapter()

        init {
            b.btnAddWeight.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onAction(getItem(pos).id, "add_weight")
            }
            b.btnAddPhoto.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onAction(getItem(pos).id, "add_photo")
            }
            b.btnNextStage.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onAction(getItem(pos).id, "next")
            }
            b.rvPhotos.adapter = photosAdapter
            b.rvPhotos.layoutManager = LinearLayoutManager(b.root.context, LinearLayoutManager.HORIZONTAL, false)
        }

        fun bind(stage: Stage) {
            b.tvStageName.text = stage.name
            b.tvDuration.text = "${stage.durationHours} h"
            b.tvCurrentWeight.text = stage.currentWeightGr?.let { "${(it/1000.0)} kg" } ?: "-"
            b.tvStartTime.text = stage.startTime?.let { formatDate(it) } ?: "Not started"
            b.tvEndTime.text = stage.endTime?.let { formatDate(it) } ?: "Ongoing"
            val timeLeftMs = if (stage.startTime != null && stage.endTime == null) {
                (stage.durationHours * 3600000) - (System.currentTimeMillis() - stage.startTime)
            } else 0L
            b.tvTimeLeft.text = if (timeLeftMs > 0) formatTimeLeft(timeLeftMs) else if (stage.endTime != null) "Completed" else "-"
            b.root.contentDescription = "${stage.name}, ${b.tvDuration.text}"

            photosAdapter.submitList(photosByStage[stage.id] ?: emptyList())
        }
    }

    private fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

    private fun formatTimeLeft(ms: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        return "$days d, $hours h"
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Stage>() {
            override fun areItemsTheSame(oldItem: Stage, newItem: Stage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Stage, newItem: Stage) = oldItem == newItem
        }
    }
}