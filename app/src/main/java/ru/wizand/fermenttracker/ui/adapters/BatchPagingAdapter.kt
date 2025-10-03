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

        fun bind(batch: Batch) {
            binding.tvBatchName.text = batch.name
            binding.tvProductType.text = batch.type // Используем type вместо productType
            binding.currentStageName.text = batch.currentStage
            binding.tvBatchStartDate.text = binding.root.context.getString(
                R.string.start_date_format,
                dateFormat.format(Date(batch.startDate))
            )

            // Расчет прогресса (пример)
            // В вашей сущности нет полей currentStageOrder и totalStages,
            // поэтому упростим расчет прогресса
            val progress = if (batch.isActive) 50 else 100
            binding.tvProgress.text = "$progress%"

            // Обработка нажатия на элемент
            binding.root.setOnClickListener { onClick(batch) }

            // Обработка нажатия на кнопку "Ещё"
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