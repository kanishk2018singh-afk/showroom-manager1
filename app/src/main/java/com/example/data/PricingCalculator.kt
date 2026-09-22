package com.example.data

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

data class PricingBreakdown(
    val mrp: Double,
    val discountPercent: Double,
    val discountAmount: Double,
    val taxablePrice: Double,
    val gstPercent: Double,
    val gstAmount: Double,
    val finalSalePrice: Double,
    val purchasePrice: Double,
    val marginAmount: Double,
    val marginPercent: Double
)

object PricingCalculator {

    fun calculate(
        mrp: Double,
        discountPercent: Double,
        gstPercent: Double,
        purchasePrice: Double
    ): PricingBreakdown {
        val safeMrp = maxOf(0.0, mrp)
        val safeDiscount = maxOf(0.0, minOf(100.0, discountPercent))
        val safeGst = maxOf(0.0, gstPercent)
        val safePurchase = maxOf(0.0, purchasePrice)

        val discountAmount = safeMrp * (safeDiscount / 100.0)
        val taxablePrice = maxOf(0.0, safeMrp - discountAmount)
        val gstAmount = taxablePrice * (safeGst / 100.0)
        val finalSalePrice = taxablePrice + gstAmount

        val marginAmount = taxablePrice - safePurchase
        val marginPercent = if (safePurchase > 0.0) {
            (marginAmount / safePurchase) * 100.0
        } else {
            0.0
        }

        return PricingBreakdown(
            mrp = safeMrp,
            discountPercent = safeDiscount,
            discountAmount = discountAmount,
            taxablePrice = taxablePrice,
            gstPercent = safeGst,
            gstAmount = gstAmount,
            finalSalePrice = finalSalePrice,
            purchasePrice = safePurchase,
            marginAmount = marginAmount,
            marginPercent = marginPercent
        )
    }

    private val indianFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatCurrency(amount: Double): String {
        return try {
            indianFormat.format(amount.roundToLong())
        } catch (_: Exception) {
            "₹${amount.roundToLong()}"
        }
    }

    fun formatPercent(percent: Double): String {
        return "%.1f%%".format(Locale.US, percent)
    }
}
