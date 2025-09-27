package ru.wizand.fermenttracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.*

@Database(
    entities = [Batch::class, Stage::class, Photo::class, BatchLog::class, Recipe::class, StageTemplate::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ... (твои миграции/инициализация, как было) ...

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ferment_tracker_db"
                )
                    // оставь миграции/фолбэки, которые у тебя были
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Закрывает и обнуляет синглтон базы. Используется перед заменой файла БД при restore.
         */
        fun closeInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (ignored: Exception) { }
                INSTANCE = null
            }
        }
    }
}
