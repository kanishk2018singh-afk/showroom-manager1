package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subcategories: String = "" // Comma-separated list of subcategories
) {
    fun getSubcategoryList(): List<String> {
        return if (subcategories.isBlank()) emptyList()
        else subcategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
