package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
@Entity(
    tableName = "stages",
    foreignKeys = [ForeignKey(entity = Batch::class, parentColumns = ["id"], childColumns = ["batchId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("batchId")]
)
data class Stage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val batchId: String? = null,
    val name: String,
    val durationHours: Long,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val plannedStartTime: Long? = null,
    val plannedEndTime: Long? = null,
    val orderIndex: Int = 0
) : android.os.Parcelable