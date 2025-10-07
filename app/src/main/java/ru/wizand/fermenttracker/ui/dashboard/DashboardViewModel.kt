package ru.wizand.fermenttracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val batchDao = database.batchDao()
    private val stageDao = database.stageDao()
    private val repository = BatchRepository(batchDao, application.applicationContext)

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
        repository.allBatches.observeForever { batches ->
            refreshData()
        }
    }

    fun refreshData() {
        _isLoading.value = true

        viewModelScope.launch {
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
                // Обработка ошибок
                e.printStackTrace()
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}