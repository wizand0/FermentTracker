package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.databinding.ItemLogBinding
import ru.wizand.fermenttracker.utils.WeightConverter
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogsAdapter(
    private val onPhotoClicked: ((String) -> Unit)? = null
) : ListAdapter<BatchLog, LogsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onPhotoClicked)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(
        private val b: ItemLogBinding,
        private val onPhotoClicked: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(log: BatchLog) {
            // Используем статический форматтер
            b.tvTimestamp.text = DATE_FORMATTER.format(Date(log.timestamp))

            // Отображение веса с конвертацией
            b.tvWeight.text = log.weightGr?.let {
                val unit = WeightConverter.getCurrentUnit(b.root.context)
                WeightConverter.formatWeight(it, unit)
            } ?: "N/A"

            log.photoPath?.let { path ->
                b.ivPhoto.load(File(path)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_error)
                    // size(640, 480) - Coil сам определит размер view в списке,
                    // но если view слишком маленькое, а грузится 4k фото, можно оставить явный size
                    // для уменьшения потребления памяти при декодировании.
                    size(300, 300) // Пример оптимизации для превью в списке логов
                    transformations(RoundedCornersTransformation(8f))
                }
                b.ivPhoto.setOnClickListener { onPhotoClicked?.invoke(path) }
            } ?: run {
                // Обязательно очищаем view при переиспользовании, если фото нет
                b.ivPhoto.setImageDrawable(null)
                b.ivPhoto.setOnClickListener(null)
            }
        }
    }

    companion object {
        // Вынесли форматтер в companion object
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        private val DIFF = object : DiffUtil.ItemCallback<BatchLog>() {
            override fun areItemsTheSame(oldItem: BatchLog, newItem: BatchLog) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: BatchLog, newItem: BatchLog) =
                oldItem == newItem
        }
    }
}