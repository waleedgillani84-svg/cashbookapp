package com.example.ui.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.DailyFlow
import com.example.ui.theme.CashBlue
import com.example.ui.theme.CashGreen
import com.example.ui.theme.CashRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsDialog(
    totalIn: Double,
    totalOut: Double,
    netBalance: Double,
    dailyFlows: List<DailyFlow>,
    onDismiss: () -> Unit
) {
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }
    }

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
                        text = "📊 Cash Flow Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 summary boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMiniCard(
                        title = "TOTAL IN",
                        value = "Rs " + formatter.format(totalIn),
                        color = CashGreen,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniCard(
                        title = "TOTAL OUT",
                        value = "Rs " + formatter.format(totalOut),
                        color = CashRed,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMiniCard(
                        title = "NET",
                        value = (if (netBalance >= 0) "+ Rs " else "- Rs ") + formatter.format(kotlin.math.abs(netBalance)),
                        color = CashBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Chart Header & Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last 7 Days Flow",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendItem("In", CashGreen)
                        LegendItem("Out", CashRed)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bar Chart
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(vertical = 4.dp)
                ) {
                    if (dailyFlows.isNotEmpty()) {
                        val maxVal = remember(dailyFlows) {
                            val maxIn = dailyFlows.maxOfOrNull { it.totalIn } ?: 0.0
                            val maxOut = dailyFlows.maxOfOrNull { it.totalOut } ?: 0.0
                            kotlin.math.max(kotlin.math.max(maxIn, maxOut), 1.0)
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            val count = dailyFlows.size
                            val slotWidth = size.width / count
                            val barWidth = slotWidth * 0.32f
                            val chartHeight = size.height - 24.dp.toPx()

                            dailyFlows.forEachIndexed { i, flow ->
                                val centerX = i * slotWidth + (slotWidth / 2f)

                                val inHeight = ((flow.totalIn / maxVal) * chartHeight).toFloat()
                                val outHeight = ((flow.totalOut / maxVal) * chartHeight).toFloat()

                                // In Bar
                                if (inHeight > 0f) {
                                    drawRoundRect(
                                        color = CashGreen,
                                        topLeft = Offset(centerX - barWidth - 2f, chartHeight - inHeight),
                                        size = Size(barWidth, inHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                } else {
                                    drawRoundRect(
                                        color = CashGreen.copy(alpha = 0.2f),
                                        topLeft = Offset(centerX - barWidth - 2f, chartHeight - 4f),
                                        size = Size(barWidth, 4f),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                }

                                // Out Bar
                                if (outHeight > 0f) {
                                    drawRoundRect(
                                        color = CashRed,
                                        topLeft = Offset(centerX + 2f, chartHeight - outHeight),
                                        size = Size(barWidth, outHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                } else {
                                    drawRoundRect(
                                        color = CashRed.copy(alpha = 0.2f),
                                        topLeft = Offset(centerX + 2f, chartHeight - 4f),
                                        size = Size(barWidth, 4f),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                // X-axis Day Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dailyFlows.forEach { flow ->
                        Text(
                            text = flow.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
