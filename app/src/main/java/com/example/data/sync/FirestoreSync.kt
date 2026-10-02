package com.example.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.auth.FirebaseAuthManager
import com.example.data.CategoryDao
import com.example.data.CategoryEntity
import com.example.data.Company
import com.example.data.CompanyDao
import com.example.data.Product
import com.example.data.ProductDao
import com.example.data.SyncQueueDao
import com.example.data.SyncQueueEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Offline-first Firestore synchronization.
 *
 * - Room hi primary database hai. Har local write Room me hota hai, phir
 *   `isSynced = false` mark hota hai, aur upload best-effort hota hai.
 * - Upload fail ho (internet nahi / permission / timeout) to data Room me
 *   surakshit rehta hai aur `isSynced = false` hi rehta hai → baad me retry.
 * - Conflict: `updatedAt` compare hota hai (local newer → upload, cloud newer →
 *   Room me update, same → kuch nahi).
 * - Delete: Firestore doc delete; offline ho to `sync_queue` me pending rehta hai.
 */
class FirestoreSync(
    context: Context,
    private val companyDao: CompanyDao,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val queueDao: SyncQueueDao,
    private val scope: CoroutineScope
) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("showroom_sync", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private var scheduledJob: Job? = null

    private val _state = MutableStateFlow(
        SyncState(lastSyncAt = prefs.getLong(KEY_LAST_SYNC, 0L))
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    // ---------------------------------------------------------------- helpers

    private fun isOnline(): Boolean = try {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        false
    }

    private fun uid(): String? = try {
        FirebaseAuthManager.currentUser?.uid
    } catch (e: Exception) {
        null
    }

    private fun firestore(): FirebaseFirestore? = FirebaseAuthManager.firestore

    /** Sync chalu ho sakta hai? (login + internet + Firebase configured) */
    fun canSync(): Boolean = FirebaseAuthManager.isConfigured && uid() != null && firestore() != null

    private fun userCollection(uid: String, collection: String) =
        firestore()?.collection(COLLECTION_USERS)?.document(uid)?.collection(collection)

    // ---------------------------------------------------------------- schedule

    /**
     * Local change ke baad upload schedule karta hai (debounce ke saath).
     * UI ko block nahi karta.
     */
    fun scheduleSync(delayMs: Long = 2_000L) {
        if (!canSync()) return
        if (scheduledJob?.isActive == true) return
        scheduledJob = scope.launch {
            delay(delayMs)
            try {
                syncPending()
            } catch (e: Exception) {
                Log.w(TAG, "scheduled sync failed: ${e.message}")
            }
        }
    }

    /** Delete ko cloud queue me daalo (internet aane par Firestore par apply hoga) */
    suspend fun enqueueDelete(collection: String, docId: String) {
        try {
            queueDao.enqueue(SyncQueueEntity(collectionName = collection, docId = docId))
            scheduleSync(delayMs = 1_000L)
        } catch (e: Exception) {
            Log.w(TAG, "enqueueDelete failed: ${e.message}")
        }
    }

    // ---------------------------------------------------------------- pending push

    /**
     * Pending (isSynced = false) rows + queued deletes ko cloud par bhejta hai.
     * Sirf SAFAL write ke baad isSynced = true karta hai.
     */
    suspend fun syncPending(): SyncResult = mutex.withLock { syncPendingInternal() }

    /**
     * Lock ke andar chalta hai (alag se call na karein). Deadlock se bachne ke liye
     * fullSync isi ko lock ke andar bulata hai, `syncPending()` ko nahi.
     */
    private suspend fun syncPendingInternal(): SyncResult {
        if (!FirebaseAuthManager.isConfigured) {
            return SyncResult(false, "Firebase configured nahi hai (google-services.json)")
        }
        val user = uid() ?: return SyncResult(false, "Login nahi hai — pehle sign in karein")
        if (!isOnline()) return SyncResult(false, "Internet nahi hai — data local me safe hai")

        _state.value = _state.value.copy(isSyncing = true, message = "Pending data upload ho raha hai…")
        var pushed = 0
        var deleted = 0
        var failed = 0

        try {
            // 1) queued deletes
            val queued = queueDao.getBatch(300)
            if (queued.isNotEmpty()) {
                val batch = firestore()?.batch() ?: return SyncResult(false, "Firestore available nahi")
                val done = mutableListOf<SyncQueueEntity>()
                for (item in queued) {
                    val coll = userCollection(user, item.collectionName) ?: continue
                    batch.delete(coll.document(item.docId))
                    done.add(item)
                }
                try {
                    batch.commit().await()
                    done.forEach { queueDao.remove(it.id) }
                    deleted += done.size
                } catch (e: Exception) {
                    Log.w(TAG, "delete batch failed: ${e.message}")
                    done.forEach { queueDao.bumpAttempts(it.id) }
                    failed += done.size
                }
            }

            // 2) pending companies
            val companies = companyDao.getUnsyncedCompanies()
            if (companies.isNotEmpty()) {
                try {
                    val coll = userCollection(user, COLLECTION_COMPANIES)
                    if (coll != null) {
                        var batch = firestore()!!.batch()
                        var ops = 0
                        val ids = mutableListOf<Long>()
                        for (company in companies) {
                            batch.set(coll.document(companyDocId(company)), company.toFirestoreMap(), SetOptions.merge())
                            ids.add(company.id)
                            ops++
                            if (ops == 400) {
                                batch.commit().await()
                                companyDao.markSynced(ids.toList())
                                pushed += ids.size
                                ids.clear()
                                ops = 0
                                batch = firestore()!!.batch()
                            }
                        }
                        if (ops > 0) {
                            batch.commit().await()
                            companyDao.markSynced(ids.toList())
                            pushed += ids.size
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "company push failed: ${e.message}")
                    failed++
                }
            }

            // 3) pending categories
            val categories = categoryDao.getUnsyncedCategories()
            if (categories.isNotEmpty()) {
                try {
                    val coll = userCollection(user, COLLECTION_CATEGORIES)
                    if (coll != null) {
                        val batch = firestore()!!.batch()
                        val ids = mutableListOf<Long>()
                        for (category in categories) {
                            batch.set(coll.document(categoryDocId(category)), category.toFirestoreMap(), SetOptions.merge())
                            ids.add(category.id)
                        }
                        if (ids.isNotEmpty()) {
                            batch.commit().await()
                            categoryDao.markSynced(ids.toList())
                            pushed += ids.size
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "category push failed: ${e.message}")
                    failed++
                }
            }

            // 4) pending products
            val products = productDao.getUnsyncedProducts()
            if (products.isNotEmpty()) {
                try {
                    val coll = userCollection(user, COLLECTION_PRODUCTS)
                    if (coll != null) {
                        var batch = firestore()!!.batch()
                        var ops = 0
                        val ids = mutableListOf<Long>()
                        for (product in products) {
                            batch.set(coll.document(productDocId(product)), product.toFirestoreMap(), SetOptions.merge())
                            ids.add(product.id)
                            ops++
                            if (ops == 400) {
                                batch.commit().await()
                                productDao.markSynced(ids.toList())
                                pushed += ids.size
                                ids.clear()
                                ops = 0
                                batch = firestore()!!.batch()
                            }
                        }
                        if (ops > 0) {
                            batch.commit().await()
                            productDao.markSynced(ids.toList())
                            pushed += ids.size
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "product push failed: ${e.message}")
                    failed++
                }
            }

            val message = when {
                pushed == 0 && deleted == 0 && failed == 0 -> "Sab kuch already synced hai ✅"
                failed > 0 -> "$pushed upload, $deleted delete — $failed me dikkat aayi (baad me retry hoga)"
                else -> "$pushed upload, $deleted delete ho gaye ✅"
            }
            _state.value = _state.value.copy(
                isSyncing = false,
                message = message,
                lastSyncAt = System.currentTimeMillis()
            )
            if (pushed > 0 || deleted > 0) stampLastSync()
            return SyncResult(true, message, pushed = pushed, deleted = deleted, failed = failed)
        } catch (e: Exception) {
            Log.e(TAG, "syncPending error: ${e.message}", e)
            _state.value = _state.value.copy(isSyncing = false, message = "Sync nahi ho paya: ${e.message}")
            return SyncResult(false, "Sync nahi ho paya (data local me safe hai)")
        }
    }

    // ---------------------------------------------------------------- pull + merge (initial sync)

    /**
     * Poora sync: cloud se pull + merge (updatedAt ke hisaab se) + pending upload.
     * Login hone ke baad aur app start par call hota hai.
     */
    suspend fun fullSync(): SyncResult {
        // Pull/merge lock ke andar; push lock ke bahar (warna same Mutex par deadlock ho jata hai)
        val pre = mutex.withLock { pullAndMerge() }
        if (pre == null) return SyncResult(false, "Cloud sync abhi nahi ho sakta (offline ya login nahi)")
        val pushResult = syncPending()
        val pulled = pre.pulled
        return pushResult.copy(
            pulled = pulled,
            message = if (pulled > 0) "$pulled records cloud se aaye · ${pushResult.message}" else pushResult.message
        )
    }

    /** Cloud se pull + updatedAt merge. `null` = sync sambhav nahi. */
    private suspend fun pullAndMerge(): SyncResult? {
        if (!FirebaseAuthManager.isConfigured) {
            _state.value = _state.value.copy(isSyncing = false, message = "Firebase configured nahi hai")
            return null
        }
        val user = uid() ?: return null
        if (!isOnline()) {
            _state.value = _state.value.copy(isSyncing = false, message = "Internet nahi hai — offline data")
            return null
        }

        _state.value = _state.value.copy(isSyncing = true, message = "Cloud se data milaa rahe hain…")
        var pulled = 0

        try {
            val companiesColl = userCollection(user, COLLECTION_COMPANIES)
            val categoriesColl = userCollection(user, COLLECTION_CATEGORIES)
            val productsColl = userCollection(user, COLLECTION_PRODUCTS)

            // ---------- companies (pehle, kyunki products in par depend karte hain) ----------
            val companyIdMap = HashMap<Long, Long>()
            if (companiesColl != null) {
                val cloudCompanies = companiesColl.get().await().documents.mapNotNull { mapToCompany(it) }
                val localCompanies = companyDao.getAllCompaniesOnce()
                val localByName = localCompanies.associateBy { it.name.trim().lowercase() }
                val localById = localCompanies.associateBy { it.id }

                for (cloud in cloudCompanies) {
                    val byId = localById[cloud.id]
                    if (byId != null && byId.name.trim().equals(cloud.name.trim(), ignoreCase = true)) {
                        companyIdMap[cloud.id] = byId.id
                        if (cloud.updatedAt > byId.updatedAt) {
                            companyDao.upsertFromCloud(cloud.copy(id = byId.id))
                            pulled++
                        }
                        continue
                    }
                    val byName = localByName[cloud.name.trim().lowercase()]
                    if (byName != null) {
                        companyIdMap[cloud.id] = byName.id
                        if (cloud.updatedAt > byName.updatedAt) {
                            companyDao.upsertFromCloud(cloud.copy(id = byName.id))
                            pulled++
                        }
                    } else {
                        val newId = companyDao.upsertFromCloud(cloud.copy(id = 0L))
                        companyIdMap[cloud.id] = newId
                        pulled++
                    }
                }
                // local newer ya cloud me nahi -> pending mark (upload syncPending me hota hai)
                for (local in localCompanies) {
                    val cloudMatch = cloudCompanies.firstOrNull { it.name.trim().equals(local.name.trim(), ignoreCase = true) }
                    if (cloudMatch == null || local.updatedAt > cloudMatch.updatedAt) {
                        companyDao.markUnsynced(local.id)
                    }
                }
            }

            // ---------- categories ----------
            if (categoriesColl != null) {
                val cloudCategories = categoriesColl.get().await().documents.mapNotNull { mapToCategory(it) }
                val localCategories = categoryDao.getAllCategoriesOnce()
                val localByName = localCategories.associateBy { it.name.trim().lowercase() }

                for (cloud in cloudCategories) {
                    val local = localByName[cloud.name.trim().lowercase()]
                    if (local == null) {
                        categoryDao.upsertFromCloud(cloud.copy(id = 0L))
                        pulled++
                    } else if (cloud.updatedAt > local.updatedAt) {
                        categoryDao.upsertFromCloud(cloud.copy(id = local.id))
                        pulled++
                    }
                }
                for (local in localCategories) {
                    val cloudMatch = cloudCategories.firstOrNull { it.name.trim().equals(local.name.trim(), ignoreCase = true) }
                    if (cloudMatch == null || local.updatedAt > cloudMatch.updatedAt) {
                        categoryDao.markUnsynced(local.id)
                    }
                }
            }

            // ---------- products ----------
            if (productsColl != null) {
                val cloudProducts = productsColl.get().await().documents.mapNotNull { mapToProduct(it) }
                val localProducts = productDao.getAllProductsOnce()

                // (companyId, code) hi stable key hai — autoGenerate id device par badal jati hai
                val localByKey = localProducts.associateBy { "${it.companyId}_${it.code.trim().lowercase()}" }
                val localById = localProducts.associateBy { it.id }

                for (cloud in cloudProducts) {
                    val resolvedCompanyId = resolveCompanyId(
                        cloudCompanyId = cloud.companyId,
                        cloudCompanyName = cloud.companyName,
                        mapping = companyIdMap,
                        cloudProductsCompanyFallback = cloud
                    )
                    val fixed = cloud.copy(companyId = resolvedCompanyId)
                    val key = "${resolvedCompanyId}_${cloud.code.trim().lowercase()}"
                    val local = localByKey[key]

                    if (local == null) {
                        // naya row — id clash ho to naya id lo (kisi doosri row ko overwrite na kare)
                        val idSafe = if (localById.containsKey(cloud.id)) 0L else cloud.id
                        productDao.upsertFromCloud(fixed.copy(id = idSafe))
                        pulled++
                    } else if (cloud.updatedAt > local.updatedAt) {
                        productDao.upsertFromCloud(fixed.copy(id = local.id))
                        pulled++
                    }
                }
                for (local in localProducts) {
                    val cloudMatch = cloudProducts.firstOrNull {
                        it.code.trim().equals(local.code.trim(), ignoreCase = true) &&
                            (it.companyId == local.companyId ||
                                it.companyName.trim().equals(local.companyName.trim(), ignoreCase = true))
                    }
                    if (cloudMatch == null || local.updatedAt > cloudMatch.updatedAt) {
                        productDao.markUnsynced(local.id)
                    }
                }
            }

            _state.value = _state.value.copy(isSyncing = false, message = "Cloud se $pulled records mile")
            stampLastSync()

            // pending upload ka kaam `fullSync()` (lock ke bahar) karta hai
            return SyncResult(true, "Cloud se $pulled records mile", pulled = pulled)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "fullSync error: ${e.message}", e)
            _state.value = _state.value.copy(isSyncing = false, message = "Cloud sync fail: ${e.message}")
            return null
        }
    }

    /** Product ka companyId local database me resolve karo (naam se match ya nayi company) */
    private suspend fun resolveCompanyId(
        cloudCompanyId: Long,
        cloudCompanyName: String,
        mapping: HashMap<Long, Long>,
        cloudProductsCompanyFallback: Product
    ): Long {
        mapping[cloudCompanyId]?.let { return it }
        val byId = companyDao.getCompanyById(cloudCompanyId)
        if (byId != null && (cloudCompanyName.isBlank() || byId.name.trim().equals(cloudCompanyName.trim(), ignoreCase = true))) {
            mapping[cloudCompanyId] = byId.id
            return byId.id
        }
        if (cloudCompanyName.isNotBlank()) {
            val byName = companyDao.getCompanyByName(cloudCompanyName.trim())
            if (byName != null) {
                mapping[cloudCompanyId] = byName.id
                return byName.id
            }
            val newId = companyDao.upsertFromCloud(
                Company(
                    id = 0L,
                    name = cloudCompanyName.trim(),
                    description = "",
                    isDefault = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = cloudProductsCompanyFallback.updatedAt,
                    isSynced = true
                )
            )
            mapping[cloudCompanyId] = newId
            return newId
        }
        // company hi nahi mili — local me pehli company use karo (ya 1L, purana behaviour)
        val fallback = companyDao.getAllCompaniesOnce().firstOrNull()?.id ?: 1L
        mapping[cloudCompanyId] = fallback
        return fallback
    }

    private fun stampLastSync() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
        _state.value = _state.value.copy(lastSyncAt = now)
    }

    companion object {
        private const val TAG = "FirestoreSync"
        private const val COLLECTION_USERS = "users"
        const val COLLECTION_COMPANIES = "companies"
        const val COLLECTION_CATEGORIES = "categories"
        const val COLLECTION_PRODUCTS = "products"
        private const val KEY_LAST_SYNC = "last_sync_at"
    }
}

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncAt: Long = 0L,
    val message: String? = null
)

data class SyncResult(
    val ok: Boolean,
    val message: String,
    val pushed: Int = 0,
    val pulled: Int = 0,
    val deleted: Int = 0,
    val failed: Int = 0
)
