package ru.wizand.fermenttracker.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.dao.StageDao
import ru.wizand.fermenttracker.data.db.entities.*

@Database(
    entities = [Batch::class, Stage::class, Photo::class, BatchLog::class, Recipe::class, StageTemplate::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao
    abstract fun stageDao(): StageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ferment_tracker_db"
                )
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
                    Log.d("DB", "Database instance closed")
                } catch (e: Exception) {
                    Log.e("DB", "Error closing database: ${e.message}")
                } finally {
                    INSTANCE = null
                }
            }
        }
    }
}