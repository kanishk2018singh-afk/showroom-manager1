package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert
    suspend fun enqueue(item: SyncQueueEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getBatch(limit: Int): List<SyncQueueEntity>

    @Query("UPDATE sync_queue SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpAttempts(id: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun countFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int
}
