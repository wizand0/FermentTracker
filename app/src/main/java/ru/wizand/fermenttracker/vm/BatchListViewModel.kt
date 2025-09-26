package ru.wizand.fermenttracker.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository
import java.util.concurrent.TimeUnit

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    val repository: BatchRepository // Changed: made public for access in fragments
    private val batchDao = AppDatabase.getInstance(application).batchDao()
    val batches: LiveData<List<Batch>>

    init {
        val batchDao = AppDatabase.getInstance(application).batchDao()
        // было: repository = BatchRepository(batchDao, application.appContext)
        repository = BatchRepository(batchDao, application.applicationContext)
        batches = repository.allBatches
    }

    fun createBatchWithStages(batch: Batch, stages: List<Stage>) {
        viewModelScope.launch {
            try {
                repository.insertBatchWithStages(batch, stages)
            } catch (e: Exception) {
                // fallback: добавляем батч и стадии по отдельности
                repository.addBatch(batch)
                stages.forEach { stage ->
                    try {
                        repository.addStage(stage)
                    } catch (stageEx: Exception) {
                        // можно залогировать или пропустить
                    }
                }
            }
        }
    }

    fun deleteBatch(batch: Batch) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBatch(batch.id)
        }
    }

    suspend fun findBatchByQrCode(qrCode: String): Batch? {
        return repository.findBatchByQrCode(qrCode)
    }

    fun createBatch(batch: Batch) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addBatch(batch)
        }
    }

    fun addStage(stage: Stage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addStage(stage)
        }
    }

    // ===================== новые методы для старта/завершения этапов =====================

    /**
     * Запустить этап вручную.
     * autoStopPrevious: если true — предыдущий активный этап завершаем автоматически.
     */
    fun startStageManual(batchId: String, stageId: String, durationHours: Long, autoStopPrevious: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = batchDao.getActiveStage(batchId)
            if (active != null && active.id != stageId) {
                if (autoStopPrevious) {
                    batchDao.completeStage(active.id, System.currentTimeMillis())
                }
                // если autoStopPrevious=false — мы всё равно разрешаем старт нового этапа,
                // но можно вместо этого вернуть ошибку/лог.
            }

            val now = System.currentTimeMillis()
            val plannedEnd = now + TimeUnit.HOURS.toMillis(durationHours)
            batchDao.startStage(stageId, now, plannedEnd)
        }
    }

    /**
     * Завершить этап. Если autoStartNext==true — автоматически стартуем следующий по orderIndex.
     * Подразумевается, что у Stage есть поле orderIndex.
     */
    fun completeStageAndMaybeStartNext(batchId: String, stageId: String, orderIndex: Int, autoStartNext: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            batchDao.completeStage(stageId, now)

            if (autoStartNext) {
                val next = batchDao.getStageByOrder(batchId, orderIndex + 1)
                next?.let {
                    val plannedEnd = now + TimeUnit.HOURS.toMillis(it.durationHours)
                    batchDao.startStage(it.id, now, plannedEnd)
                }
            }
        }
    }

    /**
     * Синхронный suspend-помощник для получения активного этапа (удобно для кнопки photo и т.п.)
     */
    suspend fun getActiveStageSuspend(batchId: String): Stage? = withContext(Dispatchers.IO) {
        batchDao.getActiveStage(batchId)
    }

    // Можно добавить Flow/LiveData-методы для stages, если нужно
    fun getStagesForBatchLive(batchId: String): LiveData<List<Stage>> {
        return batchDao.getStagesForBatch(batchId)
    }

    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }
}