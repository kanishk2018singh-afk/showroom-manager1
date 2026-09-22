package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.Quotation
import com.example.data.QuotationType
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Utility to print quotations and tax invoices using Android PrintManager.
 * Generates an HTML invoice document that supports:
 * - Direct Wi-Fi / Network / Cloud printing
 * - Bluetooth POS / Thermal Receipt Printers (80mm / 58mm)
 * - Native Android "Save as PDF" to save or share as a PDF file
 */
object BillPrintHelper {

    fun printBill(context: Context, quotation: Quotation) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

        fun fmt(amt: Double): String = try {
            currencyFormat.format(amt.roundToLong())
        } catch (_: Exception) {
            "₹${amt.roundToLong()}"
        }

        val isTaxInvoice = quotation.type == QuotationType.TAX_INVOICE
        val docTitle = if (isTaxInvoice) "TAX INVOICE" else "PRICE QUOTATION / ESTIMATE"

        val htmlContent = buildString {
            append("<!DOCTYPE html>")
            append("<html><head><meta charset='utf-8'>")
            append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
            append("<title>${quotation.quotationNumber}</title>")
            append("<style>")
            append("""
                * { box-sizing: border-box; }
                body {
                    font-family: 'Segoe UI', -apple-system, Roboto, Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 16px;
                    color: #0f172a;
                    background: #ffffff;
                    font-size: 12px;
                    line-height: 1.4;
                }
                .invoice-box {
                    max-width: 800px;
                    margin: auto;
                    border: 1px solid #cbd5e1;
                    padding: 20px;
                    border-radius: 8px;
                }
                .header {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    border-bottom: 2px solid #0f172a;
                    padding-bottom: 14px;
                    margin-bottom: 16px;
                }
                .showroom-title {
                    font-size: 20px;
                    font-weight: 800;
                    color: #0f172a;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .showroom-sub {
                    font-size: 11px;
                    color: #475569;
                    margin-top: 3px;
                }
                .badge {
                    display: inline-block;
                    padding: 5px 12px;
                    background: ${if (isTaxInvoice) "#0f172a" else "#d97706"};
                    color: #ffffff;
                    font-weight: 800;
                    border-radius: 4px;
                    font-size: 11px;
                    text-align: center;
                    letter-spacing: 0.5px;
                }
                .doc-meta {
                    text-align: right;
                    font-size: 11px;
                    margin-top: 6px;
                }
                .party-info {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 16px;
                    background: #f8fafc;
                    padding: 10px 14px;
                    border-radius: 6px;
                    border: 1px solid #e2e8f0;
                }
                .party-col {
                    width: 48%;
                }
                .party-title {
                    font-size: 10px;
                    font-weight: 700;
                    color: #64748b;
                    text-transform: uppercase;
                    margin-bottom: 4px;
                }
                .customer-name {
                    font-size: 14px;
                    font-weight: 700;
                    color: #0f172a;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 10px;
                    margin-bottom: 16px;
                }
                th {
                    background-color: #0f172a;
                    color: #ffffff;
                    text-align: left;
                    padding: 7px 8px;
                    font-size: 11px;
                    font-weight: 600;
                }
                td {
                    border-bottom: 1px solid #e2e8f0;
                    padding: 7px 8px;
                    font-size: 11px;
                }
                tr:nth-child(even) td {
                    background-color: #f8fafc;
                }
                .text-right { text-align: right; }
                .text-center { text-align: center; }
                .summary-wrap {
                    display: flex;
                    justify-content: space-between;
                    align-items: flex-start;
                    margin-top: 10px;
                }
                .words-wrap {
                    width: 50%;
                    font-size: 11px;
                    padding-top: 6px;
                }
                .summary-table {
                    width: 45%;
                    border-collapse: collapse;
                    margin-top: 0;
                }
                .summary-table td {
                    padding: 4px 8px;
                    font-size: 11px;
                    border: none;
                }
                .grand-total {
                    font-size: 15px;
                    font-weight: 800;
                    color: #15803d;
                    background: #f0fdf4 !important;
                    border-top: 2px solid #0f172a !important;
                    border-bottom: 2px solid #0f172a !important;
                }
                .terms {
                    margin-top: 24px;
                    font-size: 10px;
                    color: #64748b;
                    border-top: 1px solid #e2e8f0;
                    padding-top: 10px;
                }
                .signatures {
                    margin-top: 36px;
                    display: flex;
                    justify-content: space-between;
                    font-size: 11px;
                    font-weight: 600;
                }
                .sign-box {
                    border-top: 1px dashed #94a3b8;
                    width: 160px;
                    padding-top: 4px;
                    text-align: center;
                }

                @media print {
                    body { margin: 0; padding: 0; }
                    .invoice-box { border: none; padding: 0; }
                    @page { margin: 10mm; size: auto; }
                }
            """.trimIndent())
            append("</style></head><body>")

            append("<div class='invoice-box'>")

            // Header Row
            append("<div class='header'>")
            append("<div>")
            append("<div class='showroom-title'>${quotation.companyName.ifBlank { "SHOWROOM CATALOG & BILLING" }}</div>")
            append("<div class='showroom-sub'>${quotation.companyAddress}</div>")
            append("<div class='showroom-sub'>Phone: <strong>${quotation.companyPhone}</strong></div>")
            if (quotation.companyGstin.isNotBlank()) {
                append("<div class='showroom-sub'>GSTIN: <strong>${quotation.companyGstin}</strong></div>")
            }
            append("</div>")

            append("<div>")
            append("<div class='badge'>$docTitle</div>")
            append("<div class='doc-meta'>")
            append("<strong>Bill No:</strong> ${quotation.quotationNumber}<br>")
            append("<strong>Date:</strong> ${quotation.date}<br>")
            append("</div>")
            append("</div>")
            append("</div>")

            // Customer Info Box
            append("<div class='party-info'>")
            append("<div class='party-col'>")
            append("<div class='party-title'>Billed To / Customer</div>")
            append("<div class='customer-name'>${quotation.customerName.ifBlank { "Cash Customer (काउंटर ग्राहक)" }}</div>")
            if (quotation.customerPhone.isNotBlank()) {
                append("<div>Phone: ${quotation.customerPhone}</div>")
            }
            if (quotation.customerAddress.isNotBlank()) {
                append("<div>Address: ${quotation.customerAddress}</div>")
            }
            append("</div>")

            append("<div class='party-col text-right'>")
            append("<div class='party-title'>Document Summary</div>")
            append("<div>Items Count: <strong>${quotation.items.size} items (${quotation.totalQuantity} pcs)</strong></div>")
            append("<div>Status: <strong>Confirmed</strong></div>")
            append("</div>")
            append("</div>")

            // Items Table
            append("<table>")
            append("<thead>")
            append("<tr>")
            append("<th class='text-center' style='width: 25px;'>#</th>")
            append("<th>Item Description</th>")
            append("<th class='text-center' style='width: 35px;'>Qty</th>")
            append("<th class='text-right' style='width: 65px;'>MRP</th>")
            append("<th class='text-center' style='width: 45px;'>Disc</th>")
            append("<th class='text-right' style='width: 70px;'>Rate</th>")
            append("<th class='text-right' style='width: 80px;'>Total</th>")
            append("</tr>")
            append("</thead><tbody>")

            quotation.items.forEachIndexed { index, item ->
                append("<tr>")
                append("<td class='text-center'>${index + 1}</td>")
                append("<td><strong>${item.name}</strong>")
                if (item.brand.isNotBlank() || item.code.isNotBlank()) {
                    append("<br><small style='color: #64748b;'>${item.brand}${if (item.code.isNotBlank()) " • Code: ${item.code}" else ""}${if (item.gstPercent > 0) " • GST: ${item.gstPercent.toInt()}%" else ""}</small>")
                }
                append("</td>")
                append("<td class='text-center'><strong>${item.quantity}</strong></td>")
                append("<td class='text-right'>${fmt(item.mrp)}</td>")
                append("<td class='text-center'>${if (item.discountPercent > 0) "${item.discountPercent.toInt()}%" else "-"}</td>")
                append("<td class='text-right'>${fmt(item.unitFinalPrice)}</td>")
                append("<td class='text-right'><strong>${fmt(item.totalAmount)}</strong></td>")
                append("</tr>")
            }

            append("</tbody></table>")

            // Financial Breakdown & Amount in Words
            append("<div class='summary-wrap'>")
            append("<div class='words-wrap'>")
            append("<strong>Amount in Words:</strong><br>")
            append("<em>${quotation.amountInWords()}</em>")
            append("</div>")

            append("<table class='summary-table'>")
            append("<tr><td>Total MRP (${quotation.totalQuantity} pcs):</td><td class='text-right'>${fmt(quotation.totalMrp)}</td></tr>")
            if (quotation.totalDiscount > 0) {
                append("<tr><td style='color: #b91c1c;'>Showroom Discount:</td><td class='text-right' style='color: #b91c1c;'>-${fmt(quotation.totalDiscount)}</td></tr>")
            }
            append("<tr><td>Taxable Value:</td><td class='text-right'>${fmt(quotation.totalTaxable)}</td></tr>")
            if (quotation.totalGst > 0) {
                append("<tr><td>CGST (9%):</td><td class='text-right'>+${fmt(quotation.cgstAmount)}</td></tr>")
                append("<tr><td>SGST (9%):</td><td class='text-right'>+${fmt(quotation.sgstAmount)}</td></tr>")
            }
            append("<tr class='grand-total'><td><strong>NET PAYABLE:</strong></td><td class='text-right'><strong>${fmt(quotation.grandTotal)}</strong></td></tr>")
            append("</table>")
            append("</div>")

            // Terms & Conditions
            append("<div class='terms'>")
            append("<strong>Terms & Conditions:</strong><br>")
            quotation.notes.lines().forEach { line ->
                if (line.isNotBlank()) append("• $line<br>")
            }
            append("</div>")

            // Signatures
            append("<div class='signatures'>")
            append("<div class='sign-box'>Customer Signature</div>")
            append("<div class='sign-box'>For ${quotation.companyName}<br><small>(Authorized Signatory)</small></div>")
            append("</div>")

            append("</div>") // invoice-box
            append("</body></html>")
        }

        // Print via WebView and PrintManager
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Bill_${quotation.quotationNumber}")
                printManager?.print(
                    "Bill_${quotation.quotationNumber}",
                    printAdapter,
                    PrintAttributes.Builder()
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }
}
