package ru.wizand.fermenttracker.data.repository

import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

class BatchRepository(private val batchDao: BatchDao) {

    val allBatches = batchDao.getAllBatches()

    suspend fun addBatch(batch: Batch) {
        batchDao.insertBatch(batch)
    }

    suspend fun updateBatch(batch: Batch) {
        batchDao.updateBatch(batch)
    }

    suspend fun deleteBatch(batchId: String) {
        batchDao.deleteBatch(batchId)
    }

    fun getBatchById(batchId: String) = batchDao.getBatchById(batchId)

    suspend fun addStage(stage: Stage) {
        batchDao.insertStage(stage)
    }

    suspend fun updateStage(stage: Stage) {
        batchDao.updateStage(stage)
    }

    suspend fun deleteStage(stageId: String) {
        batchDao.deleteStage(stageId)
    }

    fun getStages(batchId: String) = batchDao.getStagesForBatch(batchId)

    suspend fun addPhoto(photo: Photo) {
        batchDao.insertPhoto(photo)
    }

    fun getPhotos(batchId: String) = batchDao.getPhotosForBatch(batchId)

    suspend fun addLog(log: BatchLog) {
        batchDao.insertLog(log)
    }

    fun getLogs(batchId: String) = batchDao.getLogsForBatch(batchId)

    suspend fun findBatchByQrCode(qrCode: String): Batch? {
        return batchDao.findBatchByQrCode(qrCode)
    }
}