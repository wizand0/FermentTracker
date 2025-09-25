package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BatchRepository(AppDatabase.getInstance(application).batchDao())
    val batches: LiveData<List<Batch>> = repository.allBatches

    fun createBatchWithStages(batch: Batch, stages: List<Stage>) {
        viewModelScope.launch {
            repository.addBatch(batch)
            stages.forEach { stage ->
                repository.addStage(stage.copy(batchId = batch.id))
            }
        }
    }

    fun deleteBatch(batch: Batch) {
        viewModelScope.launch {
            repository.deleteBatch(batch.id)
        }
    }

    fun findBatchByQrCode(qrCode: String): Batch? {
        var batch: Batch? = null
        viewModelScope.launch {
            batch = repository.findBatchByQrCode(qrCode)
        }
        return batch
    }
}