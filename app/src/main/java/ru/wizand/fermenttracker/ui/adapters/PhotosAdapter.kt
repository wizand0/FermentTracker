package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.databinding.ItemPhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import ru.wizand.fermenttracker.R

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
            // Расчитываем размер на основе ширины ImageView (после inflate она известна)
            val containerWidth = b.ivPhoto.width.takeIf { it > 0 } ?: 200 // Запасной размер, если ещё не измерено
            val containerHeight = (containerWidth * 0.75f).toInt() // Пропорции 4:3, например; настройте под нужды

            // Настраиваем ImageLoader для лучшей памяти и кэширования
            val imageLoader = ImageLoader.Builder(b.root.context)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)  // Явное включение кэша памяти
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)    // Явное включение кэша диска
                .build()

            // Прогрессивная загрузка с Coil: плейсхолдер, crossfade, ограничения на размер
            val request = ImageRequest.Builder(b.root.context)
                .data(File(photo.filePath))
                .target(b.ivPhoto)
                .size(containerWidth, containerHeight)  // Размер на основе контейнера для экономии памяти
                .crossfade(true)  // Плавное появление (прогрессивная загрузка)
                .placeholder(R.drawable.ic_placeholder)  // Плейсхолдер во время загрузки
                .error(R.drawable.ic_error)  // Изображение при ошибке
                .allowHardware(false)  // Отключаем аппаратное ускорение для больших изображений (экономит память)
                .transformations(RoundedCornersTransformation(8f))  // Трансформация для округления
                .build()

            imageLoader.enqueue(request)

            b.tvTimestamp.text = SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.getDefault()
            ).format(Date(photo.timestamp))

            b.ivPhoto.contentDescription = "Photo at ${b.tvTimestamp.text}"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Photo>() {
            override fun areItemsTheSame(oldItem: Photo, newItem: Photo) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Photo, newItem: Photo) =
                oldItem == newItem
        }
    }
}