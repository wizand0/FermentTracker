package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.repository.BatchRepository

class BatchListViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).batchDao()
    val batches: LiveData<List<Batch>> = dao.getAllBatches()

    fun createBatch(batch: Batch) = viewModelScope.launch { dao.insertBatch(batch) }
}
