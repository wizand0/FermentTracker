package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.databinding.ItemBatchBinding

class BatchAdapter(private val onClick: (Batch) -> Unit) : ListAdapter<Batch, BatchAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class VH(private val b: ItemBatchBinding) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onClick(getItem(pos))
            }
        }
        fun bind(item: Batch) {
            b.tvName.text = item.name
            b.tvType.text = item.productType
            // пример: рассчитываем прогресс потери веса по сохранённому стартовому весу и последнему этапу не реализован — показываем 0
            val progress = 0
            b.progressLoss.progress = progress
            b.tvProgressPercent.text = "${progress}%"
            // content description for accessibility
            b.ivProductIcon.contentDescription = item.productType
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Batch>() {
            override fun areItemsTheSame(oldItem: Batch, newItem: Batch) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Batch, newItem: Batch) = oldItem == newItem
        }
    }
}