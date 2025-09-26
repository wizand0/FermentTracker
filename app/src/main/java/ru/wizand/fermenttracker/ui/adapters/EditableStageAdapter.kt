package ru.wizand.fermenttracker.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.databinding.ItemStageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EditableStageAdapter(
    private val onAddStage: () -> Unit,
    private val onRemoveStage: (position: Int) -> Unit
) : ListAdapter<Stage, EditableStageAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position), position)

    inner class VH(private val b: ItemStageBinding) : RecyclerView.ViewHolder(b.root) {
        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(stage: Stage, pos: Int) {
            b.tvStageName.text = stage.name
            b.tvPlannedStartTime.text = stage.plannedStartTime?.let { "Planned Start: ${sdf.format(it)}" } ?: "Planned Start: N/A"
            b.tvPlannedEndTime.text = stage.plannedEndTime?.let { "Planned End: ${sdf.format(it)}" } ?: "Planned End: N/A"

            // Added: remove old watcher before setText to prevent triggers during binding
            (b.etDuration.tag as? TextWatcher)?.let {
                b.etDuration.removeTextChangedListener(it)
            }

            b.etDuration.setText(stage.durationHours.toString())

            // Added: create and add new watcher after setText, with change check
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newDuration = s.toString().toLongOrNull() ?: 0L
                    if (newDuration != stage.durationHours) { // Added: only submit if actually changed
                        val updatedStage = stage.copy(durationHours = newDuration)
                        submitList(currentList.toMutableList().apply { set(pos, updatedStage) })
                    }
                }
            }
            b.etDuration.addTextChangedListener(watcher)
            b.etDuration.tag = watcher // Store for future removal

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