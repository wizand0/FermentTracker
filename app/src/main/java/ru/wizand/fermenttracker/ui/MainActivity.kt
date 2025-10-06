package ru.wizand.fermenttracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.data.db.RecipeTemplates
import ru.wizand.fermenttracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // === Добавляем шаблоны рецептов при первом запуске ===
        lifecycleScope.launch(Dispatchers.IO) {   // <-- добавили IO
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
}
