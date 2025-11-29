package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository
import java.util.UUID
import java.util.concurrent.TimeUnit

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    val repository: BatchRepository
    private val batchDao = AppDatabase.getInstance(application).batchDao()
    val batches: LiveData<List<Batch>>

    val batchesPaged: Flow<PagingData<Batch>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { batchDao.getAllBatchesPaged() }
    ).flow.cachedIn(viewModelScope)

    private val _activeStageId = MutableLiveData<String?>(null)
    val activeStageId: LiveData<String?> = _activeStageId

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

            // Также обновляем название текущего этапа в самой партии
            val stage = batchDao.getStageById(stageId)
            if (stage != null) {
                val batch = repository.getBatchByIdOnce(batchId)
                batch?.let {
                    repository.updateBatch(it.copy(currentStage = stage.name))
                }
            }

            _activeStageId.postValue(stageId)
        }
    }

    fun completeStageAndMaybeStartNext(batchId: String, stageId: String, orderIndex: Int, autoStartNext: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            // 1. Завершаем активный этап
            batchDao.completeStage(stageId, now)

            // 2. Сбрасываем локальный ID активного этапа
            val active = batchDao.getActiveStage(batchId)
            if (active != null && active.id == stageId) {
                _activeStageId.postValue(null) // сброс, так как этап завершен
            } else if (active != null) {
                // Если активен другой этап (странная ситуация), не трогаем его
            } else {
                _activeStageId.postValue(null)
            }

            var nextStageStarted = false

            // 3. Пытаемся запустить следующий этап
            if (autoStartNext) {
                val next = batchDao.getStageByOrder(batchId, orderIndex + 1)
                if (next != null) {
                    val plannedEnd = now + TimeUnit.HOURS.toMillis(next.durationHours)
                    batchDao.startStage(next.id, now, plannedEnd)
                    _activeStageId.postValue(next.id)

                    // Обновляем имя этапа в батче
                    val batch = repository.getBatchByIdOnce(batchId)
                    batch?.let {
                        repository.updateBatch(it.copy(currentStage = next.name))
                    }
                    nextStageStarted = true
                }
            }

            // 4. Если следующий этап НЕ начат (либо это был последний, либо autoStart=false),
            // проверяем, нужно ли закрыть партию целиком.
            if (!nextStageStarted) {
                checkAndCompleteBatch(batchId)
            }
        }
    }

    /**
     * Проверяет, завершены ли все этапы. Если да — переводит партию в статус isActive = false.
     * Метод suspend, выполняется в контексте вызывающей корутины (IO).
     */
    private suspend fun checkAndCompleteBatch(batchId: String) {
        try {
            val stages = batchDao.getStagesForBatchOnce(batchId)

            // Проверка: список не пуст И у всех этапов есть endTime
            val allCompleted = stages.isNotEmpty() && stages.all { it.endTime != null }

            if (allCompleted) {
                val batch = repository.getBatchByIdOnce(batchId)
                batch?.let {
                    if (it.isActive) {
                        // Обновляем статус и очищаем поле текущего этапа
                        val updatedBatch = it.copy(
                            isActive = false,
                            currentStage = "" // Очищаем, так как активных этапов больше нет
                        )
                        repository.updateBatch(updatedBatch)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BatchListViewModel", "Error in checkAndCompleteBatch", e)
        }
    }

    suspend fun getActiveStageSuspend(batchId: String): Stage? = withContext(Dispatchers.IO) {
        batchDao.getActiveStage(batchId)
    }

    // ===================== weight validation and saving =====================
    fun addWeightChecked(batchId: String, newWeight: Double, photoPath: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lastLogWeight = repository.getLastLogWeight(batchId)
                var prevWeight: Double? = lastLogWeight

                if (prevWeight == null) {
                    val batch = repository.getBatchByIdOnce(batchId)
                    prevWeight = batch?.currentWeightGr ?: batch?.initialWeightGr
                }

                // Если предыдущего веса нет совсем — принимаем любой
                if (prevWeight == null) {
                    saveLogAndUpdateBatch(batchId, newWeight, photoPath)
                    return@launch
                }

                val prev = prevWeight
                if (newWeight > prev) {
                    val message = getApplication<Application>().getString(
                        R.string.weight_cannot_increase_error, prev
                    )
                    _weightSaveResult.postValue(WeightSaveResult.Failure(message))
                    return@launch
                }
                val diffPercent = (prev - newWeight) / prev
                if (diffPercent > 0.40) {
                    val message = getApplication<Application>().getString(
                        R.string.weight_diff_too_large_error, prev
                    )
                    _weightSaveResult.postValue(WeightSaveResult.Failure(message))
                    return@launch
                }

                saveLogAndUpdateBatch(batchId, newWeight, photoPath)

            } catch (e: Exception) {
                val message = getApplication<Application>().getString(
                    R.string.weight_save_error, e.message ?: e.toString()
                )
                _weightSaveResult.postValue(WeightSaveResult.Failure(message))
            }
        }
    }

    private suspend fun saveLogAndUpdateBatch(batchId: String, weight: Double, photoPath: String?) {
        val log = ru.wizand.fermenttracker.data.db.entities.BatchLog(
            id = UUID.randomUUID().toString(),
            batchId = batchId,
            timestamp = System.currentTimeMillis(),
            weightGr = weight,
            photoPath = photoPath
        )
        repository.addLog(log)

        try {
            val batch = repository.getBatchByIdOnce(batchId)
            batch?.let {
                val updated = it.copy(currentWeightGr = weight)
                repository.updateBatch(updated)
            }
        } catch (_: Exception) { }

        _weightSaveResult.postValue(WeightSaveResult.Success)
    }

    // ============ existing methods (create/delete etc.) =============

    fun createBatchWithStages(batch: Batch, stages: List<Stage>) {
        viewModelScope.launch(Dispatchers.IO) {
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

    suspend fun updateBatch(batch: Batch) {
        repository.updateBatch(batch)
    }
}