package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "stage_templates",
    foreignKeys = [ForeignKey(entity = Recipe::class, parentColumns = ["type"], childColumns = ["recipeType"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("recipeType")]
)
data class StageTemplate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipeType: String,
    val name: String,
    val durationHours: Long,
    var orderIndex: Int
)