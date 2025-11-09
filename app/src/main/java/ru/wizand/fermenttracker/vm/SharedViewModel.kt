package ru.wizand.fermenttracker.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage  // ← Этот импорт всё ещё нужен для других частей кода (например, методы addStage, getStagesForBatchLive и т.д.), так что не удаляем
import ru.wizand.fermenttracker.data.repository.BatchRepository

import kotlinx.coroutines.flow.*
import ru.wizand.fermenttracker.data.models.StageWithBatch
import kotlin.time.Duration.Companion.milliseconds

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val batchDao = database.batchDao()
    private val stageDao = database.stageDao()
    val repository = BatchRepository(batchDao, application.applicationContext)

    // --- Фильтрация и поиск ---
    private val _filterQuery = MutableLiveData("")
    val filterQuery: LiveData<String> = _filterQuery

    private val _filterCriteria = MutableLiveData("date")
    val filterCriteria: LiveData<String> = _filterCriteria

    fun updateFilterQuery(query: String) {
        _filterQuery.value = query
    }

    fun updateFilterCriteria(criteria: String) {
        _filterCriteria.value = criteria
    }

    // --- Paging 3 с debounce() ---
    val batchesPaged: Flow<PagingData<Batch>> =
        combine(
            _filterQuery.asFlow()
                .debounce(400) // ← ждём 400 мс после последнего ввода
                .distinctUntilChanged(), // ← игнорируем повторяющиеся строки
            _filterCriteria.asFlow().distinctUntilChanged()
        ) { query, criteria ->
            query to criteria
        }.flatMapLatest { (query, criteria) ->
            Pager(
                config = PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = false,
                    prefetchDistance = 5
                ),
                pagingSourceFactory = {
                    repository.getFilteredBatchesPaged(query, criteria)
                }
            ).flow
        }.cachedIn(viewModelScope)


    // --- Dashboard данные ---
    private val _activeBatchesCount = MutableLiveData<Int>()
    val activeBatchesCount: LiveData<Int> = _activeBatchesCount

    private val _completedStagesThisWeek = MutableLiveData<Int>()
    val completedStagesThisWeek: LiveData<Int> = _completedStagesThisWeek

    private val _avgWeightLoss = MutableLiveData<Double>()
    val avgWeightLoss: LiveData<Double> = _avgWeightLoss

    private val _nextEvent = MutableLiveData<Triple<String, Long, String>?>()
    val nextEvent: LiveData<Triple<String, Long, String>?> = _nextEvent

    // Изменено на List<Triple<String, String, Long>>
    private val _recentCompletedStages = MutableLiveData<List<StageWithBatch>>()
    val recentCompletedStages: LiveData<List<StageWithBatch>> = _recentCompletedStages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val batchesObserver = androidx.lifecycle.Observer<List<Batch>> {
        refreshDashboardData()
    }

    init {
        refreshDashboardData()
    }

    override fun onCleared() {
        super.onCleared()
        allBatches.removeObserver(batchesObserver)
    }

    // --- Репозиторий ---
    val allBatches = repository.allBatches

    fun startObservingBatches() {
        allBatches.observeForever(batchesObserver)
    }

    fun stopObservingBatches() {
        allBatches.removeObserver(batchesObserver)
    }

    fun refreshDashboardData() {
        Log.d("Dashboard", "refreshDashboardData() called")
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activeBatches = batchDao.getActiveBatchesCount()
                _activeBatchesCount.postValue(activeBatches ?: 0)

                val now = System.currentTimeMillis()
                val weekAgo = now - 7 * 24 * 60 * 60 * 1000
                val completedStages = stageDao.getCompletedStagesThisWeek(weekAgo, now)
                _completedStagesThisWeek.postValue(completedStages?.size ?: 0)

                val avgLoss = batchDao.getAverageWeightLoss()
                _avgWeightLoss.postValue(avgLoss ?: 0.0)

                val nextStage = stageDao.getNextUpcomingStage(now)
                if (nextStage != null) {
                    val batch = batchDao.getBatchByIdOnce(nextStage.batchId)
                    val batchName = batch?.name ?: ""
                    nextStage.plannedEndTime?.let { plannedEndTime ->
                        _nextEvent.postValue(
                            Triple("$batchName: ${nextStage.name}", plannedEndTime, nextStage.batchId)
                        )
                    } ?: _nextEvent.postValue(null)
                } else {
                    _nextEvent.postValue(null)
                }

                // Изменено на вызов batchDao с новым методом
                val recentStages = batchDao.getRecentCompletedStagesWithBatchNames(3)
                _recentCompletedStages.postValue(recentStages ?: emptyList())
            } catch (e: Exception) {
                Log.e("Dashboard", "Error refreshing dashboard", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }


    // Добавлен: Аналогичный вспомогательный метод checkAndCompleteBatch для синхронизации состояния партии.
    // Этот метод нужен для SharedViewModel, чтобы в случае, если обновления партии происходят через этот ViewModel,
    // состояние isActive тоже проверялось и обновлялось. Хотя основная логика в BatchListViewModel,
    // этот метод обеспечивает целостность данных, если SharedViewModel используется для CRUD операций.
    // Вызывается после операций, которые могут повлиять на состояние этапов, например, после updateBatch или удаления/обновления этапов.
    // Метод проверяет все этапы и, если нужно, завершает партию.
    // Фикс: Используем batchDao.getStagesForBatchOnce (новый метод).
    // Явная типизация для Lambda.
    fun checkAndCompleteBatch(batchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stages = batchDao.getStagesForBatchOnce(batchId)
                val allCompleted = stages.all { it: Stage -> it.endTime != null }
                if (allCompleted) {
                    val batch = repository.getBatchByIdOnce(batchId)
                    if (batch != null && batch.isActive) {
                        repository.updateBatch(batch.copy(isActive = false))
                        // После обновления партии, обновляем dashboard данные, если они зависят от активности
                        refreshDashboardData()
                    }
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error in checkAndCompleteBatch", e)
            }
        }
    }







    // --- CRUD операции ---
    fun addBatch(batch: Batch) = viewModelScope.launch { repository.addBatch(batch) }

    // Изменение: В updateBatch добавлен вызов checkAndCompleteBatch, если isActive истинно,
    // чтобы синхронизировать состояние после любого обновления партии.
    // Это обеспечивает, что если этапы были завершены ранее, но isActive не был сброшен,
    // то после update (например, редактирования notes) будет выполнена проверка.
    fun updateBatch(batch: Batch) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateBatch(batch)
        // Синхронизируем состояние: проверяем, нужно ли завершить партию после обновления
        if (batch.isActive) {
            checkAndCompleteBatch(batch.id)
        }
    }

    // Изменение: Аналогично, в deleteStage добавлен вызов, чтобы после удаления этапа проверить завершенность.
    // Фикс: Добавлен полный launch с suspend, чтобы получить stage и проверить batchId.
    // Используем новый метод stageDao.getStageById для получения Stage.
    fun deleteStage(stageId: String) = viewModelScope.launch(Dispatchers.IO) {
        val stage = stageDao.getStageById(stageId)
        repository.deleteStage(stageId)
        stage?.let { checkAndCompleteBatch(it.batchId) }
    }

    fun deleteBatch(batchId: String) = viewModelScope.launch { repository.deleteBatch(batchId) }

    fun createBatchWithStages(batch: Batch, stages: List<Stage>) =
        viewModelScope.launch { repository.insertBatchWithStages(batch, stages) }

    fun addStage(stage: Stage) = viewModelScope.launch { repository.addStage(stage) }


    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }

    fun getStagesForBatchLive(batchId: String): LiveData<List<Stage>> {
        return batchDao.getStagesForBatch(batchId)
    }
}