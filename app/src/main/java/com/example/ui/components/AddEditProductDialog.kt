package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.data.StockStatus
import com.example.ui.theme.MarginOrange
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import com.example.ui.theme.ShowroomNavy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditProductDialog(
    initialProduct: Product?,
    activeCompany: Company,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        name: String,
        brand: String,
        category: String,
        subcategory: String,
        imageUri: String?,
        mrp: Double,
        discountPercent: Double,
        gstPercent: Double,
        purchasePrice: Double,
        stockQuantity: Int,
        notes: String
    ) -> Unit
) {
    var code by remember { mutableStateOf(initialProduct?.code ?: "") }
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: activeCompany.name) }
    var category by remember { mutableStateOf(initialProduct?.category ?: (categories.firstOrNull()?.name ?: "CP")) }
    var subcategory by remember { mutableStateOf(initialProduct?.subcategory ?: "") }
    var imageUri by remember { mutableStateOf(initialProduct?.imageUri) }

    var mrpText by remember { mutableStateOf(initialProduct?.mrp?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var discountText by remember { mutableStateOf(initialProduct?.discountPercent?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var gstText by remember { mutableStateOf(initialProduct?.gstPercent?.toInt()?.toString() ?: "18") }
    var purchaseText by remember { mutableStateOf(initialProduct?.purchasePrice?.let { if (it > 0) it.toInt().toString() else "" } ?: "") }
    var isPurchaseRevealed by remember { mutableStateOf(false) }
    var stockText by remember { mutableStateOf(initialProduct?.stockQuantity?.toString() ?: "10") }
    var notes by remember { mutableStateOf(initialProduct?.notes ?: "") }

    var codeError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    // Dynamic Live Calculated Pricing
    val mrpVal = mrpText.toDoubleOrNull() ?: 0.0
    val discountVal = discountText.toDoubleOrNull() ?: 0.0
    val gstVal = gstText.toDoubleOrNull() ?: 18.0
    val purchaseVal = purchaseText.toDoubleOrNull() ?: 0.0

    val livePricing by remember(mrpVal, discountVal, gstVal, purchaseVal) {
        derivedStateOf {
            PricingCalculator.calculate(
                mrp = mrpVal,
                discountPercent = discountVal,
                gstPercent = gstVal,
                purchasePrice = purchaseVal
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("add_edit_product_dialog"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Dialog Header (Pinned)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (initialProduct != null) "Edit Product" else "Add New Product",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Company: ${activeCompany.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_add_edit_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    // Scrollable Inputs Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // Image Picker Preview Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProductImagePlaceholder(
                        imageUri = imageUri,
                        category = category,
                        name = name.ifBlank { "Product" },
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("select_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Photo", fontSize = 12.sp)
                        }
                        Text(
                            text = "Or choose category icon below",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Basic Information Fields
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        codeError = false
                    },
                    label = { Text("Product Code (e.g. HW-1234) *") },
                    isError = codeError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_code")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Product Name (e.g. Wall Mixer 3-in-1) *") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand Name (e.g. ${activeCompany.name})") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_brand")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector Chips
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat.name,
                            onClick = { category = cat.name },
                            label = { Text(cat.name, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subcategory,
                    onValueChange = { subcategory = it },
                    label = { Text("Subcategory (e.g. Mixer, Basin, Shower)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_product_subcategory")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Section 6 & 7: Pricing Inputs
                Text(
                    text = "PRICING INFORMATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = mrpText,
                        onValueChange = { mrpText = it },
                        label = { Text("MRP (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_mrp")
                    )

                    OutlinedTextField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_discount")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GST Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = gstText,
                        onValueChange = { gstText = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_gst")
                    )

                    if (!isPurchaseRevealed) {
                        IconButton(
                            onClick = { isPurchaseRevealed = true },
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("btn_reveal_purchase")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Reveal Purchase Rate",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = purchaseText,
                            onValueChange = { purchaseText = it },
                            label = { Text("Purchase (₹)") },
                            trailingIcon = {
                                IconButton(onClick = { isPurchaseRevealed = false }) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = "Hide Purchase Rate")
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_purchase")
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("0", "5", "12", "18", "28").forEach { rate ->
                        FilterChip(
                            selected = gstText == rate,
                            onClick = { gstText = rate },
                            label = { Text("$rate% GST", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 7 & 8: Live Calculation Result Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "LIVE CALCULATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Taxable Sale Price:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                PricingCalculator.formatCurrency(livePricing.taxablePrice),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Final Sale Price (Incl. GST):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                PricingCalculator.formatCurrency(livePricing.finalSalePrice),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PriceGreen
                            )
                        }
                        if (purchaseVal > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Internal Margin:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${PricingCalculator.formatCurrency(livePricing.marginAmount)} (${PricingCalculator.formatPercent(livePricing.marginPercent)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MarginOrange
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val currentStockNum = stockText.toIntOrNull() ?: 0
                val liveStockStatus = when {
                    currentStockNum <= 0 -> StockStatus.OUT_OF_STOCK
                    currentStockNum <= 2 -> StockStatus.LOW_STOCK
                    else -> StockStatus.IN_STOCK
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("Stock (पीस) *") },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        val cur = stockText.toIntOrNull() ?: 1
                                        stockText = maxOf(0, cur - 1).toString()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val cur = stockText.toIntOrNull() ?: 0
                                        stockText = (cur + 1).toString()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_stock")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Live Stock Status Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (liveStockStatus) {
                                StockStatus.OUT_OF_STOCK -> Color(0xFFFEE2E2)
                                StockStatus.LOW_STOCK -> Color(0xFFFEF3C7)
                                StockStatus.IN_STOCK -> Color(0xFFDCFCE7)
                            }
                        ) {
                            Text(
                                text = when (liveStockStatus) {
                                    StockStatus.OUT_OF_STOCK -> "❌ Out of Stock"
                                    StockStatus.LOW_STOCK -> "⚠️ Low Stock (<2 pcs)"
                                    StockStatus.IN_STOCK -> "● In Stock"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (liveStockStatus) {
                                    StockStatus.OUT_OF_STOCK -> Color(0xFFB91C1C)
                                    StockStatus.LOW_STOCK -> Color(0xFFB45309)
                                    StockStatus.IN_STOCK -> Color(0xFF15803D)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Finish") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.8f)
                            .testTag("input_notes")
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
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("cancel_save_product"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }

                Button(
                    onClick = {
                        var hasError = false
                        if (code.isBlank()) {
                            codeError = true
                            hasError = true
                        }
                        if (name.isBlank()) {
                            nameError = true
                            hasError = true
                        }
                        if (!hasError) {
                            onSave(
                                code.trim(),
                                name.trim(),
                                brand.trim().ifBlank { activeCompany.name },
                                category.trim(),
                                subcategory.trim(),
                                imageUri,
                                mrpVal,
                                discountVal,
                                gstVal,
                                purchaseVal,
                                stockText.toIntOrNull() ?: 1,
                                notes.trim()
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_product_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShowroomGold,
                        contentColor = ShowroomNavy
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (initialProduct != null) "Update Product" else "Save Product",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
}
}
