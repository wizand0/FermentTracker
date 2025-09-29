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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
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
import androidx.lifecycle.lifecycleScope

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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        initializeTemplates()

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
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
                    Snackbar.make(binding.root, "Ошибка при backup: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun performRestore(sourceUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.closeInstance()
                val dbFile = getDatabasePath("ferment_tracker_db")
                dbFile.parentFile?.mkdirs()
                FileUtils.copyUriToFile(sourceUri, dbFile, this@MainActivity)
                AppDatabase.getInstance(applicationContext)
                runOnUiThread {
                    Snackbar.make(binding.root, "База успешно восстановлена. Перезапустите приложение.", Snackbar.LENGTH_LONG)
                        .setAction("Перезапустить") {
                            val intent = packageManager.getLaunchIntentForPackage(packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        }.show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Snackbar.make(binding.root, "Ошибка при restore: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
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
}
