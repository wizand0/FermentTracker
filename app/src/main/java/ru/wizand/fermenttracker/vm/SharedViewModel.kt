package ru.wizand.fermenttracker.vm

import android.app.Application
import android.util.Log
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
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val batchDao = database.batchDao()
    private val stageDao = database.stageDao()
    val repository = BatchRepository(batchDao, application.applicationContext)

    // LiveData для всех партий
    val allBatches = repository.allBatches

    // Flow с PagingData для Paging 3
    val batchesPaged: Flow<PagingData<Batch>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
            prefetchDistance = 5
        ),
        pagingSourceFactory = { batchDao.getAllBatchesPaged() }
    ).flow.cachedIn(viewModelScope)

    // Данные для Dashboard
    private val _activeBatchesCount = MutableLiveData<Int>()
    val activeBatchesCount: LiveData<Int> = _activeBatchesCount

    private val _completedStagesThisWeek = MutableLiveData<Int>()
    val completedStagesThisWeek: LiveData<Int> = _completedStagesThisWeek

    private val _avgWeightLoss = MutableLiveData<Double>()
    val avgWeightLoss: LiveData<Double> = _avgWeightLoss

    private val _nextEvent = MutableLiveData<Pair<String, Long>?>()
    val nextEvent: LiveData<Pair<String, Long>?> = _nextEvent

    private val _recentCompletedStages = MutableLiveData<List<Stage>>()
    val recentCompletedStages: LiveData<List<Stage>> = _recentCompletedStages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        // Наблюдаем за изменениями в партиях
        allBatches.observeForever { batches ->
            refreshDashboardData()
        }
    }

    fun refreshDashboardData() {

        Log.d("Dashboard", "refreshDashboardData() called")

        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
        try {
                // Активные партии
                val activeBatches = batchDao.getActiveBatchesCount()
                _activeBatchesCount.postValue(activeBatches ?: 0)

                // Завершённые этапы за неделю
                val now = System.currentTimeMillis()
                val weekAgo = now - (7 * 24 * 60 * 60 * 1000)
                val completedStages = stageDao.getCompletedStagesThisWeek(weekAgo, now)
                _completedStagesThisWeek.postValue(completedStages?.size ?: 0)

                // Средняя потеря веса
                val avgLoss = batchDao.getAverageWeightLoss()
                _avgWeightLoss.postValue(avgLoss ?: 0.0)

                // Ближайшее событие
                val nextStage = stageDao.getNextUpcomingStage(now)
                if (nextStage != null) {
                    val batch = batchDao.getBatchByIdOnce(nextStage.batchId)
                    val batchName = batch?.name ?: ""
                    // Проверяем, что plannedEndTime не null
                    nextStage.plannedEndTime?.let { plannedEndTime ->
                        _nextEvent.postValue(Pair("$batchName: ${nextStage.name}", plannedEndTime))
                    } ?: run {
                        _nextEvent.postValue(null)
                    }
                } else {
                    _nextEvent.postValue(null)
                }

                // Недавние уведомления (топ-3 завершённых этапов)
                val recentStages = stageDao.getRecentCompletedStages(3)
                _recentCompletedStages.postValue(recentStages ?: emptyList())

            } catch (e: Exception) {
                Log.e("Dashboard", "Error refreshing dashboard", e)
                // Обработка ошибок
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun addBatch(batch: Batch) {
        viewModelScope.launch {
            repository.addBatch(batch)
        }
    }

    fun updateBatch(batch: Batch) {
        viewModelScope.launch {
            repository.updateBatch(batch)
        }
    }

    fun deleteBatch(batchId: String) {
        viewModelScope.launch {
            repository.deleteBatch(batchId)
        }
    }

    fun createBatchWithStages(batch: Batch, stages: List<Stage>) {
        viewModelScope.launch {
            repository.insertBatchWithStages(batch, stages)
        }
    }

    fun addStage(stage: Stage) {
        viewModelScope.launch {
            repository.addStage(stage)
        }
    }

    fun deleteStage(stageId: String) {
        viewModelScope.launch {
            repository.deleteStage(stageId)
        }
    }

    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }

    // Добавляем метод, который отсутствовал
    fun getStagesForBatchLive(batchId: String): LiveData<List<Stage>> {
        return batchDao.getStagesForBatch(batchId)
    }
}