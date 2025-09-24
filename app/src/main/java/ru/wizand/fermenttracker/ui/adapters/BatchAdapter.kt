package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.databinding.ItemBatchBinding

class BatchAdapter(
    private val onClick: (Batch) -> Unit
) : ListAdapter<Batch, BatchAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemBatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemBatchBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(batch: Batch) {
            b.tvName.text = batch.name
            b.tvType.text = batch.productType
            b.progressLoss.progress = 50 // Replace with actual progress logic
            b.tvProgressPercent.text = "50%"
            b.root.setOnClickListener { onClick(batch) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Batch>() {
            override fun areItemsTheSame(oldItem: Batch, newItem: Batch) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Batch, newItem: Batch) = oldItem == newItem
        }
    }

    // Updated to only set the swipe listener without returning ItemTouchHelper.Callback
    fun setOnItemSwipeListener(onSwipe: (Batch) -> Unit) {
        // This function now only stores the swipe action; ItemTouchHelper setup moves to Fragment
    }
}