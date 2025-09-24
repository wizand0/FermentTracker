package ru.wizand.fermenttracker.data.db

import android.content.Context
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Stage

object RecipeTemplates {
    fun getTemplateStages(context: Context): Map<String, List<Stage>> = mapOf(
        context.getString(R.string.product_cured_meat) to listOf(
            Stage(name = "Salting", durationHours = 24, orderIndex = 0, batchId = null),
            Stage(name = "Curing", durationHours = 168, orderIndex = 1, batchId = null)
        ),
        context.getString(R.string.product_cured_sausage) to listOf(
            Stage(name = "Grinding", durationHours = 12, orderIndex = 0, batchId = null),
            Stage(name = "Fermentation", durationHours = 48, orderIndex = 1, batchId = null)
        ),
        context.getString(R.string.product_cheese) to listOf(
            Stage(name = "Curdling", durationHours = 6, orderIndex = 0, batchId = null),
            Stage(name = "Aging", durationHours = 720, orderIndex = 1, batchId = null)
        ),
        context.getString(R.string.product_sauerkraut) to listOf(
            Stage(name = "Fermentation", durationHours = 168, orderIndex = 0, batchId = null),
            Stage(name = "Storage", durationHours = 240, orderIndex = 1, batchId = null)
        ),
        context.getString(R.string.product_kombucha) to listOf(
            Stage(name = "First Fermentation", durationHours = 168, orderIndex = 0, batchId = null),
            Stage(name = "Bottling", durationHours = 48, orderIndex = 1, batchId = null)
        ),
        context.getString(R.string.product_pickles) to listOf(
            Stage(name = "Brining", durationHours = 72, orderIndex = 0, batchId = null),
            Stage(name = "Fermentation", durationHours = 168, orderIndex = 1, batchId = null)
        )
    )
}