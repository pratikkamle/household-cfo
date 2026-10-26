package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class CloudSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authAccount = MutableStateFlow<AuthAccount?>(null)
    val authAccount: StateFlow<AuthAccount?> = _authAccount.asStateFlow()

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var activeHouseholdCode: String? = null

    init {
        auth.addAuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                _authAccount.value = AuthAccount(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                    photoUrl = user.photoUrl?.toString()
                )
            } else {
                _authAccount.value = null
            }
        }
    }

    fun attemptSilentSignIn(scope: CoroutineScope) {
        val user = auth.currentUser
        if (user != null) {
            _currentUser.value = user
            _authAccount.value = AuthAccount(
                uid = user.uid,
                email = user.email ?: "",
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "User",
                photoUrl = user.photoUrl?.toString()
            )
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    _currentUser.value = user
                    _authAccount.value = AuthAccount(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: email.substringBefore("@"),
                        photoUrl = null
                    )
                }
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Sign in failed")
            }
    }

    fun registerWithEmail(name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    _currentUser.value = user
                    _authAccount.value = AuthAccount(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = name.trim().ifBlank { email.substringBefore("@") },
                        photoUrl = null
                    )
                }
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Registration failed")
            }
    }

    fun signInWithGoogle(activity: Activity) {
        val credentialManager = CredentialManager.create(activity)
        val serverClientId = "690696722293-l1dm6brg6hf9474ntaiu8q5e6sbfqv08.apps.googleusercontent.com"

        val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(authCredential).await()
                }
            } catch (e: GetCredentialException) {
                Log.w("CloudSync", "Google Sign-In canceled or failed: ${e.message}")
            } catch (e: Exception) {
                Log.e("CloudSync", "Sign-in error: ${e.message}")
            }
        }
    }

    fun continueAsOfflineAdmin(name: String, email: String) {
        val cleanName = name.trim().ifBlank { "Pratik" }
        val cleanEmail = email.trim().ifBlank { "admin@householdcfo.local" }
        _authAccount.value = AuthAccount(
            uid = "offline_" + cleanName.lowercase().replace(" ", "_"),
            email = cleanEmail,
            displayName = cleanName
        )
    }

    fun signOut(scope: CoroutineScope) {
        stopRealtimeSync()
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w("CloudSync", "Sign out exception: ${e.message}")
        }
        _currentUser.value = null
        _authAccount.value = null
    }

    fun startRealtimeSync(inviteCode: String, scope: CoroutineScope) {
        val cleanCode = inviteCode.trim().uppercase()
        if (cleanCode.isBlank() || activeHouseholdCode == cleanCode) return

        stopRealtimeSync()
        activeHouseholdCode = cleanCode
        _syncState.value = _syncState.value.copy(isSyncing = true, isOnline = true, error = null)

        try {
            // 1. Household Document Listener
            val hSub = firestore.collection("households").document(cleanCode)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        try {
                            val name = snapshot.getString("name") ?: "Household"
                            val adminName = snapshot.getString("adminName") ?: "Admin"
                            val adminEmail = snapshot.getString("adminEmail") ?: ""
                            val budget = snapshot.getDouble("monthlyDiscretionaryBudget") ?: 50000.0
                            val pin = snapshot.getString("securityPin") ?: "1234"
                            val existing = database.householdDao().getPrimaryHousehold()
                            val updated = existing?.copy(
                                name = name,
                                inviteCode = cleanCode,
                                adminName = adminName,
                                adminEmail = adminEmail,
                                monthlyDiscretionaryBudget = budget,
                                securityPin = pin
                            ) ?: HouseholdEntity(
                                id = 1,
                                name = name,
                                inviteCode = cleanCode,
                                adminName = adminName,
                                adminEmail = adminEmail,
                                monthlyDiscretionaryBudget = budget,
                                securityPin = pin
                            )
                            database.householdDao().insertHousehold(updated)
                        } catch (e: Exception) {
                            Log.w("CloudSync", "Error updating household from cloud: ${e.message}")
                        }
                    }
                }
            activeListeners.add(hSub)

            // 2. Members Subcollection Listener (SAFE MERGE - NEVER WIPE LOCAL MEMBERS)
            val mSub = firestore.collection("households").document(cleanCode)
                .collection("members")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        try {
                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val data = doc.data
                                val name = data["name"] as? String ?: continue
                                val email = (data["email"] as? String)?.trim() ?: ""
                                val role = data["role"] as? String ?: "MEMBER"
                                val colorHex = data["colorHex"] as? String ?: "#2563EB"
                                val initial = data["initial"] as? String ?: name.take(1).uppercase()
                                val photoUrl = data["photoUrl"] as? String
                                val uid = data["uid"] as? String ?: doc.id

                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val existing = if (email.isNotBlank()) {
                                            database.userDao().getUserByEmail(email)
                                        } else {
                                            database.userDao().getUserByName(name)
                                        }
                                        if (existing != null) {
                                            val updated = existing.copy(
                                                name = name,
                                                email = if (email.isNotBlank()) email else existing.email,
                                                role = role,
                                                avatarInitial = initial,
                                                colorHex = colorHex,
                                                photoUrl = photoUrl ?: existing.photoUrl,
                                                firebaseUid = uid.ifBlank { existing.firebaseUid }
                                            )
                                            database.userDao().updateUser(updated)
                                        } else {
                                            val newUser = UserEntity(
                                                id = 0,
                                                householdId = 1,
                                                name = name,
                                                email = email,
                                                role = role,
                                                avatarInitial = initial,
                                                colorHex = colorHex,
                                                photoUrl = photoUrl,
                                                firebaseUid = uid
                                            )
                                            database.userDao().insertUser(newUser)
                                        }
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        // Only delete if specifically removed on remote
                                        database.userDao().deleteUserByMatching(name, email, uid)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("CloudSync", "Error updating members from cloud: ${e.message}")
                        }
                    }
                }
            activeListeners.add(mSub)

            // 3. Transactions Subcollection Listener
            val tSub = firestore.collection("households").document(cleanCode)
                .collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        try {
                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val idLong = doc.id.removePrefix("tx_").toLongOrNull() ?: continue
                                val data = doc.data
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val tx = TransactionEntity(
                                            id = idLong,
                                            householdId = 1,
                                            userId = (data["userId"] as? Number)?.toLong(),
                                            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                                            category = data["category"] as? String ?: "General",
                                            paidBy = data["paidBy"] as? String ?: "Admin",
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
                                    DocumentChange.Type.REMOVED -> {
                                        database.transactionDao().deleteTransactionById(idLong)
                                    }
                                }
                            }
                            _syncState.value = _syncState.value.copy(isSyncing = false, lastSyncTime = System.currentTimeMillis())
                        } catch (e: Exception) {
                            Log.w("CloudSync", "Error updating transactions from cloud: ${e.message}")
                        }
                    }
                }
            activeListeners.add(tSub)

            // 4. Category Budgets Listener
            val bSub = firestore.collection("households").document(cleanCode)
                .collection("category_budgets")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        try {
                            for (doc in snapshot.documents) {
                                val catName = doc.getString("category") ?: continue
                                val budget = doc.getDouble("monthlyBudget") ?: continue
                                val isEssential = doc.getBoolean("isEssential") ?: false
                                val colorHex = doc.getString("colorHex") ?: "#2563EB"
                                val existingList = database.budgetDao().getAllBudgets()
                                val match = existingList.firstOrNull { it.category.equals(catName, ignoreCase = true) }
                                if (match != null) {
                                    database.budgetDao().insertBudget(match.copy(monthlyBudget = budget, isEssential = isEssential, colorHex = colorHex))
                                } else {
                                    database.budgetDao().insertBudget(
                                        CategoryBudgetEntity(householdId = 1, category = catName, monthlyBudget = budget, isEssential = isEssential, colorHex = colorHex)
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("CloudSync", "Error updating budgets from cloud: ${e.message}")
                        }
                    }
                }
            activeListeners.add(bSub)

            // 5. Accounts Listener
            val aSub = firestore.collection("households").document(cleanCode)
                .collection("accounts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        try {
                            for (change in snapshot.documentChanges) {
                                val doc = change.document
                                val accId = doc.id.removePrefix("acc_").toLongOrNull() ?: continue
                                val data = doc.data
                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val acc = AccountEntity(
                                            id = accId,
                                            householdId = 1,
                                            name = data["name"] as? String ?: "Account",
                                            type = data["type"] as? String ?: "BANK",
                                            balance = (data["balance"] as? Number)?.toDouble() ?: 0.0,
                                            creditLimit = (data["creditLimit"] as? Number)?.toDouble() ?: 0.0,
                                            billingCycleDay = (data["billingCycleDay"] as? Number)?.toInt() ?: 1,
                                            paymentDueDay = (data["paymentDueDay"] as? Number)?.toInt() ?: 20,
                                            bankName = data["bankName"] as? String ?: "",
                                            accountNumberLast4 = data["accountNumberLast4"] as? String ?: "",
                                            colorHex = data["colorHex"] as? String ?: "#2563EB",
                                            isPrimary = data["isPrimary"] as? Boolean ?: false,
                                            notes = data["notes"] as? String ?: ""
                                        )
                                        database.accountDao().insertAccount(acc)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        database.accountDao().deleteAccount(accId)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("CloudSync", "Error updating accounts from cloud: ${e.message}")
                        }
                    }
                }
            activeListeners.add(aSub)

        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to start realtime sync: ${e.message}", e)
            _syncState.value = _syncState.value.copy(isSyncing = false, error = e.message)
        }
    }

    fun stopRealtimeSync() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        activeHouseholdCode = null
    }

    suspend fun findHouseholdForUser(uid: String?, email: String?): String? = withContext(Dispatchers.IO) {
        try {
            if (!uid.isNullOrBlank()) {
                val doc = firestore.collection("user_households").document(uid).get().await()
                if (doc.exists()) {
                    val code = doc.getString("householdCode")
                    if (!code.isNullOrBlank()) return@withContext code
                }
            }
            if (!email.isNullOrBlank()) {
                val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                val doc = firestore.collection("user_households").document(cleanEmail).get().await()
                if (doc.exists()) {
                    val code = doc.getString("householdCode")
                    if (!code.isNullOrBlank()) return@withContext code
                }
                // Try admin query
                val query = firestore.collection("households")
                    .whereEqualTo("adminEmail", email.trim())
                    .limit(1)
                    .get().await()
                if (!query.isEmpty) {
                    val code = query.documents.first().getString("inviteCode")
                    if (!code.isNullOrBlank()) return@withContext code
                }
            }
        } catch (e: Exception) {
            Log.w("CloudSync", "findHouseholdForUser warning: ${e.message}")
        }
        null
    }

    fun joinHouseholdByCode(
        inviteCode: String,
        memberName: String,
        memberEmail: String = "",
        scope: CoroutineScope,
        onResult: (Boolean, String?) -> Unit
    ) {
        val cleanCode = inviteCode.trim().uppercase()
        scope.launch(Dispatchers.IO) {
            try {
                val doc = firestore.collection("households").document(cleanCode).get().await()
                if (!doc.exists()) {
                    withContext(Dispatchers.Main) { onResult(false, "Household code not found") }
                    return@launch
                }
                val householdName = doc.getString("name") ?: "Household"
                val adminName = doc.getString("adminName") ?: "Admin"
                val adminEmail = doc.getString("adminEmail") ?: ""
                val budget = doc.getDouble("monthlyDiscretionaryBudget") ?: 50000.0
                val pin = doc.getString("securityPin") ?: "1234"

                val localHousehold = HouseholdEntity(
                    id = 1,
                    name = householdName,
                    inviteCode = cleanCode,
                    adminName = adminName,
                    adminEmail = adminEmail,
                    monthlyDiscretionaryBudget = budget,
                    securityPin = pin
                )
                database.householdDao().insertHousehold(localHousehold)

                // Add or match member locally
                val existingUser = if (memberEmail.isNotBlank()) {
                    database.userDao().getUserByEmail(memberEmail)
                } else {
                    database.userDao().getUserByName(memberName)
                }
                val memberId = existingUser?.id ?: database.userDao().insertUser(
                    UserEntity(
                        id = 0,
                        householdId = 1,
                        name = memberName,
                        email = memberEmail,
                        role = "MEMBER",
                        avatarInitial = memberName.take(1).uppercase(),
                        colorHex = "#D97706"
                    )
                )

                // Push member to Cloud
                val memberUser = database.userDao().getUserByIdSync(memberId)
                if (memberUser != null) {
                    pushMember(memberUser, cleanCode, scope)
                }

                // Link user to household in cloud
                linkUserToHousehold(memberEmail, cleanCode)

                startRealtimeSync(cleanCode, scope)
                withContext(Dispatchers.Main) { onResult(true, null) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Failed to join") }
            }
        }
    }

    private suspend fun linkUserToHousehold(email: String, code: String) {
        try {
            val user = auth.currentUser
            if (user != null) {
                firestore.collection("user_households").document(user.uid)
                    .set(mapOf("householdCode" to code, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            }
            if (email.isNotBlank()) {
                val cleanEmail = email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                firestore.collection("user_households").document(cleanEmail)
                    .set(mapOf("householdCode" to code, "updatedAt" to System.currentTimeMillis()), SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.w("CloudSync", "linkUserToHousehold warning: ${e.message}")
        }
    }

    fun publishHousehold(household: HouseholdEntity, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val code = household.inviteCode.trim().uppercase()
                val map = mapOf(
                    "name" to household.name,
                    "inviteCode" to code,
                    "adminName" to household.adminName,
                    "adminEmail" to household.adminEmail,
                    "monthlyDiscretionaryBudget" to household.monthlyDiscretionaryBudget,
                    "securityPin" to household.securityPin,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("households").document(code).set(map, SetOptions.merge()).await()
                linkUserToHousehold(household.adminEmail, code)
                startRealtimeSync(code, scope)
            } catch (e: Exception) {
                Log.w("CloudSync", "publishHousehold warning: ${e.message}")
            }
        }
    }

    fun pushMember(member: UserEntity, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val docId = if (member.email.isNotBlank()) {
                    "user_" + member.email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                } else {
                    "user_" + member.id
                }
                val map = mapOf(
                    "name" to member.name,
                    "email" to member.email,
                    "role" to member.role,
                    "colorHex" to member.colorHex,
                    "initial" to member.initial,
                    "photoUrl" to (member.photoUrl ?: ""),
                    "uid" to (member.firebaseUid ?: ""),
                    "joinedAt" to member.joinedAt
                )
                firestore.collection("households").document(code.uppercase())
                    .collection("members").document(docId)
                    .set(map, SetOptions.merge()).await()

                if (member.email.isNotBlank()) {
                    linkUserToHousehold(member.email, code)
                }
            } catch (e: Exception) {
                Log.w("CloudSync", "pushMember warning: ${e.message}")
            }
        }
    }

    fun pushTransaction(tx: TransactionEntity, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val docId = "tx_${tx.id}"
                val map = mapOf(
                    "userId" to (tx.userId ?: 0L),
                    "amount" to tx.amount,
                    "category" to tx.category,
                    "paidBy" to tx.paidBy,
                    "paymentMethod" to tx.paymentMethod,
                    "accountId" to (tx.accountId ?: 0L),
                    "toAccountId" to (tx.toAccountId ?: 0L),
                    "accountName" to tx.accountName,
                    "merchant" to tx.merchant,
                    "notes" to tx.notes,
                    "timestamp" to tx.timestamp,
                    "monthYear" to tx.monthYear,
                    "nature" to tx.nature,
                    "type" to tx.type,
                    "isRecurring" to tx.isRecurring,
                    "recurringRuleId" to (tx.recurringRuleId ?: 0L)
                )
                firestore.collection("households").document(code.uppercase())
                    .collection("transactions").document(docId)
                    .set(map, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("CloudSync", "pushTransaction warning: ${e.message}")
            }
        }
    }

    fun deleteTransaction(txId: Long, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("households").document(code.uppercase())
                    .collection("transactions").document("tx_$txId")
                    .delete().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "deleteTransaction warning: ${e.message}")
            }
        }
    }

    fun pushAccount(account: AccountEntity, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                val docId = "acc_${account.id}"
                val map = mapOf(
                    "name" to account.name,
                    "type" to account.type,
                    "balance" to account.balance,
                    "creditLimit" to account.creditLimit,
                    "billingCycleDay" to account.billingCycleDay,
                    "paymentDueDay" to account.paymentDueDay,
                    "bankName" to account.bankName,
                    "accountNumberLast4" to account.accountNumberLast4,
                    "colorHex" to account.colorHex,
                    "isPrimary" to account.isPrimary,
                    "notes" to account.notes
                )
                firestore.collection("households").document(code.uppercase())
                    .collection("accounts").document(docId)
                    .set(map, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("CloudSync", "pushAccount warning: ${e.message}")
            }
        }
    }

    fun deleteAccount(accountId: Long, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("households").document(code.uppercase())
                    .collection("accounts").document("acc_$accountId")
                    .delete().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "deleteAccount warning: ${e.message}")
            }
        }
    }

    fun pushBudgetPlan(monthlyBudget: Double, categoryBudgets: Map<String, Double>, code: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                firestore.collection("households").document(code.uppercase())
                    .set(mapOf("monthlyDiscretionaryBudget" to monthlyBudget), SetOptions.merge()).await()

                val batch = firestore.batch()
                categoryBudgets.forEach { (cat, amount) ->
                    val docRef = firestore.collection("households").document(code.uppercase())
                        .collection("category_budgets").document(cat)
                    batch.set(docRef, mapOf(
                        "category" to cat,
                        "monthlyBudget" to amount,
                        "colorHex" to CategoryConstants.getColorHex(cat)
                    ), SetOptions.merge())
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.w("CloudSync", "pushBudgetPlan warning: ${e.message}")
            }
        }
    }
}
