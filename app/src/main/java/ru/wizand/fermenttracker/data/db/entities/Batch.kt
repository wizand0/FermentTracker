package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "batches")
data class Batch(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val productType: String,
    val startDate: Long = System.currentTimeMillis(),
    val startWeightGr: Double,
    val status: String = "active" // "active", "finished"
)