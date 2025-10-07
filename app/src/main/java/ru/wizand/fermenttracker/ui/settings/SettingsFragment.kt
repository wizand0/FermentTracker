package ru.wizand.fermenttracker.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference.SummaryProvider
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.wizand.fermenttracker.R
import ru.wizand.fermenttracker.data.db.AppDatabase
import ru.wizand.fermenttracker.ui.MainActivity

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Установка версии приложения
        findPreference<Preference>("version")?.summary = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName

        // Обработка кликов на экспорт/импорт БД
        findPreference<Preference>("export_db")?.setOnPreferenceClickListener {
            (activity as? MainActivity)?.performBackup()
            true
        }

        findPreference<Preference>("import_db")?.setOnPreferenceClickListener {
            (activity as? MainActivity)?.performRestore()
            true
        }

        // Обработка клика на очистку данных
        findPreference<Preference>("clear_all_data")?.setOnPreferenceClickListener {
            showClearDataDialog()
            true
        }

        // Обработка клика на лицензии
        findPreference<Preference>("licenses")?.setOnPreferenceClickListener {
            showLicensesDialog()
            true
        }

        // Установка значений по умолчанию для ListPreference
        setupListPreferences()

        // Добавляем прямые обработчики для SwitchPreferenceCompat
        findPreference<SwitchPreferenceCompat>("notifications_enabled")?.setOnPreferenceChangeListener { preference, newValue ->
            val isEnabled = newValue as Boolean
            Log.d("Settings", "Notifications enabled changed to: $isEnabled")
            Toast.makeText(requireContext(), "Уведомления ${if (isEnabled) "включены" else "выключены"}", Toast.LENGTH_SHORT).show()

            // Здесь можно добавить логику для включения/выключения уведомлений

            true // Возвращаем true, чтобы сохранить новое значение
        }

        findPreference<SwitchPreferenceCompat>("dark_theme")?.setOnPreferenceChangeListener { preference, newValue ->
            val isDarkTheme = newValue as Boolean
            Log.d("Settings", "Dark theme changed to: $isDarkTheme")
            Toast.makeText(requireContext(), "Темная тема ${if (isDarkTheme) "включена" else "выключена"}", Toast.LENGTH_SHORT).show()

            if (isDarkTheme) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }

            true // Возвращаем true, чтобы сохранить новое значение
        }
    }

    override fun onResume() {
        super.onResume()
        // Регистрируем слушатель изменений SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        Log.d("Settings", "SharedPreferences listener registered")
    }

    override fun onPause() {
        super.onPause()
        // Отменяем регистрацию слушателя
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        Log.d("Settings", "SharedPreferences listener unregistered")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d("Settings", "Preference changed: $key")

        when (key) {
            "dark_theme" -> {
                // Эта часть уже обрабатывается в setOnPreferenceChangeListener
                // Но оставим для надежности
                val isDarkTheme = sharedPreferences?.getBoolean(key, false) ?: false
                Log.d("Settings", "Dark theme changed to: $isDarkTheme")

                if (isDarkTheme) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
            "weight_units" -> {
                // Обработка изменения единиц измерения веса
                val weightUnits = sharedPreferences?.getString(key, "grams")
                Log.d("Settings", "Weight units changed to: $weightUnits")
                Toast.makeText(requireContext(), "Единицы веса изменены на: $weightUnits", Toast.LENGTH_SHORT).show()
                // Здесь можно добавить логику для обновления UI в соответствии с выбранными единицами
            }
            "notifications_enabled" -> {
                // Эта часть уже обрабатывается в setOnPreferenceChangeListener
                // Но оставим для надежности
                val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                Log.d("Settings", "Notifications enabled changed to: $enabled")
                Toast.makeText(requireContext(), "Уведомления ${if (enabled) "включены" else "выключены"}", Toast.LENGTH_SHORT).show()
                // Здесь можно добавить логику для включения/выключения уведомлений
            }
            "reminder_time" -> {
                val time = sharedPreferences?.getString(key, "morning")
                Log.d("Settings", "Reminder time changed to: $time")
                Toast.makeText(requireContext(), "Время напоминаний изменено на: $time", Toast.LENGTH_SHORT).show()
                // Здесь можно добавить логику для изменения времени напоминаний
            }
        }
    }

    private fun setupListPreferences() {
        // Настройка ListPreference для единиц измерения веса
        val weightUnitsPreference = findPreference<ListPreference>("weight_units")
        weightUnitsPreference?.summaryProvider = SummaryProvider<ListPreference> { preference ->
            val index = preference.findIndexOfValue(preference.value)
            if (index >= 0) preference.entries[index] else ""
        }

        weightUnitsPreference?.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue as String
            Log.d("Settings", "Weight units preference changed to: $value")
            Toast.makeText(requireContext(), "Единицы веса изменены", Toast.LENGTH_SHORT).show()
            true
        }

        // Настройка ListPreference для времени напоминаний
        val reminderTimePreference = findPreference<ListPreference>("reminder_time")
        reminderTimePreference?.summaryProvider = SummaryProvider<ListPreference> { preference ->
            val index = preference.findIndexOfValue(preference.value)
            if (index >= 0) preference.entries[index] else ""
        }

        reminderTimePreference?.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue as String
            Log.d("Settings", "Reminder time preference changed to: $value")
            Toast.makeText(requireContext(), "Время напоминаний изменено", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Очистить все данные")
            .setMessage("Вы уверены, что хотите удалить все данные? Это действие нельзя отменить.")
            .setPositiveButton("Да") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Показываем диалог в главном потоке
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Очистка данных...", Toast.LENGTH_SHORT).show()
                        }

                        // Выполняем очистку в фоновом потоке
                        withContext(Dispatchers.IO) {
                            // Закрываем базу данных
                            AppDatabase.closeInstance()

                            // Удаляем файл базы данных
                            val dbFile = requireContext().getDatabasePath("ferment_tracker_db")
                            if (dbFile.exists()) {
                                dbFile.delete()
                            }

                            Log.d("DB", "Database cleared")
                        }

                        // Показываем сообщение в главном потоке
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Все данные удалены", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка при очистке данных: ${e.message}", Toast.LENGTH_SHORT).show()
                            Log.e("DB", "Error clearing database: ${e.message}")
                        }
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showLicensesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Лицензии открытого ПО")
            .setMessage("Здесь будет информация об используемых open-source библиотеках")
            .setPositiveButton("OK", null)
            .show()
    }
}