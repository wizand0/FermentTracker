package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchDetailViewModel(application: Application, private val batchId: String) : AndroidViewModel(application) {
    private val repository = BatchRepository(AppDatabase.getInstance(application).batchDao())
    val batch: LiveData<Batch?> = repository.getBatchById(batchId)
    val stages: LiveData<List<Stage>> = repository.getStages(batchId)
    val photos: LiveData<List<Photo>> = repository.getPhotos(batchId)
    val logs: LiveData<List<BatchLog>> = repository.getLogs(batchId)

    fun updateBatch(batch: Batch) {
        viewModelScope.launch {
            repository.updateBatch(batch)
        }
    }

    fun deleteBatch() {
        viewModelScope.launch {
            repository.deleteBatch(batchId)
        }
    }

    fun addStage(stage: Stage) {
        viewModelScope.launch {
            repository.addStage(stage.copy(batchId = batchId))
        }
    }

    fun updateStage(stage: Stage) {
        viewModelScope.launch {
            repository.updateStage(stage)
        }
    }

    fun deleteStage(stageId: String) {
        viewModelScope.launch {
            repository.deleteStage(stageId)
        }
    }

    fun addPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.addPhoto(photo)
        }
    }

    fun addLog(log: BatchLog) {
        viewModelScope.launch {
            repository.addLog(log)
        }
    }

    class Factory(private val application: Application, private val batchId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BatchDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BatchDetailViewModel(application, batchId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}