package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.databinding.ItemPhotoBinding
import ru.wizand.fermenttracker.utils.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            // Используем оптимизированную загрузку с thumbnails 640x480
            val bitmap = ImageUtils.decodeSampledBitmapFromFile(photo.filePath, 640, 480)
            b.ivPhoto.setImageBitmap(bitmap)

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