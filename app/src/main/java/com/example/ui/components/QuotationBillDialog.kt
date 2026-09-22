package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.data.Quotation
import com.example.data.QuotationItem
import com.example.data.QuotationType
import com.example.data.WhatsAppHelper
import com.example.util.BillPrintHelper
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import com.example.ui.theme.ShowroomNavy
import java.text.NumberFormat
import java.util.Locale

// WhatsApp Brand Green Color
val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDark = Color(0xFF128C7E)
val PaperWhite = Color(0xFFFAFAFA)
val PaperBorder = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationBillDialog(
    initialQuotation: Quotation?,
    catalogProducts: List<Product>,
    savedQuotations: List<Quotation>,
    onDismiss: () -> Unit,
    onSaveQuotation: (Quotation) -> Unit,
    onDeleteQuotation: (String) -> Unit
) {
    val context = LocalContext.current
    var currentQuotation by remember(initialQuotation) {
        mutableStateOf(initialQuotation ?: Quotation())
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCatalogPicker by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = systemBarsPadding.calculateTopPadding(), bottom = systemBarsPadding.calculateBottomPadding())
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (currentQuotation.type == QuotationType.QUOTATION) "Quotation Generator (कोटेशन)" else "Tax Invoice (पक्का बिल)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${currentQuotation.companyName} • ${currentQuotation.items.size} Items",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("quote_dialog_back")
                            ) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    BillPrintHelper.printBill(context, currentQuotation)
                                },
                                modifier = Modifier.testTag("quote_print_top_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print or Save as PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    onSaveQuotation(currentQuotation)
                                    Toast.makeText(context, "Saved to Quotation History!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("quote_save_button")
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                            }
                            IconButton(
                                onClick = {
                                    WhatsAppHelper.sendInvoice(context, currentQuotation)
                                },
                                modifier = Modifier.testTag("quote_whatsapp_top_action")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Send WhatsApp",
                                    tint = WhatsAppGreen
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Tabs: [1] Edit Bill  [2] Perfect Bill Preview  [3] History
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Edit Bill (तैयार करें)", fontSize = 13.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Bill Preview (प्रिव्यू)", fontSize = 13.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Saved (${savedQuotations.size})", fontSize = 13.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> BillEditorTab(
                                quotation = currentQuotation,
                                onQuotationChange = { currentQuotation = it },
                                onOpenCatalogPicker = { showCatalogPicker = true },
                                onOpenCustomItem = { showAddCustomDialog = true },
                                onPreviewClick = { selectedTab = 1 },
                                onWhatsAppClick = { WhatsAppHelper.sendInvoice(context, currentQuotation) },
                                onPrintBill = { BillPrintHelper.printBill(context, currentQuotation) }
                            )
                            1 -> PerfectBillPreviewTab(
                                quotation = currentQuotation,
                                onSendWhatsApp = { WhatsAppHelper.sendInvoice(context, currentQuotation) },
                                onPrintBill = { BillPrintHelper.printBill(context, currentQuotation) },
                                onCopyBillText = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Bill", currentQuotation.toWhatsAppMessage())
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Bill text copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onEditClick = { selectedTab = 0 },
                                onSaveClick = {
                                    onSaveQuotation(currentQuotation)
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            2 -> QuotationHistoryTab(
                                savedQuotations = savedQuotations,
                                onSelectQuotation = {
                                    currentQuotation = it
                                    selectedTab = 1
                                },
                                onDeleteQuotation = onDeleteQuotation,
                                onWhatsAppQuotation = { WhatsAppHelper.sendInvoice(context, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Select from Product Catalog
    if (showCatalogPicker) {
        CatalogProductPicker(
            catalogProducts = catalogProducts,
            onDismiss = { showCatalogPicker = false },
            onProductSelected = { product, qty ->
                val newItem = QuotationItem(
                    productId = product.id,
                    code = product.code,
                    name = product.name,
                    brand = product.brand,
                    category = product.category,
                    quantity = qty,
                    mrp = product.mrp,
                    discountPercent = product.discountPercent,
                    gstPercent = product.gstPercent
                )
                currentQuotation = currentQuotation.copy(items = currentQuotation.items + newItem)
                showCatalogPicker = false
            }
        )
    }

    // Modal: Add Custom Item
    if (showAddCustomDialog) {
        CustomItemDialog(
            defaultBrand = currentQuotation.companyName,
            onDismiss = { showAddCustomDialog = false },
            onAdd = { customItem ->
                currentQuotation = currentQuotation.copy(items = currentQuotation.items + customItem)
                showAddCustomDialog = false
            }
        )
    }
}

/**
 * Tab 0: Bill Editor with customer information, document switcher and item list
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillEditorTab(
    quotation: Quotation,
    onQuotationChange: (Quotation) -> Unit,
    onOpenCatalogPicker: () -> Unit,
    onOpenCustomItem: () -> Unit,
    onPreviewClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPrintBill: () -> Unit
) {
    val indianFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Type Switcher (Quotation / Tax Invoice)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuotationType.values().forEach { qType ->
                        val isSelected = quotation.type == qType
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onQuotationChange(quotation.copy(type = qType)) }
                        ) {
                            Text(
                                text = qType.displayName,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Bill / Quotation Details & Customer Information
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOMER & BILL DETAILS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = quotation.quotationNumber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Customer Name
                    OutlinedTextField(
                        value = quotation.customerName,
                        onValueChange = { onQuotationChange(quotation.copy(customerName = it)) },
                        label = { Text("Customer / Client Name (ग्राहक का नाम)") },
                        placeholder = { Text("e.g. Ramesh Patel / Sunrise Builders") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quote_customer_name"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // WhatsApp Phone Number
                    OutlinedTextField(
                        value = quotation.customerPhone,
                        onValueChange = { onQuotationChange(quotation.copy(customerPhone = it)) },
                        label = { Text("WhatsApp Mobile Number (+91)") },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quote_customer_phone"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Date & Company
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quotation.date,
                            onValueChange = { onQuotationChange(quotation.copy(date = it)) },
                            label = { Text("Date (तारीख)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = quotation.companyName,
                            onValueChange = { onQuotationChange(quotation.copy(companyName = it)) },
                            label = { Text("Showroom Brand") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Action Buttons: Add from Catalog or Add Custom Item
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenCatalogPicker,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .testTag("quote_add_catalog_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShowroomNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Catalog Product", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onOpenCustomItem,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("quote_add_custom_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Custom", fontSize = 13.sp)
                }
            }
        }

        // Section Title: Items List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ITEMS IN BILL (${quotation.items.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                if (quotation.items.isNotEmpty()) {
                    TextButton(onClick = { onQuotationChange(quotation.copy(items = emptyList())) }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        if (quotation.items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No products added to this bill yet",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Tap '+ Catalog Product' to pick products from your showroom catalog or add custom items.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenCatalogPicker,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Browse Showroom Products")
                        }
                    }
                }
            }
        } else {
            itemsIndexed(quotation.items) { index, item ->
                QuotationItemCard(
                    index = index + 1,
                    item = item,
                    onQuantityChange = { delta ->
                        val newQty = item.quantity + delta
                        val updatedItems = if (newQty > 0) {
                            quotation.items.map { if (it.id == item.id) it.copy(quantity = newQty) else it }
                        } else {
                            quotation.items.filterNot { it.id == item.id }
                        }
                        onQuotationChange(quotation.copy(items = updatedItems))
                    },
                    onDiscountChange = { newDiscount ->
                        val updatedItems = quotation.items.map {
                            if (it.id == item.id) it.copy(discountPercent = newDiscount) else it
                        }
                        onQuotationChange(quotation.copy(items = updatedItems))
                    },
                    onRemove = {
                        val updatedItems = quotation.items.filterNot { it.id == item.id }
                        onQuotationChange(quotation.copy(items = updatedItems))
                    }
                )
            }
        }

        // Live Bill Breakdown Summary Card
        if (quotation.items.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CALCULATION SUMMARY (बिल सारांश)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SummaryRow("Total MRP (${quotation.totalQuantity} pcs)", indianFormat.format(quotation.totalMrp))
                        if (quotation.totalDiscount > 0) {
                            SummaryRow("Showroom Discount", "-${indianFormat.format(quotation.totalDiscount)}", valueColor = DiscountRed)
                        }
                        SummaryRow("Taxable Amount", indianFormat.format(quotation.totalTaxable))
                        SummaryRow("CGST (9%)", "+${indianFormat.format(quotation.cgstAmount)}")
                        SummaryRow("SGST (9%)", "+${indianFormat.format(quotation.sgstAmount)}")

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GRAND TOTAL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = ShowroomNavy
                                )
                                Text(
                                    text = "Incl. all taxes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = indianFormat.format(quotation.grandTotal),
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = PriceGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = quotation.amountInWords(),
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Big WhatsApp, Print & Preview Action Bar
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onWhatsAppClick,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("quote_send_whatsapp_main"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onPrintBill,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("quote_print_bill_main"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShowroomNavy,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print / PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onPreviewClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("quote_preview_bill_main"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bill Preview & Print Format (बिल प्रिव्यू देखें)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: "Perfect Bill Type" Showroom Tax Invoice / Estimation Memo (Paper Styled)
 */
@Composable
fun PerfectBillPreviewTab(
    quotation: Quotation,
    onSendWhatsApp: () -> Unit,
    onPrintBill: () -> Unit,
    onCopyBillText: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val indianFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEFEF))
    ) {
        // Scrollable printable paper invoice card
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PaperBorder, RoundedCornerShape(8.dp)),
                    color = PaperWhite,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 1. Showroom Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quotation.companyName.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = ShowroomNavy,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = quotation.companyAddress,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Phone: ${quotation.companyPhone}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                                if (quotation.companyGstin.isNotBlank()) {
                                    Text(
                                        text = "GSTIN: ${quotation.companyGstin}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ShowroomNavy
                                    )
                                }
                            }

                            // Document Type Banner
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (quotation.type == QuotationType.QUOTATION) ShowroomGold else ShowroomNavy,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = quotation.type.badgeText,
                                    color = if (quotation.type == QuotationType.QUOTATION) ShowroomNavy else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color.LightGray,
                            thickness = 1.dp
                        )

                        // 2. Invoice / Estimate Metadata & Customer Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("BILLED TO / CUSTOMER:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = quotation.customerName.ifBlank { "Cash Customer (काउंटर ग्राहक)" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                if (quotation.customerPhone.isNotBlank()) {
                                    Text(text = "Phone: ${quotation.customerPhone}", fontSize = 11.sp, color = Color.DarkGray)
                                }
                                if (quotation.customerAddress.isNotBlank()) {
                                    Text(text = "Address: ${quotation.customerAddress}", fontSize = 11.sp, color = Color.DarkGray)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("DOCUMENT NO:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = quotation.quotationNumber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("DATE: ${quotation.date}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Perfect Bill Table Header
                        Surface(
                            color = ShowroomNavy,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(20.dp))
                                Text("Description & Code", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.weight(1.5f))
                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                                Text("Disc", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                            }
                        }

                        // Table Rows
                        if (quotation.items.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("(No items in this invoice)", color = Color.Gray, fontSize = 12.sp)
                            }
                        } else {
                            quotation.items.forEachIndexed { idx, item ->
                                val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFF8F9FA)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}", fontSize = 10.sp, modifier = Modifier.width(20.dp), color = Color.DarkGray)
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${item.brand} ${if (item.code.isNotBlank()) "• ${item.code}" else ""} (GST ${"%.0f".format(item.gstPercent)}%)",
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text("${item.quantity}", fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                                    Text(indianFormat.format(item.mrp), fontSize = 10.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.End)
                                    Text("${"%.0f".format(item.discountPercent)}%", fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End, color = DiscountRed)
                                    Text(indianFormat.format(item.totalAmount), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(65.dp), textAlign = TextAlign.End)
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Financial Calculations Summary Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(240.dp)
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total MRP:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(indianFormat.format(quotation.totalMrp), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                if (quotation.totalDiscount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discount Savings:", fontSize = 11.sp, color = DiscountRed)
                                        Text("-${indianFormat.format(quotation.totalDiscount)}", fontSize = 11.sp, color = DiscountRed, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Taxable Value:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(indianFormat.format(quotation.totalTaxable), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CGST + SGST:", fontSize = 11.sp, color = Color.DarkGray)
                                    Text("+${indianFormat.format(quotation.totalGst)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFF94A3B8))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("NET PAYABLE:", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = ShowroomNavy)
                                    Text(indianFormat.format(quotation.grandTotal), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PriceGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amount in Words
                        Text(
                            text = "Amount in Words: ${quotation.amountInWords()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 5. Terms & Signatures
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text("Terms & Conditions:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(
                                    text = quotation.notes,
                                    fontSize = 9.sp,
                                    color = Color.DarkGray,
                                    lineHeight = 12.sp
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("For ${quotation.companyName}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ShowroomNavy)
                                Spacer(modifier = Modifier.height(28.dp))
                                Text("Authorized Signatory", fontSize = 9.sp, color = Color.DarkGray, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Toolbar: WhatsApp, Copy, Save, Edit
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Button(
                    onClick = onSendWhatsApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("preview_send_whatsapp_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Bill via WhatsApp (व्हाट्सएप भेजें)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintBill,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / PDF", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCopyBillText,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onSaveClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Saved Quotations / Bills History
 */
@Composable
fun QuotationHistoryTab(
    savedQuotations: List<Quotation>,
    onSelectQuotation: (Quotation) -> Unit,
    onDeleteQuotation: (String) -> Unit,
    onWhatsAppQuotation: (Quotation) -> Unit
) {
    val indianFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    if (savedQuotations.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No saved quotations yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "When you create bills and quotations, save them to access and re-send them anytime.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(savedQuotations) { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectQuotation(quote) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = quote.customerName.ifBlank { "Cash Customer" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${quote.quotationNumber} • ${quote.date}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (quote.type == QuotationType.QUOTATION) ShowroomGold.copy(alpha = 0.2f) else ShowroomNavy.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = if (quote.type == QuotationType.QUOTATION) "QUOTATION" else "INVOICE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quote.type == QuotationType.QUOTATION) Color(0xFF856404) else ShowroomNavy
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${quote.items.size} Items (${quote.totalQuantity} pcs)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = indianFormat.format(quote.grandTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PriceGreen
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onDeleteQuotation(quote.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onWhatsAppQuotation(quote) },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual item card in the bill builder
 */
@Composable
fun QuotationItemCard(
    index: Int,
    item: QuotationItem,
    onQuantityChange: (Int) -> Unit,
    onDiscountChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    val indianFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$index",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.brand} ${if (item.code.isNotBlank()) "• ${item.code}" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing details & Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MRP: ${indianFormat.format(item.mrp)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DiscountRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "-${"%.0f".format(item.discountPercent)}% Disc",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DiscountRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Net: ${indianFormat.format(item.totalAmount)} (incl. ${"%.0f".format(item.gstPercent)}% GST)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PriceGreen
                    )
                }

                // Quantity Stepper: [-] Qty [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(-1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "${item.quantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = { onQuantityChange(1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

/**
 * Catalog Search & Selection Dialog
 */
@Composable
fun CatalogProductPicker(
    catalogProducts: List<Product>,
    onDismiss: () -> Unit,
    onProductSelected: (Product, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, catalogProducts) {
        if (searchQuery.isBlank()) catalogProducts
        else catalogProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.code.contains(searchQuery, ignoreCase = true) ||
                    it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick Product from Catalog",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, code (e.g. HW-101)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_quote"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { product ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product, 1) }
                                .testTag("quote_pick_${product.code}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${product.brand} • ${product.code} • ${product.category}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = PricingCalculator.formatCurrency(product.mrp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "-${product.discountPercent.toInt()}% Disc",
                                        fontSize = 10.sp,
                                        color = DiscountRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Item Dialog for labor, plumbing fittings, or off-catalog products
 */
@Composable
fun CustomItemDialog(
    defaultBrand: String,
    onDismiss: () -> Unit,
    onAdd: (QuotationItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(defaultBrand) }
    var qtyStr by remember { mutableStateOf("1") }
    var mrpStr by remember { mutableStateOf("") }
    var discStr by remember { mutableStateOf("0") }
    var gstStr by remember { mutableStateOf("18") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Add Custom Item / Service",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name / Description *") },
                    placeholder = { Text("e.g. Angle Valve / Plumbing Fitting / Installation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Item Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mrpStr,
                        onValueChange = { mrpStr = it },
                        label = { Text("Rate / MRP (₹) *") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discStr,
                        onValueChange = { discStr = it },
                        label = { Text("Discount %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gstStr,
                        onValueChange = { gstStr = it },
                        label = { Text("GST %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val mrp = mrpStr.toDoubleOrNull() ?: 0.0
                            val qty = qtyStr.toIntOrNull() ?: 1
                            val disc = discStr.toDoubleOrNull() ?: 0.0
                            val gst = gstStr.toDoubleOrNull() ?: 18.0
                            if (name.isNotBlank() && mrp > 0) {
                                onAdd(
                                    QuotationItem(
                                        name = name,
                                        code = code,
                                        brand = brand,
                                        quantity = qty,
                                        mrp = mrp,
                                        discountPercent = disc,
                                        gstPercent = gst
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && mrpStr.isNotBlank()
                    ) {
                        Text("Add to Bill")
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
