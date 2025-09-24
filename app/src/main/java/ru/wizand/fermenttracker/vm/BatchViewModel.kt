package ru.wizand.fermenttracker.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchViewModel(private val repository: BatchRepository) : ViewModel() {
    fun createBatch(batch: Batch) {
        viewModelScope.launch {
            repository.addBatch(batch)
        }
    }

    fun addStage(stage: Stage) {
        viewModelScope.launch {
            repository.addStage(stage)
        }
    }
}