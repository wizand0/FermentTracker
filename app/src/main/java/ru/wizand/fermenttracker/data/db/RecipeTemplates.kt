package ru.wizand.fermenttracker.data.db

import android.content.Context
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.entities.Stage

object RecipeTemplates {
    fun getTemplateStages(context: Context): Map<String, List<Stage>> {
        val types = context.resources.getStringArray(R.array.fermentation_types)
        return mapOf(
            // Index 0: Dry-cured meat / Сыровяленое мясо (Salting 1-2 days, Curing/Drying 7-21 days; avg. sources: home curing guides)
            types[0] to listOf(
                Stage(name = "Salting", durationHours = 36, orderIndex = 0, batchId = null),  // 1.5 days avg.
                Stage(name = "Curing and Drying", durationHours = 336, orderIndex = 1, batchId = null)  // 14 days avg.
            ),
            // Index 1: Dry-cured sausage / Сыровяленая колбаса (Grinding/Stuffing 12-24h, Fermentation/Drying 2-4 days initial + weeks; avg.)
            types[1] to listOf(
                Stage(name = "Grinding and Stuffing", durationHours = 18, orderIndex = 0, batchId = null),  // 18h avg.
                Stage(name = "Fermentation and Drying", durationHours = 144, orderIndex = 1, batchId = null)  // 6 days avg. initial
            ),
            // Index 2: Beer / Пиво (Brewing 6-12h, Primary Ferment 7-10 days, Conditioning 7-14 days; sources: Clawhammer, Fermentaholics, Reddit)
            types[2] to listOf(
                Stage(name = "Brewing Preparation", durationHours = 8, orderIndex = 0, batchId = null),  // Mashing/Boiling ~8h
                Stage(name = "Primary Fermentation", durationHours = 192, orderIndex = 1, batchId = null),  // 8 days avg.
                Stage(name = "Conditioning and Bottling", durationHours = 216, orderIndex = 2, batchId = null)  // 9 days avg.
            ),
            // Index 3: Wine / Вино (Crushing/Pressing 6-12h, Primary Ferment 5-7 days, Secondary/Aging 14-30 days; sources: Wine Folly, Clemson HGIC)
            types[3] to listOf(
                Stage(name = "Crushing and Pressing", durationHours = 9, orderIndex = 0, batchId = null),  // 9h avg.
                Stage(name = "Primary Fermentation", durationHours = 144, orderIndex = 1, batchId = null),  // 6 days avg.
                Stage(name = "Secondary Fermentation and Aging", durationHours = 480, orderIndex = 2, batchId = null)  // 20 days avg. (can be longer)
            ),
            // Index 4: Kvass / Квас (Steeping/Prep 12-24h, Fermentation 2-5 days; sources: Escarpment Labs, Chelsea Green, Fine Dining Lovers)
            types[4] to listOf(
                Stage(name = "Steeping and Preparation", durationHours = 18, orderIndex = 0, batchId = null),  // 18h avg. bread soak
                Stage(name = "Fermentation", durationHours = 84, orderIndex = 1, batchId = null)  // 3.5 days avg.
            ),
            // Index 5: Kimchi / Кимчи (Salting 1-3h, Packing/Paste 30min-1h, Fermentation 4-7 days; sources: Kitchn, One of Everything, Wyse Guide)
            types[5] to listOf(
                Stage(name = "Salting", durationHours = 2, orderIndex = 0, batchId = null),  // 2h avg.
                Stage(name = "Packing with Paste", durationHours = 1, orderIndex = 1, batchId = null),  // 1h avg.
                Stage(name = "Fermentation", durationHours = 132, orderIndex = 2, batchId = null)  // 5.5 days avg.
            ),
            // Index 6: Sauerkraut / Квашеная капуста (Salting/Mixing 1-2h, Fermentation 7-14 days, Storage; similar to kimchi but longer)
            types[6] to listOf(
                Stage(name = "Salting and Mixing", durationHours = 2, orderIndex = 0, batchId = null),  // 2h avg.
                Stage(name = "Fermentation", durationHours = 240, orderIndex = 1, batchId = null),  // 10 days avg.
                Stage(name = "Storage", durationHours = 336, orderIndex = 2, batchId = null)  // 14 days maturation
            ),
            // Index 7: Other / Другое (empty - fully custom)
            types[7] to emptyList()

            // Optional: Uncomment and add to string-array if needed
            // "Cheese" / "Домашний сыр" (Acidify/Curdling 2-6h, Pressing 12-24h, Aging 30+ days; sources: Instructables, Art of Cheese)
            // types[8] to listOf(
            //     Stage(name = "Acidifying and Curdling", durationHours = 4, orderIndex = 0, batchId = null),  // 4h avg.
            //     Stage(name = "Pressing and Draining", durationHours = 18, orderIndex = 1, batchId = null),  // 18h avg.
            //     Stage(name = "Aging", durationHours = 720, orderIndex = 2, batchId = null)  // 30 days avg.
            // ),
            // "Kombucha" / "Комбуча" (First Ferment 7-12 days, Bottling/Second 2-5 days; sources: You Brew Kombucha, AHA)
            // types[9] to listOf(
            //     Stage(name = "First Fermentation", durationHours = 216, orderIndex = 0, batchId = null),  // 9 days avg.
            //     Stage(name = "Bottling and Second Fermentation", durationHours = 84, orderIndex = 1, batchId = null)  // 3.5 days avg.
            // ),
            // "Pickles" / "Солёные огурцы" (Brining/Salting 3-24h, Fermentation 7-21 days; sources: Love and Lemons, NCHFP)
            // types[10] to listOf(
            //     Stage(name = "Brining and Salting", durationHours = 12, orderIndex = 0, batchId = null),  // 12h avg.
            //     Stage(name = "Fermentation", durationHours = 336, orderIndex = 1, batchId = null)  // 14 days avg.
            // )
        )
    }
}