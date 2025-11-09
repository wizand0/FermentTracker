package ru.wizand.fermenttracker.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.models.StageWithBatch
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter : ListAdapter<StageWithBatch, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNotificationTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        private val tvNotificationText: TextView = itemView.findViewById(R.id.tvNotificationText)
        private val tvNotificationTime: TextView = itemView.findViewById(R.id.tvNotificationTime)

        fun bind(stageWithBatch: StageWithBatch) {
            tvNotificationTitle.text = "${stageWithBatch.batchName} - ${stageWithBatch.stageName}"

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvNotificationText.text = itemView.context.getString(R.string.stage_completed_on, dateFormat.format(Date(stageWithBatch.endTime)))

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvNotificationTime.text = timeFormat.format(Date(stageWithBatch.endTime))
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<StageWithBatch>() {
    override fun areItemsTheSame(oldItem: StageWithBatch, newItem: StageWithBatch): Boolean {
        // Поскольку endTime может не быть уникальным, сравниваем по равенству объекта
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: StageWithBatch, newItem: StageWithBatch): Boolean {
        return oldItem == newItem
    }
}