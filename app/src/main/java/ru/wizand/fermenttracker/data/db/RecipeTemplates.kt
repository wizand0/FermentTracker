package ru.wizand.fermenttracker.data.db

import android.content.Context
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.db.entities.Recipe

object RecipeTemplates {
    fun getTemplateStages(context: Context): Map<String, Pair<List<Stage>, Recipe>> {
        val types = context.resources.getStringArray(R.array.fermentation_types)

        return mapOf(
            // Dry-cured meat (basic)
            types[0] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_salting),
                            durationHours = 36,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_curing_drying),
                            durationHours = 336,
                            orderIndex = 1,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[0],
                        ingredients = context.getString(R.string.ingredients_cured_meat_basic),
                        note = context.getString(R.string.note_cured_meat_basic)
                    )
                    ),

            // Dry-cured meat (spicy variation)
            types[1] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_salting_spices),
                            durationHours = 48,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_drying),
                            durationHours = 336,
                            orderIndex = 1,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[1],
                        ingredients = context.getString(R.string.ingredients_cured_meat_spicy),
                        note = context.getString(R.string.note_cured_meat_spicy)
                    )
                    ),

            // Dry-cured sausage
            types[2] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_grinding_stuffing),
                            durationHours = 18,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_fermentation_drying),
                            durationHours = 144,
                            orderIndex = 1,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[2],
                        ingredients = context.getString(R.string.ingredients_cured_sausage),
                        note = context.getString(R.string.note_cured_sausage)
                    )
                    ),

            // Beer
            types[3] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_brewing_preparation),
                            durationHours = 8,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_primary_fermentation),
                            durationHours = 192,
                            orderIndex = 1,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_conditioning_bottling),
                            durationHours = 216,
                            orderIndex = 2,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[3],
                        ingredients = context.getString(R.string.ingredients_beer),
                        note = context.getString(R.string.note_beer)
                    )
                    ),

            // Wine
            types[4] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_crushing_pressing),
                            durationHours = 9,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_primary_fermentation),
                            durationHours = 144,
                            orderIndex = 1,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_secondary_fermentation_aging),
                            durationHours = 480,
                            orderIndex = 2,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[4],
                        ingredients = context.getString(R.string.ingredients_wine),
                        note = context.getString(R.string.note_wine)
                    )
                    ),

            // Kvass
            types[5] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_steeping_preparation),
                            durationHours = 18,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_fermentation),
                            durationHours = 84,
                            orderIndex = 1,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[5],
                        ingredients = context.getString(R.string.ingredients_kvass),
                        note = context.getString(R.string.note_kvass)
                    )
                    ),

            // Kimchi
            types[6] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_salting),
                            durationHours = 2,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_packing_paste),
                            durationHours = 1,
                            orderIndex = 1,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_fermentation),
                            durationHours = 132,
                            orderIndex = 2,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[6],
                        ingredients = context.getString(R.string.ingredients_kimchi),
                        note = context.getString(R.string.note_kimchi)
                    )
                    ),

            // Sauerkraut
            types[7] to (
                    listOf(
                        Stage(
                            name = context.getString(R.string.stage_salting_mixing),
                            durationHours = 2,
                            orderIndex = 0,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_fermentation),
                            durationHours = 240,
                            orderIndex = 1,
                            batchId = ""
                        ),
                        Stage(
                            name = context.getString(R.string.stage_storage),
                            durationHours = 336,
                            orderIndex = 2,
                            batchId = ""
                        )
                    ) to Recipe(
                        type = types[7],
                        ingredients = context.getString(R.string.ingredients_sauerkraut),
                        note = context.getString(R.string.note_sauerkraut)
                    )
                    ),

            // Other (custom)
            types[8] to (emptyList<Stage>() to Recipe(type = types[8], ingredients = "", note = ""))
        )
    }
}
