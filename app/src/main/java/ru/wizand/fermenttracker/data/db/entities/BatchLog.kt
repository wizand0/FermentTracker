package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_logs")
data class BatchLog(
    @PrimaryKey val id: String,
    val batchId: String,
    val timestamp: Long,
    val weightGr: Double? = null,
    val photoPath: String? = null
)