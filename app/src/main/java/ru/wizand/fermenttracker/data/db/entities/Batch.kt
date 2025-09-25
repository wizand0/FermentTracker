package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey(autoGenerate = false) // Changed: no auto-gen, use UUID
    val id: String = UUID.randomUUID().toString(), // Changed: String UUID
    val name: String,
    val type: String, // Оставляем type
    val startDate: Long,
    val currentStage: String,
    val notes: String,
    val isActive: Boolean = true,
    val qrCode: String? = null
)