package ru.wizand.fermenttracker.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.databinding.ItemPhotoBinding  // Assume layout with ImageView id=ivPhoto, TextView tvTimestamp
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
            // Load image (simple; use Glide for async/thumbnail)
            val bitmap = BitmapFactory.decodeFile(photo.filePath)
            b.ivPhoto.setImageBitmap(bitmap)
            b.tvTimestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(
                Date(photo.timestamp)
            )
            b.ivPhoto.contentDescription = "Photo at ${b.tvTimestamp.text}"
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Photo>() {
            override fun areItemsTheSame(oldItem: Photo, newItem: Photo) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Photo, newItem: Photo) = oldItem == newItem
        }
    }
}