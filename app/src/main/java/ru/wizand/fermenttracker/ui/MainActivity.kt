package ru.wizand.fermenttracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAdd.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_createBatch)
        }

        binding.fabScanQr.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_qr)
        }

        // Пример регистрации периодического воркера (раскомментируйте и импортируйте WorkManager,
        // если хотите включить напоминания)
        /*
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("ct_reminders", ExistingPeriodicWorkPolicy.KEEP, work)
        */
    }
}