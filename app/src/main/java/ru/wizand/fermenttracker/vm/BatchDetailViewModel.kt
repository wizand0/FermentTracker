package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.*
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchDetailViewModel(
    application: Application,
    private val batchId: String
) : AndroidViewModel(application) {

    private val repository: BatchRepository

    val batch: LiveData<Batch?>
    val stages: LiveData<List<Stage>>
    val photos: LiveData<List<Photo>>
    val logs: LiveData<List<BatchLog>>

    init {
        val batchDao = AppDatabase.getInstance(application).batchDao()
        repository = BatchRepository(batchDao, application)
        batch = repository.getBatchById(batchId)
        stages = repository.getStages(batchId)
        photos = repository.getPhotos(batchId)
        logs = repository.getLogs(batchId)
    }

    fun updateBatch(batch: Batch) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateBatch(batch)
    }

    fun addStage(stage: Stage) = viewModelScope.launch(Dispatchers.IO) {
        repository.addStage(stage)
    }

    fun updateStage(stage: Stage) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateStage(stage)
    }

    fun addPhoto(photo: Photo) = viewModelScope.launch(Dispatchers.IO) {
        repository.addPhoto(photo)
    }

    fun addLog(log: BatchLog) = viewModelScope.launch(Dispatchers.IO) {
        repository.addLog(log)
    }

    // Добавьте этот метод:
    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }

    class Factory(
        private val application: Application,
        private val batchId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BatchDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BatchDetailViewModel(application, batchId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}