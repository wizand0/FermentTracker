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
import androidx.lifecycle.MutableLiveData
import java.util.UUID

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    val repository: BatchRepository // public
    private val batchDao = AppDatabase.getInstance(application).batchDao()
    val batches: LiveData<List<Batch>>

    // Added: LiveData состояния активного этапа
    private val _activeStageId = MutableLiveData<String?>(null)
    val activeStageId: LiveData<String?> = _activeStageId

    // Result of weight save operation
    sealed class WeightSaveResult {
        object Success : WeightSaveResult()
        data class Failure(val reason: String) : WeightSaveResult()
    }

    private val _weightSaveResult = MutableLiveData<WeightSaveResult?>()
    val weightSaveResult: LiveData<WeightSaveResult?> = _weightSaveResult

    init {
        val batchDao = AppDatabase.getInstance(application).batchDao()
        repository = BatchRepository(batchDao, application.applicationContext)
        batches = repository.allBatches
    }

    // Вызывать при старте приложения/открытии батча, чтобы инициализировать текущее активное состояние
    fun refreshActiveStage(batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = batchDao.getActiveStage(batchId)
            _activeStageId.postValue(active?.id)
        }
    }

    fun startStageManual(batchId: String, stageId: String, durationHours: Long, autoStopPrevious: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val active = batchDao.getActiveStage(batchId)
            if (active != null && active.id != stageId) {
                if (autoStopPrevious) {
                    batchDao.completeStage(active.id, System.currentTimeMillis())
                } else {
                    return@launch
                }
            }

            val now = System.currentTimeMillis()
            val plannedEnd = now + TimeUnit.HOURS.toMillis(durationHours)
            batchDao.startStage(stageId, now, plannedEnd)

            _activeStageId.postValue(stageId)
        }
    }

    fun completeStageAndMaybeStartNext(batchId: String, stageId: String, orderIndex: Int, autoStartNext: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            batchDao.completeStage(stageId, now)

            // check active and reset if needed
            val active = batchDao.getActiveStage(batchId)
            if (active == null || active.id != stageId) {
                // ok
            } else {
                _activeStageId.postValue(null)
            }

            if (autoStartNext) {
                val next = batchDao.getStageByOrder(batchId, orderIndex + 1)
                next?.let {
                    val plannedEnd = now + TimeUnit.HOURS.toMillis(it.durationHours)
                    batchDao.startStage(it.id, now, plannedEnd)
                    _activeStageId.postValue(it.id)
                }
            }
        }
    }

    suspend fun getActiveStageSuspend(batchId: String): Stage? = withContext(Dispatchers.IO) {
        batchDao.getActiveStage(batchId)
    }

    // ===================== weight validation and saving =====================
    /**
     * Проверка и сохранение веса:
     * - prevWeight: последняя запись в BatchLog.weightGr, иначе batch.currentWeightGr, иначе batch.initialWeightGr
     * - запрещаем увеличение: newWeight <= prev
     * - запрещаем уменьшение более чем на 40%: newWeight >= prev * 0.6
     */
    fun addWeightChecked(batchId: String, newWeight: Double, photoPath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastLogWeight = repository.getLastLogWeight(batchId)
                var prevWeight: Double? = lastLogWeight

                if (prevWeight == null) {
                    // try fetch batch snapshot
                    val batch = repository.getBatchByIdOnce(batchId)
                    prevWeight = batch?.currentWeightGr ?: batch?.initialWeightGr
                }

                // if no previous weight known — accept
                if (prevWeight == null) {
                    // insert log
                    val log = ru.wizand.fermenttracker.data.db.entities.BatchLog(
                        id = UUID.randomUUID().toString(),
                        batchId = batchId,
                        timestamp = System.currentTimeMillis(),
                        weightGr = newWeight,
                        photoPath = photoPath
                    )
                    repository.addLog(log)
                    // try update batch currentWeight
                    try {
                        val batch = repository.getBatchByIdOnce(batchId)
                        batch?.let {
                            val updated = it.copy(currentWeightGr = newWeight)
                            repository.updateBatch(updated)
                        }
                    } catch (_: Exception) { /* ignore */ }

                    _weightSaveResult.postValue(WeightSaveResult.Success)
                    return@launch
                }

                val prev = prevWeight
                if (newWeight > prev) {
                    _weightSaveResult.postValue(WeightSaveResult.Failure("Вес не может увеличиваться (предыдущий: $prev)"))
                    return@launch
                }
                val diffPercent = (prev - newWeight) / prev
                if (diffPercent > 0.40) {
                    _weightSaveResult.postValue(WeightSaveResult.Failure("Вес отличается более чем на 40% от предыдущего (предыдущий: $prev)"))
                    return@launch
                }

                // ok — save log and update batch.currentWeightGr
                val log = ru.wizand.fermenttracker.data.db.entities.BatchLog(
                    id = UUID.randomUUID().toString(),
                    batchId = batchId,
                    timestamp = System.currentTimeMillis(),
                    weightGr = newWeight,
                    photoPath = photoPath
                )
                repository.addLog(log)

                try {
                    val batch = repository.getBatchByIdOnce(batchId)
                    batch?.let {
                        val updated = it.copy(currentWeightGr = newWeight)
                        repository.updateBatch(updated)
                    }
                } catch (_: Exception) { /* ignore */ }

                _weightSaveResult.postValue(WeightSaveResult.Success)
            } catch (e: Exception) {
                _weightSaveResult.postValue(WeightSaveResult.Failure("Ошибка при сохранении: ${e.message ?: e.toString()}"))
            }
        }
    }

    // ============ existing methods (create/delete etc.) =============
    fun createBatchWithStages(batch: Batch, stages: List<Stage>) {
        viewModelScope.launch {
            try {
                repository.insertBatchWithStages(batch, stages)
            } catch (e: Exception) {
                repository.addBatch(batch)
                stages.forEach { stage ->
                    try {
                        repository.addStage(stage)
                    } catch (stageEx: Exception) { }
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

    fun getStagesForBatchLive(batchId: String): LiveData<List<Stage>> {
        return batchDao.getStagesForBatch(batchId)
    }

    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }
}