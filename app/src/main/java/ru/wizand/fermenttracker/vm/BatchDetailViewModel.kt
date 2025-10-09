package ru.wizand.fermenttracker.vm

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.entities.*
import ru.wizand.fermenttracker.data.repository.BatchRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class BatchDetailViewModel(
    application: Application,
    private val batchId: String
) : AndroidViewModel(application) {

    private val repository: BatchRepository

    val batch: LiveData<Batch?>
    val stages: LiveData<List<Stage>>
    val photos: LiveData<List<Photo>>
    val logs: LiveData<List<BatchLog>>

    // Для управления временными файлами
    private val tempFiles = ConcurrentHashMap<String, Long>()

    // LiveData для состояний и ошибок
    private val _errorState = MutableLiveData<String?>(null)
    val errorState: LiveData<String?> = _errorState

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val batchDao = AppDatabase.getInstance(application).batchDao()
        repository = BatchRepository(batchDao, application)
        batch = repository.getBatchById(batchId)
        stages = repository.getStages(batchId)
        photos = repository.getPhotos(batchId)
        logs = repository.getLogs(batchId)

        // Начинаем очистку старых временных файлов при инициализации
        cleanupTempFiles()
    }

    fun updateBatch(batch: Batch) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.updateBatch(batch)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to update batch: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun addStage(stage: Stage) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.addStage(stage)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to add stage: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun updateStage(stage: Stage) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.updateStage(stage)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to update stage: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun addPhoto(photo: Photo) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.addPhoto(photo)

            // Добавляем путь к файлу в наш список для отслеживания
            photo.filePath?.let { path ->
                tempFiles[path] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to add photo: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun addLog(log: BatchLog) = viewModelScope.launch {
        try {
            _isLoading.value = true
            repository.addLog(log)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to add log: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun scheduleStageNotification(stage: Stage, batch: Batch) {
        repository.scheduleStageNotification(stage, batch)
    }

    suspend fun getRecipeForBatch(productType: String): Recipe? = withContext(Dispatchers.IO) {
        try {
            repository.getRecipeByType(productType)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _errorState.postValue("Failed to get recipe: ${e.message}")
            null
        }
    }

    /**
     * Регистрирует временный файл для последующей очистки.
     */
    fun registerTempFile(filePath: String) {
        tempFiles[filePath] = System.currentTimeMillis()
    }

    /**
     * Очищает временные файлы старше указанного возраста (по умолчанию 24 часа).
     */
    private fun cleanupTempFiles(maxAgeMs: Long = 24 * 60 * 60 * 1000) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val filesToDelete = tempFiles.entries
                    .filter { now - it.value > maxAgeMs }
                    .map { it.key }

                filesToDelete.forEach { path ->
                    try {
                        val file = File(path)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                        tempFiles.remove(path)
                    } catch (e: Exception) {
                        // Просто логируем ошибку, не прерываем процесс
                        android.util.Log.e("BatchDetailVM", "Failed to delete temp file: $path", e)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("BatchDetailVM", "Error in cleanupTempFiles", e)
            }
        }
    }

    // Сброс состояния ошибки
    fun clearError() {
        _errorState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Запускаем окончательную очистку при уничтожении ViewModel
        cleanupTempFiles(0)  // 0 = удалить все временные файлы
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