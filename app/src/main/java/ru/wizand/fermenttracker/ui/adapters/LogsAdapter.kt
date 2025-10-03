// src/main/java/ru/wizand/fermenttracker/ui/adapters/LogsAdapter.kt
package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.databinding.ItemLogBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import ru.wizand.fermenttracker.R

class LogsAdapter : ListAdapter<BatchLog, LogsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemLogBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: BatchLog) {
            b.tvTimestamp.text = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date(log.timestamp))

            b.tvWeight.text = log.weightGr?.toString() ?: "N/A"

            log.photoPath?.let { path ->
                b.ivPhoto.load(File(path)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_error)
                    size(640, 480) // Изменение размера для экономии памяти
                    transformations(RoundedCornersTransformation(8f))
                }
            } ?: run {
                b.ivPhoto.setImageDrawable(null)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BatchLog>() {
            override fun areItemsTheSame(oldItem: BatchLog, newItem: BatchLog) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BatchLog, newItem: BatchLog) =
                oldItem == newItem
        }
    }
}