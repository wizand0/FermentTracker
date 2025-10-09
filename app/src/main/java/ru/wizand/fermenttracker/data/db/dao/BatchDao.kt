package ru.wizand.fermenttracker.data.db.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import ru.wizand.fermenttracker.data.db.entities.*


@Dao
interface BatchDao {

    // === Основные операции с партиями ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: Batch)

    @Transaction
    suspend fun insertBatchWithStages(batch: Batch, stages: List<Stage>) {
        insertBatch(batch)
        stages.forEach { insertStage(it) }
    }

    @Update
    suspend fun updateBatch(batch: Batch)

    @Query("DELETE FROM batches WHERE id = :batchId")
    suspend fun deleteBatch(batchId: String)

    // Сортировка по дате старта (новые сверху)
    @Query("SELECT * FROM batches ORDER BY startDate DESC")
    fun getAllBatches(): LiveData<List<Batch>>

    @Query("SELECT * FROM batches ORDER BY startDate DESC")
    fun getAllBatchesPaged(): PagingSource<Int, Batch>

    @Query("SELECT * FROM batches WHERE id = :batchId")
    fun getBatchById(batchId: String): LiveData<Batch?>

    @Query("SELECT * FROM batches WHERE id = :batchId LIMIT 1")
    suspend fun getBatchByIdOnce(batchId: String): Batch?

    @Query("SELECT * FROM batches WHERE qrCode = :qrCode LIMIT 1")
    suspend fun findBatchByQrCode(qrCode: String): Batch?

    // === Этапы (Stages) ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: Stage)

    @Update
    suspend fun updateStage(stage: Stage)

    @Query("DELETE FROM stages WHERE id = :stageId")
    suspend fun deleteStage(stageId: String)

    @Query("SELECT * FROM stages WHERE batchId = :batchId ORDER BY orderIndex ASC")
    fun getStagesForBatch(batchId: String): LiveData<List<Stage>>

    @Query("SELECT * FROM stages WHERE batchId = :batchId ORDER BY orderIndex ASC")
    fun getStagesForBatchFlow(batchId: String): Flow<List<Stage>>

    @Query("SELECT * FROM stages WHERE batchId = :batchId AND startTime IS NOT NULL AND endTime IS NULL LIMIT 1")
    suspend fun getActiveStage(batchId: String): Stage?

    @Query("UPDATE stages SET startTime = :startTime, plannedEndTime = :plannedEnd WHERE id = :stageId")
    suspend fun startStage(stageId: String, startTime: Long, plannedEnd: Long)

    @Query("UPDATE stages SET endTime = :endTime WHERE id = :stageId")
    suspend fun completeStage(stageId: String, endTime: Long)

    @Query("SELECT * FROM stages WHERE batchId = :batchId AND orderIndex = :orderIndex LIMIT 1")
    suspend fun getStageByOrder(batchId: String, orderIndex: Int): Stage?

    // === Фото ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: Photo)

    @Query("""
        SELECT photos.* FROM photos
        INNER JOIN stages ON photos.stageId = stages.id
        WHERE stages.batchId = :batchId
        ORDER BY photos.timestamp DESC
    """)
    fun getPhotosForBatch(batchId: String): LiveData<List<Photo>>

    // === Логи ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BatchLog)

    @Query("SELECT * FROM batch_logs WHERE batchId = :batchId ORDER BY timestamp DESC")
    fun getLogsForBatch(batchId: String): LiveData<List<BatchLog>>

    @Query("SELECT weightGr FROM batch_logs WHERE batchId = :batchId AND weightGr IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLogWeight(batchId: String): Double?

    // === Рецепты ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<Recipe>

    @Query("SELECT type FROM recipes")
    suspend fun getAllRecipeTypes(): List<String>

    @Query("SELECT * FROM recipes WHERE type = :type LIMIT 1")
    suspend fun getRecipeByType(type: String): Recipe?

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("DELETE FROM recipes WHERE type = :type")
    suspend fun deleteRecipe(type: String)

    // === Шаблоны этапов ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStageTemplate(template: StageTemplate)

    @Query("SELECT * FROM stage_templates WHERE recipeType = :recipeType ORDER BY orderIndex ASC")
    suspend fun getStageTemplatesForType(recipeType: String): List<StageTemplate>

    @Update
    suspend fun updateStageTemplate(template: StageTemplate)

    @Query("DELETE FROM stage_templates WHERE id = :id")
    suspend fun deleteStageTemplate(id: String)

    // === Метрики для Dashboard ===

    @Query("SELECT COUNT(*) FROM batches")
    fun getBatchCount(): Int

    @Query("SELECT COUNT(*) FROM recipes")
    fun getRecipeCount(): Int

    @Query("SELECT COUNT(*) FROM batches WHERE isActive = 1")
    fun getActiveBatchesCount(): Int?

    @Query("""
        SELECT AVG((initialWeightGr - currentWeightGr) / initialWeightGr * 100)
        FROM batches
        WHERE type IN ('Dry-cured meat', 'Dry-cured sausage')
          AND isActive = 0
          AND initialWeightGr IS NOT NULL
          AND currentWeightGr IS NOT NULL
    """)
    fun getAverageWeightLoss(): Double?


    @RawQuery(observedEntities = [Batch::class])
    fun getFilteredBatchesPaged(query: SupportSQLiteQuery): PagingSource<Int, Batch>
}
