// src/main/java/ru/wizand/fermenttracker/ui/adapters/PhotosAdapter.kt
package ru.wizand.fermenttracker.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
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
            b.ivPhoto.load(File(photo.filePath)) {
                crossfade(true)
                placeholder(R.drawable.ic_placeholder)
                error(R.drawable.ic_error)
                size(640, 480) // Изменение размера для экономии памяти
                transformations(RoundedCornersTransformation(8f))
            }

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