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
import ru.wizand.fermenttracker.utils.WeightConverter  // Добавленный импорт для поддержки конвертации веса
import ru.wizand.fermenttracker.utils.WeightUnit      // Добавленный импорт для перечислений единиц измерения
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import ru.wizand.fermenttracker.R

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
            b.tvTimestamp.text = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date(log.timestamp))

            // Изменение: Отображение веса с конвертацией в выбранные единицы
            b.tvWeight.text = log.weightGr?.let {
                val unit = WeightConverter.getCurrentUnit(b.root.context)  // Получаем текущую единицу
                WeightConverter.formatWeight(it, unit)  // Форматируем вес
            } ?: "N/A"

            log.photoPath?.let { path ->
                b.ivPhoto.load(File(path)) {
                    crossfade(true)
                    placeholder(R.drawable.ic_placeholder)
                    error(R.drawable.ic_error)
                    size(640, 480) // Изменение размера для экономии памяти
                    transformations(RoundedCornersTransformation(8f))
                }
                b.ivPhoto.setOnClickListener { onPhotoClicked?.invoke(path) } // Обработчик клика
            } ?: run {
                b.ivPhoto.setImageDrawable(null)
                b.ivPhoto.setOnClickListener(null) // Убрать клик, если фото нет
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