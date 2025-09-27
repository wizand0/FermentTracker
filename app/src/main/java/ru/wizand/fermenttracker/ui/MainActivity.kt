package ru.wizand.fermenttracker.ui

import android.Manifest
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
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.databinding.ActivityMainBinding
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.utils.FileUtils
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // launcher для множественных разрешений (camera & notification для Android13+ и storage legacy)
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

    // SAF: CreateDocument launcher (экспорт базы)
    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            performBackup(uri)
        } else {
            // пользователь отменил
        }
    }

    // SAF: OpenDocument launcher (импорт базы)
    private val openDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            performRestore(uri)
        } else {
            // отмена
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // seed DB ако нужно
        seedRecipesIfNeeded()

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
            R.id.menu_export_db -> {
                // Запрос имени файла, default "ferment_backup.db"
                createDocumentLauncher.launch("ferment_tracker_backup_${System.currentTimeMillis()}.db")
                true
            }
            R.id.menu_import_db -> {
                // Разрешаем выбрать любой файл
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

    private fun seedRecipesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(applicationContext).batchDao()
            try {
                val list = dao.getAllRecipes()
                if (list.isEmpty()) {
                    val templates = RecipeTemplates.getTemplateStages(applicationContext)
                    templates.forEach { (type, stageList) ->
                        val recipe = Recipe(type = type, ingredients = "", note = "")
                        dao.insertRecipe(recipe)
                        stageList.forEachIndexed { index, stage ->
                            val template = StageTemplate(
                                recipeType = type,
                                name = stage.name,
                                durationHours = stage.durationHours,
                                orderIndex = index
                            )
                            dao.insertStageTemplate(template)
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore seed errors
            }
        }
    }

    /**
     * Выполняет backup БД -> выбранный URI (SAF). Работает в IO-потоке.
     */
    private fun performBackup(targetUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // path to DB file
                val dbFile = getDatabasePath("ferment_tracker_db")
                if (!dbFile.exists()) {
                    runOnUiThread {
                        Snackbar.make(binding.root, "Файл базы не найден", Snackbar.LENGTH_LONG).show()
                    }
                    return@launch
                }

                FileUtils.copyFileToUri(dbFile, targetUri, this@MainActivity)

                runOnUiThread {
                    Snackbar.make(binding.root, "Резервная копия создана", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(binding.root, "Ошибка при backup: ${e.message ?: e.toString()}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Выполняет restore из выбранного URI -> файл БД приложения.
     * Закрывает текущую инстанцию DB перед заменой файла.
     */
    private fun performRestore(sourceUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // close Room instance to allow overwrite
                AppDatabase.closeInstance()

                val dbFile = getDatabasePath("ferment_tracker_db")
                // ensure parent exists
                dbFile.parentFile?.mkdirs()

                // copy selected file into DB location
                FileUtils.copyUriToFile(sourceUri, dbFile, this@MainActivity)

                // recreate DB instance (getInstance will re-open DB)
                AppDatabase.getInstance(applicationContext)

                runOnUiThread {
                    Snackbar.make(binding.root, "База успешно восстановлена. Перезапустите приложение для полной корректности.", Snackbar.LENGTH_LONG)
                        .setAction("Перезапустить") {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        }.show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(binding.root, "Ошибка при restore: ${e.message ?: e.toString()}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
