package com.example.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * One-to-Many Room relationship connecting a [Company] with its list of [Product]s.
 * Used for multi-company product management and batch retrieval.
 */
data class CompanyWithProducts(
    @Embedded
    val company: Company,

    @Relation(
        parentColumn = "id",
        entityColumn = "companyId"
    )
    val products: List<Product> = emptyList()
)
