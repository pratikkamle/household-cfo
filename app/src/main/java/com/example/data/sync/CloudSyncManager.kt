package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.AuthAccount
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.util.CategoryBudgetScaler
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Live(val householdCode: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class CloudSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            Firebase.auth
        } catch (e: Exception) {
            Log.w("CloudSync", "FirebaseAuth initialization deferred: ${e.message}")
            null
        }
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    // Custom Firestore database instance
    private val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.w("CloudSync", "Firestore instance error: ${e.message}")
            null
        }
    }

    private val authPrefs = context.getSharedPreferences("household_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authAccount = MutableStateFlow<AuthAccount?>(
        authPrefs.getString("auth_user_email", null)?.let { savedEmail ->
            AuthAccount(
                uid = authPrefs.getString("auth_user_uid", "local_user") ?: "local_user",
                email = savedEmail,
                displayName = authPrefs.getString("auth_user_name", "Household Admin") ?: "Household Admin",
                isGoogle = authPrefs.getBoolean("auth_is_google", false)
            )
        }
    )
    val authAccount: StateFlow<AuthAccount?> = _authAccount.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var activeHouseholdCode: String? = null

    init {
        try {
            auth?.let { fbAuth ->
                _currentUser.value = fbAuth.currentUser
                syncAuthAccountFromFirebase(fbAuth.currentUser)
                fbAuth.addAuthStateListener { firebaseAuth ->
                    _currentUser.value = firebaseAuth.currentUser
                    syncAuthAccountFromFirebase(firebaseAuth.currentUser)
                }
            }
        } catch (e: Exception) {
            Log.w("CloudSync", "Could not register auth listener: ${e.message}")
        }
    }

    private fun syncAuthAccountFromFirebase(user: FirebaseUser?) {
        if (user != null) {
            val account = AuthAccount(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName?.ifBlank { null }
                    ?: (user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"),
                isGoogle = user.providerData.any { it.providerId == "google.com" }
            )
            setAuthenticatedUser(account)
        }
    }

    fun setAuthenticatedUser(account: AuthAccount) {
        authPrefs.edit()
            .putString("auth_user_email", account.email)
            .putString("auth_user_name", account.displayName)
            .putString("auth_user_uid", account.uid)
            .putBoolean("auth_is_google", account.isGoogle)
            .apply()
        _authAccount.value = account
    }

    fun attemptSilentSignIn(scope: CoroutineScope) {
        val fbAuth = auth ?: return
        if (fbAuth.currentUser != null) {
            _currentUser.value = fbAuth.currentUser
            syncAuthAccountFromFirebase(fbAuth.currentUser)
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch(Dispatchers.IO) {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCred = GoogleAuthProvider.getCredential(token, null)
                    fbAuth.signInWithCredential(authCred).await()
                    _currentUser.value = fbAuth.currentUser
                    syncAuthAccountFromFirebase(fbAuth.currentUser)
                }
            } catch (e: Exception) {
                Log.d("CloudSync", "Silent sign-in not available: ${e.message}")
            }
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        scope: CoroutineScope,
        onSuccess: (AuthAccount) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            // Offline/Local fallback
            val account = AuthAccount(
                uid = "local_" + email.hashCode(),
                email = email.trim(),
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            )
            setAuthenticatedUser(account)
            onSuccess(account)
            return
        }
        scope.launch {
            try {
                val res = fbAuth.signInWithEmailAndPassword(email.trim(), pass).await()
                val user = res.user
                if (user != null) {
                    val account = AuthAccount(
                        uid = user.uid,
                        email = user.email ?: email.trim(),
                        displayName = user.displayName?.ifBlank { null }
                            ?: (user.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "User"),
                        isGoogle = false
                    )
                    setAuthenticatedUser(account)
                    _currentUser.value = user
                    onSuccess(account)
                } else {
                    onError("Unable to retrieve user credentials.")
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "Email sign in error: ${e.message}", e)
                onError(e.localizedMessage ?: "Invalid email or password.")
            }
        }
    }

    fun registerWithEmail(
        name: String,
        email: String,
        pass: String,
        scope: CoroutineScope,
        onSuccess: (AuthAccount) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            val account = AuthAccount(
                uid = "local_" + email.hashCode(),
                email = email.trim(),
                displayName = name.trim()
            )
            setAuthenticatedUser(account)
            onSuccess(account)
            return
        }
        scope.launch {
            try {
                val res = fbAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
                val user = res.user
                if (user != null) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim())
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        Log.w("CloudSync", "Could not set display name: ${e.message}")
                    }
                    val account = AuthAccount(
                        uid = user.uid,
                        email = user.email ?: email.trim(),
                        displayName = name.trim(),
                        isGoogle = false
                    )
                    setAuthenticatedUser(account)
                    _currentUser.value = user
                    onSuccess(account)
                } else {
                    onError("Failed to create user account.")
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "Registration error: ${e.message}", e)
                onError(e.localizedMessage ?: "Registration failed.")
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (AuthAccount) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val fbAuth = auth
        if (fbAuth == null) {
            onError("Cloud service is not initialized")
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            onError("Google Sign-In configuration missing")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    fbAuth.signInWithCredential(authCredential).await()
                    val user = fbAuth.currentUser
                    _currentUser.value = user
                    val account = AuthAccount(
                        uid = user?.uid ?: "google_user",
                        email = user?.email ?: "",
                        displayName = user?.displayName ?: (user?.email?.substringBefore("@") ?: "Google User"),
                        isGoogle = true
                    )
                    setAuthenticatedUser(account)
                    onSuccess(account)
                } else {
                    onError("Unexpected credential format")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("CloudSync", "Google Sign-In dismissed: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("CloudSync", "Google Sign-In failed", e)
                onError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    suspend fun findHouseholdForUser(uid: String, email: String): String? {
        val db = firestore ?: return null
        try {
            // 1. Check user_households by UID
            val doc = db.collection("user_households").document(uid).get().await()
            if (doc.exists()) {
                val code = doc.getString("householdCode")
                if (!code.isNullOrBlank()) return code
            }
            // 2. Check user_households by sanitized email
            val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
            if (cleanEmail.isNotBlank()) {
                val emailDoc = db.collection("user_households").document(cleanEmail).get().await()
                if (emailDoc.exists()) {
                    val code = emailDoc.getString("householdCode")
                    if (!code.isNullOrBlank()) return code
                }
            }
            // 3. Check households by adminEmail
            val query = db.collection("households")
                .whereEqualTo("adminEmail", email.trim().lowercase())
                .limit(1)
                .get().await()
            if (!query.isEmpty) {
                val hDoc = query.documents.first()
                return hDoc.getString("inviteCode") ?: hDoc.id
            }
        } catch (e: Exception) {
            Log.w("CloudSync", "findHouseholdForUser query failed: ${e.message}")
        }
        return null
    }

    fun linkUserToHousehold(uid: String, email: String, householdCode: String, role: String, scope: CoroutineScope) {
        val db = firestore ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "householdCode" to householdCode.trim().uppercase(),
                    "role" to role,
                    "email" to email.trim().lowercase(),
                    "uid" to uid,
                    "linkedAt" to System.currentTimeMillis()
                )
                db.collection("user_households").document(uid).set(data, SetOptions.merge()).await()
                val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                if (cleanEmail.isNotBlank()) {
                    db.collection("user_households").document(cleanEmail).set(data, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                Log.w("CloudSync", "linkUserToHousehold error: ${e.message}")
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        stopRealtimeSync()
        authPrefs.edit().clear().apply()
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w("CloudSync", "Sign out error: ${e.message}")
        }
        _currentUser.value = null
        _authAccount.value = null
        _syncState.value = SyncState.Idle
        activeHouseholdCode = null
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to clear credential state", e)
            } finally {
                onComplete()
            }
        }
    }

    /**
     * Start live real-time sync listeners for a shared household
     */
    fun startRealtimeSync(householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val cleanCode = householdCode.trim().uppercase()
        if (activeHouseholdCode == cleanCode && activeListeners.isNotEmpty()) return

        stopRealtimeSync()
        activeHouseholdCode = cleanCode
        _syncState.value = SyncState.Live(cleanCode)

        try {
            // 1. Listen to Household metadata & Budget Plan
            val hSub = db.collection("households").document(cleanCode)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val name = snapshot.getString("name") ?: return@launch
                        val budget = snapshot.getDouble("monthlyDiscretionaryBudget") ?: 30000.0
                        val admin = snapshot.getString("adminName") ?: "Admin"
                        val existing = database.householdDao().getPrimaryHousehold()
                        if (existing != null) {
                            database.householdDao().updateHousehold(
                                existing.copy(
                                    name = name,
                                    monthlyDiscretionaryBudget = budget,
                                    adminName = admin,
                                    inviteCode = cleanCode,
                                    isCloudSynced = true
                                )
                            )
                        } else {
                            database.householdDao().insertHousehold(
                                HouseholdEntity(
                                    id = 1L,
                                    name = name,
                                    currency = snapshot.getString("currency") ?: "INR",
                                    monthlyDiscretionaryBudget = budget,
                                    inviteCode = cleanCode,
                                    adminName = admin,
                                    adminEmail = snapshot.getString("adminEmail") ?: "",
                                    securityPin = snapshot.getString("securityPin") ?: "1234",
                                    isCloudSynced = true,
                                    createdAt = snapshot.getLong("createdAt") ?: System.currentTimeMillis()
                                )
                            )
                        }
                        if (budget > 0) {
                            val currentBudgets = database.budgetDao().getBudgets("").first()
                            val sum = currentBudgets.sumOf { it.monthlyBudget }
                            if (currentBudgets.isEmpty() || kotlin.math.abs(sum - budget) > 1.0) {
                                val scaled = com.example.util.CategoryBudgetScaler.scaleBudgets(budget)
                                database.budgetDao().insertBudgets(scaled)
                            }
                        }
                    }
                }
            activeListeners.add(hSub)

            // 2. Listen to Transactions in real-time
            val tSub = db.collection("households").document(cleanCode)
                .collection("transactions")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshots.documentChanges) {
                            val data = doc.document.data
                            val id = (data["id"] as? String)?.toLongOrNull() ?: doc.document.id.removePrefix("tx_").toLongOrNull() ?: continue
                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val tx = TransactionEntity(
                                        id = id,
                                        householdId = 1L,
                                        userId = (data["userId"] as? Number)?.toLong(),
                                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                        category = data["category"] as? String ?: "General",
                                        paidBy = data["paidBy"] as? String ?: "PRATIK",
                                        paymentMethod = data["paymentMethod"] as? String ?: "UPI",
                                        accountId = (data["accountId"] as? Number)?.toLong(),
                                        toAccountId = (data["toAccountId"] as? Number)?.toLong(),
                                        accountName = data["accountName"] as? String ?: "",
                                        merchant = data["merchant"] as? String ?: "",
                                        notes = data["notes"] as? String ?: "",
                                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                        monthYear = data["monthYear"] as? String ?: "2026-10",
                                        nature = data["nature"] as? String ?: "DISCRETIONARY",
                                        type = data["type"] as? String ?: "EXPENSE",
                                        isRecurring = data["isRecurring"] as? Boolean ?: false,
                                        recurringRuleId = (data["recurringRuleId"] as? Number)?.toLong()
                                    )
                                    database.transactionDao().insertTransaction(tx)
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    database.transactionDao().deleteTransactionById(id)
                                }
                            }
                        }
                    }
                }
            activeListeners.add(tSub)

            // 3. Listen to Members in real-time
            val mSub = db.collection("households").document(cleanCode)
                .collection("members")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshots.documents) {
                            val data = doc.data ?: continue
                            val name = data["name"] as? String ?: continue
                            val role = data["role"] as? String ?: "MEMBER"
                            val colorHex = data["colorHex"] as? String ?: "#0D9488"
                            val email = data["email"] as? String ?: ""
                            val initial = data["initial"] as? String ?: name.take(1).uppercase()
                            val docUid = (data["uid"] as? String)?.ifBlank { null }
                                ?: doc.id.takeIf { !it.startsWith("user_") }

                            val existing = (if (email.isNotBlank()) database.userDao().getUserByEmail(email) else null)
                                ?: (if (!docUid.isNullOrBlank()) database.userDao().getUserByFirebaseUid(docUid) else null)
                                ?: database.userDao().getUserByName(name)

                            if (existing != null) {
                                if (existing.role != role || (email.isNotBlank() && existing.email != email) || existing.colorHex != colorHex) {
                                    database.userDao().updateUser(
                                        existing.copy(
                                            role = role,
                                            email = if (email.isNotBlank()) email else existing.email,
                                            colorHex = if (colorHex.isNotBlank()) colorHex else existing.colorHex,
                                            initial = initial,
                                            firebaseUid = if (!docUid.isNullOrBlank()) docUid else existing.firebaseUid
                                        )
                                    )
                                }
                            } else {
                                database.userDao().insertUser(
                                    UserEntity(
                                        householdId = 1L,
                                        name = name.trim(),
                                        email = email.trim(),
                                        role = role,
                                        initial = initial,
                                        colorHex = colorHex,
                                        firebaseUid = docUid ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
            activeListeners.add(mSub)

            // 4. Listen to Category Budgets in real-time
            val bSub = db.collection("households").document(cleanCode)
                .collection("category_budgets")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        val budgets = snapshots.documents.mapNotNull { doc ->
                            val cat = doc.getString("category") ?: doc.id
                            val amt = doc.getDouble("budgetAmount") ?: doc.getDouble("monthlyBudget") ?: return@mapNotNull null
                            CategoryBudgetEntity(category = cat, monthlyBudget = amt)
                        }
                        if (budgets.isNotEmpty()) {
                            database.budgetDao().insertBudgets(budgets)
                        }
                    }
                }
            activeListeners.add(bSub)

            // 5. Listen to Accounts in real-time
            val aSub = db.collection("households").document(cleanCode)
                .collection("accounts")
                .addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshots.documentChanges) {
                            val data = doc.document.data
                            val id = (data["id"] as? String)?.toLongOrNull() ?: doc.document.id.removePrefix("acc_").toLongOrNull() ?: continue
                            when (doc.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    val acc = AccountEntity(
                                        id = id,
                                        householdId = 1L,
                                        userId = (data["userId"] as? Number)?.toLong(),
                                        name = data["name"] as? String ?: "Account",
                                        holder = data["holder"] as? String ?: "JOINT",
                                        accountType = data["accountType"] as? String ?: "BANK",
                                        balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                                        accountNumberMask = data["accountNumberMask"] as? String ?: "",
                                        institution = data["institution"] as? String ?: "",
                                        notes = data["notes"] as? String ?: "",
                                        creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0
                                    )
                                    database.accountDao().insertAccount(acc)
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    database.accountDao().deleteAccountById(id)
                                }
                            }
                        }
                    }
                }
            activeListeners.add(aSub)

        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to start realtime sync: ${e.message}", e)
        }
    }

    fun stopRealtimeSync() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        activeHouseholdCode = null
    }

    /**
     * Join an existing household via Invite Code
     */
    fun joinHouseholdByCode(
        inviteCode: String,
        memberName: String,
        memberEmail: String = "",
        scope: CoroutineScope,
        onResult: (Boolean, String) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onResult(false, "Cloud service is currently unavailable")
            return
        }
        val cleanCode = inviteCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Please enter a valid invite code")
            return
        }

        _syncState.value = SyncState.Syncing

        scope.launch(Dispatchers.IO) {
            try {
                val doc = db.collection("households").document(cleanCode).get().await()
                if (!doc.exists()) {
                    withContext(Dispatchers.Main) {
                        _syncState.value = SyncState.Error("Household not found")
                        onResult(false, "Household with code '$cleanCode' not found. Check with your admin.")
                    }
                    return@launch
                }

                val householdName = doc.getString("name") ?: "Shared Household"
                val adminName = doc.getString("adminName") ?: "Admin"
                val monthlyBudget = doc.getDouble("monthlyDiscretionaryBudget") ?: 66500.0

                // 1. Update local household
                val localH = HouseholdEntity(
                    id = 1L,
                    name = householdName,
                    currency = doc.getString("currency") ?: "INR",
                    monthlyDiscretionaryBudget = monthlyBudget,
                    inviteCode = cleanCode,
                    adminName = adminName,
                    isCloudSynced = true
                )
                database.householdDao().insertHousehold(localH)

                // 2. Register user in household in Firestore
                val userUid = auth?.currentUser?.uid ?: "user_${System.currentTimeMillis()}"
                val cleanEmail = memberEmail.trim().lowercase().replace(".", "_").replace("@", "_at_")
                
                // Check if user document already exists in Firestore to preserve any promoted role (Admin/Contributor)
                val existingDoc = try {
                    val byUid = db.collection("households").document(cleanCode)
                        .collection("members").document(userUid).get().await()
                    if (byUid.exists()) byUid else if (cleanEmail.isNotBlank()) {
                        db.collection("households").document(cleanCode)
                            .collection("members").document("user_${cleanEmail}").get().await()
                    } else null
                } catch (e: Exception) { null }

                val assignedRole = existingDoc?.getString("role")?.takeIf { it.isNotBlank() } ?: "MEMBER"
                val assignedColor = existingDoc?.getString("colorHex")?.takeIf { it.isNotBlank() } ?: "#E11D48"

                val memberMap = mapOf(
                    "name" to memberName.trim(),
                    "email" to memberEmail.trim(),
                    "role" to assignedRole,
                    "colorHex" to assignedColor,
                    "initial" to memberName.trim().take(1).uppercase(),
                    "uid" to userUid,
                    "joinedAt" to System.currentTimeMillis()
                )
                db.collection("households").document(cleanCode)
                    .collection("members").document(userUid)
                    .set(memberMap, SetOptions.merge()).await()

                if (cleanEmail.isNotBlank()) {
                    db.collection("households").document(cleanCode)
                        .collection("members").document("user_${cleanEmail}")
                        .set(memberMap, SetOptions.merge()).await()
                }

                // Insert into local users table
                database.userDao().insertUser(
                    UserEntity(
                        householdId = 1L,
                        name = memberName.trim(),
                        email = memberEmail.trim(),
                        role = assignedRole,
                        initial = memberName.trim().take(1).uppercase(),
                        colorHex = assignedColor,
                        firebaseUid = userUid
                    )
                )

                // 3. Pull existing transactions from Firestore
                val txDocs = db.collection("households").document(cleanCode)
                    .collection("transactions").get().await()
                val txList = txDocs.documents.mapNotNull { d ->
                    val data = d.data ?: return@mapNotNull null
                    val id = (data["id"] as? String)?.toLongOrNull() ?: d.id.removePrefix("tx_").toLongOrNull() ?: return@mapNotNull null
                    TransactionEntity(
                        id = id,
                        householdId = 1L,
                        userId = (data["userId"] as? Number)?.toLong(),
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        category = data["category"] as? String ?: "General",
                        paidBy = data["paidBy"] as? String ?: memberName,
                        paymentMethod = data["paymentMethod"] as? String ?: "UPI",
                        accountId = (data["accountId"] as? Number)?.toLong(),
                        toAccountId = (data["toAccountId"] as? Number)?.toLong(),
                        accountName = data["accountName"] as? String ?: "",
                        merchant = data["merchant"] as? String ?: "",
                        notes = data["notes"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        monthYear = data["monthYear"] as? String ?: "2026-10",
                        nature = data["nature"] as? String ?: "DISCRETIONARY",
                        type = data["type"] as? String ?: "EXPENSE"
                    )
                }
                if (txList.isNotEmpty()) {
                    database.transactionDao().insertTransactions(txList)
                }

                // 4. Pull category budgets
                val bDocs = db.collection("households").document(cleanCode)
                    .collection("category_budgets").get().await()
                val bList = bDocs.documents.mapNotNull { d ->
                    val cat = d.getString("category") ?: d.id
                    val amt = d.getDouble("budgetAmount") ?: d.getDouble("monthlyBudget") ?: return@mapNotNull null
                    CategoryBudgetEntity(category = cat, monthlyBudget = amt)
                }
                if (bList.isNotEmpty()) {
                    database.budgetDao().insertBudgets(bList)
                } else {
                    val scaled = CategoryBudgetScaler.scaleBudgets(monthlyBudget)
                    database.budgetDao().insertBudgets(scaled)
                }

                linkUserToHousehold(userUid, memberEmail, cleanCode, "MEMBER", scope)

                // 5. Start real-time sync
                startRealtimeSync(cleanCode, scope)

                withContext(Dispatchers.Main) {
                    _syncState.value = SyncState.Live(cleanCode)
                    onResult(true, "Connected to $householdName! Live sync active.")
                }
            } catch (e: Exception) {
                Log.e("CloudSync", "Join household error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _syncState.value = SyncState.Error(e.localizedMessage ?: "Failed to join")
                    onResult(false, e.localizedMessage ?: "Failed to join household")
                }
            }
        }
    }

    /**
     * Create or publish local household to Cloud so partners can join
     */
    fun publishHousehold(household: HouseholdEntity, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = household.inviteCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "name" to household.name,
                    "currency" to household.currency,
                    "monthlyDiscretionaryBudget" to household.monthlyDiscretionaryBudget,
                    "inviteCode" to code,
                    "adminName" to household.adminName,
                    "adminEmail" to (auth?.currentUser?.email ?: household.adminEmail),
                    "createdAt" to household.createdAt
                )
                db.collection("households").document(code).set(data, SetOptions.merge()).await()

                // Upload members
                val members = database.userDao().getAllUsers().first()
                members.forEach { m ->
                    val mMap = mapOf(
                        "name" to m.name,
                        "email" to m.email,
                        "role" to m.role,
                        "colorHex" to m.colorHex,
                        "initial" to m.initial
                    )
                    db.collection("households").document(code)
                        .collection("members").document("user_${m.id}")
                        .set(mMap, SetOptions.merge())
                }

                // Upload budgets scaled accurately to the household's monthlyDiscretionaryBudget
                val scaled = CategoryBudgetScaler.scaleBudgets(household.monthlyDiscretionaryBudget)
                database.budgetDao().insertBudgets(scaled)
                scaled.forEach { b ->
                    db.collection("households").document(code)
                        .collection("category_budgets").document(b.category)
                        .set(mapOf("category" to b.category, "monthlyBudget" to b.monthlyBudget, "budgetAmount" to b.monthlyBudget), SetOptions.merge())
                }

                val adminUid = auth?.currentUser?.uid ?: "admin_local"
                val adminEmail = auth?.currentUser?.email ?: household.adminEmail
                linkUserToHousehold(adminUid, adminEmail, code, "ADMIN", scope)

                startRealtimeSync(code, scope)
            } catch (e: Exception) {
                Log.w("CloudSync", "Publish household error: ${e.message}")
            }
        }
    }

    /**
     * Push a newly created/updated transaction to Cloud for immediate partner visibility
     */
    fun pushTransaction(tx: TransactionEntity, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                val map = mapOf(
                    "id" to tx.id.toString(),
                    "userId" to tx.userId,
                    "amount" to tx.amount,
                    "category" to tx.category,
                    "paidBy" to tx.paidBy,
                    "paymentMethod" to tx.paymentMethod,
                    "accountId" to tx.accountId,
                    "toAccountId" to tx.toAccountId,
                    "accountName" to tx.accountName,
                    "merchant" to tx.merchant,
                    "notes" to tx.notes,
                    "timestamp" to tx.timestamp,
                    "monthYear" to tx.monthYear,
                    "nature" to tx.nature,
                    "type" to tx.type,
                    "isRecurring" to tx.isRecurring,
                    "recurringRuleId" to tx.recurringRuleId,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("households").document(code)
                    .collection("transactions").document("tx_${tx.id}")
                    .set(map, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to push transaction: ${e.message}")
            }
        }
    }

    fun deleteTransaction(txId: Long, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                db.collection("households").document(code)
                    .collection("transactions").document("tx_$txId")
                    .delete().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to delete transaction from cloud: ${e.message}")
            }
        }
    }

    /**
     * Push household budget plan updates (Admin editable)
     */
    fun pushBudgetPlan(
        householdCode: String,
        monthlyBudget: Double,
        categoryBudgets: List<CategoryBudgetEntity>,
        scope: CoroutineScope
    ) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                // Update household discretionary budget limit
                db.collection("households").document(code).update("monthlyDiscretionaryBudget", monthlyBudget).await()
                // Update category budgets
                val batch = db.batch()
                categoryBudgets.forEach { b ->
                    val ref = db.collection("households").document(code)
                        .collection("category_budgets").document(b.category)
                    batch.set(ref, mapOf("category" to b.category, "monthlyBudget" to b.monthlyBudget, "budgetAmount" to b.monthlyBudget), SetOptions.merge())
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to push budget plan: ${e.message}")
            }
        }
    }

    /**
     * Push a newly added member
     */
    fun pushMember(member: UserEntity, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                val map = mapOf(
                    "name" to member.name,
                    "email" to member.email,
                    "role" to member.role,
                    "colorHex" to member.colorHex,
                    "initial" to member.initial,
                    "uid" to member.firebaseUid,
                    "joinedAt" to System.currentTimeMillis()
                )
                db.collection("households").document(code)
                    .collection("members").document("user_${member.id}")
                    .set(map, SetOptions.merge()).await()

                if (member.firebaseUid.isNotBlank()) {
                    db.collection("households").document(code)
                        .collection("members").document(member.firebaseUid)
                        .set(map, SetOptions.merge()).await()
                    db.collection("user_households").document(member.firebaseUid)
                        .set(mapOf("role" to member.role), SetOptions.merge())
                }

                if (member.email.isNotBlank()) {
                    val cleanEmail = member.email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                    db.collection("households").document(code)
                        .collection("members").document("user_${cleanEmail}")
                        .set(map, SetOptions.merge()).await()
                    db.collection("user_households").document(cleanEmail)
                        .set(mapOf("role" to member.role), SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to push member: ${e.message}")
            }
        }
    }

    fun pushAccount(account: AccountEntity, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                val map = mapOf(
                    "id" to account.id.toString(),
                    "name" to account.name,
                    "holder" to account.holder,
                    "accountType" to account.accountType,
                    "balance" to account.balance,
                    "accountNumberMask" to account.accountNumberMask,
                    "institution" to account.institution,
                    "notes" to account.notes,
                    "creditLimit" to account.creditLimit,
                    "userId" to account.userId,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("households").document(code)
                    .collection("accounts").document("acc_${account.id}")
                    .set(map, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to push account: ${e.message}")
            }
        }
    }

    fun deleteAccount(accountId: Long, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                db.collection("households").document(code)
                    .collection("accounts").document("acc_$accountId")
                    .delete().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to delete account from cloud: ${e.message}")
            }
        }
    }

    fun deleteMember(userId: Long, householdCode: String, scope: CoroutineScope) {
        val db = firestore ?: return
        val code = householdCode.ifBlank { "CFO-8821" }
        scope.launch(Dispatchers.IO) {
            try {
                db.collection("households").document(code)
                    .collection("members").document("user_$userId")
                    .delete().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "Failed to delete member from cloud: ${e.message}")
            }
        }
    }

    /**
     * Native Android Share Intent for household invite code and shareable link
     */
    fun shareHouseholdInvite(activityContext: Context, householdName: String, inviteCode: String) {
        val shareText = """
            🏡 Join our household "$householdName" on Household CFO!
            
            Use Household Invite Code: $inviteCode
            
            Link to see and track our family expenses, accounts, and budget plan together in real-time!
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Invite to join $householdName on Household CFO")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Household Invite Link")
        activityContext.startActivity(shareIntent)
    }

    /**
     * Backup all data to cloud manually
     */
    fun syncToCloud(scope: CoroutineScope) {
        val user = auth?.currentUser
        if (user == null) {
            _syncState.value = SyncState.Error("Sign in required for cloud backup")
            return
        }
        val db = firestore
        if (db == null) {
            _syncState.value = SyncState.Error("Firestore database unavailable")
            return
        }

        _syncState.value = SyncState.Syncing

        scope.launch(Dispatchers.IO) {
            try {
                val household = database.householdDao().getPrimaryHousehold()
                val code = household?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
                publishHousehold(household ?: HouseholdEntity(name = "Family Household"), scope)

                val transactions = database.transactionDao().getAllTransactions().first()
                val accounts = database.accountDao().getAllAccounts().first()

                val batch = db.batch()

                transactions.forEach { tx ->
                    val docRef = db.collection("households").document(code)
                        .collection("transactions").document("tx_${tx.id}")
                    val map = mapOf(
                        "id" to tx.id.toString(),
                        "amount" to tx.amount,
                        "category" to tx.category,
                        "paidBy" to tx.paidBy,
                        "paymentMethod" to tx.paymentMethod,
                        "accountId" to tx.accountId,
                        "accountName" to tx.accountName,
                        "merchant" to tx.merchant,
                        "notes" to tx.notes,
                        "timestamp" to tx.timestamp,
                        "monthYear" to tx.monthYear,
                        "nature" to tx.nature,
                        "type" to tx.type,
                        "isRecurring" to tx.isRecurring,
                        "syncUserId" to user.uid
                    )
                    batch.set(docRef, map, SetOptions.merge())
                }

                accounts.forEach { acc ->
                    val docRef = db.collection("households").document(code)
                        .collection("accounts").document("acc_${acc.id}")
                    val map = mapOf(
                        "id" to acc.id.toString(),
                        "name" to acc.name,
                        "holder" to acc.holder,
                        "accountType" to acc.accountType,
                        "balance" to acc.balance,
                        "accountNumberMask" to acc.accountNumberMask,
                        "syncUserId" to user.uid
                    )
                    batch.set(docRef, map, SetOptions.merge())
                }

                batch.commit().await()
                _syncState.value = SyncState.Live(code)
            } catch (e: Exception) {
                Log.e("CloudSync", "Sync failed", e)
                _syncState.value = SyncState.Error("Sync error: ${e.localizedMessage}")
            }
        }
    }
}
