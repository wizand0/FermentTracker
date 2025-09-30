package ru.wizand.fermenttracker.ui

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.databinding.ActivityMainBinding
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.data.repository.BatchRepository
import ru.wizand.fermenttracker.utils.FileUtils
import java.io.File
import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import ru.wizand.fermenttracker.utils.BatteryOptimizationHelper
import ru.wizand.fermenttracker.utils.ImageUtils
import ru.wizand.fermenttracker.utils.NotificationHelper
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val multiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val denied = result.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            Snackbar.make(
                binding.root,
                "Некоторые разрешения не предоставлены — функционал ограничен",
                Snackbar.LENGTH_LONG
            ).setAction("Настройки") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
            }.show()
        }
    }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) performBackup(uri)
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) performRestore(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initializeTemplates()

        // Обработка открытия батча из уведомления
        handleNotificationIntent(intent)

        checkBatteryOptimization()


        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAdd) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, systemBars.right, systemBars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabScanQr) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, systemBars.right, systemBars.bottom + 64)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ensureCriticalPermissions()

        binding.fabAdd.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_createBatch)
        }

        binding.fabScanQr.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_qr)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_edit_recipes -> {
                startActivity(Intent(this, RecipeEditorActivity::class.java))
                true
            }
            R.id.menu_export_db -> {
                createDocumentLauncher.launch("ferment_tracker_backup_${System.currentTimeMillis()}.db")
                true
            }
            R.id.menu_import_db -> {
                openDocumentLauncher.launch(arrayOf("*/*"))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun ensureCriticalPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) multiplePermissionsLauncher.launch(perms.toTypedArray())
    }

    private fun performBackup(targetUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dbFile = getDatabasePath("ferment_tracker_db")

                // Проверяем существование и доступность файла БД
                when {
                    !dbFile.exists() -> {
                        showError("База данных не найдена")
                        return@launch
                    }
                    !dbFile.canRead() -> {
                        showError("Нет доступа на чтение базы данных")
                        return@launch
                    }
                    dbFile.length() == 0L -> {
                        showError("Файл базы данных пуст")
                        return@launch
                    }
                }

                // Проверяем свободное место (нужно минимум столько же, сколько занимает БД + 10% запас)
                val requiredSpace = (dbFile.length() * 1.1).toLong()
                val availableSpace = dbFile.parentFile?.usableSpace ?: 0L
                if (availableSpace < requiredSpace) {
                    showError("Недостаточно свободного места (требуется ${requiredSpace / 1024 / 1024} МБ)")
                    return@launch
                }

                // Закрываем БД перед копированием для консистентности
                AppDatabase.closeInstance()

                // Выполняем копирование
                FileUtils.copyFileToUri(dbFile, targetUri, this@MainActivity)

                // Переоткрываем БД
                AppDatabase.getInstance(applicationContext)

                showSuccess("Резервная копия создана (${dbFile.length() / 1024} КБ)")

            } catch (e: FileNotFoundException) {
                showError("Файл не найден: ${e.message}")
            } catch (e: SecurityException) {
                showError("Нет прав доступа к файлу")
            } catch (e: IOException) {
                showError("Ошибка ввода-вывода: ${e.message}")
            } catch (e: Exception) {
                showError("Неизвестная ошибка при создании резервной копии: ${e.message}")
            } finally {
                // Гарантируем, что БД будет переоткрыта
                try {
                    AppDatabase.getInstance(applicationContext)
                } catch (e: Exception) {
                    showError("Критическая ошибка: не удалось переоткрыть базу данных")
                }
            }
        }
    }

    private fun performRestore(sourceUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            var backupFile: File? = null
            try {
                // Проверяем размер исходного файла
                val fileSize = contentResolver.openFileDescriptor(sourceUri, "r")?.use { fd ->
                    fd.statSize
                } ?: 0L

                when {
                    fileSize == 0L -> {
                        showError("Файл резервной копии пуст или недоступен")
                        return@launch
                    }
                    fileSize < 4096 -> { // SQLite database минимум ~4KB
                        showError("Файл резервной копии слишком мал (${fileSize} байт). Возможно, файл поврежден")
                        return@launch
                    }
                }

                val dbFile = getDatabasePath("ferment_tracker_db")

                // Проверяем свободное место
                val requiredSpace = (fileSize * 1.5).toLong() // дополнительное место для временной копии
                val availableSpace = dbFile.parentFile?.usableSpace ?: 0L
                if (availableSpace < requiredSpace) {
                    showError("Недостаточно свободного места (требуется ${requiredSpace / 1024 / 1024} МБ)")
                    return@launch
                }

                // Создаем резервную копию текущей БД
                backupFile = File(dbFile.parentFile, "ferment_tracker_db.backup")
                if (dbFile.exists()) {
                    try {
                        dbFile.copyTo(backupFile, overwrite = true)
                    } catch (e: Exception) {
                        showError("Не удалось создать резервную копию текущей БД: ${e.message}")
                        return@launch
                    }
                }

                // Закрываем текущую БД
                AppDatabase.closeInstance()

                // Создаем директорию если нужно
                dbFile.parentFile?.mkdirs()

                // Копируем новую БД
                FileUtils.copyUriToFile(sourceUri, dbFile, this@MainActivity)

                // Проверяем целостность восстановленной БД
                val verificationResult = verifyDatabase()

                if (!verificationResult.isValid) {
                    // Восстанавливаем старую БД
                    if (backupFile?.exists() == true) {
                        backupFile.copyTo(dbFile, overwrite = true)
                    }
                    AppDatabase.getInstance(applicationContext)
                    showError("Восстановление не удалось: ${verificationResult.errorMessage}\nСтарая база данных восстановлена")
                    return@launch
                }

                // Удаляем временную резервную копию
                backupFile?.delete()

                // Переоткрываем БД
                AppDatabase.getInstance(applicationContext)

                showRestoreSuccess(verificationResult.batchCount)

            } catch (e: FileNotFoundException) {
                restoreBackup(backupFile, getDatabasePath("ferment_tracker_db"))
                showError("Файл не найден: ${e.message}")
            } catch (e: SecurityException) {
                restoreBackup(backupFile, getDatabasePath("ferment_tracker_db"))
                showError("Нет прав доступа к файлу")
            } catch (e: IOException) {
                restoreBackup(backupFile, getDatabasePath("ferment_tracker_db"))
                showError("Ошибка ввода-вывода: ${e.message}")
            } catch (e: Exception) {
                restoreBackup(backupFile, getDatabasePath("ferment_tracker_db"))
                showError("Неизвестная ошибка при восстановлении: ${e.message}")
            } finally {
                // Гарантируем, что БД будет переоткрыта
                try {
                    AppDatabase.getInstance(applicationContext)
                } catch (e: Exception) {
                    showError("Критическая ошибка: не удалось переоткрыть базу данных")
                }
                // Очищаем временный файл
                backupFile?.delete()
            }
        }
    }

    /**
     * Восстанавливает резервную копию БД в случае ошибки
     */
    private fun restoreBackup(backupFile: File?, targetFile: File) {
        try {
            if (backupFile?.exists() == true) {
                AppDatabase.closeInstance()
                backupFile.copyTo(targetFile, overwrite = true)
                AppDatabase.getInstance(applicationContext)
            }
        } catch (e: Exception) {
            // Логируем, но не показываем пользователю, чтобы не перегружать информацией
            e.printStackTrace()
        }
    }

    /**
     * Результат проверки целостности базы данных
     */
    private data class DatabaseVerificationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val batchCount: Int = 0
    )

    /**
     * Проверяет целостность базы данных после восстановления
     */
    private fun verifyDatabase(): DatabaseVerificationResult {
        return try {
            val db = AppDatabase.getInstance(applicationContext)

            // Проверяем, что БД открывается и доступна
            val dao = db.batchDao()

            // Проверяем, что можем выполнять запросы
            val batchCount = dao.getBatchCount()

            // Проверяем наличие базовых таблиц
            val recipeCount = dao.getRecipeCount()

            // БД валидна, если она открывается и можно выполнять запросы
            // (даже если данных нет - это может быть новая пустая БД)
            DatabaseVerificationResult(
                isValid = true,
                batchCount = batchCount
            )
        } catch (e: android.database.sqlite.SQLiteDatabaseCorruptException) {
            DatabaseVerificationResult(
                isValid = false,
                errorMessage = "База данных повреждена"
            )
        } catch (e: android.database.sqlite.SQLiteException) {
            DatabaseVerificationResult(
                isValid = false,
                errorMessage = "Ошибка SQLite: ${e.message}"
            )
        } catch (e: Exception) {
            DatabaseVerificationResult(
                isValid = false,
                errorMessage = "Не удалось открыть базу данных: ${e.message}"
            )
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSuccess(message: String) {
        runOnUiThread {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showRestoreSuccess(batchCount: Int) {
        runOnUiThread {
            val message = if (batchCount > 0) {
                "База восстановлена ($batchCount партий)"
            } else {
                "База восстановлена (данные отсутствуют)"
            }

            Snackbar.make(
                binding.root,
                message,
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Перезапустить") {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }.show()
        }
    }

    private fun initializeTemplates() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)
        if (isFirstRun) {
            val repository = BatchRepository(AppDatabase.getInstance(this).batchDao(), this)
            lifecycleScope.launch(Dispatchers.IO) {
                val templates = RecipeTemplates.getTemplateStages(this@MainActivity)
                templates.forEach { (type, pair) ->
                    val recipe = pair.second
                    repository.insertRecipe(recipe)
                    pair.first.forEachIndexed { index, stage ->
                        val template = StageTemplate(
                            recipeType = type,
                            name = stage.name,
                            durationHours = stage.durationHours.toLong(),
                            orderIndex = index
                        )
                        repository.insertStageTemplate(template)
                    }
                }
                prefs.edit().putBoolean("first_run", false).apply()
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            ImageUtils.clearCache()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Убедитесь, что intent не nullable
        setIntent(intent)  // если нужно обновить intent
        handleNotificationIntent(intent)
    }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        intent?.let { handleNotificationIntent(it) }
//    }

    private fun handleNotificationIntent(intent: Intent) {
        intent.getStringExtra("batchId")?.let { batchId ->
            try {
                val navController = findNavController(R.id.nav_host_fragment)

                // Проверяем текущий destination
                val currentDestination = navController.currentDestination?.id

                // Если мы не на BatchListFragment, сначала переходим туда
                if (currentDestination != R.id.batchListFragment) {
                    navController.navigate(R.id.batchListFragment)
                }

                // Используем правильное имя, сгенерированное SafeArgs
                val action = ru.wizand.fermenttracker.ui.batches.BatchListFragmentDirections
                    .actionBatchListToBatchDetail(batchId)  // ← Исправлено имя
                navController.navigate(action)

                Log.d("MainActivity", "Navigated to batch: $batchId from notification")
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error: ${e.message}", e)
                Snackbar.make(
                    binding.root,
                    "Ошибка навигации к партии",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Оптимизация батареи")
                .setMessage("Для надежной работы уведомлений рекомендуется отключить оптимизацию батареи для этого приложения.")
                .setPositiveButton("Настроить") { _, _ ->
                    BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
                }
                .setNegativeButton("Позже", null)
                .show()
        }
    }
}