package ru.wizand.fermenttracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage

@Database(entities = [Batch::class, Stage::class, Photo::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "batches.db")
                    //.openHelperFactory(...) // for SQLCipher
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
