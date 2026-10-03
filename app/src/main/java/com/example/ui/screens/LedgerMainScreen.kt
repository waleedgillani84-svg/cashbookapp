package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntry
import com.example.ui.DateFilter
import com.example.ui.LedgerViewModel
import com.example.ui.components.BalanceCards
import com.example.ui.components.BottomActionButtons
import com.example.ui.components.LedgerNavDrawer
import com.example.ui.components.LedgerTopBar
import com.example.ui.components.TransactionItemCard
import com.example.ui.dialogs.AccountManagementDialog
import com.example.ui.dialogs.AddEditTransactionSheet
import com.example.ui.dialogs.AnalyticsDialog
import com.example.ui.dialogs.AuthDialog
import com.example.ui.dialogs.FilterSearchDialog
import com.example.ui.dialogs.MoveTransactionDialog
import com.example.ui.dialogs.StatementExportDialog
import com.example.ui.dialogs.TrashSheet
import kotlinx.coroutines.launch

@Composable
fun LedgerMainScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val currentUser by viewModel.currentUser.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val activeAccountId by viewModel.activeAccountId.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val totalIn by viewModel.totalIn.collectAsState()
    val totalOut by viewModel.totalOut.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    val trashItems by viewModel.trash.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val dailyFlows by viewModel.last7DaysFlow.collectAsState()

    val scopeFilter by viewModel.scopeFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    // Dialog & Sheet States
    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TransactionEntry?>(null) }
    var initialSheetType by remember { mutableStateOf("in") }

    var showAccountDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showStatementDialog by remember { mutableStateOf(false) }
    var showTrashSheet by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var moveCandidateEntry by remember { mutableStateOf<TransactionEntry?>(null) }

    val hasActiveFilter = scopeFilter != "current" || typeFilter != "all" ||
            dateFilter != DateFilter.ALL || searchQuery.isNotBlank()

    // Handle back press if drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            LedgerNavDrawer(
                currentUser = currentUser,
                syncStatus = syncStatus,
                onAnalyticsClick = {
                    scope.launch { drawerState.close() }
                    showAnalyticsDialog = true
                },
                onAccountsClick = {
                    scope.launch { drawerState.close() }
                    showAccountDialog = true
                },
                onStatementClick = {
                    scope.launch { drawerState.close() }
                    showStatementDialog = true
                },
                onTrashClick = {
                    scope.launch { drawerState.close() }
                    showTrashSheet = true
                },
                onSyncNowClick = {
                    viewModel.syncNow()
                    Toast.makeText(context, "Syncing data with cloud...", Toast.LENGTH_SHORT).show()
                },
                onAuthClick = {
                    scope.launch { drawerState.close() }
                    showAuthDialog = true
                },
                onSignOutClick = {
                    scope.launch { drawerState.close() }
                    viewModel.signOut()
                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                LedgerTopBar(
                    activeAccount = activeAccount,
                    syncStatus = syncStatus,
                    hasActiveFilter = hasActiveFilter,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAccountClick = { showAccountDialog = true },
                    onFilterClick = { showFilterDialog = true },
                    onSyncClick = {
                        viewModel.syncNow()
                        Toast.makeText(context, "Cloud sync triggered", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                BottomActionButtons(
                    onCashInClick = {
                        editingEntry = null
                        initialSheetType = "in"
                        showAddEditSheet = true
                    },
                    onCashOutClick = {
                        editingEntry = null
                        initialSheetType = "out"
                        showAddEditSheet = true
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Balance Cards (Net, In, Out)
                BalanceCards(
                    netBalance = netBalance,
                    totalIn = totalIn,
                    totalOut = totalOut
                )

                // Statement Header & Filter Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📄 Statement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (hasActiveFilter) {
                            Spacer(modifier = Modifier.size(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Filtered",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "${filteredTransactions.size} entries",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Transactions List or Empty State
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "📭", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (hasActiveFilter) "No transactions match your filters"
                                else "No transactions found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hasActiveFilter) "Try adjusting or resetting your search & filters"
                                else "Tap + CASH IN or - CASH OUT at the bottom to add your first transaction.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = filteredTransactions,
                            key = { it.id }
                        ) { entry ->
                            TransactionItemCard(
                                entry = entry,
                                onEdit = {
                                    editingEntry = entry
                                    initialSheetType = entry.type
                                    showAddEditSheet = true
                                },
                                onMove = {
                                    moveCandidateEntry = entry
                                },
                                onDelete = {
                                    viewModel.deleteTransaction(entry.id)
                                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Transaction Sheet
    if (showAddEditSheet) {
        AddEditTransactionSheet(
            isEditing = editingEntry != null,
            initialType = initialSheetType,
            editingEntry = editingEntry,
            accounts = accounts,
            activeAccountId = activeAccountId,
            suggestions = suggestions,
            onDismiss = {
                showAddEditSheet = false
                editingEntry = null
            },
            onSave = { type, amount, details, accountId ->
                if (editingEntry != null) {
                    viewModel.updateTransaction(
                        id = editingEntry!!.id,
                        type = type,
                        amount = amount,
                        details = details,
                        timestamp = editingEntry!!.timestamp,
                        accountId = accountId
                    )
                    Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addTransaction(
                        type = type,
                        amount = amount,
                        details = details,
                        targetAccountId = accountId
                    )
                    Toast.makeText(
                        context,
                        if (type == "in") "+ Cash In recorded" else "- Cash Out recorded",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showAddEditSheet = false
                editingEntry = null
            }
        )
    }

    // Account Management Dialog
    if (showAccountDialog) {
        AccountManagementDialog(
            accounts = accounts,
            activeAccountId = activeAccountId,
            transactions = allTransactions,
            onSelectAccount = { viewModel.selectAccount(it) },
            onCreateAccount = { name, icon ->
                viewModel.createAccount(name, icon)
                Toast.makeText(context, "Account created", Toast.LENGTH_SHORT).show()
            },
            onEditAccount = { id, name, icon ->
                viewModel.updateAccount(id, name, icon)
                Toast.makeText(context, "Account updated", Toast.LENGTH_SHORT).show()
            },
            onDeleteAccount = {
                viewModel.deleteAccount(it)
                Toast.makeText(context, "Account moved to trash", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showAccountDialog = false }
        )
    }

    // Filter & Search Dialog
    if (showFilterDialog) {
        FilterSearchDialog(
            initialSearchQuery = searchQuery,
            initialScope = scopeFilter,
            initialType = typeFilter,
            initialDateFilter = dateFilter,
            onApply = { q, sc, tp, df ->
                viewModel.setSearchQuery(q)
                viewModel.setScopeFilter(sc)
                viewModel.setTypeFilter(tp)
                viewModel.setDateFilter(df)
            },
            onReset = {
                viewModel.clearFilters()
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    // Analytics Dialog
    if (showAnalyticsDialog) {
        AnalyticsDialog(
            totalIn = totalIn,
            totalOut = totalOut,
            netBalance = netBalance,
            dailyFlows = dailyFlows,
            onDismiss = { showAnalyticsDialog = false }
        )
    }

    // Statement Export Dialog
    if (showStatementDialog) {
        StatementExportDialog(
            account = activeAccount,
            transactions = filteredTransactions,
            totalIn = totalIn,
            totalOut = totalOut,
            netBalance = netBalance,
            onDismiss = { showStatementDialog = false }
        )
    }

    // Trash / Recycle Bin Sheet
    if (showTrashSheet) {
        TrashSheet(
            trashItems = trashItems,
            onRestore = {
                viewModel.restoreTrashItem(it)
                Toast.makeText(context, "Item restored", Toast.LENGTH_SHORT).show()
            },
            onPermanentDelete = {
                viewModel.permanentlyDeleteTrashItem(it)
                Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
                Toast.makeText(context, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showTrashSheet = false }
        )
    }

    // Move Entry Dialog
    if (moveCandidateEntry != null) {
        MoveTransactionDialog(
            entry = moveCandidateEntry!!,
            accounts = accounts,
            onMove = { targetAccId ->
                viewModel.moveTransaction(moveCandidateEntry!!.id, targetAccId)
                Toast.makeText(context, "Transaction moved to new account", Toast.LENGTH_SHORT).show()
                moveCandidateEntry = null
            },
            onDismiss = { moveCandidateEntry = null }
        )
    }

    // Auth Dialog
    if (showAuthDialog) {
        AuthDialog(
            isLoading = isAuthLoading,
            errorMessage = authError,
            onGoogleSignIn = { act ->
                viewModel.signInWithGoogle(act)
                showAuthDialog = false
            },
            onEmailSignIn = { email, pass ->
                viewModel.signInWithEmail(email, pass) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                        showAuthDialog = false
                    }
                }
            },
            onEmailSignUp = { email, pass, name ->
                viewModel.signUpWithEmail(email, pass, name) { ok ->
                    if (ok) {
                        Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        showAuthDialog = false
                    }
                }
            },
            onGuestSignIn = {
                viewModel.signInAsGuest()
            },
            onClearError = { viewModel.clearAuthError() },
            onDismiss = {
                viewModel.clearAuthError()
                showAuthDialog = false
            }
        )
    }
}
