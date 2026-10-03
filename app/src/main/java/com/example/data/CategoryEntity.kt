package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val subcategories: String = "", // Comma-separated list of subcategories
    /** Conflict resolution ke liye (cloud vs local) */
    val updatedAt: Long = System.currentTimeMillis(),
    /** Cloud sync: false = Firestore par upload pending hai */
    val isSynced: Boolean = false
) {
    fun getSubcategoryList(): List<String> {
        return if (subcategories.isBlank()) emptyList()
        else subcategories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
