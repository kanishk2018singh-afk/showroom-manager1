package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // ---------- Cloud sync (Firestore) ke liye ----------

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE isSynced = 0 LIMIT :limit")
    suspend fun getUnsyncedCategories(limit: Int = 500): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories WHERE isSynced = 0")
    fun unsyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM categories WHERE isSynced = 0")
    suspend fun unsyncedCount(): Int

    @Query("UPDATE categories SET isSynced = :synced WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>, synced: Boolean = true)

    @Query("UPDATE categories SET isSynced = 0 WHERE id = :id")
    suspend fun markUnsynced(id: Long)

    @Query("UPDATE categories SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(category: CategoryEntity): Long
}
