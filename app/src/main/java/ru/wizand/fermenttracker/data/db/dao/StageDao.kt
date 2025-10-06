package ru.wizand.fermenttracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import ru.wizand.fermenttracker.data.db.entities.Stage

@Dao
interface StageDao {
    @Query("SELECT * FROM stages WHERE endTime >= :weekAgo AND endTime <= :now")
    suspend fun getCompletedStagesThisWeek(weekAgo: Long, now: Long): List<Stage>?

    @Query("SELECT * FROM stages WHERE plannedEndTime > :now ORDER BY plannedEndTime ASC LIMIT 1")
    suspend fun getNextUpcomingStage(now: Long): Stage?

    @Query("SELECT * FROM stages WHERE endTime IS NOT NULL ORDER BY endTime DESC LIMIT :limit")
    suspend fun getRecentCompletedStages(limit: Int): List<Stage>?
}