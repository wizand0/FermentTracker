package ru.wizand.fermenttracker.ui.settings

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
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
        findPreference<Preference>("version")?.summary =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName

        // Экспорт / импорт БД
        findPreference<Preference>("export_db")?.setOnPreferenceClickListener {
            (activity as? MainActivity)?.performBackup()
            true
        }

        findPreference<Preference>("import_db")?.setOnPreferenceClickListener {
            (activity as? MainActivity)?.performRestore()
            true
        }

        // Очистка данных
        findPreference<Preference>("clear_all_data")?.setOnPreferenceClickListener {
            showClearDataDialog()
            true
        }

        // Лицензии
        findPreference<Preference>("licenses")?.setOnPreferenceClickListener {
            showLicensesDialog()
            true
        }

        // Настройка списков (единицы и время)
        setupListPreferences()

        // Уведомления
        findPreference<SwitchPreferenceCompat>("notifications_enabled")?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            Log.d("Settings", "Notifications: $isEnabled")
            Toast.makeText(
                requireContext(),
                getString(R.string.notifications_status, if (isEnabled) getString(R.string.enabled) else getString(R.string.disabled)),
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        // Тёмная тема
        findPreference<SwitchPreferenceCompat>("dark_theme")?.setOnPreferenceChangeListener { _, newValue ->
            val isDark = newValue as Boolean
            Log.d("Settings", "Dark theme: $isDark")

            val mode = if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)

            Toast.makeText(
                requireContext(),
                getString(R.string.dark_theme_status, if (isDark) getString(R.string.enabled) else getString(R.string.disabled)),
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Добавляем нижний отступ для списка
        view.post {
            view.findViewById<View>(android.R.id.list)?.apply {
                setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom + 150)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "dark_theme" -> {
                val dark = sharedPreferences?.getBoolean(key, false) ?: false
                AppCompatDelegate.setDefaultNightMode(
                    if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
            }

            "weight_units" -> {
                val units = sharedPreferences?.getString(key, "grams")
                Toast.makeText(requireContext(), getString(R.string.weight_units_changed, units), Toast.LENGTH_SHORT).show()
            }

            "notifications_enabled" -> {
                val enabled = sharedPreferences?.getBoolean(key, true) ?: true
                Toast.makeText(
                    requireContext(),
                    getString(R.string.notifications_status, if (enabled) getString(R.string.enabled) else getString(R.string.disabled)),
                    Toast.LENGTH_SHORT
                ).show()
            }

            "reminder_time" -> {
                val time = sharedPreferences?.getString(key, "morning")
                Toast.makeText(requireContext(), getString(R.string.reminder_time_changed, time), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListPreferences() {
        // Единицы измерения
        findPreference<ListPreference>("weight_units")?.apply {
            summaryProvider = SummaryProvider<ListPreference> { pref ->
                val index = pref.findIndexOfValue(pref.value)
                if (index >= 0) pref.entries[index] else ""
            }
        }

        // Время напоминаний
        findPreference<ListPreference>("reminder_time")?.apply {
            summaryProvider = SummaryProvider<ListPreference> { pref ->
                val index = pref.findIndexOfValue(pref.value)
                if (index >= 0) pref.entries[index] else ""
            }
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext(), R.style.Theme_FermentTracker_Dialog)
            .setTitle(R.string.clear_all_data_title)
            .setMessage(R.string.clear_all_data_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, getString(R.string.clearing_data), Toast.LENGTH_SHORT).show()
                        }

                        withContext(Dispatchers.IO) {
                            AppDatabase.closeInstance()
                            val dbFile = requireContext().getDatabasePath("ferment_tracker_db")
                            if (dbFile.exists()) dbFile.delete()
                            Log.d("DB", "Database cleared")
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, getString(R.string.all_data_deleted), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, getString(R.string.error_clearing_data, e.message), Toast.LENGTH_SHORT).show()
                            Log.e("DB", "Error clearing database: ${e.message}")
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showLicensesDialog() {
        AlertDialog.Builder(requireContext(), R.style.Theme_FermentTracker_Dialog)
            .setTitle(R.string.open_source_licenses_title)
            .setMessage(R.string.open_source_licenses_text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
