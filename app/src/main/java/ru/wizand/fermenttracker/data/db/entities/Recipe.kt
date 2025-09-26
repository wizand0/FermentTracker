package ru.wizand.fermenttracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey val type: String,
    val ingredients: String = "",
    val note: String = ""
)