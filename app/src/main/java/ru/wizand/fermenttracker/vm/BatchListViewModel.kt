package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).batchDao()
    private val repository = BatchRepository(dao)
    val batches: LiveData<List<Batch>> = repository.allBatches

    // MediatorLiveData for batch progress (unchanged)
    val batchProgress: LiveData<Map<String, Int>> = MediatorLiveData<Map<String, Int>>().apply {
        addSource(batches) { batchList ->
            val progressMap = mutableMapOf<String, Int>()
            batchList.forEach { batch ->
                val stagesLiveData = repository.getStages(batch.id)
                addSource(stagesLiveData) { stages ->
                    val completed = stages.count { it.endTime != null }
                    val total = stages.size
                    progressMap[batch.id] = if (total > 0) (completed * 100 / total) else 0
                    value = progressMap.toMap()
                }
            }
        }
    }

    // LiveData to signal batch creation status
    private val _batchCreationStatus = MutableLiveData<Result<Unit>>()
    val batchCreationStatus: LiveData<Result<Unit>> get() = _batchCreationStatus

    fun createBatch(batch: Batch) = viewModelScope.launch {
        try {
            repository.addBatch(batch)
            _batchCreationStatus.postValue(Result.success(Unit))
        } catch (e: Exception) {
            _batchCreationStatus.postValue(Result.failure(e))
        }
    }

    fun addStage(stage: Stage) = viewModelScope.launch {
        try {
            repository.addStage(stage)
        } catch (e: Exception) {
            android.util.Log.e("BatchListViewModel", "Error inserting stage", e)
        }
    }

    fun updateStage(stage: Stage) {
        viewModelScope.launch {
            repository.updateStage(stage)
        }
    }

    fun deleteBatch(batch: Batch) {
        viewModelScope.launch {
            try {
                repository.deleteBatch(batch)
            } catch (e: Exception) {
                android.util.Log.e("BatchListViewModel", "Error deleting batch", e)
            }
        }
    }

    fun getBatchById(batchId: String): LiveData<Batch?> = repository.getBatchById(batchId)
    fun getStagesForBatch(batchId: String): LiveData<List<Stage>> = repository.getStages(batchId)
    fun getPhotosForStage(stageId: String): LiveData<List<Photo>> = repository.getPhotos(stageId)
}