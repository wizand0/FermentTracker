package ru.wizand.fermenttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

@Dao
interface BatchDao {
    @Transaction
    @Query("SELECT * FROM batches ORDER BY startDate DESC")
    fun getAllBatches(): LiveData<List<Batch>>

    @Query("SELECT * FROM batches WHERE id = :id")
    fun getBatchById(id: String): LiveData<Batch?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: Batch)

    @Update
    suspend fun updateBatch(batch: Batch)

    @Delete
    suspend fun deleteBatch(batch: Batch)

    // Stages and photos
    @Query("SELECT * FROM stages WHERE batchId = :batchId ORDER BY orderIndex")
    fun getStagesForBatch(batchId: String): LiveData<List<Stage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: Stage)

    @Update
    suspend fun updateStage(stage: Stage)

    // If needed, add for Photo
    @Update
    suspend fun updatePhoto(photo: Photo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)

    @Query("SELECT * FROM photos WHERE stageId = :stageId ORDER BY timestamp")
    fun getPhotosForStage(stageId: String): LiveData<List<Photo>>
}
