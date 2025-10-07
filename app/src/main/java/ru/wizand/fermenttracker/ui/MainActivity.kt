package ru.wizand.fermenttracker.ui

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var progressDialog: ProgressDialog? = null

    companion object {
        private const val CREATE_FILE_REQUEST_CODE = 1001
        private const val PICK_FILE_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Применение настройки темной темы
        applyDarkThemeSetting()

        // Toolbar
        setSupportActionBar(binding.toolbar)

        // Навигация
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Верхняя панель
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.dashboardFragment,
                R.id.batchListFragment,
                R.id.recipesFragment,
                R.id.settingsFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Добавляем слушатель для навигации, чтобы управлять видимостью нижней панели
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.dashboardFragment,
                R.id.batchListFragment,
                R.id.recipesFragment,
                R.id.settingsFragment -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }

        // Устанавливаем отступ снизу для фрагментов
        binding.navHostFragment.post {
            val bottomNavHeight = binding.bottomNavigation.height
            binding.navHostFragment.setPadding(
                binding.navHostFragment.paddingLeft,
                binding.navHostFragment.paddingTop,
                binding.navHostFragment.paddingRight,
                bottomNavHeight
            )
        }

        // === Добавляем шаблоны рецептов при первом запуске ===
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(this@MainActivity)
            val dao = db.batchDao()

            val recipeCount = dao.getRecipeCount()
            if (recipeCount == 0) {
                RecipeTemplates.saveToDatabase(this@MainActivity)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Применение настройки темной темы
    private fun applyDarkThemeSetting() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkTheme = sharedPrefs.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    // Метод для экспорта базы данных
    fun performBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, "ferment_tracker_backup.db")
        }
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE)
    }

    // Метод для импорта базы данных
    fun performRestore() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream"
        }
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CREATE_FILE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        lifecycleScope.launch {
                            try {
                                // Показываем диалог в главном потоке
                                withContext(Dispatchers.Main) {
                                    showProgressDialog("Экспорт базы данных...")
                                }

                                // Выполняем экспорт в фоновом потоке
                                withContext(Dispatchers.IO) {
                                    exportDatabase(uri)
                                }

                                // Скрываем диалог и показываем сообщение в главном потоке
                                withContext(Dispatchers.Main) {
                                    hideProgressDialog()
                                    Toast.makeText(this@MainActivity, "База данных успешно экспортирована", Toast.LENGTH_SHORT).show()
                                    Log.d("DB", "База данных успешно экспортирована")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    hideProgressDialog()
                                    Toast.makeText(this@MainActivity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.d("DB", "Ошибка экспорта: ${e.message}")
                                }
                            }
                        }
                    }
                }
                PICK_FILE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        lifecycleScope.launch {
                            try {
                                // Показываем диалог в главном потоке
                                withContext(Dispatchers.Main) {
                                    showProgressDialog("Импорт базы данных...")
                                }

                                // Выполняем импорт в фоновом потоке
                                withContext(Dispatchers.IO) {
                                    importDatabase(uri)
                                }

                                // Скрываем диалог и показываем сообщение в главном потоке
                                withContext(Dispatchers.Main) {
                                    hideProgressDialog()
                                    Toast.makeText(this@MainActivity, "База данных успешно импортирована", Toast.LENGTH_SHORT).show()
                                    Log.d("DB", "База данных успешно импортирована")
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    hideProgressDialog()
                                    Toast.makeText(this@MainActivity, "Ошибка импорта: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.d("DB", "Ошибка импорта: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun exportDatabase(uri: Uri) {
        // Получаем путь к файлу базы данных
        val dbFile = getDatabasePath("ferment_tracker_db")

        if (!dbFile.exists()) {
            throw IOException("Файл базы данных не найден")
        }

        val inputStream = contentResolver.openInputStream(Uri.fromFile(dbFile))
        val outputStream = contentResolver.openOutputStream(uri)

        inputStream?.use { input ->
            outputStream?.use { output ->
                input.copyTo(output)
            }
        }
    }

    private suspend fun importDatabase(uri: Uri) {
        val dbFile = getDatabasePath("ferment_tracker_db")

        // Закрываем текущую базу данных
        AppDatabase.closeInstance()

        // Копируем новый файл базы данных
        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(dbFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        // Пересоздаем экземпляр базы данных
        AppDatabase.getInstance(this)
    }

    private fun showProgressDialog(message: String) {
        progressDialog = ProgressDialog(this).apply {
            setMessage(message)
            setCancelable(false)
            show()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}