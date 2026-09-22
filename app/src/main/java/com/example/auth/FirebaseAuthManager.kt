package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.data.Product
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class ShowroomUserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "Showroom Manager",
    val activeCompanyName: String = "Hindware",
    val cloudProductCount: Int = 0,
    val lastSyncTime: Long = System.currentTimeMillis()
)

object FirebaseAuthManager {

    private const val TAG = "FirebaseAuthManager"

    fun init(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                FirebaseApp.initializeApp(context)
                Log.i(TAG, "Standard FirebaseApp initialized successfully.")
            } catch (e: Exception) {
                Log.d(TAG, "Default Firebase options not found in resources: ${e.message}")
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(context.packageName)
                        .setApiKey("AIzaSyFakeKeyForShowroomDemo1234567890")
                        .setProjectId("showroom-demo-app")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "Initialized Firebase with fallback demo options.")
                } catch (fallbackError: Exception) {
                    Log.w(TAG, "Fallback Firebase initialization failed: ${fallbackError.message}")
                }
            }
        }
    }

    fun isFirebaseInitialized(): Boolean {
        return try {
            FirebaseApp.getApps(com.google.firebase.FirebaseApp.getInstance().applicationContext).isNotEmpty()
        } catch (e: Throwable) {
            false
        }
    }

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Throwable) {
            null
        }

    val isUserSignedIn: Boolean
        get() = currentUser != null

    /**
     * Listens to auth state changes reactively.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val currentAuth = auth
        if (currentAuth != null) {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                trySend(firebaseAuth.currentUser)
            }
            currentAuth.addAuthStateListener(listener)
            awaitClose { currentAuth.removeAuthStateListener(listener) }
        } else {
            trySend(null)
            awaitClose { }
        }
    }

    /**
     * Signs in with Google using Credential Manager.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized. Please add google-services.json to enable Google Sign-In.")
        )
        try {
            val credentialManager = CredentialManager.create(activity)

            // Web Client ID: can be passed or empty for default discovery
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId("dummy-client-id.apps.googleusercontent.com") // Handled gracefully if not set
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = currentAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user was null after Google sign in.")
                updateUserProfileInFirestore(user)
                Result.success(user)
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Direct Sign In with Email & Password.
     */
    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized. Please add google-services.json to enable Sign-In.")
        )
        try {
            val res = currentAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw IllegalStateException("User null after sign in")
            updateUserProfileInFirestore(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create Account with Email & Password.
     */
    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized. Please add google-services.json to enable Sign-Up.")
        )
        try {
            val res = currentAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = res.user ?: throw IllegalStateException("User null after create")
            updateUserProfileInFirestore(user, customName = name)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Anonymous Guest Sign-In (instant fallback for offline/emulator testing).
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val currentAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase is not initialized. Please add google-services.json to enable Guest mode.")
        )
        try {
            val res = currentAuth.signInAnonymously().await()
            val user = res.user ?: throw IllegalStateException("User null after anonymous sign in")
            updateUserProfileInFirestore(user, customName = "Guest Showroom Admin")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth?.signOut()
    }

    /**
     * Saves or updates the user profile record in Firestore under `users/{uid}`.
     */
    suspend fun updateUserProfileInFirestore(
        user: FirebaseUser,
        customName: String? = null,
        activeCompany: String? = null
    ) = withContext(Dispatchers.IO) {
        val currentFirestore = firestore ?: return@withContext
        try {
            val docRef = currentFirestore.collection("users").document(user.uid)
            val data = hashMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: "guest@showroom.local"),
                "displayName" to (customName ?: user.displayName ?: "Showroom Manager"),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "role" to if (user.isAnonymous) "Guest Evaluator" else "Showroom Manager",
                "lastSyncTime" to System.currentTimeMillis()
            )
            if (activeCompany != null) {
                data["activeCompanyName"] = activeCompany
            }
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update user profile in Firestore: ${e.message}")
        }
    }

    /**
     * Persists product catalogue data to Firestore under `users/{uid}/products/{productId}`.
     */
    suspend fun syncProductsToFirestore(
        products: List<Product>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext Result.failure(IllegalStateException("No user logged in to sync data"))
        val currentFirestore = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized. Please configure Firebase to enable cloud sync.")
        )
        try {
            val userDoc = currentFirestore.collection("users").document(user.uid)
            val productsColl = userDoc.collection("products")

            var count = 0
            val total = products.size

            for (p in products) {
                val docId = "${p.companyId}_${p.code.replace("/", "_")}"
                val data = hashMapOf(
                    "id" to p.id,
                    "companyId" to p.companyId,
                    "companyName" to p.companyName,
                    "code" to p.code,
                    "name" to p.name,
                    "brand" to p.brand,
                    "category" to p.category,
                    "subcategory" to p.subcategory,
                    "mrp" to p.mrp,
                    "discountPercent" to p.discountPercent,
                    "gstPercent" to p.gstPercent,
                    "purchasePrice" to p.purchasePrice,
                    "finalSalePrice" to p.finalSalePrice,
                    "marginAmount" to p.marginAmount,
                    "marginPercent" to p.marginPercent,
                    "stockQuantity" to p.stockQuantity,
                    "notes" to p.notes,
                    "updatedAt" to p.updatedAt
                )
                productsColl.document(docId).set(data, SetOptions.merge()).await()
                count++
                onProgress(count, total)
            }

            // Update user sync timestamp
            userDoc.set(
                hashMapOf("lastSyncTime" to System.currentTimeMillis(), "cloudProductCount" to count),
                SetOptions.merge()
            ).await()

            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restores/fetches products backed up in Firestore for the current user.
     */
    suspend fun fetchProductsFromFirestore(): Result<List<Product>> = withContext(Dispatchers.IO) {
        val user = currentUser ?: return@withContext Result.failure(IllegalStateException("No user logged in"))
        val currentFirestore = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized. Please configure Firebase to enable cloud restore.")
        )
        try {
            val snapshot = currentFirestore.collection("users")
                .document(user.uid)
                .collection("products")
                .get()
                .await()

            val products = snapshot.documents.mapNotNull { doc ->
                try {
                    Product(
                        id = doc.getLong("id") ?: 0L,
                        companyId = doc.getLong("companyId") ?: 1L,
                        companyName = doc.getString("companyName") ?: "",
                        code = doc.getString("code") ?: "",
                        name = doc.getString("name") ?: "",
                        brand = doc.getString("brand") ?: "",
                        category = doc.getString("category") ?: "",
                        subcategory = doc.getString("subcategory") ?: "",
                        mrp = doc.getDouble("mrp") ?: 0.0,
                        discountPercent = doc.getDouble("discountPercent") ?: 0.0,
                        gstPercent = doc.getDouble("gstPercent") ?: 18.0,
                        purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
                        stockQuantity = doc.getLong("stockQuantity")?.toInt() ?: 1,
                        notes = doc.getString("notes") ?: "",
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
