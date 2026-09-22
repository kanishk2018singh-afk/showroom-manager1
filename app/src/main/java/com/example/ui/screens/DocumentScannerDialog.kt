package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Company
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DetectedProductItem(
    val selected: Boolean = true,
    val code: String,
    val name: String,
    val brand: String,
    val category: String,
    val subcategory: String,
    val mrp: Double,
    val discountPercent: Double,
    val gstPercent: Double,
    val purchasePrice: Double
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentScannerDialog(
    activeCompany: Company,
    onDismiss: () -> Unit,
    onSaveDetectedProducts: (List<Product>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedDocType by remember { mutableStateOf("📷 Bill Image") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var hasAnalyzed by remember { mutableStateOf(false) }

    val detectedItems = remember {
        mutableStateListOf(
            DetectedProductItem(
                selected = true,
                code = "INV-${activeCompany.name.take(2).uppercase()}-101",
                name = "Wall Mixer with Telephonic Shower Arrangement",
                brand = activeCompany.name,
                category = "CP",
                subcategory = "Mixer",
                mrp = 7450.0,
                discountPercent = 30.0,
                gstPercent = 18.0,
                purchasePrice = 3800.0
            ),
            DetectedProductItem(
                selected = true,
                code = "INV-${activeCompany.name.take(2).uppercase()}-102",
                name = "Counter Top Vessel Basin Glazed Finish",
                brand = activeCompany.name,
                category = "Sanitary",
                subcategory = "Basin",
                mrp = 6200.0,
                discountPercent = 25.0,
                gstPercent = 18.0,
                purchasePrice = 3100.0
            ),
            DetectedProductItem(
                selected = true,
                code = "INV-${activeCompany.name.take(2).uppercase()}-103",
                name = "Overhead 5-Spray Shower Rose Chrome",
                brand = activeCompany.name,
                category = "CP",
                subcategory = "Shower",
                mrp = 2850.0,
                discountPercent = 35.0,
                gstPercent = 18.0,
                purchasePrice = 1350.0
            ),
            DetectedProductItem(
                selected = true,
                code = "INV-${activeCompany.name.take(2).uppercase()}-104",
                name = "Floor Drain Stainless Steel Anti-Cockroach Trap",
                brand = activeCompany.name,
                category = "Hardware",
                subcategory = "Drainer",
                mrp = 950.0,
                discountPercent = 20.0,
                gstPercent = 18.0,
                purchasePrice = 450.0
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("document_scanner_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ShowroomGold.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ShowroomGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Intelligent Document Import",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Extract products automatically from bills & PDFs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Document Type Chips
                Text(
                    text = "Select Document Source:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("📷 Bill Image", "📄 Invoice PDF", "📊 Excel Sheet", "📋 XML / Word").forEach { docType ->
                        FilterChip(
                            selected = selectedDocType == docType,
                            onClick = {
                                selectedDocType = docType
                                hasAnalyzed = false
                            },
                            label = { Text(docType, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Document Selection / OCR trigger box
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scanning document & extracting product lines...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Document: Sample Distributor Wholesale Invoice ($selectedDocType)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isAnalyzing = true
                                    coroutineScope.launch {
                                        delay(800)
                                        isAnalyzing = false
                                        hasAnalyzed = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (hasAnalyzed) "Re-Scan Document" else "Scan & Extract Products", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Review Before Save Section (Section 22 requirement!)
                Text(
                    text = "REVIEW DETECTED PRODUCTS BEFORE SAVE:",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(detectedItems) { index, item ->
                        val pricing = PricingCalculator.calculate(
                            mrp = item.mrp,
                            discountPercent = item.discountPercent,
                            gstPercent = item.gstPercent,
                            purchasePrice = item.purchasePrice
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detectedItems[index] = item.copy(selected = !item.selected)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.selected,
                                    onCheckedChange = { checked ->
                                        detectedItems[index] = item.copy(selected = checked)
                                    }
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        fontSize = 13.sp
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.code,
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                        Text(
                                            text = "${item.category} • ${item.subcategory}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "MRP: ${PricingCalculator.formatCurrency(item.mrp)} • Disc: ${item.discountPercent.toInt()}% • GST: ${item.gstPercent.toInt()}%",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Sale",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = PricingCalculator.formatCurrency(pricing.finalSalePrice),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PriceGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Save Action
                val selectedCount = detectedItems.count { it.selected }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val productsToSave = detectedItems.filter { it.selected }.map { item ->
                                Product(
                                    companyId = activeCompany.id,
                                    companyName = activeCompany.name,
                                    code = item.code,
                                    name = item.name,
                                    brand = item.brand,
                                    category = item.category,
                                    subcategory = item.subcategory,
                                    mrp = item.mrp,
                                    discountPercent = item.discountPercent,
                                    gstPercent = item.gstPercent,
                                    purchasePrice = item.purchasePrice,
                                    stockQuantity = 10,
                                    notes = "Extracted via $selectedDocType"
                                )
                            }
                            onSaveDetectedProducts(productsToSave)
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("save_detected_products_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save ($selectedCount)")
                    }
                }
            }
        }
    }
}
