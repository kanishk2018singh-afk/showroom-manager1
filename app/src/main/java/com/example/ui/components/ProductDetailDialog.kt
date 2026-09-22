package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.data.StockStatus
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.MarginOrange
import com.example.ui.theme.PriceGreen

@Composable
fun ProductDetailDialog(
    product: Product,
    isCustomerMode: Boolean,
    onDismiss: () -> Unit,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onAddToQuote: (Product) -> Unit = {},
    onAdjustStock: ((Product, Int) -> Unit)? = null
) {
    val pricing = product.pricing

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("product_detail_dialog"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header with Close Button (Pinned)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_detail_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    // Scrollable Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Hero Image + Main Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductImagePlaceholder(
                        imageUri = product.imageUri,
                        category = product.category,
                        name = product.name,
                        modifier = Modifier.size(90.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brand: ${product.brand}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Code: ${product.code}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Category: ${product.category}${if (product.subcategory.isNotBlank()) " / ${product.subcategory}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 7: Pricing Calculation Flow
                Text(
                    text = "PRICING BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PricingRow(
                            label = "MRP",
                            value = PricingCalculator.formatCurrency(pricing.mrp),
                            isBold = false
                        )
                        PricingRow(
                            label = "Discount (${pricing.discountPercent.toInt()}%)",
                            value = "-${PricingCalculator.formatCurrency(pricing.discountAmount)}",
                            color = DiscountRed
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        PricingRow(
                            label = "Taxable Price",
                            value = PricingCalculator.formatCurrency(pricing.taxablePrice),
                            isBold = true
                        )
                        PricingRow(
                            label = "GST (${pricing.gstPercent.toInt()}%)",
                            value = "+${PricingCalculator.formatCurrency(pricing.gstAmount)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        PricingRow(
                            label = "Final Sale Price",
                            value = PricingCalculator.formatCurrency(pricing.finalSalePrice),
                            isBold = true,
                            fontSize = 18.sp,
                            color = PriceGreen
                        )
                    }
                }

                // Add to Quotation / Bill Quick Action Button
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onAddToQuote(product)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("detail_add_to_quote_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add to Quotation / Bill (कोटेशन में जोड़ें)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Section 8: Internal Margin Breakdown (if not in Customer Mode)
                if (!isCustomerMode) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "INTERNAL MARGIN (Staff Only)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MarginOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PricingRow(
                                label = "Taxable Sale Price",
                                value = PricingCalculator.formatCurrency(pricing.taxablePrice)
                            )
                            PricingRow(
                                label = "Purchase Price",
                                value = PricingCalculator.formatCurrency(pricing.purchasePrice)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            PricingRow(
                                label = "Margin ₹",
                                value = PricingCalculator.formatCurrency(pricing.marginAmount),
                                isBold = true,
                                color = MarginOrange
                            )
                            PricingRow(
                                label = "Margin %",
                                value = PricingCalculator.formatPercent(pricing.marginPercent),
                                isBold = true,
                                color = MarginOrange
                            )
                        }
                    }
                }

                // Stock Status & Quick Adjustment
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (product.stockStatus) {
                            StockStatus.OUT_OF_STOCK -> Color(0xFFFEE2E2)
                            StockStatus.LOW_STOCK -> Color(0xFFFEF3C7)
                            StockStatus.IN_STOCK -> Color(0xFFDCFCE7)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when (product.stockStatus) {
                                    StockStatus.OUT_OF_STOCK -> "❌ Out of Stock (स्टॉक खत्म)"
                                    StockStatus.LOW_STOCK -> "⚠️ Low Stock (<2 pcs) (कम स्टॉक)"
                                    StockStatus.IN_STOCK -> "● In Stock (गोडाउन में उपलब्ध)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = when (product.stockStatus) {
                                    StockStatus.OUT_OF_STOCK -> Color(0xFFB91C1C)
                                    StockStatus.LOW_STOCK -> Color(0xFFB45309)
                                    StockStatus.IN_STOCK -> Color(0xFF15803D)
                                }
                            )
                            Text(
                                text = "Current Available: ${product.stockQuantity} pcs",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }

                        if (onAdjustStock != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { onAdjustStock(product, -1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Stock", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${product.stockQuantity}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { onAdjustStock(product, 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Stock", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                if (product.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: ${product.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            HorizontalDivider()

            // Action Buttons (Pinned, clear spacing, never cut off!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDeleteClick(product)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("detail_delete_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onDismiss()
                        onEditClick(product)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("detail_edit_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}
}

@Composable
private fun PricingRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
