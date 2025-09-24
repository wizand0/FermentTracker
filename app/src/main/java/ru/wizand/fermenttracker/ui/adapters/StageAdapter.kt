package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding

class StageAdapter(private val onAction: (stageId: String, action: String) -> Unit) :
    ListAdapter<Stage, StageAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemStageBinding) : RecyclerView.ViewHolder(b.root) {
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
        }
        fun bind(stage: Stage) {
            b.tvStageName.text = stage.name
            b.tvDuration.text = "${stage.durationHours} h"
            b.tvCurrentWeight.text = stage.currentWeightGr?.let { "${(it/1000.0)} kg" } ?: "-"
            // accessibility
            b.root.contentDescription = "${stage.name}, ${b.tvDuration.text}"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Stage>() {
            override fun areItemsTheSame(oldItem: Stage, newItem: Stage) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Stage, newItem: Stage) = oldItem == newItem
        }
    }
}