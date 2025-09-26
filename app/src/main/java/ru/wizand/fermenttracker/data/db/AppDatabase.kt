package ru.wizand.fermenttracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.wizand.fermenttracker.data.db.dao.BatchDao
import ru.wizand.fermenttracker.data.db.entities.Batch
import ru.wizand.fermenttracker.data.db.entities.BatchLog
import ru.wizand.fermenttracker.data.db.entities.Photo
import ru.wizand.fermenttracker.data.db.entities.Stage
import ru.wizand.fermenttracker.data.db.entities.Recipe // Added
import ru.wizand.fermenttracker.data.db.entities.StageTemplate // Added

@Database(entities = [Batch::class, Stage::class, Photo::class, BatchLog::class, Recipe::class, StageTemplate::class], version = 3, exportSchema = false) // Bumped version to 3
abstract class AppDatabase : RoomDatabase() {
    abstract fun batchDao(): BatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new tables for recipes and stage_templates
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recipes` (
                        `type` TEXT NOT NULL, 
                        `ingredients` TEXT NOT NULL, 
                        `note` TEXT NOT NULL, 
                        PRIMARY KEY(`type`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stage_templates` (
                        `id` TEXT NOT NULL, 
                        `recipeType` TEXT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `durationHours` INTEGER NOT NULL, 
                        `orderIndex` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`recipeType`) REFERENCES `recipes`(`type`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stage_templates_recipeType` ON `stage_templates` (`recipeType`)")

                // Add currentWeightGr and plannedCompletionDate to batches
                db.execSQL("ALTER TABLE batches ADD COLUMN currentWeightGr REAL")
                db.execSQL("ALTER TABLE batches ADD COLUMN plannedCompletionDate INTEGER")

                // Drop currentWeightGr from stages: Recreate table without the column
                db.execSQL("""
                    CREATE TABLE new_stages (
                        id TEXT PRIMARY KEY NOT NULL,
                        batchId TEXT,
                        name TEXT NOT NULL,
                        durationHours INTEGER NOT NULL,
                        startTime INTEGER,
                        endTime INTEGER,
                        plannedStartTime INTEGER,
                        plannedEndTime INTEGER,
                        orderIndex INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(batchId) REFERENCES batches(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("DROP INDEX IF EXISTS index_stages_batchId") // Added: drop the old index to avoid name conflict
                db.execSQL("CREATE INDEX index_stages_batchId ON new_stages(batchId)")
                db.execSQL("""
                    INSERT INTO new_stages (id, batchId, name, durationHours, startTime, endTime, plannedStartTime, plannedEndTime, orderIndex)
                    SELECT id, batchId, name, durationHours, startTime, endTime, plannedStartTime, plannedEndTime, orderIndex FROM stages
                """.trimIndent())
                db.execSQL("DROP TABLE stages")
                db.execSQL("ALTER TABLE new_stages RENAME TO stages")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ferment_tracker_db"
                )
                    .addMigrations(MIGRATION_2_3) // Added: migration for version 3
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}