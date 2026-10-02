package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.ChatMessage
import com.example.ai.GeminiClient
import com.example.ai.GeminiRole
import com.example.auth.FirebaseAuthManager
import com.example.data.sync.SyncState
import com.example.auth.ShowroomUserProfile
import com.example.data.AppDatabase
import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.CsvHelper
import com.example.data.ImportSummary
import com.example.data.Product
import com.example.data.Quotation
import com.example.data.QuotationItem
import com.example.data.QuotationType
import com.example.data.ShowroomRepository
import com.example.data.StockStatus
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StockFilter(val label: String, val hindiLabel: String) {
    ALL("All Stock", "सभी"),
    IN_STOCK("In Stock", "उपलब्ध"),
    LOW_STOCK("Low Stock (<2 pcs)", "कम स्टॉक ⚠️"),
    OUT_OF_STOCK("Out of Stock", "स्टॉक खत्म ❌")
}

data class DashboardStats(
    val totalProducts: Int = 0,
    val totalBrands: Int = 0,
    val totalCategories: Int = 0,
    val totalCatalogValue: Double = 0.0,
    val avgMarginPercent: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0
)

class ShowroomViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("showroom_prefs", Context.MODE_PRIVATE)
    private val repository: ShowroomRepository

    init {
        try {
            FirebaseAuthManager.init(application.applicationContext)
        } catch (e: Exception) {
            android.util.Log.w("ShowroomViewModel", "Safe Firebase init notice: ${e.message}")
        }

        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ShowroomRepository(
            database.companyDao(),
            database.categoryDao(),
            database.productDao(),
            syncQueueDao = database.syncQueueDao(),
            appContext = application.applicationContext,
            syncScope = viewModelScope
        )
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultDataPopulated()
        }

        // Internet wapas aane par pending data apne aap upload ho jaye
        registerNetworkRetry(application)

        // App khulte hi: agar login hai to cloud se data milao (offline-first, best effort)
        viewModelScope.launch(Dispatchers.IO) {
            if (FirebaseAuthManager.isConfigured && FirebaseAuthManager.currentUser != null) {
                runCatching { repository.syncNow() }
                    .onFailure { android.util.Log.w("ShowroomViewModel", "startup sync failed: ${it.message}") }
            }
        }

        // Har 2 minute me halka retry (sirf jab pending data ho)
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(120_000L)
                if (FirebaseAuthManager.currentUser != null) {
                    runCatching { repository.syncPending() }
                }
            }
        }
    }

    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkRetry(application: Application) {
        try {
            val cm = application.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                ?: return
            val callback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    repository.requestCloudSync()
                }
            }
            cm.registerDefaultNetworkCallback(callback)
            networkCallback = callback
        } catch (e: Exception) {
            android.util.Log.w("ShowroomViewModel", "Network callback register nahi hua: ${e.message}")
        }
    }

    override fun onCleared() {
        try {
            networkCallback?.let { cb ->
                val cm = getApplication<Application>()
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                cm?.unregisterNetworkCallback(cb)
            }
        } catch (e: Exception) {
            android.util.Log.w("ShowroomViewModel", "Network callback unregister failed: ${e.message}")
        }
        super.onCleared()
    }

    // Navigation Tab: 0 = Home, 1 = Products, 2 = Companies, 3 = More
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setCurrentTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    // Companies
    val companies: StateFlow<List<Company>> = repository.allCompanies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _activeCompany = MutableStateFlow<Company?>(null)
    val activeCompany: StateFlow<Company?> = _activeCompany.asStateFlow()

    fun setActiveCompany(company: Company) {
        _activeCompany.value = company
    }

    // Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filters and Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Customer-facing mode: Hides Purchase Price and Margins (Default ON / True, persisted in SharedPreferences)
    private val _isCustomerMode = MutableStateFlow(prefs.getBoolean("is_customer_mode", true))
    val isCustomerMode: StateFlow<Boolean> = _isCustomerMode.asStateFlow()

    fun toggleCustomerMode() {
        val newValue = !_isCustomerMode.value
        _isCustomerMode.value = newValue
        prefs.edit().putBoolean("is_customer_mode", newValue).apply()
    }

    fun setCustomerMode(enabled: Boolean) {
        _isCustomerMode.value = enabled
        prefs.edit().putBoolean("is_customer_mode", enabled).apply()
    }

    // Owner Security PIN for Safe Mode (Default PIN: "1234")
    private val _ownerPin = MutableStateFlow(prefs.getString("owner_security_pin", "1234") ?: "1234")
    val ownerPin: StateFlow<String> = _ownerPin.asStateFlow()

    fun verifyAndUnlockCustomerMode(pin: String): Boolean {
        if (pin.trim() == _ownerPin.value) {
            setCustomerMode(false)
            return true
        }
        return false
    }

    fun lockCustomerMode() {
        setCustomerMode(true)
    }

    fun changeOwnerPin(oldPin: String, newPin: String): Boolean {
        if (oldPin.trim() == _ownerPin.value && newPin.trim().length >= 4) {
            val cleanPin = newPin.trim()
            _ownerPin.value = cleanPin
            prefs.edit().putString("owner_security_pin", cleanPin).apply()
            return true
        }
        return false
    }

    // Quick Counter Calculator Global State
    private val _isQuickCalculatorOpen = MutableStateFlow(false)
    val isQuickCalculatorOpen: StateFlow<Boolean> = _isQuickCalculatorOpen.asStateFlow()

    fun openQuickCalculator() {
        _isQuickCalculatorOpen.value = true
    }

    fun closeQuickCalculator() {
        _isQuickCalculatorOpen.value = false
    }

    // Stock Filter State (All, In Stock, Low Stock <2 pcs, Out of Stock)
    private val _selectedStockFilter = MutableStateFlow(StockFilter.ALL)
    val selectedStockFilter: StateFlow<StockFilter> = _selectedStockFilter.asStateFlow()

    fun setSelectedStockFilter(filter: StockFilter) {
        _selectedStockFilter.value = filter
    }

    // Table view vs Card view toggle
    private val _isTableView = MutableStateFlow(false)
    val isTableView: StateFlow<Boolean> = _isTableView.asStateFlow()

    fun toggleTableView() {
        _isTableView.value = !_isTableView.value
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBrand(brand: String?) {
        _selectedBrand.value = brand
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    // Filtered Products for Active Company
    private val _allCompanyProducts = MutableStateFlow<List<Product>>(emptyList())
    val allCompanyProducts: StateFlow<List<Product>> = _allCompanyProducts.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        _allCompanyProducts,
        _searchQuery,
        _selectedBrand,
        _selectedCategory,
        _selectedStockFilter
    ) { products, query, brand, cat, stockFilter ->
        products.filter { p ->
            val matchesQuery = query.isBlank() ||
                p.name.contains(query, ignoreCase = true) ||
                p.code.contains(query, ignoreCase = true) ||
                p.brand.contains(query, ignoreCase = true) ||
                p.category.contains(query, ignoreCase = true) ||
                p.subcategory.contains(query, ignoreCase = true)

            val matchesBrand = brand == null || brand == "All Brands" || p.brand.equals(brand, ignoreCase = true)
            val matchesCategory = cat == null || cat == "All Categories" || p.category.equals(cat, ignoreCase = true)

            val matchesStock = when (stockFilter) {
                StockFilter.ALL -> true
                StockFilter.IN_STOCK -> p.stockQuantity > 2
                StockFilter.LOW_STOCK -> p.stockQuantity in 1..2
                StockFilter.OUT_OF_STOCK -> p.stockQuantity <= 0
            }

            matchesQuery && matchesBrand && matchesCategory && matchesStock
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stats: StateFlow<DashboardStats> = _allCompanyProducts.combine(categories) { products, cats ->
        val total = products.size
        val brands = products.map { it.brand }.distinct().size
        val totalValue = products.sumOf { it.finalSalePrice }
        val margins = products.filter { it.purchasePrice > 0 }.map { it.marginPercent }
        val avgMargin = if (margins.isNotEmpty()) margins.average() else 0.0
        val lowStock = products.count { it.stockQuantity in 1..2 }
        val outOfStock = products.count { it.stockQuantity <= 0 }
        DashboardStats(
            totalProducts = total,
            totalBrands = brands,
            totalCategories = cats.size,
            totalCatalogValue = totalValue,
            avgMarginPercent = avgMargin,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardStats()
    )

    init {
        // Automatically link active company when companies load
        viewModelScope.launch {
            companies.collect { list ->
                if (list.isNotEmpty() && _activeCompany.value == null) {
                    val defaultCompany = list.find { it.isDefault } ?: list.first()
                    _activeCompany.value = defaultCompany
                }
            }
        }

        // Listen to active company changes and collect products
        viewModelScope.launch {
            _activeCompany.collect { comp ->
                if (comp != null) {
                    repository.getProductsByCompany(comp.id).collect { prods ->
                        _allCompanyProducts.value = prods
                    }
                } else {
                    _allCompanyProducts.value = emptyList()
                }
            }
        }
    }

    // Modal / Dialog states
    private val _viewingProduct = MutableStateFlow<Product?>(null)
    val viewingProduct: StateFlow<Product?> = _viewingProduct.asStateFlow()

    fun setViewingProduct(product: Product?) {
        _viewingProduct.value = product
    }

    private val _editingProduct = MutableStateFlow<Product?>(null)
    val editingProduct: StateFlow<Product?> = _editingProduct.asStateFlow()

    private val _isAddProductDialogOpen = MutableStateFlow(false)
    val isAddProductDialogOpen: StateFlow<Boolean> = _isAddProductDialogOpen.asStateFlow()

    fun openAddProductDialog() {
        _editingProduct.value = null
        _isAddProductDialogOpen.value = true
    }

    fun openEditProductDialog(product: Product) {
        _editingProduct.value = product
        _isAddProductDialogOpen.value = true
    }

    fun closeProductFormDialog() {
        _editingProduct.value = null
        _isAddProductDialogOpen.value = false
    }

    private val _productToDelete = MutableStateFlow<Product?>(null)
    val productToDelete: StateFlow<Product?> = _productToDelete.asStateFlow()

    fun setProductToDelete(product: Product?) {
        _productToDelete.value = product
    }

    fun confirmDeleteProduct() {
        val prod = _productToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProduct(prod)
            _productToDelete.value = null
            if (_viewingProduct.value?.id == prod.id) {
                _viewingProduct.value = null
            }
        }
    }

    fun saveProduct(
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
    ) {
        val currentComp = _activeCompany.value ?: return
        val currentEdit = _editingProduct.value

        viewModelScope.launch(Dispatchers.IO) {
            if (currentEdit != null) {
                repository.updateProduct(
                    currentEdit.copy(
                        code = code,
                        name = name,
                        brand = brand,
                        category = category,
                        subcategory = subcategory,
                        imageUri = imageUri ?: currentEdit.imageUri,
                        mrp = mrp,
                        discountPercent = discountPercent,
                        gstPercent = gstPercent,
                        purchasePrice = purchasePrice,
                        stockQuantity = stockQuantity,
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                repository.insertProduct(
                    Product(
                        companyId = currentComp.id,
                        companyName = currentComp.name,
                        code = code,
                        name = name,
                        brand = brand,
                        category = category,
                        subcategory = subcategory,
                        imageUri = imageUri,
                        mrp = mrp,
                        discountPercent = discountPercent,
                        gstPercent = gstPercent,
                        purchasePrice = purchasePrice,
                        stockQuantity = stockQuantity,
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            _isAddProductDialogOpen.value = false
            _editingProduct.value = null
        }
    }

    fun updateProductStock(productId: Long, newStock: Int) {
        val safeStock = maxOf(0, newStock)
        viewModelScope.launch(Dispatchers.IO) {
            val existing = _allCompanyProducts.value.find { it.id == productId } ?: return@launch
            val updated = existing.copy(stockQuantity = safeStock, updatedAt = System.currentTimeMillis())
            repository.updateProduct(updated)
            if (_viewingProduct.value?.id == productId) {
                _viewingProduct.value = updated
            }
        }
    }

    fun adjustProductStock(productId: Long, delta: Int) {
        val existing = _allCompanyProducts.value.find { it.id == productId } ?: return
        val newStock = maxOf(0, existing.stockQuantity + delta)
        updateProductStock(productId, newStock)
    }

    // Company Management
    private val _isCompanyDialogOpen = MutableStateFlow(false)
    val isCompanyDialogOpen: StateFlow<Boolean> = _isCompanyDialogOpen.asStateFlow()

    private val _editingCompany = MutableStateFlow<Company?>(null)
    val editingCompany: StateFlow<Company?> = _editingCompany.asStateFlow()

    private val _companyToDelete = MutableStateFlow<Company?>(null)
    val companyToDelete: StateFlow<Company?> = _companyToDelete.asStateFlow()

    fun openAddCompanyDialog() {
        _editingCompany.value = null
        _isCompanyDialogOpen.value = true
    }

    fun openEditCompanyDialog(company: Company) {
        _editingCompany.value = company
        _isCompanyDialogOpen.value = true
    }

    fun closeCompanyDialog() {
        _isCompanyDialogOpen.value = false
        _editingCompany.value = null
    }

    fun setCompanyToDelete(company: Company?) {
        _companyToDelete.value = company
    }

    fun saveCompany(name: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val editing = _editingCompany.value
            if (editing != null) {
                repository.updateCompany(editing.copy(name = name, description = description))
                if (_activeCompany.value?.id == editing.id) {
                    _activeCompany.value = editing.copy(name = name, description = description)
                }
            } else {
                val newId = repository.insertCompany(
                    Company(name = name, description = description)
                )
                val newComp = repository.getCompanyById(newId)
                if (newComp != null && _activeCompany.value == null) {
                    _activeCompany.value = newComp
                }
            }
            _isCompanyDialogOpen.value = false
            _editingCompany.value = null
        }
    }

    fun confirmDeleteCompany() {
        val comp = _companyToDelete.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCompany(comp)
            _companyToDelete.value = null
            val remaining = repository.allCompanies.first()
            if (remaining.isNotEmpty()) {
                _activeCompany.value = remaining.first()
            } else {
                _activeCompany.value = null
            }
        }
    }

    // Category Management
    private val _isCategoryDialogOpen = MutableStateFlow(false)
    val isCategoryDialogOpen: StateFlow<Boolean> = _isCategoryDialogOpen.asStateFlow()

    fun openCategoryDialog() {
        _isCategoryDialogOpen.value = true
    }

    fun closeCategoryDialog() {
        _isCategoryDialogOpen.value = false
    }

    fun addCategory(name: String, subcategories: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(CategoryEntity(name = name, subcategories = subcategories))
        }
    }

    // Excel CSV Import / Export
    private val _isImportDialogOpen = MutableStateFlow(false)
    val isImportDialogOpen: StateFlow<Boolean> = _isImportDialogOpen.asStateFlow()

    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    private val _importSummary = MutableStateFlow<ImportSummary?>(null)
    val importSummary: StateFlow<ImportSummary?> = _importSummary.asStateFlow()

    fun openImportDialog() {
        _importSummary.value = null
        _isImportDialogOpen.value = true
    }

    fun closeImportDialog() {
        _isImportDialogOpen.value = false
        _importSummary.value = null
    }

    fun openExportDialog() {
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun importCsvData(csvText: String, overwriteDuplicates: Boolean) {
        val comp = _activeCompany.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val summary = repository.bulkImportCsv(
                csvData = csvText,
                companyId = comp.id,
                companyName = comp.name,
                overwriteDuplicates = overwriteDuplicates
            )
            _importSummary.value = summary
        }
    }

    fun generateExportCsv(includeInternal: Boolean): String {
        val products = _allCompanyProducts.value
        return CsvHelper.exportToCsv(products, includeInternalCosts = includeInternal)
    }

    // Future Document / Bill Intelligent Scanner Dialog (Section 22)
    private val _isDocScannerOpen = MutableStateFlow(false)
    val isDocScannerOpen: StateFlow<Boolean> = _isDocScannerOpen.asStateFlow()

    fun openDocScanner() {
        _isDocScannerOpen.value = true
    }

    fun closeDocScanner() {
        _isDocScannerOpen.value = false
    }

    fun saveScannedProducts(products: List<Product>) {
        val comp = _activeCompany.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val assigned = products.map { it.copy(companyId = comp.id, companyName = comp.name) }
            repository.insertProducts(assigned)
            _isDocScannerOpen.value = false
        }
    }

    // --- Gemini Multi-Turn Chatbot ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _currentGeminiRole = MutableStateFlow(GeminiRole.GENERAL)
    val currentGeminiRole: StateFlow<GeminiRole> = _currentGeminiRole.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun setGeminiRole(role: GeminiRole) {
        _currentGeminiRole.value = role
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = ChatMessage(role = "user", content = text)
        val updatedList = _chatMessages.value + userMsg
        _chatMessages.value = updatedList
        _isChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val role = _currentGeminiRole.value
            val result = GeminiClient.sendChatMessage(updatedList, role)
            _isChatLoading.value = false
            result.onSuccess { reply ->
                val modelMsg = ChatMessage(
                    role = "model",
                    content = reply,
                    modelUsed = role.modelName
                )
                _chatMessages.value = _chatMessages.value + modelMsg
            }.onFailure { err ->
                val errorMsg = ChatMessage(
                    role = "model",
                    content = "⚠️ ${err.message ?: "Could not reach Gemini."}",
                    modelUsed = role.modelName
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }

    // --- Gemini Image Studio (gemini-3.1-flash-image-preview) ---
    private val _isGeneratingImage = MutableStateFlow(false)
    val isGeneratingImage: StateFlow<Boolean> = _isGeneratingImage.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _imageError = MutableStateFlow<String?>(null)
    val imageError: StateFlow<String?> = _imageError.asStateFlow()

    fun generateImage(prompt: String, aspectRatio: String = "1:1") {
        if (prompt.isBlank()) return
        _isGeneratingImage.value = true
        _imageError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = GeminiClient.generateOrEditImage(
                prompt = prompt,
                sourceBitmap = null,
                aspectRatio = aspectRatio
            )
            _isGeneratingImage.value = false
            if (res.bitmap != null) {
                _generatedBitmap.value = res.bitmap
            } else {
                _imageError.value = res.errorMessage ?: "Failed to generate image"
            }
        }
    }

    fun editImage(prompt: String, baseBitmap: Bitmap) {
        if (prompt.isBlank()) return
        _isGeneratingImage.value = true
        _imageError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val res = GeminiClient.generateOrEditImage(
                prompt = prompt,
                sourceBitmap = baseBitmap
            )
            _isGeneratingImage.value = false
            if (res.bitmap != null) {
                _generatedBitmap.value = res.bitmap
            } else {
                _imageError.value = res.errorMessage ?: "Failed to edit image"
            }
        }
    }

    fun assignImageToProduct(productId: Long, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>().applicationContext
            val filename = "product_${productId}_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(context.filesDir, filename)
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val existing = _allCompanyProducts.value.find { it.id == productId }
            if (existing != null) {
                val updated = existing.copy(imageUri = file.absolutePath)
                repository.updateProduct(updated)
            }
        }
    }

    fun resetImageStudio() {
        _generatedBitmap.value = null
        _imageError.value = null
    }

    // --- Firebase Auth & Firestore Persistence ---
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseAuthManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<ShowroomUserProfile?>(null)
    val userProfile: StateFlow<ShowroomUserProfile?> = _userProfile.asStateFlow()

    private val _isAccountDialogOpen = MutableStateFlow(false)
    val isAccountDialogOpen: StateFlow<Boolean> = _isAccountDialogOpen.asStateFlow()

    private val _isSyncingFirestore = MutableStateFlow(false)
    val isSyncingFirestore: StateFlow<Boolean> = _isSyncingFirestore.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage: StateFlow<String?> = _syncStatusMessage.asStateFlow()

    // --- Cloud sync (offline-first Firestore synchronization) ---
    val cloudConfigured: Boolean = FirebaseAuthManager.isConfigured

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val cloudSyncState: StateFlow<SyncState?> = repository.syncState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Manual sync — pending upload + cloud se merge */
    fun syncNow() {
        viewModelScope.launch {
            _syncStatusMessage.value = "Cloud sync chal raha hai…"
            val res = repository.syncNow()
            _syncStatusMessage.value = res.message
            _currentUser.value = FirebaseAuthManager.currentUser
        }
    }

    /** Password reset email */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _syncStatusMessage.value = "Password reset email bhej rahe hain…"
            val res = FirebaseAuthManager.sendPasswordReset(email)
            _syncStatusMessage.value = res.fold(
                onSuccess = { "Password reset email bhej diya 📧 (inbox check karein)" },
                onFailure = { "Password reset fail: ${it.message}" }
            )
        }
    }

    fun openAccountDialog() {
        _isAccountDialogOpen.value = true
    }

    fun closeAccountDialog() {
        _isAccountDialogOpen.value = false
        _syncStatusMessage.value = null
    }

    fun signInWithGoogle(activity: android.app.Activity) {
        viewModelScope.launch {
            _syncStatusMessage.value = "Signing in with Google..."
            val res = FirebaseAuthManager.signInWithGoogle(activity)
            res.onSuccess { user ->
                _currentUser.value = user
                _syncStatusMessage.value = "Signed in as ${user.displayName ?: user.email}"
                syncAfterLogin()
            }.onFailure { err ->
                _syncStatusMessage.value = "Sign in error: ${err.message}"
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _syncStatusMessage.value = "Authenticating..."
            val res = FirebaseAuthManager.signInWithEmail(email, pass)
            res.onSuccess { user ->
                _currentUser.value = user
                _syncStatusMessage.value = "Signed in successfully"
                syncAfterLogin()
            }.onFailure { err ->
                _syncStatusMessage.value = "Error: ${err.message}"
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String) {
        viewModelScope.launch {
            _syncStatusMessage.value = "Creating account..."
            val res = FirebaseAuthManager.signUpWithEmail(email, pass, name)
            res.onSuccess { user ->
                _currentUser.value = user
                _syncStatusMessage.value = "Account created!"
                syncAfterLogin()
            }.onFailure { err ->
                _syncStatusMessage.value = "Error: ${err.message}"
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _syncStatusMessage.value = "Entering guest mode..."
            val res = FirebaseAuthManager.signInAnonymously()
            res.onSuccess { user ->
                _currentUser.value = user
                _syncStatusMessage.value = "Signed in as Guest"
                syncAfterLogin()
            }.onFailure { err ->
                _syncStatusMessage.value = "Error: ${err.message}"
            }
        }
    }

    /** Login ke baad: initials sync (cloud <-> Room merge) — UI block nahi hota */
    private fun syncAfterLogin() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = repository.syncNow()
                withContext(Dispatchers.Main) {
                    _syncStatusMessage.value = res.message
                }
            } catch (e: Exception) {
                android.util.Log.w("ShowroomViewModel", "post-login sync failed: ${e.message}")
            }
        }
    }

    fun signOut() {
        FirebaseAuthManager.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _syncStatusMessage.value = "Signed out"
    }

    fun backupToFirestore() {
        val user = _currentUser.value ?: return
        _isSyncingFirestore.value = true
        _syncStatusMessage.value = "Syncing products to Firestore..."
        viewModelScope.launch(Dispatchers.IO) {
            val products = _allCompanyProducts.value
            val res = FirebaseAuthManager.syncProductsToFirestore(products)
            _isSyncingFirestore.value = false
            res.onSuccess { count ->
                _syncStatusMessage.value = "Successfully synced $count products to Google Firestore!"
                _userProfile.value = _userProfile.value?.copy(
                    cloudProductCount = count,
                    lastSyncTime = System.currentTimeMillis()
                ) ?: ShowroomUserProfile(
                    uid = user.uid,
                    displayName = user.displayName ?: "Showroom Manager",
                    cloudProductCount = count,
                    lastSyncTime = System.currentTimeMillis()
                )
            }.onFailure { err ->
                _syncStatusMessage.value = "Firestore sync error: ${err.message}"
            }
        }
    }

    fun restoreFromFirestore() {
        _isSyncingFirestore.value = true
        _syncStatusMessage.value = "Fetching catalog from Firestore..."
        viewModelScope.launch(Dispatchers.IO) {
            val res = FirebaseAuthManager.fetchProductsFromFirestore()
            _isSyncingFirestore.value = false
            res.onSuccess { cloudProducts ->
                if (cloudProducts.isNotEmpty()) {
                    repository.insertProducts(cloudProducts)
                    _syncStatusMessage.value = "Restored ${cloudProducts.size} products from Firestore into local database!"
                } else {
                    _syncStatusMessage.value = "No products found in your Firestore cloud storage."
                }
            }.onFailure { err ->
                _syncStatusMessage.value = "Firestore restore error: ${err.message}"
            }
        }
    }

    // ==========================================
    // --- Quotation & Tax Invoice Generator ---
    // ==========================================
    private val _isQuotationDialogOpen = MutableStateFlow(false)
    val isQuotationDialogOpen: StateFlow<Boolean> = _isQuotationDialogOpen.asStateFlow()

    private val _quotationCart = MutableStateFlow<List<QuotationItem>>(emptyList())
    val quotationCart: StateFlow<List<QuotationItem>> = _quotationCart.asStateFlow()

    private val _activeQuotation = MutableStateFlow<Quotation?>(null)
    val activeQuotation: StateFlow<Quotation?> = _activeQuotation.asStateFlow()

    private val _savedQuotations = MutableStateFlow<List<Quotation>>(emptyList())
    val savedQuotations: StateFlow<List<Quotation>> = _savedQuotations.asStateFlow()

    fun openQuotationDialog(initialQuotation: Quotation? = null) {
        val comp = _activeCompany.value
        val defaultQuote = initialQuotation ?: _activeQuotation.value ?: Quotation(
            companyName = comp?.name ?: "Showroom Catalog",
            companyAddress = if (comp?.description?.isNotBlank() == true) comp.description else "Main Market, Commercial Center",
            companyPhone = "+91 98765 43210",
            items = _quotationCart.value
        )
        // Ensure items in quote match cart if quote was empty
        val quoteWithItems = if (defaultQuote.items.isEmpty() && _quotationCart.value.isNotEmpty()) {
            defaultQuote.copy(items = _quotationCart.value)
        } else {
            defaultQuote
        }
        _activeQuotation.value = quoteWithItems
        _isQuotationDialogOpen.value = true
    }

    fun closeQuotationDialog() {
        _isQuotationDialogOpen.value = false
    }

    fun addProductToQuote(product: Product, quantity: Int = 1) {
        val currentItems = _quotationCart.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.productId == product.id || it.code.equals(product.code, ignoreCase = true) }
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            currentItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentItems.add(
                QuotationItem(
                    productId = product.id,
                    code = product.code,
                    name = product.name,
                    brand = product.brand,
                    category = product.category,
                    quantity = maxOf(1, quantity),
                    mrp = product.mrp,
                    discountPercent = product.discountPercent,
                    gstPercent = product.gstPercent
                )
            )
        }
        _quotationCart.value = currentItems

        // Also update active quotation if open
        _activeQuotation.value?.let { currentQuote ->
            _activeQuotation.value = currentQuote.copy(items = currentItems)
        }
    }

    fun addCustomItemToQuote(
        name: String,
        code: String = "",
        brand: String = "",
        quantity: Int = 1,
        mrp: Double,
        discountPercent: Double = 0.0,
        gstPercent: Double = 18.0
    ) {
        val item = QuotationItem(
            code = code.trim(),
            name = name.trim(),
            brand = brand.trim().ifBlank { _activeCompany.value?.name ?: "" },
            quantity = maxOf(1, quantity),
            mrp = maxOf(0.0, mrp),
            discountPercent = discountPercent.coerceIn(0.0, 100.0),
            gstPercent = gstPercent.coerceAtLeast(0.0)
        )
        val updated = _quotationCart.value + item
        _quotationCart.value = updated
        _activeQuotation.value?.let {
            _activeQuotation.value = it.copy(items = updated)
        }
    }

    fun updateQuoteItemQuantity(itemId: String, delta: Int) {
        val updated = _quotationCart.value.mapNotNull { item ->
            if (item.id == itemId) {
                val newQty = item.quantity + delta
                if (newQty > 0) item.copy(quantity = newQty) else null
            } else {
                item
            }
        }
        _quotationCart.value = updated
        _activeQuotation.value?.let {
            _activeQuotation.value = it.copy(items = updated)
        }
    }

    fun updateQuoteItem(updatedItem: QuotationItem) {
        val updated = _quotationCart.value.map { if (it.id == updatedItem.id) updatedItem else it }
        _quotationCart.value = updated
        _activeQuotation.value?.let {
            _activeQuotation.value = it.copy(items = updated)
        }
    }

    fun removeQuoteItem(itemId: String) {
        val updated = _quotationCart.value.filterNot { it.id == itemId }
        _quotationCart.value = updated
        _activeQuotation.value?.let {
            _activeQuotation.value = it.copy(items = updated)
        }
    }

    fun clearQuoteCart() {
        _quotationCart.value = emptyList()
        _activeQuotation.value?.let {
            _activeQuotation.value = it.copy(items = emptyList())
        }
    }

    fun updateActiveQuotation(quotation: Quotation) {
        _activeQuotation.value = quotation
        _quotationCart.value = quotation.items
    }

    fun saveQuotation(quotation: Quotation) {
        val existing = _savedQuotations.value.toMutableList()
        val index = existing.indexOfFirst { it.id == quotation.id }
        if (index >= 0) {
            existing[index] = quotation
        } else {
            existing.add(0, quotation)
        }
        _savedQuotations.value = existing
        _activeQuotation.value = quotation
    }

    fun deleteSavedQuotation(quotationId: String) {
        _savedQuotations.value = _savedQuotations.value.filterNot { it.id == quotationId }
    }
}
