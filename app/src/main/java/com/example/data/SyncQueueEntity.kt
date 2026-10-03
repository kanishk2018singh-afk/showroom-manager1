package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Pending cloud operations (sirf DELETE).
 *
 * Upload pending hone ka pata har entity ke `isSynced` flag se chalta hai,
 * lekin delete ke baad local row hi nahi bachti — isliye delete ka kaam yahan
 * queue me rakha jata hai aur internet aane par Firestore par apply hota hai.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Firestore subcollection: products / companies / categories */
    val collectionName: String,
    /** Firestore document id (stable local id se banaya gaya) */
    val docId: String,
    /** Filhaal sirf DELETE (upsert `isSynced=false` se hota hai) */
    val operation: String = "DELETE",
    val createdAt: Long = System.currentTimeMillis(),
    /** Kitni baar try kiya (debugging ke liye) */
    val attempts: Int = 0
) {
    companion object {
        const val OP_DELETE = "DELETE"
    }
}
