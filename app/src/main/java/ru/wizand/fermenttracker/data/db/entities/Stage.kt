package ru.wizand.fermenttracker.data.db.entities

import android.os.Parcel
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID


@Entity(
    tableName = "stages",
    foreignKeys = [ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("batchId")]
)
data class Stage(
    @PrimaryKey // Убираем autoGenerate
    val id: String = UUID.randomUUID().toString(), // Используем UUID
    val batchId: String, // batchId теперь обязателен, даже при создании из шаблона
    val name: String,
    val durationHours: Long,
    var startTime: Long? = null,
    var endTime: Long? = null,
    val plannedStartTime: Long? = null,
    val plannedEndTime: Long? = null,
    val orderIndex: Int
) {
    val status: String
        get() = when {
            startTime == null -> "Not started"
            endTime == null -> "Ongoing"
            else -> "Completed"
        }
}

