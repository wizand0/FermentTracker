package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Stage

class BatchDetailViewModel(application: Application, private val batchId: String) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).batchDao()
    val batch: LiveData<Batch?> = dao.getBatchById(batchId)
    val stages: LiveData<List<Stage>> = dao.getStagesForBatch(batchId)

    class Factory(private val app: Application, private val batchId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BatchDetailViewModel(app, batchId) as T
        }
    }
}