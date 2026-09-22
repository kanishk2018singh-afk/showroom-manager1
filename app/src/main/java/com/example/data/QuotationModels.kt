package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

enum class QuotationType(val displayName: String, val badgeText: String) {
    QUOTATION("Quotation / Estimate (कोटेशन)", "ESTIMATE / QUOTATION"),
    TAX_INVOICE("Tax Invoice / GST Bill (पक्का बिल)", "TAX INVOICE")
}

data class QuotationItem(
    val id: String = UUID.randomUUID().toString(),
    val productId: Long? = null,
    val code: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val quantity: Int = 1,
    val mrp: Double = 0.0,
    val discountPercent: Double = 0.0,
    val gstPercent: Double = 18.0
) {
    val unitDiscountAmount: Double
        get() = mrp * (discountPercent.coerceIn(0.0, 100.0) / 100.0)

    val unitTaxablePrice: Double
        get() = maxOf(0.0, mrp - unitDiscountAmount)

    val unitGstAmount: Double
        get() = unitTaxablePrice * (gstPercent.coerceAtLeast(0.0) / 100.0)

    val unitFinalPrice: Double
        get() = unitTaxablePrice + unitGstAmount

    val totalMrp: Double
        get() = mrp * quantity

    val totalDiscount: Double
        get() = unitDiscountAmount * quantity

    val totalTaxable: Double
        get() = unitTaxablePrice * quantity

    val totalGst: Double
        get() = unitGstAmount * quantity

    val totalAmount: Double
        get() = unitFinalPrice * quantity
}

data class Quotation(
    val id: String = UUID.randomUUID().toString(),
    val quotationNumber: String = generateNumber(),
    val type: QuotationType = QuotationType.QUOTATION,
    val companyName: String = "Hindware Showroom",
    val companyAddress: String = "12/A Sanitary Market, Ring Road",
    val companyPhone: String = "+91 98765 43210",
    val companyGstin: String = "07AAAAA0000A1Z5",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val date: String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
    val items: List<QuotationItem> = emptyList(),
    val notes: String = "1. Quotation valid for 15 days.\n2. Goods once sold cannot be returned without bill.\n3. 100% genuine manufacturer warranty."
) {
    val totalQuantity: Int
        get() = items.sumOf { it.quantity }

    val totalMrp: Double
        get() = items.sumOf { it.totalMrp }

    val totalDiscount: Double
        get() = items.sumOf { it.totalDiscount }

    val totalTaxable: Double
        get() = items.sumOf { it.totalTaxable }

    val totalGst: Double
        get() = items.sumOf { it.totalGst }

    val cgstAmount: Double
        get() = totalGst / 2.0

    val sgstAmount: Double
        get() = totalGst / 2.0

    val grandTotal: Double
        get() = items.sumOf { it.totalAmount }

    val grandTotalRounded: Long
        get() = grandTotal.roundToLong()

    /**
     * Converts Grand Total amount to Indian Currency Words (e.g. Rupees Twelve Thousand...)
     */
    fun amountInWords(): String {
        return NumberToWordsConverter.convert(grandTotalRounded)
    }

    /**
     * Formats this quotation / invoice into a professional WhatsApp receipt message.
     */
    fun toWhatsAppMessage(): String {
        val indianFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        fun fmt(amt: Double) = try { indianFormat.format(amt.roundToLong()) } catch (_: Exception) { "₹${amt.roundToLong()}" }

        return buildString {
            appendLine("🧾 *${companyName.uppercase()}*")
            if (companyAddress.isNotBlank()) appendLine("📍 $companyAddress")
            if (companyPhone.isNotBlank()) appendLine("📞 $companyPhone")
            if (companyGstin.isNotBlank()) appendLine("🏢 GSTIN: $companyGstin")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📑 *${type.badgeText}*")
            appendLine("📄 *Doc No:* $quotationNumber")
            appendLine("📅 *Date:* $date")
            if (customerName.isNotBlank()) appendLine("👤 *Customer:* $customerName")
            if (customerPhone.isNotBlank()) appendLine("📱 *Phone:* $customerPhone")
            if (customerAddress.isNotBlank()) appendLine("🏠 *Address:* $customerAddress")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("*PARTICULARS & ITEMS:*")

            if (items.isEmpty()) {
                appendLine("_(No items added)_")
            } else {
                items.forEachIndexed { index, item ->
                    val num = index + 1
                    appendLine("$num️⃣ *${item.name}* ${if (item.code.isNotBlank()) "(${item.code})" else ""}")
                    if (item.brand.isNotBlank()) appendLine("   Brand: ${item.brand}")
                    appendLine("   Qty: ${item.quantity} | MRP: ${fmt(item.mrp)} | Disc: ${"%.0f".format(item.discountPercent)}%")
                    appendLine("   Taxable: ${fmt(item.totalTaxable)} | GST (${"%.0f".format(item.gstPercent)}%): ${fmt(item.totalGst)}")
                    appendLine("   *Net Amount: ${fmt(item.totalAmount)}*")
                    appendLine()
                }
            }

            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *Total MRP:* ${fmt(totalMrp)}")
            if (totalDiscount > 0) {
                appendLine("🏷️ *Discount Savings:* -${fmt(totalDiscount)}")
            }
            appendLine("📦 *Taxable Amount:* ${fmt(totalTaxable)}")
            appendLine("📊 *CGST:* ${fmt(cgstAmount)} | *SGST:* ${fmt(sgstAmount)}")
            appendLine("⭐ *GRAND TOTAL:* *${fmt(grandTotal)}*")
            appendLine("🔤 *In Words:* ${amountInWords()}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")

            if (notes.isNotBlank()) {
                appendLine("📝 *Terms & Notes:*")
                appendLine(notes)
                appendLine()
            }
            appendLine("🙏 *Thank you for your valuable business!*")
        }
    }

    companion object {
        fun generateNumber(): String {
            val randomDigits = (100..999).random()
            val year = SimpleDateFormat("yy", Locale.getDefault()).format(Date())
            return "QT-$year-$randomDigits"
        }
    }
}

object WhatsAppHelper {
    /**
     * Sends the formatted invoice directly via WhatsApp to the customer's phone number,
     * or opens the WhatsApp contact chooser.
     */
    fun sendInvoice(context: Context, quotation: Quotation) {
        val text = quotation.toWhatsAppMessage()
        val cleanPhone = sanitizeIndianPhone(quotation.customerPhone)

        try {
            if (cleanPhone.isNotBlank()) {
                // Direct WhatsApp to customer's number
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage("com.whatsapp")
                }
                // Try WhatsApp directly, fallback to general browser/chooser
                try {
                    context.startActivity(intent)
                    return
                } catch (_: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(browserIntent)
                    return
                }
            }

            // Fallback: General WhatsApp / messaging share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage("com.whatsapp")
            }
            try {
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                // If WhatsApp is not installed, open standard Android share sheet
                val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }, "Share Bill / Quotation")
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sanitizeIndianPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91" + digits.substring(1)
            digits.length == 12 && digits.startsWith("91") -> digits
            digits.isNotBlank() -> digits
            else -> ""
        }
    }
}

object NumberToWordsConverter {
    private val units = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )

    private val tens = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    fun convert(n: Long): String {
        if (n == 0L) return "Zero Rupees Only"
        if (n < 0L) return "Minus " + convert(-n)

        val words = StringBuilder()

        val crores = n / 10000000L
        var remainder = n % 10000000L

        val lakhs = remainder / 100000L
        remainder %= 100000L

        val thousands = remainder / 1000L
        remainder %= 1000L

        val hundreds = remainder / 100L
        remainder %= 100L

        if (crores > 0) words.append(convertUnderThousand(crores.toInt())).append(" Crore ")
        if (lakhs > 0) words.append(convertUnderThousand(lakhs.toInt())).append(" Lakh ")
        if (thousands > 0) words.append(convertUnderThousand(thousands.toInt())).append(" Thousand ")
        if (hundreds > 0) words.append(convertUnderThousand(hundreds.toInt())).append(" Hundred ")

        if (remainder > 0) {
            if (words.isNotEmpty()) words.append("and ")
            words.append(convertUnderThousand(remainder.toInt())).append(" ")
        }

        return "Rupees " + words.toString().trim() + " Only"
    }

    private fun convertUnderThousand(n: Int): String {
        var num = n
        val sb = StringBuilder()
        if (num >= 100) {
            sb.append(units[num / 100]).append(" Hundred ")
            num %= 100
        }
        if (num in 1..19) {
            sb.append(units[num])
        } else if (num >= 20) {
            sb.append(tens[num / 10])
            if (num % 10 > 0) {
                sb.append(" ").append(units[num % 10])
            }
        }
        return sb.toString().trim()
    }
}
