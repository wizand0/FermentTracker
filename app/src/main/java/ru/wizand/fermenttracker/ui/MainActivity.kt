package ru.wizand.fermenttracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import androidx.navigation.findNavController
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.databinding.ActivityMainBinding
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate
import ru.wizand.fermenttracker.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                // Пользователь отказал — покажем Snackbar с переходом в настройки
                Snackbar.make(
                    binding.root,
                    "Уведомления отключены. Включите их в настройках",
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Added: seed recipes from templates if DB empty
        seedRecipesIfNeeded()

        // ✅ Проверка разрешения только для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.fabAdd.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_createBatch)
        }

        binding.fabScanQr.setOnClickListener {
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_batchList_to_qr)
        }
    }

    private fun seedRecipesIfNeeded() {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(applicationContext).batchDao()
            if (dao.getAllRecipes().isEmpty()) {
                val templates = RecipeTemplates.getTemplateStages(applicationContext)
                templates.forEach { (type, stageList) ->
                    val recipe = Recipe(type = type, ingredients = "", note = "") // Default empty ingredients/note
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
        }
    }
}