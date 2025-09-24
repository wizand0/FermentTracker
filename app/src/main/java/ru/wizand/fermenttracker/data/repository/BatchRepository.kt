package ru.wizand.fermenttracker.data.repository

import androidx.lifecycle.LiveData
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

class BatchRepository(private val dao: BatchDao) {
    val allBatches: LiveData<List<Batch>> = dao.getAllBatches()

    suspend fun addBatch(batch: Batch) = dao.insertBatch(batch)
    suspend fun addStage(stage: Stage) = dao.insertStage(stage)
    suspend fun addPhoto(photo: Photo) = dao.insertPhoto(photo)
    fun getStages(batchId:String) = dao.getStagesForBatch(batchId)
    fun getPhotos(stageId:String) = dao.getPhotosForStage(stageId)
}
