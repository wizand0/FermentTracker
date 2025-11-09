package ru.wizand.fermenttracker.utils

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Enum class representing the supported weight units in the FermentTracker application.
 * Each unit has a unique key used for storage in SharedPreferences.
 */
enum class WeightUnit(val key: String) {
    GRAMS("grams"),
    KILOGRAMS("kilograms"),
    POUNDS("pounds")
}

/**
 * Utility object for converting and formatting weight measurements in the FermentTracker application.
 * This object provides methods to convert weights from grams to other units, format weights with unit symbols,
 * retrieve the current preferred unit from SharedPreferences, and get unit symbols.
 *
 * Conversion coefficients:
 * - 1 kg = 1000 g
 * - 1 lb = 453.592 g
 */
object WeightConverter {

    // Private constants for conversion coefficients to ensure consistency and avoid magic numbers.
    private const val GRAMS_TO_KILOGRAMS = 1000.0
    private const val GRAMS_TO_POUNDS = 453.592

    /**
     * Converts a weight value from grams to the specified target unit.
     *
     * @param grams The weight in grams as a Double.
     * @param targetUnit The target WeightUnit to convert to.
     * @return The converted weight value as a Double in the target unit.
     *
     * Example usage:
     * val kg = WeightConverter.convertFromGrams(1500.0, WeightUnit.KILOGRAMS)  // Returns 1.5
     */
    fun convertFromGrams(grams: Double, targetUnit: WeightUnit): Double {
        return when (targetUnit) {
            WeightUnit.GRAMS -> grams
            WeightUnit.KILOGRAMS -> grams / GRAMS_TO_KILOGRAMS
            WeightUnit.POUNDS -> grams / GRAMS_TO_POUNDS
        }
    }

    /**
     * Formats a weight value from grams into a human-readable string with the specified unit.
     * The value is first converted to the target unit and then formatted with the given number of decimal places.
     * The result includes the unit symbol (e.g., "1.5 kg").
     *
     * @param grams The weight in grams as a Double.
     * @param targetUnit The WeightUnit to format in.
     * @param decimals The number of decimal places to show (default is 1).
     * @return A formatted string like "1.5 kg" or "1500 g".
     *
     * Example usage:
     * val formatted = WeightConverter.formatWeight(1500.0, WeightUnit.KILOGRAMS, 1)  // Returns "1.5 kg"
     */
    fun formatWeight(grams: Double, targetUnit: WeightUnit, decimals: Int = 1): String {
        val convertedWeight = convertFromGrams(grams, targetUnit)
        val formattedValue = String.format("%.${decimals}f", convertedWeight)
        val symbol = getUnitSymbol(targetUnit)
        return "$formattedValue $symbol"
    }

    /**
     * Retrieves the current preferred weight unit from SharedPreferences.
     * It reads the value stored under the key "weight_units". If no value is found or it doesn't match,
     * it defaults to WeightUnit.GRAMS.
     *
     * @param context The Android Context used to access SharedPreferences.
     * @return The WeightUnit representing the user's preferred unit.
     *
     * Example usage:
     * val currentUnit = WeightConverter.getCurrentUnit(context)  // e.g., WeightUnit.KILOGRAMS
     */
    fun getCurrentUnit(context: Context): WeightUnit {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val storedKey = sharedPreferences.getString("weight_units", WeightUnit.GRAMS.key)
        // Find the matching WeightUnit by key, defaulting to GRAMS if not found.
        return WeightUnit.values().find { it.key == storedKey } ?: WeightUnit.GRAMS
    }

    /**
     * Returns the short symbol (abbreviation) for the given WeightUnit.
     *
     * @param unit The WeightUnit to get the symbol for.
     * @return The unit symbol as a String ("g", "kg", or "lb").
     *
     * Example usage:
     * val symbol = WeightConverter.getUnitSymbol(WeightUnit.KILOGRAMS)  // Returns "kg"
     */
    fun getUnitSymbol(unit: WeightUnit): String {
        return when (unit) {
            WeightUnit.GRAMS -> "g"
            WeightUnit.KILOGRAMS -> "kg"
            WeightUnit.POUNDS -> "lb"
        }
    }
}