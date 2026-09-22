package com.example.data

object CsvHelper {

    fun exportToCsv(products: List<Product>, includeInternalCosts: Boolean): String {
        val sb = StringBuilder()
        if (includeInternalCosts) {
            sb.appendLine("Code,Name,Brand,Category,Subcategory,MRP,Discount %,Taxable Price,GST %,Sale Price,Purchase Price,Margin ₹,Margin %,Stock,Notes")
            for (p in products) {
                val pricing = p.pricing
                sb.appendLine(
                    "${escape(p.code)},${escape(p.name)},${escape(p.brand)},${escape(p.category)},${escape(p.subcategory)}," +
                    "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.1f,%d,${escape(p.notes)}".format(
                        p.mrp,
                        p.discountPercent,
                        pricing.taxablePrice,
                        p.gstPercent,
                        pricing.finalSalePrice,
                        p.purchasePrice,
                        pricing.marginAmount,
                        pricing.marginPercent,
                        p.stockQuantity
                    )
                )
            }
        } else {
            sb.appendLine("Code,Name,Brand,Category,Subcategory,MRP,Discount %,GST %,Sale Price,Notes")
            for (p in products) {
                val pricing = p.pricing
                sb.appendLine(
                    "${escape(p.code)},${escape(p.name)},${escape(p.brand)},${escape(p.category)},${escape(p.subcategory)}," +
                    "%.2f,%.2f,%.2f,%.2f,${escape(p.notes)}".format(
                        p.mrp,
                        p.discountPercent,
                        p.gstPercent,
                        pricing.finalSalePrice
                    )
                )
            }
        }
        return sb.toString()
    }

    fun parseCsv(
        csvContent: String,
        targetCompanyId: Long,
        targetCompanyName: String
    ): Pair<List<Product>, List<String>> {
        val parsedProducts = mutableListOf<Product>()
        val warnings = mutableListOf<String>()
        val lines = csvContent.lines().filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return Pair(emptyList(), listOf("File is empty"))
        }

        // Header detection
        val header = lines.first().lowercase()
        val hasHeader = header.contains("code") || header.contains("mrp") || header.contains("name")
        val dataLines = if (hasHeader) lines.drop(1) else lines

        for ((index, line) in dataLines.withIndex()) {
            val tokens = parseCsvLine(line)
            if (tokens.size < 4) {
                warnings.add("Row ${index + 1}: Skipped (insufficient columns)")
                continue
            }

            try {
                // Expected order loosely: Code, Name, Brand, Category, MRP, Discount, GST, Purchase
                val code = tokens.getOrNull(0)?.trim() ?: "PRD-${System.currentTimeMillis() % 10000}"
                val name = tokens.getOrNull(1)?.trim() ?: "Item"
                val brand = tokens.getOrNull(2)?.trim()?.ifBlank { targetCompanyName } ?: targetCompanyName
                val category = tokens.getOrNull(3)?.trim()?.ifBlank { "CP" } ?: "CP"
                val mrp = tokens.getOrNull(4)?.toDoubleOrNull() ?: 1000.0
                val discount = tokens.getOrNull(5)?.toDoubleOrNull() ?: 0.0
                val gst = tokens.getOrNull(6)?.toDoubleOrNull() ?: 18.0
                val purchase = tokens.getOrNull(7)?.toDoubleOrNull() ?: (mrp * 0.5)

                parsedProducts.add(
                    Product(
                        companyId = targetCompanyId,
                        companyName = targetCompanyName,
                        code = code,
                        name = name,
                        brand = brand,
                        category = category,
                        subcategory = "",
                        mrp = mrp,
                        discountPercent = discount,
                        gstPercent = gst,
                        purchasePrice = purchase,
                        stockQuantity = 10,
                        notes = "Imported from Excel/CSV"
                    )
                )
            } catch (e: Exception) {
                warnings.add("Row ${index + 1}: Parse error (${e.message})")
            }
        }

        return Pair(parsedProducts, warnings)
    }

    private fun escape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    cur.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i++
        }
        result.add(cur.toString())
        return result
    }
}
