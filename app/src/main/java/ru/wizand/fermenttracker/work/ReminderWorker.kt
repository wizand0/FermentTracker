package ru.wizand.fermenttracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ru.wizand.fermenttracker.R
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.wizand.fermenttracker.data.db.AppDatabase
import kotlin.random.Random


class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.batchDao()
            // Здесь можно выбрать логику: найти стадии с endTime <= now и показать уведомления
            // Для простоты — отправим тестовое уведомление
            showNotification("CuringTracker", "Reminder: check your batches")
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun showNotification(title: String, body: String) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ct_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification) // добавьте иконку в res/drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        nm.notify(Random.nextInt(), notif)
    }
}