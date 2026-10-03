package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Account
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntry
import com.example.data.model.TrashItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LedgerRepository(private val context: Context) {
    private val tag = "LedgerRepository"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs = context.getSharedPreferences("cashbook_data_store", Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _activeAccountId = MutableStateFlow<String?>(null)
    val activeAccountId: StateFlow<String?> = _activeAccountId.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntry>>(emptyList())
    val transactions: StateFlow<List<TransactionEntry>> = _transactions.asStateFlow()

    private val _trash = MutableStateFlow<List<TrashItem>>(emptyList())
    val trash: StateFlow<List<TrashItem>> = _trash.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private var currentUserId: String? = null
    private var firestore: FirebaseFirestore? = null

    private var accountsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var trashListener: ListenerRegistration? = null

    init {
        loadFromLocalStorage()
        initFirestore()
    }

    private fun initFirestore() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firestore = FirebaseFirestore.getInstance()
                Log.d(tag, "Firestore successfully obtained")
            } else {
                Log.w(tag, "FirebaseApp not configured; operating in offline local mode")
                _syncStatus.value = SyncStatus.OFFLINE
            }
        } catch (e: Exception) {
            Log.e(tag, "Firestore initialization error: ${e.message}", e)
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    fun onUserChanged(userId: String?) {
        if (currentUserId == userId) return
        currentUserId = userId

        // Detach previous listeners
        accountsListener?.remove()
        transactionsListener?.remove()
        trashListener?.remove()

        if (userId.isNullOrBlank()) {
            loadFromLocalStorage()
            return
        }

        // Load cached data for this user
        loadFromLocalStorage()

        // Attach Firestore real-time listeners
        attachFirestoreListeners(userId)
    }

    private fun attachFirestoreListeners(userId: String) {
        val db = firestore ?: run {
            _syncStatus.value = SyncStatus.OFFLINE
            return
        }

        _syncStatus.value = SyncStatus.SYNCING

        try {
            val userDoc = db.collection("users").document(userId)

            // Listen to Accounts
            accountsListener = userDoc.collection("accounts")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Accounts listener error: ${error.message}")
                        _syncStatus.value = SyncStatus.ERROR
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val accs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { Account.fromMap(doc.id, it) }
                        }
                        if (accs.isNotEmpty()) {
                            _accounts.value = accs
                            if (_activeAccountId.value == null || accs.none { it.id == _activeAccountId.value }) {
                                _activeAccountId.value = accs.first().id
                            }
                            saveToLocalStorage()
                        } else if (_accounts.value.isEmpty()) {
                            // Seed default account
                            createDefaultAccount()
                        }
                        _syncStatus.value = SyncStatus.SYNCED
                    }
                }

            // Listen to Transactions
            transactionsListener = userDoc.collection("transactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Transactions listener error: ${error.message}")
                        _syncStatus.value = SyncStatus.ERROR
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val txs = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { TransactionEntry.fromMap(doc.id, it) }
                        }
                        _transactions.value = txs
                        saveToLocalStorage()
                        _syncStatus.value = SyncStatus.SYNCED
                    }
                }

            // Listen to Trash
            trashListener = userDoc.collection("trash")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(tag, "Trash listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val tr = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { TrashItem.fromMap(doc.id, it) }
                        }
                        _trash.value = tr
                        saveToLocalStorage()
                    }
                }

        } catch (e: Exception) {
            Log.e(tag, "Failed to attach Firestore listeners: ${e.message}", e)
            _syncStatus.value = SyncStatus.OFFLINE
        }
    }

    private fun createDefaultAccount() {
        val def = Account(
            id = "acc_" + UUID.randomUUID().toString().take(8),
            name = "Main Cash",
            icon = "💵",
            createdAt = System.currentTimeMillis()
        )
        _accounts.value = listOf(def)
        _activeAccountId.value = def.id
        saveAccount(def)
    }

    fun selectAccount(accountId: String) {
        _activeAccountId.value = accountId
        prefs.edit().putString("active_acc_id", accountId).apply()
    }

    fun saveAccount(account: Account) {
        val current = _accounts.value.toMutableList()
        val index = current.indexOfFirst { it.id == account.id }
        if (index >= 0) {
            current[index] = account
        } else {
            current.add(account)
        }
        _accounts.value = current
        if (_activeAccountId.value == null) {
            _activeAccountId.value = account.id
        }
        saveToLocalStorage()

        // Sync to Firestore
        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            _syncStatus.value = SyncStatus.SYNCING
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("accounts").document(account.id)
                        .set(account.toMap())
                    _syncStatus.value = SyncStatus.SYNCED
                } catch (e: Exception) {
                    Log.e(tag, "Error saving account to Firestore: ${e.message}")
                    _syncStatus.value = SyncStatus.ERROR
                }
            }
        }
    }

    fun deleteAccount(accountId: String) {
        val target = _accounts.value.find { it.id == accountId } ?: return
        val current = _accounts.value.filterNot { it.id == accountId }
        _accounts.value = current
        if (_activeAccountId.value == accountId) {
            _activeAccountId.value = current.firstOrNull()?.id
        }

        // Delete associated transactions & add account to trash
        val toTrash = TrashItem(
            id = "tr_" + UUID.randomUUID().toString().take(8),
            originalId = target.id,
            itemType = "account",
            title = target.name,
            details = target.icon,
            deletedAt = System.currentTimeMillis()
        )
        moveToTrash(toTrash)

        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            _syncStatus.value = SyncStatus.SYNCING
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("accounts").document(accountId)
                        .delete()
                    _syncStatus.value = SyncStatus.SYNCED
                } catch (e: Exception) {
                    Log.e(tag, "Error deleting account in Firestore", e)
                }
            }
        }
    }

    fun addOrUpdateTransaction(entry: TransactionEntry) {
        val current = _transactions.value.toMutableList()
        val index = current.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            current[index] = entry
        } else {
            current.add(0, entry)
        }
        _transactions.value = current
        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            _syncStatus.value = SyncStatus.SYNCING
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("transactions").document(entry.id)
                        .set(entry.toMap())
                    _syncStatus.value = SyncStatus.SYNCED
                } catch (e: Exception) {
                    Log.e(tag, "Error saving transaction to Firestore", e)
                    _syncStatus.value = SyncStatus.ERROR
                }
            }
        }
    }

    fun moveTransaction(entryId: String, targetAccountId: String) {
        val entry = _transactions.value.find { it.id == entryId } ?: return
        val updated = entry.copy(accountId = targetAccountId)
        addOrUpdateTransaction(updated)
    }

    fun deleteTransaction(entryId: String) {
        val entry = _transactions.value.find { it.id == entryId } ?: return
        val current = _transactions.value.filterNot { it.id == entryId }
        _transactions.value = current

        val trashItem = TrashItem(
            id = "tr_" + UUID.randomUUID().toString().take(8),
            originalId = entry.id,
            itemType = "entry",
            title = entry.details.ifBlank { if (entry.type == "in") "Cash In" else "Cash Out" },
            amount = entry.amount,
            details = entry.details,
            accountId = entry.accountId,
            entryType = entry.type,
            deletedAt = System.currentTimeMillis()
        )
        moveToTrash(trashItem)

        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            _syncStatus.value = SyncStatus.SYNCING
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("transactions").document(entryId)
                        .delete()
                    _syncStatus.value = SyncStatus.SYNCED
                } catch (e: Exception) {
                    Log.e(tag, "Error deleting transaction in Firestore", e)
                }
            }
        }
    }

    private fun moveToTrash(trashItem: TrashItem) {
        val current = _trash.value.toMutableList()
        current.add(0, trashItem)
        _trash.value = current

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("trash").document(trashItem.id)
                        .set(trashItem.toMap())
                } catch (e: Exception) {
                    Log.e(tag, "Error saving to trash in Firestore", e)
                }
            }
        }
    }

    fun restoreTrashItem(trashId: String) {
        val item = _trash.value.find { it.id == trashId } ?: return
        val currentTrash = _trash.value.filterNot { it.id == trashId }
        _trash.value = currentTrash

        if (item.itemType == "entry") {
            val restored = TransactionEntry(
                id = item.originalId.ifBlank { UUID.randomUUID().toString() },
                accountId = item.accountId.ifBlank { _activeAccountId.value ?: "" },
                type = item.entryType,
                amount = item.amount,
                details = item.details,
                timestamp = System.currentTimeMillis()
            )
            addOrUpdateTransaction(restored)
        } else if (item.itemType == "account") {
            val restoredAcc = Account(
                id = item.originalId.ifBlank { UUID.randomUUID().toString() },
                name = item.title,
                icon = item.details.ifBlank { "💵" },
                createdAt = System.currentTimeMillis()
            )
            saveAccount(restoredAcc)
        }

        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("trash").document(trashId)
                        .delete()
                } catch (e: Exception) {
                    Log.e(tag, "Error removing trash item in Firestore", e)
                }
            }
        }
    }

    fun permanentlyDeleteTrashItem(trashId: String) {
        _trash.value = _trash.value.filterNot { it.id == trashId }
        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            scope.launch {
                try {
                    db.collection("users").document(uid)
                        .collection("trash").document(trashId)
                        .delete()
                } catch (e: Exception) {
                    Log.e(tag, "Error deleting trash item permanently", e)
                }
            }
        }
    }

    fun emptyTrash() {
        val allTrash = _trash.value
        _trash.value = emptyList()
        saveToLocalStorage()

        val uid = currentUserId
        val db = firestore
        if (uid != null && db != null) {
            scope.launch {
                try {
                    for (t in allTrash) {
                        db.collection("users").document(uid)
                            .collection("trash").document(t.id)
                            .delete()
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error emptying trash in Firestore", e)
                }
            }
        }
    }

    fun syncNow() {
        val uid = currentUserId ?: return
        attachFirestoreListeners(uid)
    }

    private fun saveToLocalStorage() {
        try {
            val accsJson = JSONArray()
            _accounts.value.forEach { a ->
                val o = JSONObject()
                o.put("id", a.id)
                o.put("name", a.name)
                o.put("icon", a.icon)
                o.put("createdAt", a.createdAt)
                accsJson.put(o)
            }

            val txsJson = JSONArray()
            _transactions.value.forEach { t ->
                val o = JSONObject()
                o.put("id", t.id)
                o.put("accountId", t.accountId)
                o.put("type", t.type)
                o.put("amount", t.amount)
                o.put("details", t.details)
                o.put("timestamp", t.timestamp)
                txsJson.put(o)
            }

            val trashJson = JSONArray()
            _trash.value.forEach { tr ->
                val o = JSONObject()
                o.put("id", tr.id)
                o.put("originalId", tr.originalId)
                o.put("itemType", tr.itemType)
                o.put("title", tr.title)
                o.put("amount", tr.amount)
                o.put("details", tr.details)
                o.put("accountId", tr.accountId)
                o.put("entryType", tr.entryType)
                o.put("deletedAt", tr.deletedAt)
                trashJson.put(o)
            }

            prefs.edit()
                .putString("accounts_json", accsJson.toString())
                .putString("transactions_json", txsJson.toString())
                .putString("trash_json", trashJson.toString())
                .putString("active_acc_id", _activeAccountId.value)
                .apply()
        } catch (e: Exception) {
            Log.e(tag, "Failed to save local data: ${e.message}")
        }
    }

    private fun loadFromLocalStorage() {
        try {
            val accsStr = prefs.getString("accounts_json", null)
            val txsStr = prefs.getString("transactions_json", null)
            val trStr = prefs.getString("trash_json", null)
            val activeAcc = prefs.getString("active_acc_id", null)

            val accList = mutableListOf<Account>()
            if (!accsStr.isNullOrBlank()) {
                val jArray = JSONArray(accsStr)
                for (i in 0 until jArray.length()) {
                    val o = jArray.getJSONObject(i)
                    accList.add(
                        Account(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            icon = o.optString("icon", "💵"),
                            createdAt = o.optLong("createdAt")
                        )
                    )
                }
            }

            if (accList.isEmpty()) {
                val def = Account(
                    id = "acc_main",
                    name = "Main Cash",
                    icon = "💵",
                    createdAt = System.currentTimeMillis()
                )
                accList.add(def)
            }
            _accounts.value = accList
            _activeAccountId.value = if (activeAcc != null && accList.any { it.id == activeAcc }) activeAcc else accList.first().id

            val txList = mutableListOf<TransactionEntry>()
            if (!txsStr.isNullOrBlank()) {
                val jArray = JSONArray(txsStr)
                for (i in 0 until jArray.length()) {
                    val o = jArray.getJSONObject(i)
                    txList.add(
                        TransactionEntry(
                            id = o.optString("id"),
                            accountId = o.optString("accountId"),
                            type = o.optString("type", "in"),
                            amount = o.optDouble("amount", 0.0),
                            details = o.optString("details", ""),
                            timestamp = o.optLong("timestamp")
                        )
                    )
                }
            }
            _transactions.value = txList

            val trList = mutableListOf<TrashItem>()
            if (!trStr.isNullOrBlank()) {
                val jArray = JSONArray(trStr)
                for (i in 0 until jArray.length()) {
                    val o = jArray.getJSONObject(i)
                    trList.add(
                        TrashItem(
                            id = o.optString("id"),
                            originalId = o.optString("originalId"),
                            itemType = o.optString("itemType", "entry"),
                            title = o.optString("title", ""),
                            amount = o.optDouble("amount", 0.0),
                            details = o.optString("details", ""),
                            accountId = o.optString("accountId", ""),
                            entryType = o.optString("entryType", "in"),
                            deletedAt = o.optLong("deletedAt")
                        )
                    )
                }
            }
            _trash.value = trList

        } catch (e: Exception) {
            Log.e(tag, "Failed to load local data: ${e.message}")
            createDefaultAccount()
        }
    }
}
