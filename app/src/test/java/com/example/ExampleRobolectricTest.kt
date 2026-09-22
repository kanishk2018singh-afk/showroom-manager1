package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.CsvHelper
import com.example.data.PricingCalculator
import com.example.data.Product
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `verify main activity launches without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Showroom Manager", appName)
    }

    @Test
    fun `verify showroom pricing calculation flow`() {
        // Section 7 specification test:
        // MRP = 10,000, Discount = 35%, GST = 18%, Purchase = 5,000
        val pricing = PricingCalculator.calculate(
            mrp = 10000.0,
            discountPercent = 35.0,
            gstPercent = 18.0,
            purchasePrice = 5000.0
        )

        assertEquals(3500.0, pricing.discountAmount, 0.01)
        assertEquals(6500.0, pricing.taxablePrice, 0.01)
        assertEquals(1170.0, pricing.gstAmount, 0.01)
        assertEquals(7670.0, pricing.finalSalePrice, 0.01)
        assertEquals(1500.0, pricing.marginAmount, 0.01)
        assertEquals(30.0, pricing.marginPercent, 0.01)
    }

    @Test
    fun `verify csv parser and exporter`() {
        val sampleCsv = """
            Code,Name,Brand,Category,MRP,Discount,GST,Purchase
            HW-101,Table Top Basin,Hindware,Sanitary,8000,20,18,4000
        """.trimIndent()

        val (parsed, warnings) = CsvHelper.parseCsv(
            csvContent = sampleCsv,
            targetCompanyId = 1L,
            targetCompanyName = "Hindware"
        )

        assertEquals(1, parsed.size)
        val product = parsed[0]
        assertEquals("HW-101", product.code)
        assertEquals("Table Top Basin", product.name)
        assertEquals(8000.0, product.mrp, 0.01)
        assertEquals(20.0, product.discountPercent, 0.01)
        assertEquals(18.0, product.gstPercent, 0.01)
        assertEquals(4000.0, product.purchasePrice, 0.01)

        val exportedCustomerCsv = CsvHelper.exportToCsv(parsed, includeInternalCosts = false)
        assertTrue(!exportedCustomerCsv.contains("Purchase Price"))

        val exportedInternalCsv = CsvHelper.exportToCsv(parsed, includeInternalCosts = true)
        assertTrue(exportedInternalCsv.contains("Purchase Price"))
        assertTrue(exportedInternalCsv.contains("Margin"))
    }

    @Test
    fun `verify room database multi-company operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val companyDao = db.companyDao()
            val categoryDao = db.categoryDao()
            val productDao = db.productDao()

            // 1. Insert Company
            val companyId = companyDao.insertCompany(
                Company(name = "Jaquar Luxury", description = "Premium bathroom fittings")
            )
            assertTrue(companyId > 0)

            // 2. Insert Category
            val catId = categoryDao.insertCategory(
                CategoryEntity(name = "CP Fittings", subcategories = "Mixer, Shower, Tap")
            )
            assertTrue(catId > 0)

            // 3. Insert Product linked to companyId
            val prodId = productDao.insertProduct(
                Product(
                    companyId = companyId,
                    companyName = "Jaquar Luxury",
                    code = "JQ-9901",
                    name = "Thermostatic Shower Mixer",
                    brand = "Jaquar",
                    category = "CP Fittings",
                    mrp = 15000.0,
                    discountPercent = 25.0,
                    gstPercent = 18.0,
                    purchasePrice = 8000.0
                )
            )
            assertTrue(prodId > 0)

            // 4. Query products by company
            val products = productDao.getProductsByCompany(companyId).first()
            assertEquals(1, products.size)
            assertEquals("JQ-9901", products[0].code)

            // 5. Query CompanyWithProducts relationship
            val companyWithProducts = companyDao.getCompanyWithProducts(companyId).first()
            assertNotNull(companyWithProducts)
            assertEquals(1, companyWithProducts!!.products.size)
            assertEquals("Thermostatic Shower Mixer", companyWithProducts.products[0].name)

            // 6. Test delete company cascade
            companyDao.deleteCompanyById(companyId)
            val remainingProducts = productDao.getProductsByCompany(companyId).first()
            assertEquals(0, remainingProducts.size)
        } finally {
            db.close()
        }
    }
}
