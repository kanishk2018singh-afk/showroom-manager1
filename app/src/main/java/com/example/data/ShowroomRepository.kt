package com.example.data

import android.content.Context
import com.example.data.sync.FirestoreSync
import com.example.data.sync.SyncResult
import com.example.data.sync.SyncState
import com.example.data.sync.categoryDocId
import com.example.data.sync.companyDocId
import com.example.data.sync.productDocId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ShowroomRepository(
    private val companyDao: CompanyDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    syncQueueDao: SyncQueueDao? = null,
    appContext: Context? = null,
    syncScope: CoroutineScope? = null
) {

    /**
     * Cloud sync engine (nullable — jab tak Firebase configure na ho / login na ho,
     * app poora offline chalti hai aur koi sync call nahi hota).
     */
    val sync: FirestoreSync? =
        if (syncQueueDao != null && appContext != null && syncScope != null) {
            FirestoreSync(appContext, companyDao, categoryDao, productDao, syncQueueDao, syncScope)
        } else null

    /** Pending (upload hone wale) records ki ginti — UI me "X pending" dikhane ke liye */
    val pendingSyncCount: Flow<Int> =
        if (syncQueueDao != null) {
            combine(
                companyDao.unsyncedCountFlow(),
                categoryDao.unsyncedCountFlow(),
                productDao.unsyncedCountFlow(),
                syncQueueDao.countFlow()
            ) { companies, categories, products, queuedDeletes ->
                companies + categories + products + queuedDeletes
            }
        } else {
            flowOf(0)
        }

    /** Sync state (last sync time + message) */
    val syncState: Flow<SyncState?> = sync?.state ?: flowOf(null)

    /** Manual sync — UI button / login ke baad */
    suspend fun syncNow(): SyncResult =
        sync?.fullSync() ?: SyncResult(false, "Cloud sync available nahi hai (Firebase + login chahiye)")

    /** Sirf pending upload (halka sync) */
    suspend fun syncPending(): SyncResult =
        sync?.syncPending() ?: SyncResult(false, "Cloud sync available nahi hai")

    /** Local change ke baad upload schedule karo (UI block nahi hota) */
    fun requestCloudSync() {
        sync?.scheduleSync()
    }
    // Companies
    val allCompanies: Flow<List<Company>> = companyDao.getAllCompanies()
    val allCompaniesWithProducts: Flow<List<CompanyWithProducts>> = companyDao.getAllCompaniesWithProducts()

    fun getCompanyWithProducts(companyId: Long): Flow<CompanyWithProducts?> =
        companyDao.getCompanyWithProducts(companyId)

    suspend fun insertCompany(company: Company): Long {
        val id = companyDao.insertCompany(company)
        companyDao.markUnsynced(id)
        requestCloudSync()
        return id
    }

    suspend fun updateCompany(company: Company) {
        val before = companyDao.getCompanyById(company.id)
        companyDao.updateCompany(company)
        // naam badla to purana cloud document hata do (warna duplicate company ban jayegi)
        if (before != null && before.name.trim() != company.name.trim()) {
            sync?.enqueueDelete(FirestoreSync.COLLECTION_COMPANIES, companyDocId(before))
        }
        companyDao.markUnsynced(company.id)
        requestCloudSync()
    }

    suspend fun deleteCompany(company: Company) {
        // cloud se company + uske saare products hata do (offline ho to queue me)
        val products = productDao.getProductsByCompanyOnce(company.id)
        products.forEach { sync?.enqueueDelete(FirestoreSync.COLLECTION_PRODUCTS, productDocId(it)) }
        sync?.enqueueDelete(FirestoreSync.COLLECTION_COMPANIES, companyDocId(company))
        productDao.deleteProductsByCompanyId(company.id)
        companyDao.deleteCompany(company)
        requestCloudSync()
    }

    suspend fun getCompanyById(id: Long): Company? = companyDao.getCompanyById(id)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long {
        val id = categoryDao.insertCategory(category)
        categoryDao.markUnsynced(id)
        requestCloudSync()
        return id
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val before = categoryDao.getAllCategoriesOnce().firstOrNull { it.id == category.id }
        categoryDao.updateCategory(category)
        if (before != null && before.name.trim() != category.name.trim()) {
            sync?.enqueueDelete(FirestoreSync.COLLECTION_CATEGORIES, categoryDocId(before))
        }
        categoryDao.markUnsynced(category.id)
        requestCloudSync()
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        sync?.enqueueDelete(FirestoreSync.COLLECTION_CATEGORIES, categoryDocId(category))
        categoryDao.deleteCategory(category)
        requestCloudSync()
    }

    // Products
    fun getProductsByCompany(companyId: Long): Flow<List<Product>> =
        productDao.getProductsByCompany(companyId)

    fun searchProducts(companyId: Long, query: String): Flow<List<Product>> =
        productDao.searchProducts(companyId, query)

    fun getDistinctBrands(companyId: Long): Flow<List<String>> =
        productDao.getDistinctBrands(companyId)

    fun getDistinctCategories(companyId: Long): Flow<List<String>> =
        productDao.getDistinctCategories(companyId)

    suspend fun insertProduct(product: Product): Long {
        val id = productDao.insertProduct(product)
        productDao.markUnsynced(id)
        requestCloudSync()
        return id
    }

    suspend fun updateProduct(product: Product) {
        val before = productDao.getProductByIdOnce(product.id)
        productDao.updateProduct(product)
        // code ya company badla to purana cloud document hata do
        if (before != null && (before.code.trim() != product.code.trim() || before.companyId != product.companyId)) {
            sync?.enqueueDelete(FirestoreSync.COLLECTION_PRODUCTS, productDocId(before))
        }
        productDao.markUnsynced(product.id)
        requestCloudSync()
    }

    suspend fun deleteProduct(product: Product) {
        sync?.enqueueDelete(FirestoreSync.COLLECTION_PRODUCTS, productDocId(product))
        productDao.deleteProduct(product)
        requestCloudSync()
    }

    suspend fun deleteProductById(id: Long) {
        val before = productDao.getProductByIdOnce(id)
        if (before != null) {
            sync?.enqueueDelete(FirestoreSync.COLLECTION_PRODUCTS, productDocId(before))
        }
        productDao.deleteProductById(id)
        requestCloudSync()
    }

    suspend fun insertProducts(products: List<Product>): List<Long> {
        val ids = productDao.insertProducts(products)
        if (ids.isNotEmpty()) {
            productDao.markSynced(ids, synced = false)
            requestCloudSync()
        }
        return ids
    }

    suspend fun ensureDefaultDataPopulated() {
        var seeded = false
        if (companyDao.getCompanyCount() == 0) {
            companyDao.insertCompanies(InitialData.defaultCompanies)
            companyDao.getAllCompaniesOnce().forEach { companyDao.markUnsynced(it.id) }
            seeded = true
        }
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(InitialData.defaultCategories)
            categoryDao.getAllCategoriesOnce().forEach { categoryDao.markUnsynced(it.id) }
            seeded = true
        }
        val defaultHindwareCompany = companyDao.getCompanyByName("Hindware")
        val hindwareId = defaultHindwareCompany?.id ?: 1L
        if (productDao.getProductCountByCompany(hindwareId) == 0) {
            val ids = productDao.insertProducts(InitialData.getDefaultProducts())
            if (ids.isNotEmpty()) productDao.markSynced(ids, synced = false)
            seeded = true
        }
        if (seeded) requestCloudSync()
    }

    suspend fun bulkImportCsv(
        csvData: String,
        companyId: Long,
        companyName: String,
        overwriteDuplicates: Boolean
    ): ImportSummary {
        val (parsedProducts, warnings) = CsvHelper.parseCsv(csvData, companyId, companyName)
        var addedCount = 0
        var updatedCount = 0
        var skippedCount = 0

        for (product in parsedProducts) {
            val existing = productDao.getProductByCodeAndCompany(product.code, companyId)
            if (existing != null) {
                if (overwriteDuplicates) {
                    productDao.updateProduct(product.copy(id = existing.id))
                    productDao.markUnsynced(existing.id)
                    updatedCount++
                } else {
                    skippedCount++
                }
            } else {
                val newId = productDao.insertProduct(product)
                productDao.markUnsynced(newId)
                addedCount++
            }
        }
        if (addedCount > 0 || updatedCount > 0) requestCloudSync()

        return ImportSummary(
            totalRows = parsedProducts.size,
            addedCount = addedCount,
            updatedCount = updatedCount,
            skippedCount = skippedCount,
            warnings = warnings
        )
    }

    suspend fun getAllProductsForExport(companyId: Long): List<Product> {
        return productDao.getProductsByCompany(companyId).first()
    }
}

data class ImportSummary(
    val totalRows: Int,
    val addedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val warnings: List<String>
)
