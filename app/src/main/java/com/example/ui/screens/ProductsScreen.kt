package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.PricingCalculator
import com.example.data.Product
import com.example.data.StockStatus
import com.example.ui.StockFilter
import com.example.ui.components.ProductCard
import com.example.ui.theme.DiscountRed
import com.example.ui.theme.PriceGreen
import com.example.ui.theme.ShowroomGold
import com.example.ui.theme.ShowroomNavy

@Composable
fun ProductsScreen(
    activeCompany: Company?,
    products: List<Product>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    selectedBrand: String?,
    selectedCategory: String?,
    selectedStockFilter: StockFilter = StockFilter.ALL,
    isCustomerMode: Boolean,
    isTableView: Boolean,
    onSearchChange: (String) -> Unit,
    onBrandSelect: (String?) -> Unit,
    onCategorySelect: (String?) -> Unit,
    onStockFilterSelect: (StockFilter) -> Unit = {},
    onToggleCustomerMode: () -> Unit,
    onToggleTableView: () -> Unit,
    onAddProductClick: () -> Unit,
    onViewProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onAddToQuote: (Product) -> Unit = {},
    onOpenQuotation: () -> Unit = {},
    quoteCartCount: Int = 0,
    onOpenCalculator: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val allBrands = listOf("All Brands") + products.map { it.brand }.distinct()
    val allCategoriesList = listOf("All Categories") + categories.map { it.name }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("products_screen"),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header: Company Label & Modes
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Products",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${activeCompany?.name ?: "All"} Catalog • ${products.size} Items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Counter Calculator 1-Tap Launcher
                            if (onOpenCalculator != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = PriceGreen.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onOpenCalculator() }
                                        .testTag("products_quick_calc_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "Quick Calculator",
                                            tint = PriceGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Calc ⚡",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PriceGreen
                                        )
                                    }
                                }
                            }

                            // Discreet Eye Icon Toggle (No Safe Mode text)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCustomerMode) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else ShowroomGold.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onToggleCustomerMode)
                                    .testTag("products_customer_mode_toggle")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCustomerMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle View Mode",
                                        tint = if (isCustomerMode) MaterialTheme.colorScheme.onSurfaceVariant else ShowroomNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Section 9: Search Input (code, name, brand)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_search_input"),
                        placeholder = { Text("Search by name, code (HW-1234), brand...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Section 10: Brand Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allBrands) { brand ->
                            val isSelected = (selectedBrand == null && brand == "All Brands") || selectedBrand == brand
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onBrandSelect(if (brand == "All Brands") null else brand)
                                },
                                label = { Text(brand, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Section 11: Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allCategoriesList) { cat ->
                            val isSelected = (selectedCategory == null && cat == "All Categories") || selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onCategorySelect(if (cat == "All Categories") null else cat)
                                },
                                label = { Text(cat, fontSize = 12.sp) }
                            )
                        }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Stock Availability Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(StockFilter.values()) { filter ->
                            val isSelected = selectedStockFilter == filter
                            val count = when (filter) {
                                StockFilter.ALL -> products.size
                                StockFilter.IN_STOCK -> products.count { it.stockQuantity > 2 }
                                StockFilter.LOW_STOCK -> products.count { it.stockQuantity in 1..2 }
                                StockFilter.OUT_OF_STOCK -> products.count { it.stockQuantity <= 0 }
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onStockFilterSelect(filter) },
                                label = {
                                    Text(
                                        text = "${filter.label} ($count)",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (filter) {
                                        StockFilter.LOW_STOCK -> Color(0xFFFEF3C7)
                                        StockFilter.OUT_OF_STOCK -> Color(0xFFFEE2E2)
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    selectedLabelColor = when (filter) {
                                        StockFilter.LOW_STOCK -> Color(0xFFB45309)
                                        StockFilter.OUT_OF_STOCK -> Color(0xFFB91C1C)
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // View Mode Switcher: Card View vs Table View and Quotation Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { onOpenQuotation() }
                                .testTag("products_quotation_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (quoteCartCount > 0) "Quote / Bill ($quoteCartCount)" else "Quote / Bill",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        ) {
                            Row {
                                IconButton(
                                    onClick = { if (isTableView) onToggleTableView() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = "Card View",
                                        tint = if (!isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(
                                    onClick = { if (!isTableView) onToggleTableView() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableRows,
                                        contentDescription = "Table View",
                                        tint = if (isTableView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (products.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No products found", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Try adjusting search or filters, or add a product.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            IconButton(onClick = onAddProductClick) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    }
                }
            } else if (isTableView) {
                // Section 5: Table Format View
                item {
                    TableFormatProductList(
                        products = products,
                        isCustomerMode = isCustomerMode,
                        onViewProduct = onViewProduct,
                        onEditProduct = onEditProduct,
                        onDeleteProduct = onDeleteProduct
                    )
                }
            } else {
                // Card Format View (Mobile Primary)
                items(products, key = { it.id }) { product ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        ProductCard(
                            product = product,
                            isCustomerMode = isCustomerMode,
                            onViewClick = { onViewProduct(product) },
                            onEditClick = { onEditProduct(product) },
                            onDeleteClick = { onDeleteProduct(product) },
                            onAddToQuote = { onAddToQuote(product) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Product
        FloatingActionButton(
            onClick = onAddProductClick,
            containerColor = ShowroomGold,
            contentColor = ShowroomNavy,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_product")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
        }
    }
}

@Composable
fun TableFormatProductList(
    products: List<Product>,
    isCustomerMode: Boolean,
    onViewProduct: (Product) -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Code", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text("Product Name", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(170.dp))
                Text("Brand", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text("Category", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(90.dp))
                Text("MRP", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(85.dp))
                Text("Disc%", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                Text("GST%", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(60.dp))
                Text("Sale Price", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(95.dp))
                Text("Stock", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(75.dp))
                if (!isCustomerMode) {
                    Text("Purchase", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(85.dp))
                    Text("Margin", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(85.dp))
                }
                Text("Actions", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(110.dp))
            }
            HorizontalDivider()

            products.forEach { prod ->
                val pricing = prod.pricing
                Row(
                    modifier = Modifier
                        .clickable { onViewProduct(prod) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prod.code, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Text(prod.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(170.dp))
                    Text(prod.brand, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Text(prod.category, fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Text(PricingCalculator.formatCurrency(prod.mrp), fontSize = 11.sp, modifier = Modifier.width(85.dp))
                    Text("${prod.discountPercent.toInt()}%", fontSize = 11.sp, modifier = Modifier.width(60.dp))
                    Text("${prod.gstPercent.toInt()}%", fontSize = 11.sp, modifier = Modifier.width(60.dp))
                    Text(PricingCalculator.formatCurrency(pricing.finalSalePrice), fontWeight = FontWeight.Bold, color = PriceGreen, fontSize = 12.sp, modifier = Modifier.width(95.dp))
                    val stockColor = when (prod.stockStatus) {
                        StockStatus.OUT_OF_STOCK -> Color(0xFFB91C1C)
                        StockStatus.LOW_STOCK -> Color(0xFFB45309)
                        StockStatus.IN_STOCK -> Color(0xFF15803D)
                    }
                    Text("${prod.stockQuantity} pcs", fontWeight = FontWeight.Bold, color = stockColor, fontSize = 11.sp, modifier = Modifier.width(75.dp))
                    if (!isCustomerMode) {
                        Text(PricingCalculator.formatCurrency(prod.purchasePrice), fontSize = 11.sp, modifier = Modifier.width(85.dp))
                        Text(PricingCalculator.formatCurrency(pricing.marginAmount), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(85.dp))
                    }
                    Row(modifier = Modifier.width(110.dp)) {
                        IconButton(onClick = { onViewProduct(prod) }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onEditProduct(prod) }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDeleteProduct(prod) }, modifier = Modifier.size(28.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}
