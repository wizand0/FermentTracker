package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stages",
    foreignKeys = [ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = CASCADE)],
    indices = [Index("batchId")]
)
data class Stage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val batchId: String,
    val name: String,
    val durationHours: Long, // длительность в часах
    val startTime: Long? = null,
    val endTime: Long? = null,
    val currentWeightGr: Double? = null,
    val orderIndex: Int = 0
)