package com.example.data.sync

import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.Product
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Room entity <-> Firestore map — EXPLICIT mapping (koi `toObject()` nahi).
 *
 * Firestore structure (rules ke hisaab se, sirf apne uid ke andar):
 *   users/{uid}/companies/{docId}
 *   users/{uid}/categories/{docId}
 *   users/{uid}/products/{docId}
 *
 * Timestamps: Room me Long rehta hai (jaise pehle tha) aur Firestore me
 * `updatedAt` (Timestamp) + `updatedAtMillis` (Long) dono likhe jate hain —
 * console me padhne me aasan, aur purana Long-only data bhi padha ja sakta hai.
 */

// ---------- Timestamps (Phase 7) ----------

fun Long.toFirestoreTimestamp(): Timestamp = Timestamp(Date(this))

fun Timestamp.toRoomLong(): Long = this.toDate().time

/** Document se updatedAt nikaalo — Timestamp pehle, warna purana Long field */
fun DocumentSnapshot.readUpdatedAt(fallback: Long = 0L): Long {
    getTimestamp(FIELD_UPDATED_AT)?.let { return it.toRoomLong() }
    return getLong(FIELD_UPDATED_AT_MILLIS) ?: getLong(FIELD_UPDATED_AT) ?: fallback
}

// ---------- Document ids ----------

const val FIELD_UPDATED_AT = "updatedAt"
const val FIELD_UPDATED_AT_MILLIS = "updatedAtMillis"

/** Firestore document id me sirf safe characters */
fun sanitizeDocId(raw: String): String {
    val cleaned = raw.trim().replace("/", "_").replace("\\", "_")
    return cleaned.ifBlank { "unnamed" }.take(180)
}

/** Product ka stable doc id — pehle se project me yahi convention thi: companyId_code */
fun productDocId(product: Product): String = sanitizeDocId("${product.companyId}_${product.code}")

/** Company / category ke naam hi natural key hain (autoGenerate id device par badal jati hai) */
fun companyDocId(company: Company): String = sanitizeDocId(company.name.lowercase())

fun categoryDocId(category: CategoryEntity): String = sanitizeDocId(category.name.lowercase())

// ---------- Company ----------

fun Company.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "description" to description,
    "isDefault" to isDefault,
    "createdAt" to createdAt,
    FIELD_UPDATED_AT to updatedAt.toFirestoreTimestamp(),
    FIELD_UPDATED_AT_MILLIS to updatedAt
)

fun mapToCompany(doc: DocumentSnapshot): Company? = try {
    val name = doc.getString("name") ?: return null
    Company(
        id = doc.getLong("id") ?: 0L,
        name = name,
        description = doc.getString("description") ?: "",
        isDefault = doc.getBoolean("isDefault") ?: false,
        createdAt = doc.getLong("createdAt") ?: doc.readUpdatedAt(System.currentTimeMillis()),
        updatedAt = doc.readUpdatedAt(),
        isSynced = true
    )
} catch (e: Exception) {
    null
}

// ---------- Category ----------

fun CategoryEntity.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "subcategories" to subcategories,
    FIELD_UPDATED_AT to updatedAt.toFirestoreTimestamp(),
    FIELD_UPDATED_AT_MILLIS to updatedAt
)

fun mapToCategory(doc: DocumentSnapshot): CategoryEntity? = try {
    val name = doc.getString("name") ?: return null
    CategoryEntity(
        id = doc.getLong("id") ?: 0L,
        name = name,
        subcategories = doc.getString("subcategories") ?: "",
        updatedAt = doc.readUpdatedAt(),
        isSynced = true
    )
} catch (e: Exception) {
    null
}

// ---------- Product ----------

fun Product.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "companyId" to companyId,
    "companyName" to companyName,
    "code" to code,
    "name" to name,
    "brand" to brand,
    "category" to category,
    "subcategory" to subcategory,
    "imageUri" to imageUri,
    "mrp" to mrp,
    "discountPercent" to discountPercent,
    "gstPercent" to gstPercent,
    "purchasePrice" to purchasePrice,
    "stockQuantity" to stockQuantity,
    "notes" to notes,
    // pehle se bheje ja rahe computed fields (purane docs ke saath compatible rehne ke liye)
    "finalSalePrice" to finalSalePrice,
    "marginAmount" to marginAmount,
    "marginPercent" to marginPercent,
    FIELD_UPDATED_AT to updatedAt.toFirestoreTimestamp(),
    FIELD_UPDATED_AT_MILLIS to updatedAt
)

fun mapToProduct(doc: DocumentSnapshot): Product? = try {
    val code = doc.getString("code") ?: return null
    val name = doc.getString("name") ?: return null
    Product(
        id = doc.getLong("id") ?: 0L,
        companyId = doc.getLong("companyId") ?: 0L,
        companyName = doc.getString("companyName") ?: "",
        code = code,
        name = name,
        brand = doc.getString("brand") ?: "",
        category = doc.getString("category") ?: "",
        subcategory = doc.getString("subcategory") ?: "",
        imageUri = doc.getString("imageUri"),
        mrp = doc.getDouble("mrp") ?: 0.0,
        discountPercent = doc.getDouble("discountPercent") ?: 0.0,
        gstPercent = doc.getDouble("gstPercent") ?: 18.0,
        purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
        stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 1,
        notes = doc.getString("notes") ?: "",
        updatedAt = doc.readUpdatedAt(),
        isSynced = true
    )
} catch (e: Exception) {
    null
}
