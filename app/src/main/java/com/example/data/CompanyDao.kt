package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompanies(): Flow<List<Company>>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyById(id: Long): Company?

    @Query("SELECT * FROM companies WHERE name = :name LIMIT 1")
    suspend fun getCompanyByName(name: String): Company?

    @Transaction
    @Query("SELECT * FROM companies WHERE id = :companyId LIMIT 1")
    fun getCompanyWithProducts(companyId: Long): Flow<CompanyWithProducts?>

    @Transaction
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllCompaniesWithProducts(): Flow<List<CompanyWithProducts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanies(companies: List<Company>)

    @Update
    suspend fun updateCompany(company: Company)

    @Delete
    suspend fun deleteCompany(company: Company)

    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteCompanyById(id: Long)

    @Query("SELECT COUNT(*) FROM companies")
    suspend fun getCompanyCount(): Int

    // ---------- Cloud sync (Firestore) ke liye ----------

    @Query("SELECT * FROM companies")
    suspend fun getAllCompaniesOnce(): List<Company>

    @Query("SELECT * FROM companies WHERE isSynced = 0 LIMIT :limit")
    suspend fun getUnsyncedCompanies(limit: Int = 500): List<Company>

    @Query("SELECT COUNT(*) FROM companies WHERE isSynced = 0")
    fun unsyncedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM companies WHERE isSynced = 0")
    suspend fun unsyncedCount(): Int

    @Query("UPDATE companies SET isSynced = :synced WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>, synced: Boolean = true)

    @Query("UPDATE companies SET isSynced = 0 WHERE id = :id")
    suspend fun markUnsynced(id: Long)

    @Query("UPDATE companies SET isSynced = 0")
    suspend fun markAllUnsynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFromCloud(company: Company): Long
}
