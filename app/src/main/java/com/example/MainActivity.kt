package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.ShowroomUserProfile
import com.example.ui.ShowroomViewModel
import com.example.ui.StockFilter
import com.example.ui.components.AccountSyncDialog
import com.example.ui.components.AddEditProductDialog
import com.example.ui.components.CompanyEditDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.ExportCsvDialog
import com.example.ui.components.ImportCsvDialog
import com.example.ui.components.ProductDetailDialog
import com.example.ui.components.QuickCalculatorDialog
import com.example.ui.components.QuotationBillDialog
import com.example.ui.screens.AiHubScreen
import com.example.ui.screens.CompaniesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentScannerDialog
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ShowroomViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ShowroomApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ShowroomApp(viewModel: ShowroomViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeCompany by viewModel.activeCompany.collectAsStateWithLifecycle()
    val companies by viewModel.companies.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val filteredProducts by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBrand by viewModel.selectedBrand.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isCustomerMode by viewModel.isCustomerMode.collectAsStateWithLifecycle()
    val selectedStockFilter by viewModel.selectedStockFilter.collectAsStateWithLifecycle()
    val isQuickCalculatorOpen by viewModel.isQuickCalculatorOpen.collectAsStateWithLifecycle()
    val isTableView by viewModel.isTableView.collectAsStateWithLifecycle()

    // Gemini Chatbot & Image Studio state
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val currentGeminiRole by viewModel.currentGeminiRole.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    val isGeneratingImage by viewModel.isGeneratingImage.collectAsStateWithLifecycle()
    val generatedBitmap by viewModel.generatedBitmap.collectAsStateWithLifecycle()
    val imageError by viewModel.imageError.collectAsStateWithLifecycle()

    // Firebase Auth & Firestore Persistence state
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isAccountDialogOpen by viewModel.isAccountDialogOpen.collectAsStateWithLifecycle()
    val isSyncingFirestore by viewModel.isSyncingFirestore.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
    val isAuthReady by viewModel.isAuthReady.collectAsStateWithLifecycle()

    // Modals & Dialogs
    val viewingProduct by viewModel.viewingProduct.collectAsStateWithLifecycle()
    val isAddProductOpen by viewModel.isAddProductDialogOpen.collectAsStateWithLifecycle()
    val editingProduct by viewModel.editingProduct.collectAsStateWithLifecycle()
    val productToDelete by viewModel.productToDelete.collectAsStateWithLifecycle()

    val isCompanyDialogOpen by viewModel.isCompanyDialogOpen.collectAsStateWithLifecycle()
    val editingCompany by viewModel.editingCompany.collectAsStateWithLifecycle()
    val companyToDelete by viewModel.companyToDelete.collectAsStateWithLifecycle()

    val isImportDialogOpen by viewModel.isImportDialogOpen.collectAsStateWithLifecycle()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsStateWithLifecycle()
    val importSummary by viewModel.importSummary.collectAsStateWithLifecycle()
    val isDocScannerOpen by viewModel.isDocScannerOpen.collectAsStateWithLifecycle()

    val isQuotationDialogOpen by viewModel.isQuotationDialogOpen.collectAsStateWithLifecycle()
    val activeQuotation by viewModel.activeQuotation.collectAsStateWithLifecycle()
    val savedQuotations by viewModel.savedQuotations.collectAsStateWithLifecycle()
    val quotationCart by viewModel.quotationCart.collectAsStateWithLifecycle()
    val allCompanyProducts by viewModel.allCompanyProducts.collectAsStateWithLifecycle()

    // ---------- Auth gate: login hone tak existing login screen ----------
    var continueOffline by remember { mutableStateOf(false) }

    // Firebase session restore (cold start) ka chhota wait — login screen ka flash nahi hota
    if (viewModel.cloudConfigured && !isAuthReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Login check ho raha hai…",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (viewModel.cloudConfigured && currentUser == null && !continueOffline) {
        CloudLoginScreen(
            userProfile = userProfile,
            isSyncing = isSyncingFirestore,
            syncStatusMessage = syncStatusMessage,
            pendingCount = pendingSyncCount,
            lastSyncAt = cloudSyncState?.lastSyncAt ?: 0L,
            cloudConfigured = viewModel.cloudConfigured,
            onGoogleSignIn = { activity: Activity -> viewModel.signInWithGoogle(activity) },
            onEmailSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
            onEmailSignUp = { email, pass, name -> viewModel.signUpWithEmail(email, pass, name) },
            onAnonymousSignIn = { viewModel.signInAnonymously() },
            onPasswordReset = { email -> viewModel.sendPasswordReset(email) },
            onContinueOffline = { continueOffline = true }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Bottom Navigation: Home, Products, Companies, AI Studio, More
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setCurrentTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 10.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_home")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setCurrentTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = "Products"
                        )
                    },
                    label = { Text("Products", fontSize = 10.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_products")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setCurrentTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.Business else Icons.Outlined.Business,
                            contentDescription = "Companies"
                        )
                    },
                    label = { Text("Companies", fontSize = 10.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_companies")
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.setCurrentTab(3) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 3) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "AI Studio"
                        )
                    },
                    label = { Text("AI Studio", fontSize = 10.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_ai")
                )

                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { viewModel.setCurrentTab(4) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 4) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                            contentDescription = "More"
                        )
                    },
                    label = { Text("More", fontSize = 10.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_tab_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    activeCompany = activeCompany,
                    companies = companies,
                    stats = stats,
                    recentProducts = filteredProducts,
                    isCustomerMode = isCustomerMode,
                    onSelectCompany = { viewModel.setActiveCompany(it) },
                    onAddProductClick = { viewModel.openAddProductDialog() },
                    onViewAllProductsClick = { viewModel.setCurrentTab(1) },
                    onManageCompaniesClick = { viewModel.setCurrentTab(2) },
                    onImportExportClick = { viewModel.openImportDialog() },
                    onScanDocumentClick = { viewModel.openDocScanner() },
                    onViewProduct = { viewModel.setViewingProduct(it) },
                    onEditProduct = { viewModel.openEditProductDialog(it) },
                    onDeleteProduct = { viewModel.setProductToDelete(it) },
                    onAccountClick = { viewModel.openAccountDialog() },
                    onAiHubClick = { viewModel.setCurrentTab(3) },
                    onOpenQuotation = { viewModel.openQuotationDialog() },
                    quoteCartCount = quotationCart.size,
                    onToggleCustomerMode = { viewModel.toggleCustomerMode() },
                    onLowStockBannerClick = {
                        viewModel.setSelectedStockFilter(StockFilter.LOW_STOCK)
                        viewModel.setCurrentTab(1)
                    },
                    onAddToQuote = { name, mrp, disc, gst, qty ->
                        viewModel.addCustomItemToQuote(
                            name = name,
                            quantity = qty,
                            mrp = mrp,
                            discountPercent = disc,
                            gstPercent = gst
                        )
                    }
                )

                1 -> ProductsScreen(
                    activeCompany = activeCompany,
                    products = filteredProducts,
                    categories = categories,
                    searchQuery = searchQuery,
                    selectedBrand = selectedBrand,
                    selectedCategory = selectedCategory,
                    selectedStockFilter = selectedStockFilter,
                    isCustomerMode = isCustomerMode,
                    isTableView = isTableView,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onBrandSelect = { viewModel.setSelectedBrand(it) },
                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                    onStockFilterSelect = { viewModel.setSelectedStockFilter(it) },
                    onToggleCustomerMode = { viewModel.toggleCustomerMode() },
                    onToggleTableView = { viewModel.toggleTableView() },
                    onAddProductClick = { viewModel.openAddProductDialog() },
                    onViewProduct = { viewModel.setViewingProduct(it) },
                    onEditProduct = { viewModel.openEditProductDialog(it) },
                    onDeleteProduct = { viewModel.setProductToDelete(it) },
                    onAddToQuote = { viewModel.addProductToQuote(it) },
                    onOpenQuotation = { viewModel.openQuotationDialog() },
                    quoteCartCount = quotationCart.size,
                    onOpenCalculator = { viewModel.openQuickCalculator() }
                )

                2 -> CompaniesScreen(
                    activeCompany = activeCompany,
                    companies = companies,
                    onSelectActiveCompany = { viewModel.setActiveCompany(it) },
                    onAddCompanyClick = { viewModel.openAddCompanyDialog() },
                    onEditCompanyClick = { viewModel.openEditCompanyDialog(it) },
                    onDeleteCompanyClick = { viewModel.setCompanyToDelete(it) }
                )

                3 -> AiHubScreen(
                    chatMessages = chatMessages,
                    currentRole = currentGeminiRole,
                    isChatLoading = isChatLoading,
                    onRoleSelected = { viewModel.setGeminiRole(it) },
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onClearHistory = { viewModel.clearChatHistory() },
                    products = filteredProducts,
                    isGeneratingImage = isGeneratingImage,
                    generatedBitmap = generatedBitmap,
                    imageErrorMessage = imageError,
                    onGenerateImage = { prompt, ratio -> viewModel.generateImage(prompt, ratio) },
                    onEditImage = { prompt, bitmap -> viewModel.editImage(prompt, bitmap) },
                    onAssignImageToProduct = { prodId, bitmap -> viewModel.assignImageToProduct(prodId, bitmap) },
                    onResetImage = { viewModel.resetImageStudio() }
                )

                4 -> ToolsScreen(
                    activeCompany = activeCompany,
                    categories = categories,
                    isCustomerMode = isCustomerMode,
                    onToggleCustomerMode = { viewModel.toggleCustomerMode() },
                    onOpenImportDialog = { viewModel.openImportDialog() },
                    onOpenExportDialog = { viewModel.openExportDialog() },
                    onOpenDocScanner = { viewModel.openDocScanner() },
                    onAddCategory = { name, subcats -> viewModel.addCategory(name, subcats) },
                    onOpenAccountDialog = { viewModel.openAccountDialog() },
                    onNavigateToAi = { viewModel.setCurrentTab(3) },
                    onOpenQuotation = { viewModel.openQuotationDialog() },
                    pendingSyncCount = pendingSyncCount,
                    onSyncNow = { viewModel.syncNow() }
                )
            }
        }
    }

    // Product Details Modal
    viewingProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            isCustomerMode = isCustomerMode,
            onDismiss = { viewModel.setViewingProduct(null) },
            onEditClick = { viewModel.openEditProductDialog(it) },
            onDeleteClick = { viewModel.setProductToDelete(it) },
            onAddToQuote = { viewModel.addProductToQuote(it) },
            onAdjustStock = { prod, delta -> viewModel.adjustProductStock(prod.id, delta) }
        )
    }

    // Add / Edit Product Modal Form
    if (isAddProductOpen && activeCompany != null) {
        AddEditProductDialog(
            initialProduct = editingProduct,
            activeCompany = activeCompany!!,
            categories = categories,
            onDismiss = { viewModel.closeProductFormDialog() },
            onSave = { code, name, brand, cat, subcat, img, mrp, disc, gst, purchase, stock, notes ->
                viewModel.saveProduct(code, name, brand, cat, subcat, img, mrp, disc, gst, purchase, stock, notes)
            }
        )
    }

    // Delete Product Confirmation
    productToDelete?.let { prod ->
        DeleteConfirmDialog(
            title = "Delete Product?",
            itemName = prod.name,
            itemCode = prod.code,
            onDismiss = { viewModel.setProductToDelete(null) },
            onConfirm = { viewModel.confirmDeleteProduct() }
        )
    }

    // Add / Edit Company Dialog
    if (isCompanyDialogOpen) {
        CompanyEditDialog(
            initialCompany = editingCompany,
            onDismiss = { viewModel.closeCompanyDialog() },
            onSave = { name, desc ->
                viewModel.saveCompany(name, desc)
            }
        )
    }

    // Delete Company Confirmation
    companyToDelete?.let { comp ->
        DeleteConfirmDialog(
            title = "Delete Company?",
            itemName = comp.name,
            onDismiss = { viewModel.setCompanyToDelete(null) },
            onConfirm = { viewModel.confirmDeleteCompany() }
        )
    }

    // Excel CSV Import Dialog
    if (isImportDialogOpen && activeCompany != null) {
        ImportCsvDialog(
            activeCompany = activeCompany!!,
            importSummary = importSummary,
            onDismiss = { viewModel.closeImportDialog() },
            onImport = { csvText, overwrite ->
                viewModel.importCsvData(csvText, overwrite)
            }
        )
    }

    // Excel CSV Export Dialog
    if (isExportDialogOpen && activeCompany != null) {
        ExportCsvDialog(
            activeCompany = activeCompany!!,
            products = filteredProducts,
            onDismiss = { viewModel.closeExportDialog() },
            onGenerateCsv = { includeInternal ->
                viewModel.generateExportCsv(includeInternal)
            }
        )
    }

    // Intelligent Document / Bill OCR Scanner Dialog
    if (isDocScannerOpen && activeCompany != null) {
        DocumentScannerDialog(
            activeCompany = activeCompany!!,
            onDismiss = { viewModel.closeDocScanner() },
            onSaveDetectedProducts = { scannedProducts ->
                viewModel.saveScannedProducts(scannedProducts)
            }
        )
    }

    // Firebase Cloud Account & Firestore Persistence Dialog
    if (isAccountDialogOpen) {
        AccountSyncDialog(
            currentUser = currentUser,
            userProfile = userProfile,
            isSyncing = isSyncingFirestore,
            syncStatusMessage = syncStatusMessage,
            onDismiss = { viewModel.closeAccountDialog() },
            onGoogleSignIn = { activity -> viewModel.signInWithGoogle(activity) },
            onEmailSignIn = { email, pass -> viewModel.signInWithEmail(email, pass) },
            onEmailSignUp = { email, pass, name -> viewModel.signUpWithEmail(email, pass, name) },
            onAnonymousSignIn = { viewModel.signInAnonymously() },
            onSignOut = { viewModel.signOut() },
            onBackupToFirestore = { viewModel.backupToFirestore() },
            onRestoreFromFirestore = { viewModel.restoreFromFirestore() },
            pendingCount = pendingSyncCount,
            lastSyncAt = cloudSyncState?.lastSyncAt ?: 0L,
            cloudConfigured = viewModel.cloudConfigured,
            onSyncNow = { viewModel.syncNow() },
            onPasswordReset = { email -> viewModel.sendPasswordReset(email) }
        )
    }

    // Quotation, Tax Invoice & WhatsApp Billing Dialog
    if (isQuotationDialogOpen) {
        QuotationBillDialog(
            initialQuotation = activeQuotation,
            catalogProducts = allCompanyProducts,
            savedQuotations = savedQuotations,
            onDismiss = { viewModel.closeQuotationDialog() },
            onSaveQuotation = { viewModel.saveQuotation(it) },
            onDeleteQuotation = { viewModel.deleteSavedQuotation(it) }
        )
    }

    // Quick Counter Calculator Modal (Global 1-Tap Access)
    if (isQuickCalculatorOpen) {
        QuickCalculatorDialog(
            isCustomerMode = isCustomerMode,
            onDismiss = { viewModel.closeQuickCalculator() },
            onToggleCustomerMode = { viewModel.toggleCustomerMode() },
            onAddToQuote = { name, mrp, disc, gst, qty ->
                viewModel.addCustomItemToQuote(
                    name = name,
                    quantity = qty,
                    mrp = mrp,
                    discountPercent = disc,
                    gstPercent = gst
                )
            }
        )
    }
}

/**
 * Login screen — existing [AccountSyncDialog] ko full-screen wrapper ke saath reuse karta hai
 * (UI redesign nahi kiya gaya). Login hone par ye composable apne aap hat jata hai, kyunki
 * MainActivity me `currentUser != null` ho jata hai.
 */
@Composable
private fun CloudLoginScreen(
    userProfile: ShowroomUserProfile?,
    isSyncing: Boolean,
    syncStatusMessage: String?,
    pendingCount: Int,
    lastSyncAt: Long,
    cloudConfigured: Boolean,
    onGoogleSignIn: (Activity) -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailSignUp: (String, String, String) -> Unit,
    onAnonymousSignIn: () -> Unit,
    onPasswordReset: (String) -> Unit,
    onContinueOffline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Text("🏪", fontSize = 46.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Showroom",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Sign in karein — catalog cloud me safe rahega aur doosre phone par bhi mil jayega",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AccountSyncDialog(
            currentUser = null,
            userProfile = userProfile,
            isSyncing = isSyncing,
            syncStatusMessage = syncStatusMessage,
            onDismiss = { /* login zaroori hai — bahar tap karne par band nahi hoga */ },
            onGoogleSignIn = onGoogleSignIn,
            onEmailSignIn = onEmailSignIn,
            onEmailSignUp = onEmailSignUp,
            onAnonymousSignIn = onAnonymousSignIn,
            onSignOut = { },
            onBackupToFirestore = { },
            onRestoreFromFirestore = { },
            pendingCount = pendingCount,
            lastSyncAt = lastSyncAt,
            cloudConfigured = cloudConfigured,
            onSyncNow = { },
            onPasswordReset = onPasswordReset,
            onContinueOffline = onContinueOffline
        )
    }
}
