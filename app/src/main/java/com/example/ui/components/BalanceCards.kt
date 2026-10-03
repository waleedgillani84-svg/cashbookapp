package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CashBlue
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BalanceCards(
    netBalance: Double,
    totalIn: Double,
    totalOut: Double,
    modifier: Modifier = Modifier
) {
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Net Balance Card
        BalanceItemCard(
            label = "NET BALANCE",
            amount = (if (netBalance > 0) "+ Rs " else if (netBalance < 0) "- Rs " else "Rs ") +
                    formatter.format(kotlin.math.abs(netBalance)),
            accentColor = CashBlue,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f)
        )

        // Cash In Card
        BalanceItemCard(
            label = "CASH IN (+)",
            amount = "Rs " + formatter.format(totalIn),
            accentColor = CashGreen,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f)
        )

        // Cash Out Card
        BalanceItemCard(
            label = "CASH OUT (-)",
            amount = "Rs " + formatter.format(totalOut),
            accentColor = CashRed,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BalanceItemCard(
    label: String,
    amount: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
