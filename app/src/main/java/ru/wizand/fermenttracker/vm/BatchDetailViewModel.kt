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
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchDetailViewModel(application: Application, private val batchId: String) : AndroidViewModel(application) {
    private val repository = BatchRepository(AppDatabase.getInstance(application).batchDao())
    val batch: LiveData<Batch?> = repository.getBatchById(batchId)
    val stages: LiveData<List<Stage>> = repository.getStages(batchId)

    fun updateStage(stage: Stage) {
        viewModelScope.launch {
            repository.updateStage(stage)
        }
    }

    fun addPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.addPhoto(photo)
        }
    }

    fun getPhotosForStage(stageId: String): LiveData<List<Photo>> = repository.getPhotos(stageId)

    class Factory(private val app: Application, private val batchId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BatchDetailViewModel(app, batchId) as T
        }
    }
}