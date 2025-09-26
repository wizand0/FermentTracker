package ru.wizand.fermenttracker.data.repository

import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import ru.wizand.fermenttracker.workers.StageNotificationWorker

class BatchRepository(
    private val batchDao: BatchDao,
    private val appContext: Context
) {

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
    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        val delay = stage.plannedEndTime?.let { it - System.currentTimeMillis() } ?: return
        if (delay <= 0) return

        val data = Data.Builder()
            .putString("stageName", stage.name)
            .putString("batchName", batch.name)
            .putInt("notificationId", stage.id.hashCode())
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StageNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(appContext).enqueue(workRequest)
    }

    suspend fun insertBatchWithStages(batch: Batch, stages: List<Stage>) {
        batchDao.insertBatchWithStages(batch, stages)
    }
}