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
                        Stage(name = "Salting", durationHours = 36, orderIndex = 0, batchId = ""),
                        Stage(name = "Curing and Drying", durationHours = 336, orderIndex = 1, batchId = "")
                    ) to Recipe(
                        type = types[0],
                        ingredients = "Мясо свинины/говядины — 1 кг\nСоль — 30 г\nПерец чёрный — 5 г\nЧеснок — 3 зубчика",
                        note = "Выдерживать мясо в соли 1,5 суток, затем сушить при +4…+8°C 14 дней. **Хранение:** в холодильнике до 2 месяцев, в вакууме — до 6 месяцев."
                    )
                    ),

            // Dry-cured meat (spicy variation)
            types[1] to (
                    listOf(
                        Stage(name = "Salting with Spices", durationHours = 48, orderIndex = 0, batchId = ""),
                        Stage(name = "Drying", durationHours = 336, orderIndex = 1, batchId = "")
                    ) to Recipe(
                        type = types[1],
                        ingredients = "Свинина — 1 кг\nСоль нитритная — 25 г\nПаприка копчёная — 10 г\nКориандр — 5 г",
                        note = "Сухой посол с приправами 2 суток, затем сушка 14 дней при +5°C. **Хранение:** в холодильнике до 3 месяцев."
                    )
                    ),

            // Dry-cured sausage
            types[2] to (
                    listOf(
                        Stage(name = "Grinding and Stuffing", durationHours = 18, orderIndex = 0, batchId = ""),
                        Stage(name = "Fermentation and Drying", durationHours = 144, orderIndex = 1, batchId = "")
                    ) to Recipe(
                        type = types[2],
                        ingredients = "Свинина — 700 г\nГовядина — 300 г\nСоль нитритная — 25 г\nПерец — 5 г\nЧеснок — 3 зубчика\nОболочка натуральная — 1 м",
                        note = "Фаршировать, ферментировать 6 дней при +18°C, затем сушить 2–3 недели. **Хранение:** в холодильнике до 2 месяцев."
                    )
                    ),

            // Beer
            types[3] to (
                    listOf(
                        Stage(name = "Brewing Preparation", durationHours = 8, orderIndex = 0, batchId = ""),
                        Stage(name = "Primary Fermentation", durationHours = 192, orderIndex = 1, batchId = ""),
                        Stage(name = "Conditioning and Bottling", durationHours = 216, orderIndex = 2, batchId = "")
                    ) to Recipe(
                        type = types[3],
                        ingredients = "Солод ячменный — 4 кг\nХмель — 50 г\nДрожжи пивные — 1 пакет",
                        note = "Брожение 8 дней, дображивание 9 дней. **Хранение:** в бутылках при +4°C до 6 месяцев."
                    )
                    ),

            // Wine
            types[4] to (
                    listOf(
                        Stage(name = "Crushing and Pressing", durationHours = 9, orderIndex = 0, batchId = ""),
                        Stage(name = "Primary Fermentation", durationHours = 144, orderIndex = 1, batchId = ""),
                        Stage(name = "Secondary Fermentation and Aging", durationHours = 480, orderIndex = 2, batchId = "")
                    ) to Recipe(
                        type = types[4],
                        ingredients = "Виноград — 10 кг\nСахар — 2 кг\nДрожжи винные — 1 пакет",
                        note = "Первичное брожение 6 дней, вторичное 20+ дней. **Хранение:** в бутылках при +12°C до 2 лет."
                    )
                    ),

            // Kvass
            types[5] to (
                    listOf(
                        Stage(name = "Steeping and Preparation", durationHours = 18, orderIndex = 0, batchId = ""),
                        Stage(name = "Fermentation", durationHours = 84, orderIndex = 1, batchId = "")
                    ) to Recipe(
                        type = types[5],
                        ingredients = "Ржаной хлеб — 500 г\nСахар — 100 г\nДрожжи сухие — 5 г",
                        note = "Выдержка хлеба 18 ч, ферментация 3,5 дня. **Хранение:** в холодильнике до 5 дней."
                    )
                    ),

            // Kimchi
            types[6] to (
                    listOf(
                        Stage(name = "Salting", durationHours = 2, orderIndex = 0, batchId = ""),
                        Stage(name = "Packing with Paste", durationHours = 1, orderIndex = 1, batchId = ""),
                        Stage(name = "Fermentation", durationHours = 132, orderIndex = 2, batchId = "")
                    ) to Recipe(
                        type = types[6],
                        ingredients = "Капуста пекинская — 1 кг\nСоль — 50 г\nПерец чили — 30 г\nЧеснок — 5 зубчиков\nИмбирь — 20 г",
                        note = "Ферментация 5–6 дней при комнатной температуре. **Хранение:** в холодильнике до 2 месяцев."
                    )
                    ),

            // Sauerkraut
            types[7] to (
                    listOf(
                        Stage(name = "Salting and Mixing", durationHours = 2, orderIndex = 0, batchId = ""),
                        Stage(name = "Fermentation", durationHours = 240, orderIndex = 1, batchId = ""),
                        Stage(name = "Storage", durationHours = 336, orderIndex = 2, batchId = "")
                    ) to Recipe(
                        type = types[7],
                        ingredients = "Капуста белокочанная — 2 кг\nМорковь — 200 г\nСоль — 40 г",
                        note = "Ферментация 10 дней при +18°C. **Хранение:** в холодильнике до 6 месяцев."
                    )
                    ),

            // Other (custom)
            types[8] to (emptyList<Stage>() to Recipe(type = types[8], ingredients = "", note = ""))
        )
    }
}
