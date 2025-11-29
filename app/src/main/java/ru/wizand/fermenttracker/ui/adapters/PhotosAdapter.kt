package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.databinding.ItemPhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotosAdapter : ListAdapter<Photo, PhotosAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val b: ItemPhotoBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(photo: Photo) {
            // Оптимизация: используем глобальный Coil через extension-функцию load.
            // Coil сам определит размер View и подгонит картинку, экономя память.
            b.ivPhoto.load(File(photo.filePath)) {
                crossfade(true)                         // Плавное появление
                placeholder(R.drawable.ic_placeholder)  // Плейсхолдер
                error(R.drawable.ic_error)              // Ошибка
                // allowHardware(false) - можно добавить, если битмапы слишком большие и вызывают краши,
                // но обычно Coil справляется сам с downsampling.
                transformations(RoundedCornersTransformation(8f))
            }

            // Используем статический форматтер
            b.tvTimestamp.text = DATE_FORMATTER.format(Date(photo.timestamp))

            b.ivPhoto.contentDescription = "Photo at ${b.tvTimestamp.text}"
        }
    }

    companion object {
        // Вынесли форматтер, чтобы не создавать его на каждый bind
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        private val DIFF = object : DiffUtil.ItemCallback<Photo>() {
            override fun areItemsTheSame(oldItem: Photo, newItem: Photo) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Photo, newItem: Photo) =
                oldItem == newItem
        }
    }
}