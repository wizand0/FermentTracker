package ru.wizand.fermenttracker.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.wizand.fermenttracker.utils.NotificationHelper

class StageNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val stageName = inputData.getString("stageName") ?: return Result.failure()
        val batchName = inputData.getString("batchName") ?: "Партия"
        val batchId = inputData.getString("batchId") ?: return Result.failure()
        val notificationId = inputData.getInt("notificationId", 0)

        Log.d("StageNotificationWorker", "Notification triggered for stage: $stageName, batchId: $batchId, notificationId: $notificationId")

        NotificationHelper.showNotification(
            context,
            "Этап завершён",
            "Этап \"$stageName\" в партии \"$batchName\" подошёл к концу",
            notificationId,
            batchId  // Добавлен недостающий параметр
        )

        return Result.success()
    }
}