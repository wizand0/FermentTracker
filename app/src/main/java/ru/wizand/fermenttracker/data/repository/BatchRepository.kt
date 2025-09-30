package ru.wizand.fermenttracker.data.repository

import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.db.entities.Recipe // Added
import ru.wizand.fermenttracker.data.db.entities.StageTemplate // Added

import android.content.Context
import android.util.Log
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

    // returns LiveData (existing)
    fun getBatchById(batchId: String) = batchDao.getBatchById(batchId)

    // Added: synchronous suspend fetch (single-shot)
    suspend fun getBatchByIdOnce(batchId: String): Batch? = batchDao.getBatchByIdOnce(batchId)

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

    // Added: last log weight
    suspend fun getLastLogWeight(batchId: String): Double? = batchDao.getLastLogWeight(batchId)

    suspend fun findBatchByQrCode(qrCode: String): Batch? {
        return batchDao.findBatchByQrCode(qrCode)
    }
    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        val delay = stage.plannedEndTime?.let { it - System.currentTimeMillis() } ?: return
        if (delay <= 0) {
            Log.d("BatchRepository", "Skipping notification for stage: ${stage.name} - delay is non-positive: $delay")
            return
        }

        val data = Data.Builder()
            .putString("stageName", stage.name)
            .putString("batchName", batch.name)
            .putString("batchId", batch.id)  // Добавлен batchId
            .putInt("notificationId", stage.id.hashCode())
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StageNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("stage_notification_${stage.id}")  // Добавлен тег для отслеживания
            .build()

        WorkManager.getInstance(appContext).enqueue(workRequest)

        Log.d("BatchRepository", "Scheduled notification for stage: ${stage.name}, batchId: ${batch.id}, delay: ${delay}ms (${delay / 1000 / 60} min), workRequestId: ${workRequest.id}")
    }

    suspend fun insertBatchWithStages(batch: Batch, stages: List<Stage>) {
        batchDao.insertBatchWithStages(batch, stages)
    }

    // Added: Recipe and StageTemplate methods
    suspend fun insertRecipe(recipe: Recipe) {
        batchDao.insertRecipe(recipe)
    }

    suspend fun getAllRecipes(): List<Recipe> {
        return batchDao.getAllRecipes()
    }

    suspend fun getAllRecipeTypes(): List<String> {
        return batchDao.getAllRecipeTypes()
    }

    // Added: Get recipe by its type
    suspend fun getRecipeByType(type: String): Recipe? = batchDao.getRecipeByType(type)

    suspend fun updateRecipe(recipe: Recipe) {
        batchDao.updateRecipe(recipe)
    }

    suspend fun deleteRecipe(type: String) {
        batchDao.deleteRecipe(type)
    }

    suspend fun insertStageTemplate(template: StageTemplate) {
        batchDao.insertStageTemplate(template)
    }

//    suspend fun getStageTemplatesForType(recipeType: String): List<StageTemplate> {
//        return batchDao.getStageTemplatesForType(recipeType)
//    }

    suspend fun getStageTemplatesForType(recipeType: String): List<StageTemplate> {
        val templates = batchDao.getStageTemplatesForType(recipeType)
            .groupBy { it.name + it.durationHours }  // Группируем по уникальному ключу
            .map { it.value.sortedBy { t -> t.orderIndex }.first() }  // Берём первого в группе с min orderIndex
            .sortedBy { it.orderIndex }  // Сортируем по orderIndex
        return templates
    }

    suspend fun updateStageTemplate(template: StageTemplate) {
        batchDao.updateStageTemplate(template)
    }

    suspend fun deleteStageTemplate(id: String) {
        batchDao.deleteStageTemplate(id)
    }
}