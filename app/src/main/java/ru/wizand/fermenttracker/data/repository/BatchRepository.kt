package ru.wizand.fermenttracker.data.repository

import androidx.lifecycle.LiveData
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

class BatchRepository(private val dao: BatchDao) {
    val allBatches: LiveData<List<Batch>> = dao.getAllBatches()

    suspend fun addBatch(batch: Batch) {
        android.util.Log.d("BatchRepository", "Inserting batch: $batch")
        dao.insertBatch(batch)
    }

    suspend fun addStage(stage: Stage) {
        android.util.Log.d("BatchRepository", "Inserting stage: $stage")
        dao.insertStage(stage)
    }

    suspend fun updateStage(stage: Stage) {
        android.util.Log.d("BatchRepository", "Updating stage: $stage")
        dao.updateStage(stage)
    }

    suspend fun addPhoto(photo: Photo) {
        android.util.Log.d("BatchRepository", "Inserting photo: $photo")
        dao.insertPhoto(photo)
    }

    suspend fun updatePhoto(photo: Photo) {
        android.util.Log.d("BatchRepository", "Updating photo: $photo")
        dao.updatePhoto(photo)
    }

    suspend fun deleteBatch(batch: Batch) = dao.deleteBatch(batch)

    fun getBatchById(id: String): LiveData<Batch?> = dao.getBatchById(id)

    fun getStages(batchId: String): LiveData<List<Stage>> = dao.getStagesForBatch(batchId)

    fun getPhotos(stageId: String): LiveData<List<Photo>> = dao.getPhotosForStage(stageId)
}