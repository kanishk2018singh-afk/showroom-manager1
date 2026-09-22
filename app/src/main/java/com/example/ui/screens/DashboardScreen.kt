package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Company
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.ui.DashboardStats
import com.example.ui.components.ProductCard
import com.example.ui.components.QuickCalculatorDialog
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import com.example.ui.theme.ShowroomNavy
import com.example.ui.theme.ShowroomNavyLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    activeCompany: Company?,
    companies: List<Company>,
    stats: DashboardStats,
    recentProducts: List<Product>,
    isCustomerMode: Boolean,
    onSelectCompany: (Company) -> Unit,
    onAddProductClick: () -> Unit,
    onViewAllProductsClick: () -> Unit,
    onManageCompaniesClick: () -> Unit,
    onImportExportClick: () -> Unit,
    onScanDocumentClick: () -> Unit,
    onViewProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onOpenQuotation: () -> Unit = {},
    quoteCartCount: Int = 0,
    onAccountClick: () -> Unit = {},
    onAiHubClick: () -> Unit = {},
    onToggleCustomerMode: (() -> Unit)? = null,
    onLowStockBannerClick: (() -> Unit)? = null,
    onAddToQuote: ((name: String, mrp: Double, discountPercent: Double, gstPercent: Double, quantity: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var companyDropdownExpanded by remember { mutableStateOf(false) }
    var showQuickCalculator by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Header Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ShowroomNavy,
                                ShowroomNavyLight
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Bar: Title & Active Company Dropdown & Fast Cloud/AI Access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Branding: Store Icon + Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ShowroomGold,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = ShowroomNavy,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "SHOWROOM MANAGER",
                                    fontSize = 11.sp,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShowroomGold
                                )
                                Text(
                                    text = activeCompany?.name ?: "All Companies",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Top Actions: Safe Mode, Calculator, AI Studio, Cloud Account & Company Dropdown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Discreet Eye Icon Toggle (No Safe Mode text)
                            Surface(
                                shape = CircleShape,
                                color = if (isCustomerMode) Color.White.copy(alpha = 0.16f) else ShowroomGold.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { onToggleCustomerMode?.invoke() }
                                    .testTag("dashboard_customer_mode_toggle")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCustomerMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle View Mode",
                                        tint = if (isCustomerMode) Color.White else ShowroomNavy,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // 1-Tap Counter Calculator Icon Button
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { showQuickCalculator = true }
                                    .testTag("dashboard_top_calc_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Quick Calculator",
                                        tint = PriceGreen,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // AI Hub Action Button (Non-overlapping surface)
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onAiHubClick)
                                    .testTag("top_ai_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini AI",
                                        tint = ShowroomGold,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            // Cloud Account & Sync Button (Non-overlapping surface)
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onAccountClick)
                                    .testTag("top_cloud_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Cloud Sync & Auth",
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }

                            // Company Switcher Dropdown
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { companyDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = activeCompany?.name ?: "Company",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Switch Company",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = companyDropdownExpanded,
                                    onDismissRequest = { companyDropdownExpanded = false }
                                ) {
                                    companies.forEach { comp ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(comp.name, fontWeight = if (comp.id == activeCompany?.id) FontWeight.Bold else FontWeight.Normal)
                                                    if (comp.id == activeCompany?.id) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onSelectCompany(comp)
                                                companyDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Manage your showroom products, pricing and inventory from one place.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ShowroomGold,
                                contentColor = ShowroomNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("dashboard_add_product_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = onOpenQuotation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_quotation_button")
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp), tint = ShowroomGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (quoteCartCount > 0) "Quote ($quoteCartCount)" else "Quote / Bill",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Low Stock & Out of Stock Alert Banner (गोडाउन स्टॉक अलर्ट)
        if (stats.lowStockCount > 0 || stats.outOfStockCount > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { onLowStockBannerClick?.invoke() }
                        .testTag("low_stock_alert_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (stats.outOfStockCount > 0) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (stats.outOfStockCount > 0) Color(0xFFB91C1C) else Color(0xFFB45309),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = if (stats.outOfStockCount > 0)
                                        "⚠️ ${stats.outOfStockCount} Out of Stock • ${stats.lowStockCount} Low Stock"
                                    else
                                        "⚠️ ${stats.lowStockCount} Items Low in Stock (<2 pcs)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (stats.outOfStockCount > 0) Color(0xFF991B1B) else Color(0xFF92400E)
                                )
                                Text(
                                    text = "सप्लायर से नया माल मंगाने के लिए यहाँ टैप करें",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                text = "View Items →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.outOfStockCount > 0) Color(0xFFB91C1C) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Dashboard Statistics (2x2 Grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "DASHBOARD OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Inventory2,
                        title = "Products",
                        value = stats.totalProducts.toString(),
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.LocalOffer,
                        title = "Brands",
                        value = stats.totalBrands.toString(),
                        accentColor = ShowroomGold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Category,
                        title = "Categories",
                        value = stats.totalCategories.toString(),
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Payments,
                        title = "Catalog Value",
                        value = PricingCalculator.formatCurrency(stats.totalCatalogValue),
                        accentColor = PriceGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 3: Quick Actions (as explicitly specified in Section 3)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.Add,
                        label = "Add Product",
                        onClick = onAddProductClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.Receipt,
                        label = "Quotation / Bill",
                        onClick = onOpenQuotation,
                        badgeCount = quoteCartCount,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.Calculate,
                        label = "Calculator",
                        onClick = { showQuickCalculator = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Default.AutoAwesome,
                        label = "Document OCR",
                        onClick = onScanDocumentClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.SwapVert,
                        label = "Import / Export",
                        onClick = onImportExportClick,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Default.Business,
                        label = "Companies",
                        onClick = onManageCompaniesClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Products in Active Company
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RECENT PRODUCTS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${activeCompany?.name} Catalog (${recentProducts.size} total)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onViewAllProductsClick) {
                    Text("View All", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (recentProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No products yet for ${activeCompany?.name}",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Add manually or import Excel data",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddProductClick) {
                            Text("+ Add First Product")
                        }
                    }
                }
            }
        } else {
            items(recentProducts.take(4)) { product ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ProductCard(
                        product = product,
                        isCustomerMode = isCustomerMode,
                        onViewClick = { onViewProduct(product) },
                        onEditClick = { onEditProduct(product) },
                        onDeleteClick = { onDeleteProduct(product) }
                    )
                }
            }
        }
    }

    if (showQuickCalculator) {
        QuickCalculatorDialog(
            isCustomerMode = isCustomerMode,
            onDismiss = { showQuickCalculator = false },
            onToggleCustomerMode = onToggleCustomerMode,
            onAddToQuote = onAddToQuote
        )
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (badgeCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (badgeCount > 9) "9+" else "$badgeCount",
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}
