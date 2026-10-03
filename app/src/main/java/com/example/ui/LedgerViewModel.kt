package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Account
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntry
import com.example.data.model.TrashItem
import com.example.data.model.UserSession
import com.example.data.repository.AuthRepository
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

data class DailyFlow(
    val dayLabel: String,
    val dateString: String,
    val totalIn: Double,
    val totalOut: Double
)

data class LedgerFilters(
    val scope: String = "current",
    val type: String = "all",
    val dateFilter: DateFilter = DateFilter.ALL,
    val searchQuery: String = ""
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    val authRepo = AuthRepository(application)
    val ledgerRepo = LedgerRepository(application)

    val currentUser: StateFlow<UserSession?> = authRepo.currentUser
    val authError: StateFlow<String?> = authRepo.authError
    val isAuthLoading: StateFlow<Boolean> = authRepo.isLoading

    val accounts: StateFlow<List<Account>> = ledgerRepo.accounts
    val activeAccountId: StateFlow<String?> = ledgerRepo.activeAccountId
    val transactions: StateFlow<List<TransactionEntry>> = ledgerRepo.transactions
    val trash: StateFlow<List<TrashItem>> = ledgerRepo.trash
    val syncStatus: StateFlow<SyncStatus> = ledgerRepo.syncStatus

    // Filters
    private val _scopeFilter = MutableStateFlow("current") // "current" or "all"
    val scopeFilter: StateFlow<String> = _scopeFilter.asStateFlow()

    private val _typeFilter = MutableStateFlow("all") // "all", "in", "out"
    val typeFilter: StateFlow<String> = _typeFilter.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilter.ALL)
    val dateFilter: StateFlow<DateFilter> = _dateFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val filterState = combine(
        _scopeFilter,
        _typeFilter,
        _dateFilter,
        _searchQuery
    ) { sc, tp, df, sq ->
        LedgerFilters(sc, tp, df, sq)
    }

    init {
        // Wire user changes to ledger repo
        viewModelScope.launch {
            currentUser.collect { user ->
                ledgerRepo.onUserChanged(user?.uid)
            }
        }
    }

    val activeAccount: StateFlow<Account?> = combine(accounts, activeAccountId) { accs: List<Account>, id: String? ->
        accs.find { it.id == id } ?: accs.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val filteredTransactions: StateFlow<List<TransactionEntry>> = combine(
        transactions,
        activeAccountId,
        filterState
    ) { txs: List<TransactionEntry>, activeAcc: String?, f: LedgerFilters ->
        var list = if (f.scope == "all") txs else txs.filter { it.accountId == activeAcc }

        if (f.type != "all") {
            list = list.filter { it.type == f.type }
        }

        val now = Calendar.getInstance()
        when (f.dateFilter) {
            DateFilter.TODAY -> {
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                val startOfDay = now.timeInMillis
                list = list.filter { it.timestamp >= startOfDay }
            }
            DateFilter.THIS_WEEK -> {
                now.set(Calendar.DAY_OF_WEEK, now.firstDayOfWeek)
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                val startOfWeek = now.timeInMillis
                list = list.filter { it.timestamp >= startOfWeek }
            }
            DateFilter.THIS_MONTH -> {
                now.set(Calendar.DAY_OF_MONTH, 1)
                now.set(Calendar.HOUR_OF_DAY, 0)
                now.set(Calendar.MINUTE, 0)
                now.set(Calendar.SECOND, 0)
                now.set(Calendar.MILLISECOND, 0)
                val startOfMonth = now.timeInMillis
                list = list.filter { it.timestamp >= startOfMonth }
            }
            DateFilter.ALL -> { /* no-op */ }
        }

        if (f.searchQuery.isNotBlank()) {
            val q = f.searchQuery.trim().lowercase(Locale.ROOT)
            list = list.filter {
                it.details.lowercase(Locale.ROOT).contains(q) ||
                it.amount.toString().contains(q)
            }
        }

        list.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalIn: StateFlow<Double> = filteredTransactions.combine(transactions) { filtered: List<TransactionEntry>, _ ->
        filtered.filter { it.type == "in" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalOut: StateFlow<Double> = filteredTransactions.combine(transactions) { filtered: List<TransactionEntry>, _ ->
        filtered.filter { it.type == "out" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val netBalance: StateFlow<Double> = combine(totalIn, totalOut) { inAmt: Double, outAmt: Double ->
        inAmt - outAmt
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val suggestions: StateFlow<List<String>> = transactions.combine(activeAccountId) { txs: List<TransactionEntry>, _ ->
        txs.mapNotNull { it.details.trim().ifEmpty { null } }
            .distinct()
            .take(15)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val last7DaysFlow: StateFlow<List<DailyFlow>> = transactions.combine(activeAccountId) { txs: List<TransactionEntry>, _ ->
        val list = mutableListOf<DailyFlow>()
        val cal = Calendar.getInstance()
        val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val d = Calendar.getInstance()
            d.add(Calendar.DAY_OF_YEAR, -i)
            val dayKey = dateFmt.format(d.time)
            val label = dayFmt.format(d.time)

            val dayTxs = txs.filter {
                dateFmt.format(Date(it.timestamp)) == dayKey
            }
            val inSum = dayTxs.filter { it.type == "in" }.sumOf { it.amount }
            val outSum = dayTxs.filter { it.type == "out" }.sumOf { it.amount }

            list.add(DailyFlow(dayLabel = label, dateString = dayKey, totalIn = inSum, totalOut = outSum))
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Actions
    fun setScopeFilter(scope: String) { _scopeFilter.value = scope }
    fun setTypeFilter(type: String) { _typeFilter.value = type }
    fun setDateFilter(filter: DateFilter) { _dateFilter.value = filter }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun clearFilters() {
        _scopeFilter.value = "current"
        _typeFilter.value = "all"
        _dateFilter.value = DateFilter.ALL
        _searchQuery.value = ""
    }

    fun selectAccount(accountId: String) {
        ledgerRepo.selectAccount(accountId)
    }

    fun addTransaction(
        type: String,
        amount: Double,
        details: String,
        timestamp: Long = System.currentTimeMillis(),
        targetAccountId: String? = null
    ) {
        val accId = targetAccountId ?: activeAccountId.value ?: accounts.value.firstOrNull()?.id ?: "acc_main"
        val entry = TransactionEntry(
            id = "tx_" + UUID.randomUUID().toString().take(10),
            accountId = accId,
            type = type,
            amount = amount,
            details = details.trim(),
            timestamp = timestamp
        )
        ledgerRepo.addOrUpdateTransaction(entry)
    }

    fun updateTransaction(
        id: String,
        type: String,
        amount: Double,
        details: String,
        timestamp: Long,
        accountId: String
    ) {
        val entry = TransactionEntry(
            id = id,
            accountId = accountId,
            type = type,
            amount = amount,
            details = details.trim(),
            timestamp = timestamp
        )
        ledgerRepo.addOrUpdateTransaction(entry)
    }

    fun moveTransaction(entryId: String, targetAccountId: String) {
        ledgerRepo.moveTransaction(entryId, targetAccountId)
    }

    fun deleteTransaction(entryId: String) {
        ledgerRepo.deleteTransaction(entryId)
    }

    fun restoreTrashItem(trashId: String) {
        ledgerRepo.restoreTrashItem(trashId)
    }

    fun permanentlyDeleteTrashItem(trashId: String) {
        ledgerRepo.permanentlyDeleteTrashItem(trashId)
    }

    fun emptyTrash() {
        ledgerRepo.emptyTrash()
    }

    fun createAccount(name: String, icon: String = "💵") {
        val acc = Account(
            id = "acc_" + UUID.randomUUID().toString().take(8),
            name = name.trim().ifBlank { "Account" },
            icon = icon,
            createdAt = System.currentTimeMillis()
        )
        ledgerRepo.saveAccount(acc)
    }

    fun updateAccount(id: String, name: String, icon: String) {
        val acc = Account(
            id = id,
            name = name.trim(),
            icon = icon,
            createdAt = System.currentTimeMillis()
        )
        ledgerRepo.saveAccount(acc)
    }

    fun deleteAccount(accountId: String) {
        ledgerRepo.deleteAccount(accountId)
    }

    fun syncNow() {
        ledgerRepo.syncNow()
    }

    // Auth actions
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            authRepo.signInWithGoogle(context)
        }
    }

    fun signInWithEmail(email: String, pass: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = authRepo.signInWithEmail(email, pass)
            onDone(ok)
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = authRepo.signUpWithEmail(email, pass, name)
            onDone(ok)
        }
    }

    fun signInAsGuest() {
        authRepo.signInAsGuest()
    }

    fun signOut() {
        authRepo.signOut()
    }

    fun clearAuthError() {
        authRepo.clearError()
    }
}
