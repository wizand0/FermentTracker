package ru.wizand.fermenttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

@Dao
interface BatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: Batch)

    @Update
    suspend fun updateBatch(batch: Batch)

    @Query("DELETE FROM batches WHERE id = :batchId")
    suspend fun deleteBatch(batchId: String)

    @Query("SELECT * FROM batches")
    fun getAllBatches(): LiveData<List<Batch>>

    @Query("SELECT * FROM batches WHERE id = :batchId")
    fun getBatchById(batchId: String): LiveData<Batch?>

    @Query("SELECT * FROM batches WHERE qrCode = :qrCode LIMIT 1")
    suspend fun findBatchByQrCode(qrCode: String): Batch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: Stage)

    @Update
    suspend fun updateStage(stage: Stage)

    @Query("DELETE FROM stages WHERE id = :stageId")
    suspend fun deleteStage(stageId: String)

    @Query("SELECT * FROM stages WHERE batchId = :batchId ORDER BY orderIndex ASC")
    fun getStagesForBatch(batchId: String): LiveData<List<Stage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)

    @Query("SELECT photos.* FROM photos INNER JOIN stages ON photos.stageId = stages.id WHERE stages.batchId = :batchId ORDER BY photos.timestamp DESC")
    fun getPhotosForBatch(batchId: String): LiveData<List<Photo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatchLog)

    @Query("SELECT * FROM batch_logs WHERE batchId = :batchId ORDER BY timestamp DESC")
    fun getLogsForBatch(batchId: String): LiveData<List<BatchLog>>
}