package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Account
import com.example.data.model.TransactionEntry
import com.example.ui.theme.CashBlue
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountManagementDialog(
    accounts: List<Account>,
    activeAccountId: String?,
    transactions: List<TransactionEntry>,
    onSelectAccount: (String) -> Unit,
    onCreateAccount: (name: String, icon: String) -> Unit,
    onEditAccount: (id: String, name: String, icon: String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var newAccountName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("💵") }
    var deleteCandidate by remember { mutableStateOf<Account?>(null) }

    val availableIcons = listOf("💵", "👤", "💼", "🏦", "💰", "🪙", "💳", "🛒")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💳 Manage Accounts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Accounts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { acc ->
                        val isActive = acc.id == activeAccountId
                        val accTxs = transactions.filter { it.accountId == acc.id }
                        val inSum = accTxs.filter { it.type == "in" }.sumOf { it.amount }
                        val outSum = accTxs.filter { it.type == "out" }.sumOf { it.amount }
                        val net = inSum - outSum
                        val formatter = NumberFormat.getNumberInstance(Locale.getDefault())

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.background,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectAccount(acc.id)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = acc.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Net: " + (if (net > 0) "+ Rs " else if (net < 0) "- Rs " else "Rs ") +
                                                formatter.format(kotlin.math.abs(net)),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (net >= 0) CashGreen else CashRed
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingAccount = acc
                                            newAccountName = acc.name
                                            selectedIcon = acc.icon
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Account",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (accounts.size > 1) {
                                        IconButton(
                                            onClick = { deleteCandidate = acc },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Account",
                                                tint = CashRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Create or Edit Account Inline Form
                if (showAddAccount || editingAccount != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (editingAccount != null) "Edit Account" else "Add New Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newAccountName,
                                onValueChange = { newAccountName = it },
                                label = { Text("Account Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Choose Icon:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                availableIcons.forEach { icon ->
                                    val isSelected = selectedIcon == icon
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) CashBlue.copy(alpha = 0.3f) else Color.Transparent,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, CashBlue) else null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedIcon = icon }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = icon, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    showAddAccount = false
                                    editingAccount = null
                                    newAccountName = ""
                                }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newAccountName.isNotBlank()) {
                                            if (editingAccount != null) {
                                                onEditAccount(editingAccount!!.id, newAccountName.trim(), selectedIcon)
                                            } else {
                                                onCreateAccount(newAccountName.trim(), selectedIcon)
                                            }
                                            showAddAccount = false
                                            editingAccount = null
                                            newAccountName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CashBlue)
                                ) {
                                    Text(if (editingAccount != null) "Update" else "Create")
                                }
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            showAddAccount = true
                            newAccountName = ""
                            selectedIcon = "💵"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CashBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_account_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation for account deletion
    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Account?") },
            text = {
                Text("Deleting \"${deleteCandidate?.name}\" will move this account and its records to the Recycle Bin. Continue?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate?.let { onDeleteAccount(it.id) }
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CashRed)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
