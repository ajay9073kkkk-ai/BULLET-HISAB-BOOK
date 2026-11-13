package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategoryTotal
import com.example.domain.model.ChartBucket
import com.example.domain.util.CurrencyFormatter
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun IncomeVsExpenseBarChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val gridColor = ledgerColors.ruledLine
    val axisColor = MaterialTheme.colorScheme.outline

    val maxValue = max(
        1.0,
        buckets.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = ledgerColors.income, label = "आमदनी (Income)")
            LegendDot(color = ledgerColors.expense, label = "खर्च (Expense)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-axis max & mid labels
            Column(
                modifier = Modifier
                    .width(44.dp)
                    .height(170.dp)
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "₹${CurrencyFormatter.formatCompactNumber(maxValue)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = LedgerNumberFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${CurrencyFormatter.formatCompactNumber(maxValue / 2.0)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = LedgerNumberFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹0",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = LedgerNumberFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            ) {
                val width = size.width
                val height = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Horizontal dashed gridlines at top and middle
                drawLine(
                    color = gridColor,
                    start = Offset(0f, 4.dp.toPx()),
                    end = Offset(width, 4.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, height / 2f),
                    end = Offset(width, height / 2f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
                // Baseline
                drawLine(
                    color = axisColor,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = 1.5.dp.toPx()
                )

                if (buckets.isNotEmpty()) {
                    val slotWidth = width / buckets.size
                    val barWidth = (slotWidth * 0.28f).coerceIn(6.dp.toPx(), 18.dp.toPx())
                    val barGap = (slotWidth * 0.08f).coerceAtLeast(3.dp.toPx())
                    val usableHeight = height - 8.dp.toPx()

                    buckets.forEachIndexed { index, bucket ->
                        val centerX = slotWidth * index + slotWidth / 2f
                        val incomeHeight = ((bucket.income / maxValue) * usableHeight).toFloat()
                        val expenseHeight = ((bucket.expense / maxValue) * usableHeight).toFloat()

                        if (incomeHeight > 0f) {
                            val h = incomeHeight.coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                color = ledgerColors.income,
                                topLeft = Offset(centerX - barGap / 2f - barWidth, height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                        if (expenseHeight > 0f) {
                            val h = expenseHeight.coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                color = ledgerColors.expense,
                                topLeft = Offset(centerX + barGap / 2f, height - h),
                                size = Size(barWidth, h),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // X-axis bucket labels aligned with canvas slots
        val isDense = buckets.size >= 10
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.label,
                    style = if (isDense) {
                        MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                    } else {
                        MaterialTheme.typography.labelSmall
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CategoryBreakdownList(
    items: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { item ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryPill(
                            label = item.category.labelHi,
                            icon = item.category.iconVector(),
                            containerColor = ledgerColors.expenseContainer,
                            contentColor = ledgerColors.expense
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(item.percentage * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = LedgerNumberFont),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatRupees(item.amount),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = LedgerNumberFont,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { item.percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ledgerColors.expense,
                    trackColor = ledgerColors.expenseContainer
                )
            }
        }
    }
}
