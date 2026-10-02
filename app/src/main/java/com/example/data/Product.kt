package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class StockStatus(val label: String, val hindiLabel: String) {
    IN_STOCK("In Stock", "उपलब्ध"),
    LOW_STOCK("Low Stock (< 2 pcs)", "कम स्टॉक (<2 pcs)"),
    OUT_OF_STOCK("Out of Stock", "स्टॉक खत्म")
}

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Company::class,
            parentColumns = ["id"],
            childColumns = ["companyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["companyId"]),
        Index(value = ["code"]),
        Index(value = ["brand"]),
        Index(value = ["category"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val companyId: Long,
    val companyName: String = "",
    val code: String,
    val name: String,
    val brand: String,
    val category: String,
    val subcategory: String = "",
    val imageUri: String? = null,
    val mrp: Double,
    val discountPercent: Double = 0.0,
    val gstPercent: Double = 18.0,
    val purchasePrice: Double = 0.0,
    val stockQuantity: Int = 1,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    /** Cloud sync: false = Firestore par upload pending hai */
    val isSynced: Boolean = false
) {
    val pricing: PricingBreakdown
        get() = PricingCalculator.calculate(
            mrp = mrp,
            discountPercent = discountPercent,
            gstPercent = gstPercent,
            purchasePrice = purchasePrice
        )

    val finalSalePrice: Double
        get() = pricing.finalSalePrice

    val taxablePrice: Double
        get() = pricing.taxablePrice

    val marginAmount: Double
        get() = pricing.marginAmount

    val marginPercent: Double
        get() = pricing.marginPercent

    val stockStatus: StockStatus
        get() = when {
            stockQuantity <= 0 -> StockStatus.OUT_OF_STOCK
            stockQuantity <= 2 -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
}
