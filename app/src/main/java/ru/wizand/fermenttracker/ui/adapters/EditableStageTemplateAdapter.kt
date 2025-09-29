package ru.wizand.fermenttracker.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.databinding.ItemEditableStageBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EditableStageTemplateAdapter(
    private val onRemoveStage: (position: Int) -> Unit
) : ListAdapter<StageTemplate, EditableStageTemplateAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEditableStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position), position)

    inner class VH(private val b: ItemEditableStageBinding) : RecyclerView.ViewHolder(b.root) {
        private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())


        fun bind(template: StageTemplate, pos: Int) {
            // Для имени
            (b.etStageName.tag as? TextWatcher)?.let { b.etStageName.removeTextChangedListener(it) }
            b.etStageName.setText(template.name)
            val nameWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newName = s.toString()
                    if (newName != template.name && pos < currentList.size) {
                        val updated = template.copy(name = newName)
                        val mutableList = currentList.toMutableList()
                        if (pos < mutableList.size) {
                            mutableList[pos] = updated
                            submitList(mutableList)
                        }
                    }
                }
            }
            b.etStageName.addTextChangedListener(nameWatcher)
            b.etStageName.tag = nameWatcher

            // Для duration
            (b.etDuration.tag as? TextWatcher)?.let { b.etDuration.removeTextChangedListener(it) }
            b.etDuration.setText(template.durationHours.toString())
            val durationWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newDuration = s.toString().toLongOrNull() ?: 0L
                    if (newDuration != template.durationHours && pos < currentList.size) {
                        val updated = template.copy(durationHours = newDuration)
                        val mutableList = currentList.toMutableList()
                        if (pos < mutableList.size) {
                            mutableList[pos] = updated
                            submitList(mutableList)
                        }
                    }
                }
            }
            b.etDuration.addTextChangedListener(durationWatcher)
            b.etDuration.tag = durationWatcher

            b.btnRemoveStage.setOnClickListener {
                if (pos < currentList.size) {
                    onRemoveStage(pos)
                }
            }
        }

    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StageTemplate>() {
            override fun areItemsTheSame(old: StageTemplate, new: StageTemplate) = old.id == new.id
            override fun areContentsTheSame(old: StageTemplate, new: StageTemplate) = old == new
        }
    }
}