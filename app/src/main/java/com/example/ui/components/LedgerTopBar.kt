package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.SyncStatus
import com.example.ui.theme.CashBlue
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashRed
import com.example.ui.theme.CashYellow

@Composable
fun LedgerTopBar(
    activeAccount: Account?,
    syncStatus: SyncStatus,
    hasActiveFilter: Boolean,
    onMenuClick: () -> Unit,
    onAccountClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Account selector pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAccountClick() }
                    .testTag("account_selector_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = activeAccount?.icon ?: "💵",
                        fontSize = 18.sp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAccount?.name ?: "Select Account",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (syncStatus) {
                                SyncStatus.SYNCED -> "Cloud Synced ✓"
                                SyncStatus.SYNCING -> "Syncing..."
                                SyncStatus.OFFLINE -> "Offline Mode"
                                SyncStatus.ERROR -> "Sync Offline"
                            },
                            fontSize = 10.sp,
                            color = when (syncStatus) {
                                SyncStatus.SYNCED -> CashGreen
                                SyncStatus.SYNCING -> CashYellow
                                SyncStatus.OFFLINE -> MaterialTheme.colorScheme.onSurfaceVariant
                                SyncStatus.ERROR -> CashRed
                            }
                        )
                    }

                    // Sync status pill badge
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (syncStatus) {
                                    SyncStatus.SYNCED -> CashGreen
                                    SyncStatus.SYNCING -> CashYellow
                                    SyncStatus.OFFLINE -> Color.Gray
                                    SyncStatus.ERROR -> CashRed
                                }
                            )
                    )
                }
            }

            // Quick Sync button
            IconButton(
                onClick = onSyncClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("sync_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync Cloud Data",
                    tint = when (syncStatus) {
                        SyncStatus.SYNCED -> CashGreen
                        SyncStatus.SYNCING -> CashYellow
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            // Search & Filter button
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (hasActiveFilter) CashBlue.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("filter_search_button")
            ) {
                BadgedBox(
                    badge = {
                        if (hasActiveFilter) {
                            Badge(containerColor = CashYellow)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search and Filters",
                        tint = if (hasActiveFilter) CashBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
