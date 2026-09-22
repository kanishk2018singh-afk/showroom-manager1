package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShowroomRepository(
    private val companyDao: CompanyDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao
) {
    // Companies
    val allCompanies: Flow<List<Company>> = companyDao.getAllCompanies()
    val allCompaniesWithProducts: Flow<List<CompanyWithProducts>> = companyDao.getAllCompaniesWithProducts()

    fun getCompanyWithProducts(companyId: Long): Flow<CompanyWithProducts?> =
        companyDao.getCompanyWithProducts(companyId)

    suspend fun insertCompany(company: Company): Long = companyDao.insertCompany(company)

    suspend fun updateCompany(company: Company) = companyDao.updateCompany(company)

    suspend fun deleteCompany(company: Company) {
        productDao.deleteProductsByCompanyId(company.id)
        companyDao.deleteCompany(company)
    }

    suspend fun getCompanyById(id: Long): Company? = companyDao.getCompanyById(id)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    suspend fun insertCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: CategoryEntity) = categoryDao.deleteCategory(category)

    // Products
    fun getProductsByCompany(companyId: Long): Flow<List<Product>> =
        productDao.getProductsByCompany(companyId)

    fun searchProducts(companyId: Long, query: String): Flow<List<Product>> =
        productDao.searchProducts(companyId, query)

    fun getDistinctBrands(companyId: Long): Flow<List<String>> =
        productDao.getDistinctBrands(companyId)

    fun getDistinctCategories(companyId: Long): Flow<List<String>> =
        productDao.getDistinctCategories(companyId)

    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)

    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)

    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) = productDao.deleteProductById(id)

    suspend fun insertProducts(products: List<Product>): List<Long> =
        productDao.insertProducts(products)

    suspend fun ensureDefaultDataPopulated() {
        if (companyDao.getCompanyCount() == 0) {
            companyDao.insertCompanies(InitialData.defaultCompanies)
        }
        if (categoryDao.getCategoryCount() == 0) {
            categoryDao.insertCategories(InitialData.defaultCategories)
        }
        val defaultHindwareCompany = companyDao.getCompanyByName("Hindware")
        val hindwareId = defaultHindwareCompany?.id ?: 1L
        if (productDao.getProductCountByCompany(hindwareId) == 0) {
            productDao.insertProducts(InitialData.getDefaultProducts())
        }
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
                    updatedCount++
                } else {
                    skippedCount++
                }
            } else {
                productDao.insertProduct(product)
                addedCount++
            }
        }

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
