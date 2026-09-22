package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE companyId = :companyId ORDER BY name ASC")
    fun getProductsByCompany(companyId: Long): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductById(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE companyId = :companyId AND code = :code LIMIT 1")
    suspend fun getProductByCodeAndCompany(code: String, companyId: Long): Product?

    @Query("""
        SELECT * FROM products 
        WHERE companyId = :companyId 
        AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%')
        ORDER BY name ASC
    """)
    fun searchProducts(companyId: Long, query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>): List<Long>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM products WHERE companyId = :companyId")
    suspend fun deleteProductsByCompanyId(companyId: Long)

    @Query("SELECT COUNT(*) FROM products WHERE companyId = :companyId")
    suspend fun getProductCountByCompany(companyId: Long): Int

    @Query("SELECT COUNT(DISTINCT brand) FROM products WHERE companyId = :companyId")
    suspend fun getDistinctBrandCountByCompany(companyId: Long): Int

    @Query("SELECT DISTINCT brand FROM products WHERE companyId = :companyId ORDER BY brand ASC")
    fun getDistinctBrands(companyId: Long): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM products WHERE companyId = :companyId ORDER BY category ASC")
    fun getDistinctCategories(companyId: Long): Flow<List<String>>
}
