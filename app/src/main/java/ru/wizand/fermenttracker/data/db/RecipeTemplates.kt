package ru.wizand.fermenttracker.data.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Recipe
import ru.wizand.fermenttracker.data.db.entities.StageTemplate

object RecipeTemplates {

    fun getTemplateStages(context: Context): Map<String, Pair<List<StageTemplate>, Recipe>> {
        val types = context.resources.getStringArray(R.array.fermentation_types)

        return mapOf(
            // Dry-cured meat (basic)
            types[0] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_meat_preparation), durationHours = 6, orderIndex = 0, recipeType = types[0]),
                        StageTemplate(name = context.getString(R.string.stage_salting), durationHours = 144, orderIndex = 1, recipeType = types[0]),
                        StageTemplate(name = context.getString(R.string.stage_drying_spices), durationHours = 48, orderIndex = 2, recipeType = types[0]),
                        StageTemplate(name = context.getString(R.string.stage_drying_curing), durationHours = 336, orderIndex = 3, recipeType = types[0])
                    ) to Recipe(
                        type = types[0],
                        ingredients = context.getString(R.string.ingredients_cured_meat_basic),
                        note = context.getString(R.string.note_cured_meat_basic)
                    )
                    ),

            // Dry-cured meat (spicy)
            types[1] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_meat_preparation), durationHours = 6, orderIndex = 0, recipeType = types[1]),
                        StageTemplate(name = context.getString(R.string.stage_salting_spices), durationHours = 48, orderIndex = 1, recipeType = types[1]),
                        StageTemplate(name = context.getString(R.string.stage_drying), durationHours = 336, orderIndex = 2, recipeType = types[1])
                    ) to Recipe(
                        type = types[1],
                        ingredients = context.getString(R.string.ingredients_cured_meat_spicy),
                        note = context.getString(R.string.note_cured_meat_spicy)
                    )
                    ),

            // Dry-cured sausage
            types[2] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_grinding_stuffing), durationHours = 12, orderIndex = 0, recipeType = types[2]),
                        StageTemplate(name = context.getString(R.string.stage_warm_fermentation), durationHours = 72, orderIndex = 1, recipeType = types[2]),
                        StageTemplate(name = context.getString(R.string.stage_drying_curing), durationHours = 720, orderIndex = 2, recipeType = types[2])
                    ) to Recipe(
                        type = types[2],
                        ingredients = context.getString(R.string.ingredients_cured_sausage),
                        note = context.getString(R.string.note_cured_sausage)
                    )
                    ),

            // Beer
            types[3] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_mash), durationHours = 2, orderIndex = 0, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_lauter_sparge), durationHours = 1, orderIndex = 1, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_boil), durationHours = 2, orderIndex = 2, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_cooling_pitching), durationHours = 4, orderIndex = 3, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_primary_fermentation), durationHours = 168, orderIndex = 4, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_conditioning), durationHours = 168, orderIndex = 5, recipeType = types[3]),
                        StageTemplate(name = context.getString(R.string.stage_bottling_carbonation), durationHours = 72, orderIndex = 6, recipeType = types[3])
                    ) to Recipe(
                        type = types[3],
                        ingredients = context.getString(R.string.ingredients_beer),
                        note = context.getString(R.string.note_beer)
                    )
                    ),

            // Wine
            types[4] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_crushing_pressing), durationHours = 12, orderIndex = 0, recipeType = types[4]),
                        StageTemplate(name = context.getString(R.string.stage_primary_fermentation), durationHours = 192, orderIndex = 1, recipeType = types[4]),
                        StageTemplate(name = context.getString(R.string.stage_secondary_fermentation), durationHours = 720, orderIndex = 2, recipeType = types[4]),
                        StageTemplate(name = context.getString(R.string.stage_aging), durationHours = 1512, orderIndex = 3, recipeType = types[4])
                    ) to Recipe(
                        type = types[4],
                        ingredients = context.getString(R.string.ingredients_wine),
                        note = context.getString(R.string.note_wine)
                    )
                    ),

            // Kvass
            types[5] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_steeping_preparation), durationHours = 12, orderIndex = 0, recipeType = types[5]),
                        StageTemplate(name = context.getString(R.string.stage_fermentation), durationHours = 72, orderIndex = 1, recipeType = types[5])
                    ) to Recipe(
                        type = types[5],
                        ingredients = context.getString(R.string.ingredients_kvass),
                        note = context.getString(R.string.note_kvass)
                    )
                    ),

            // Kimchi
            types[6] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_salting), durationHours = 2, orderIndex = 0, recipeType = types[6]),
                        StageTemplate(name = context.getString(R.string.stage_packing_paste), durationHours = 1, orderIndex = 1, recipeType = types[6]),
                        StageTemplate(name = context.getString(R.string.stage_fermentation), durationHours = 132, orderIndex = 2, recipeType = types[6])
                    ) to Recipe(
                        type = types[6],
                        ingredients = context.getString(R.string.ingredients_kimchi),
                        note = context.getString(R.string.note_kimchi)
                    )
                    ),

            // Sauerkraut
            types[7] to (
                    listOf(
                        StageTemplate(name = context.getString(R.string.stage_salting_mixing), durationHours = 2, orderIndex = 0, recipeType = types[7]),
                        StageTemplate(name = context.getString(R.string.stage_fermentation), durationHours = 240, orderIndex = 1, recipeType = types[7]),
                        StageTemplate(name = context.getString(R.string.stage_storage), durationHours = 336, orderIndex = 2, recipeType = types[7])
                    ) to Recipe(
                        type = types[7],
                        ingredients = context.getString(R.string.ingredients_sauerkraut),
                        note = context.getString(R.string.note_sauerkraut)
                    )
                    ),

            // Other (custom)
            types[8] to (emptyList<StageTemplate>() to Recipe(type = types[8], ingredients = "", note = ""))
        )
    }

    // ✅ Сохранение шаблонов в базу данных
    suspend fun saveToDatabase(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val dao = db.batchDao()
        val templates = getTemplateStages(context)

        templates.forEach { (_, pair) ->
            val (stages, recipe) = pair
            dao.insertRecipe(recipe)
            stages.forEach { dao.insertStageTemplate(it) }
        }
    }
}
