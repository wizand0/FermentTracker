package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(entity = Stage::class, parentColumns = ["id"], childColumns = ["stageId"], onDelete = CASCADE)],
    indices = [Index("stageId")]
)
data class Photo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val stageId: String,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis()
)