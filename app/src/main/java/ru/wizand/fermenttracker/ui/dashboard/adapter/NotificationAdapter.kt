package ru.wizand.fermenttracker.ui.dashboard.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Stage
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter : ListAdapter<Stage, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

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

        fun bind(stage: Stage) {
            tvNotificationTitle.text = stage.name

            // Проверяем, что endTime не null
            stage.endTime?.let { endTime ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvNotificationText.text = itemView.context.getString(R.string.stage_completed_on, dateFormat.format(Date(endTime)))

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvNotificationTime.text = timeFormat.format(Date(endTime))
            } ?: run {
                // Если endTime null, устанавливаем значения по умолчанию
                tvNotificationText.text = itemView.context.getString(R.string.stage_not_completed)
                tvNotificationTime.text = ""
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<Stage>() {
    override fun areItemsTheSame(oldItem: Stage, newItem: Stage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Stage, newItem: Stage): Boolean {
        return oldItem == newItem
    }
}