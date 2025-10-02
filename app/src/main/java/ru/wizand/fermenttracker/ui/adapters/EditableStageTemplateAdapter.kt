package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.databinding.ItemEditableStageBinding

/**
 * Sealed class для payloads, используемых при частичном обновлении элементов списка.
 */
sealed class StageTemplatePayload {
    data class NameChanged(val newName: String) : StageTemplatePayload()
    data class DurationChanged(val newDuration: Long) : StageTemplatePayload()
}

class EditableStageTemplateAdapter(
    private val onRemoveStage: (position: Int) -> Unit
) : ListAdapter<StageTemplate, EditableStageTemplateAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEditableStageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), position)
    }

    /**
     * Обрабатывает частичные обновления элементов списка через payloads
     */
    override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            val item = getItem(position)
            payloads.forEach { payload ->
                when (payload) {
                    is StageTemplatePayload.NameChanged -> holder.bindNameChanged(item, payload.newName)
                    is StageTemplatePayload.DurationChanged -> holder.bindDurationChanged(item, payload.newDuration)
                }
            }
        }
    }

    inner class VH(private val binding: ItemEditableStageBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Устанавливаем слушатели один раз в init блоке, а не при каждом bind()
            binding.etStageName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Когда поле теряет фокус, обрабатываем изменение имени
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val template = currentList[adapterPosition]
                        val newName = binding.etStageName.text.toString()

                        if (newName != template.name) {
                            // Обновляем модель
                            val updatedTemplate = template.copy(name = newName)
                            val newList = currentList.toMutableList()
                            newList[adapterPosition] = updatedTemplate
                            submitList(newList)
                        }
                    }
                }
            }

            binding.etDuration.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Когда поле теряет фокус, обрабатываем изменение длительности
                    val adapterPosition = bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val template = currentList[adapterPosition]
                        val newDuration = binding.etDuration.text.toString().toLongOrNull() ?: 0L

                        if (newDuration != template.durationHours) {
                            // Обновляем модель
                            val updatedTemplate = template.copy(durationHours = newDuration)
                            val newList = currentList.toMutableList()
                            newList[adapterPosition] = updatedTemplate
                            submitList(newList)
                        }
                    }
                }
            }
        }

        fun bind(template: StageTemplate, pos: Int) {
            binding.etStageName.setText(template.name)
            binding.etDuration.setText(template.durationHours.toString())

            // Кнопка удаления стадии
            binding.btnRemoveStage.setOnClickListener {
                if (pos < currentList.size) {
                    onRemoveStage(pos)
                }
            }
        }

        /**
         * Обрабатывает изменение имени через payload
         */
        fun bindNameChanged(template: StageTemplate, newName: String) {
            binding.etStageName.setText(newName)
        }

        /**
         * Обрабатывает изменение длительности через payload
         */
        fun bindDurationChanged(template: StageTemplate, newDuration: Long) {
            binding.etDuration.setText(newDuration.toString())
        }

        /**
         * Очищает слушатели для предотвращения утечек памяти
         */
        fun clearListeners() {
            binding.etStageName.setOnFocusChangeListener(null)
            binding.etDuration.setOnFocusChangeListener(null)
            binding.btnRemoveStage.setOnClickListener(null)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StageTemplate>() {
            override fun areItemsTheSame(old: StageTemplate, new: StageTemplate) = old.id == new.id
            override fun areContentsTheSame(old: StageTemplate, new: StageTemplate) = old == new
        }
    }
}