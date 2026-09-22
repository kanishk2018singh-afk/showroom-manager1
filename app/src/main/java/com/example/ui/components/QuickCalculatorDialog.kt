package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.MarginOrange
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import com.example.ui.theme.ShowroomNavy
import java.text.NumberFormat
import java.util.Locale

/**
 * ⚡ Quick Counter Calculator Dialog:
 * Instantly computes on-the-spot customer billing with GST,
 * your purchase cost, and net profit when a customer is at the showroom counter.
 * Includes Customer Safe Mode protection to keep purchase rates and margins hidden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCalculatorDialog(
    isCustomerMode: Boolean,
    onDismiss: () -> Unit,
    onToggleCustomerMode: (() -> Unit)? = null,
    onAddToQuote: ((name: String, mrp: Double, discountPercent: Double, gstPercent: Double, quantity: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    var itemNameInput by remember { mutableStateOf("") }
    var mrpInput by remember { mutableStateOf("") }
    var discountPercentInput by remember { mutableStateOf("35") }
    var gstPercentInput by remember { mutableStateOf("18") }
    var quantityInput by remember { mutableStateOf("1") }
    var purchasePriceInput by remember { mutableStateOf("") }

    // Toggle: Add GST after discount (Standard in sanitaryware/hardware/tiles) vs MRP inclusive
    var isGstAddedAfterDiscount by remember { mutableStateOf(true) }

    val mrp = mrpInput.toDoubleOrNull() ?: 0.0
    val discountPercent = (discountPercentInput.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0)
    val gstPercent = (gstPercentInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val quantity = (quantityInput.toIntOrNull() ?: 1).coerceAtLeast(1)
    val purchasePrice = purchasePriceInput.toDoubleOrNull() ?: 0.0

    // Calculations
    val discountAmountPerUnit = mrp * (discountPercent / 100.0)
    val taxablePerUnit: Double
    val gstAmountPerUnit: Double
    val netRatePerUnit: Double

    if (isGstAddedAfterDiscount) {
        taxablePerUnit = (mrp - discountAmountPerUnit).coerceAtLeast(0.0)
        gstAmountPerUnit = taxablePerUnit * (gstPercent / 100.0)
        netRatePerUnit = taxablePerUnit + gstAmountPerUnit
    } else {
        netRatePerUnit = (mrp - discountAmountPerUnit).coerceAtLeast(0.0)
        taxablePerUnit = if (gstPercent > 0) netRatePerUnit / (1.0 + (gstPercent / 100.0)) else netRatePerUnit
        gstAmountPerUnit = netRatePerUnit - taxablePerUnit
    }

    val totalTaxable = taxablePerUnit * quantity
    val totalGst = gstAmountPerUnit * quantity
    val totalFinalSalePrice = netRatePerUnit * quantity

    // Internal dealer calculation (Only when Safe Mode is OFF)
    val totalCost = purchasePrice * quantity
    val totalProfit = if (purchasePrice > 0) totalTaxable - totalCost else 0.0
    val profitMarginPercent = if (totalCost > 0) (totalProfit / totalCost) * 100.0 else 0.0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("quick_calculator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PriceGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = PriceGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Counter Calculator",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "⚡ 1-Sec",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PriceGreen
                                )
                            }
                            Text(
                                text = "तुरंत फाइनल भाव व मुनाफा निकालें",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Optional Item Name
                OutlinedTextField(
                    value = itemNameInput,
                    onValueChange = { itemNameInput = it },
                    label = { Text("Item Name / Model (आइटम का नाम - Optional)") },
                    placeholder = { Text("e.g. डायवर्टर, बेसिन मिक्सर, टाइल") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // MRP Input (Large, prominent)
                OutlinedTextField(
                    value = mrpInput,
                    onValueChange = { mrpInput = it },
                    label = { Text("Enter MRP (एम.आर.पी ₹) *") },
                    placeholder = { Text("e.g. 14500") },
                    leadingIcon = {
                        Text("₹", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (mrpInput.isNotBlank()) {
                            IconButton(onClick = { mrpInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear MRP", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_mrp_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Customer Discount Quick Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Discount (छूट %):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$discountPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DiscountRed
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("15", "20", "25", "30", "35", "40", "45", "50").forEach { disc ->
                        FilterChip(
                            selected = discountPercentInput == disc,
                            onClick = { discountPercentInput = disc },
                            label = { Text("$disc%", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DiscountRed.copy(alpha = 0.15f),
                                selectedLabelColor = DiscountRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Discount & Quantity Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = discountPercentInput,
                        onValueChange = { discountPercentInput = it },
                        label = { Text("Custom Disc %") },
                        trailingIcon = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { quantityInput = it },
                        label = { Text("Quantity (पीस)") },
                        trailingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GST Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GST Rate (जी.एस.टी %):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$gstPercent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("0", "5", "12", "18", "28").forEach { gst ->
                        FilterChip(
                            selected = gstPercentInput == gst,
                            onClick = { gstPercentInput = gst },
                            label = { Text("$gst% GST", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // GST Mode Switch (Discount then GST vs MRP inclusive)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isGstAddedAfterDiscount = !isGstAddedAfterDiscount }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isGstAddedAfterDiscount) "Mode: MRP - Discount + GST (स्टैंडर्ड)" else "Mode: MRP में GST शामिल",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGstAddedAfterDiscount) "छूट के बाद GST जुड़ेगा" else "MRP में टैक्स पहले से शामिल है",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "बदलें ⇄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Purchase Rate (Only visible if Customer Mode is OFF)
                if (!isCustomerMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = purchasePriceInput,
                        onValueChange = { purchasePriceInput = it },
                        label = { Text("Your Purchase Cost (आपकी खरीद दर ₹ - Optional)") },
                        placeholder = { Text("e.g. 7000") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = MarginOrange) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calc_purchase_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    // Safe Mode Guard Badge
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ShowroomGold.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Customer Safe Mode Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF856404)
                                )
                                Text(
                                    text = "खरीद दर व डीलर मार्जिन ग्राहक से सुरक्षित छिपा हुआ है।",
                                    fontSize = 10.sp,
                                    color = Color(0xFF856404).copy(alpha = 0.85f)
                                )
                            }
                            if (onToggleCustomerMode != null) {
                                TextButton(onClick = onToggleCustomerMode) {
                                    Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF856404))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Customer Final Rate Card (Highlighted in Green)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PriceGreen.copy(alpha = 0.1f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FINAL NET PAYABLE (अंतिम देय भाव)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PriceGreen,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = currencyFormat.format(totalFinalSalePrice),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = PriceGreen
                        )

                        if (quantity > 1) {
                            Text(
                                text = "(${currencyFormat.format(netRatePerUnit)} per pc × $quantity pcs)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = PriceGreen.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Detailed Calculation Rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total MRP (${quantity} pcs):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormat.format(mrp * quantity), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (discountPercent > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount ($discountPercent%):", fontSize = 12.sp, color = DiscountRed)
                                Text("-${currencyFormat.format(discountAmountPerUnit * quantity)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DiscountRed)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxable Subtotal:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormat.format(totalTaxable), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (gstPercent > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GST ($gstPercent%):", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text("+${currencyFormat.format(totalGst)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Internal Profit Card (Only when Staff Mode is Active)
                if (!isCustomerMode && purchasePrice > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MarginOrange.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Your Net Profit (शुद्ध मुनाफा)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MarginOrange
                                )
                                Text(
                                    text = "Total Cost: ${currencyFormat.format(totalCost)} (${currencyFormat.format(purchasePrice)}/pc)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${currencyFormat.format(totalProfit)}",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MarginOrange
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", profitMarginPercent)}% Margin",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MarginOrange
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Share on WhatsApp & Add to Quotation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Share Button
                    Button(
                        onClick = {
                            if (mrp <= 0) {
                                Toast.makeText(context, "Please enter MRP first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val shareItemName = itemNameInput.ifBlank { "Item" }
                            val msg = buildString {
                                appendLine("🏷️ *SHOWROOM ESTIMATE RATE*")
                                appendLine("Item: *$shareItemName*")
                                appendLine("Qty: $quantity pcs")
                                appendLine("MRP: ${currencyFormat.format(mrp * quantity)}")
                                if (discountPercent > 0) {
                                    appendLine("Discount: $discountPercent% (-${currencyFormat.format(discountAmountPerUnit * quantity)})")
                                }
                                appendLine("Taxable: ${currencyFormat.format(totalTaxable)}")
                                if (gstPercent > 0) {
                                    appendLine("GST ($gstPercent%): +${currencyFormat.format(totalGst)}")
                                }
                                appendLine("━━━━━━━━━━━━━━━━━━━")
                                appendLine("💰 *NET PAYABLE: ${currencyFormat.format(totalFinalSalePrice)}*")
                                appendLine("_(All taxes included)_")
                            }

                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(msg)}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Rate"))
                            }
                        },
                        modifier = Modifier.weight(1.1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Add to Quotation / Bill
                    if (onAddToQuote != null) {
                        Button(
                            onClick = {
                                if (mrp <= 0) {
                                    Toast.makeText(context, "Please enter MRP first", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onAddToQuote(
                                    itemNameInput.ifBlank { "Counter Item" },
                                    mrp,
                                    discountPercent,
                                    gstPercent,
                                    quantity
                                )
                                Toast.makeText(context, "Added to Quotation / Bill!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Quote", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Clear Button
                    OutlinedButton(
                        onClick = {
                            itemNameInput = ""
                            mrpInput = ""
                            purchasePriceInput = ""
                            quantityInput = "1"
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(46.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done (वापस जाएं)", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
