package ru.wizand.fermenttracker.data.models

import androidx.room.ColumnInfo

// Data class для POJO результата Room query (JOIN stages и batches)
data class StageWithBatch(
    @ColumnInfo(name = "batchName") val batchName: String,
    @ColumnInfo(name = "stageName") val stageName: String,
    @ColumnInfo(name = "endTime") val endTime: Long
)