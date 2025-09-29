package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EditableStageAdapter(
    private val onRemoveStage: (position: Int) -> Unit
) : ListAdapter<Stage, EditableStageAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position), position)

    inner class VH(private val b: ItemStageBinding) : RecyclerView.ViewHolder(b.root) {
        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(stage: Stage, pos: Int) {
            b.tvStageName.text = stage.name
            b.tvDuration.text = "${stage.durationHours} h"

            when (stage.status) {
                "Not started" -> {
                    b.btnStartStage.visibility = View.VISIBLE
                    b.btnCompleteStage.visibility = View.GONE
                }
                "Ongoing" -> {
                    b.btnStartStage.visibility = View.GONE
                    b.btnCompleteStage.visibility = View.VISIBLE
                }
                "Completed" -> {
                    b.btnStartStage.visibility = View.GONE
                    b.btnCompleteStage.visibility = View.GONE
                }
            }

            b.btnRemoveStage.setOnClickListener { onRemoveStage(pos) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Stage>() {
            override fun areItemsTheSame(old: Stage, new: Stage) = old.id == new.id
            override fun areContentsTheSame(old: Stage, new: Stage) = old == new
        }
    }
}
