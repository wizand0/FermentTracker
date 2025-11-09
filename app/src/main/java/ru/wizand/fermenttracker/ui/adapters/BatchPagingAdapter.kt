package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.databinding.ItemBatchBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class BatchPagingAdapter(
    private val onItemClick: (Batch) -> Unit,
    private val onDeleteClick: (Batch) -> Unit,
    private val onEditClick: (Batch) -> Unit = {} // Добавляем обработчик редактирования
) : PagingDataAdapter<Batch, BatchPagingAdapter.VH>(DIFF_CALLBACK) {

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBatchBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding, onItemClick, onDeleteClick, onEditClick)
    }

    class VH(
        private val binding: ItemBatchBinding,
        private val onClick: (Batch) -> Unit,
        private val onDelete: (Batch) -> Unit,
        private val onEdit: (Batch) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        // Приватная функция для расчета прогресса партии.
        // Логика идентична BatchAdapter.computeProgress, но адаптирована для использования в VH.
        // Для типов продуктов, основанных на весе ("Dry-cured meat", "Dry-cured meat (basic)", "Dry-cured meat (spicy)", "Dry-cured sausage"),
        // рассчитывается процент потери веса из начального и текущего веса.
        // Для остальных типов рассчитывается временной прогресс на основе запланированной даты завершения и текущего времени.
        // Если данных недостаточно, возвращается "N/A".
        private fun computeProgress(batch: Batch): String {
            val now = System.currentTimeMillis()
            // Список типов продуктов, для которых прогресс рассчитывается на основе потери веса
            val weightBasedTypes = listOf("Dry-cured meat", "Dry-cured meat (basic)", "Dry-cured meat (spicy)", "Dry-cured sausage")

            // Если тип продукта основан на весе, и есть начальный и текущий вес, и начальный вес > 0
            if (batch.type in weightBasedTypes && batch.initialWeightGr != null && batch.currentWeightGr != null && batch.initialWeightGr > 0) {
                // Расчет процента потери веса
                val lossPercent = ((batch.initialWeightGr - batch.currentWeightGr) / batch.initialWeightGr * 100)
                // Форматируем до 1 знака после запятой и добавляем "% loss"
                return "%.1f%% loss".format(lossPercent)
            }
            // Если тип не основан на весе, но есть запланированная дата завершения, сравниваем с текущим временем
            else if (batch.plannedCompletionDate != null && batch.plannedCompletionDate > batch.startDate) {
                // Общее время с начала до планируемого завершения
                val totalMs = batch.plannedCompletionDate - batch.startDate
                // Прошедшее время с начала
                val passedMs = now - batch.startDate
                // Процент прогресса (не более 100%, не менее 0%)
                val percent = min(100, max(0, (passedMs.toDouble() / totalMs * 100).toInt()))
                // Возвращаем процент как строку
                return "$percent%"
            }
            // Если данных недостаточно, возвращаем "N/A"
            return "N/A"
        }

        fun bind(batch: Batch) {
            // Устанавливаем название партии
            binding.tvBatchName.text = batch.name
            // Устанавливаем тип продукта
            binding.tvProductType.text = batch.type // Используем type вместо productType
            // Устанавливаем текущий этап
            binding.currentStageName.text = batch.currentStage
            // Устанавливаем дату начала в формате строки из ресурсов
            binding.tvBatchStartDate.text = binding.root.context.getString(
                R.string.start_date_format,
                dateFormat.format(Date(batch.startDate))
            )

            // Расчет прогресса с помощью приватного метода вместо статического значения
            // Сначала определяем динамический прогресс на основе логики потери веса или временного прогресса
            val progress = computeProgress(batch)
            // Устанавливаем расчетный текст прогресса в UI
            binding.tvProgress.text = progress

            // Обработка нажатия на элемент списка - вызывает onItemClick с передачей batch
            binding.root.setOnClickListener { onClick(batch) }

            // Обработка нажатия на кнопку "Ещё" - показывает всплывающее меню с опциями редактирования и удаления
            binding.btnMore.setOnClickListener { view ->
                showPopupMenu(view, batch)
            }
        }

        private fun showPopupMenu(view: View, batch: Batch) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_batch_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEdit(batch)
                        true
                    }
                    R.id.action_delete -> {
                        onDelete(batch)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Batch>() {
            override fun areItemsTheSame(old: Batch, new: Batch) = old.id == new.id
            override fun areContentsTheSame(old: Batch, new: Batch) = old == new
        }
    }
}
