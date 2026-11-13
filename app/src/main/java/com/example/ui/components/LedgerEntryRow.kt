package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.HisabTransaction
import com.example.domain.model.LedgerEntry
import com.example.domain.model.TransactionType
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerEntryRow(
    entry: LedgerEntry,
    onEdit: ((HisabTransaction) -> Unit)? = null,
    onDelete: ((HisabTransaction) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val tx = entry.transaction

    val accentColor = when (tx.type) {
        TransactionType.INCOME -> ledgerColors.income
        TransactionType.EXPENSE -> ledgerColors.expense
        TransactionType.TRANSFER -> ledgerColors.brass
    }
    val containerColor = when (tx.type) {
        TransactionType.INCOME -> ledgerColors.incomeContainer
        TransactionType.EXPENSE -> ledgerColors.expenseContainer
        TransactionType.TRANSFER -> ledgerColors.brassContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ledger_entry_${tx.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ledgerColors.paperCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val marginX = 14.dp.toPx()
                    drawLine(
                        color = ledgerColors.marginLine.copy(alpha = 0.55f),
                        start = Offset(marginX, 0f),
                        end = Offset(marginX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(containerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (tx.type) {
                                    TransactionType.INCOME -> tx.incomeCategory?.iconVector() ?: Icons.Rounded.ArrowDownward
                                    TransactionType.EXPENSE -> tx.category?.iconVector() ?: Icons.Rounded.ArrowUpward
                                    TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
                                },
                                contentDescription = tx.type.labelHi,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.partyName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${DateUtils.formatDateHindi(tx.dateKey)} • ${DateUtils.formatTimeMinutes(tx.timeMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CategoryPill(
                                    label = tx.displayCategoryLabel,
                                    icon = null,
                                    containerColor = containerColor,
                                    contentColor = accentColor
                                )
                                val accountLabel = if (tx.type == TransactionType.TRANSFER && tx.toAccount != null) {
                                    "${tx.account.labelHi} → ${tx.toAccount.labelHi}"
                                } else {
                                    tx.account.labelHi
                                }
                                CategoryPill(
                                    label = accountLabel,
                                    icon = tx.account.iconVector(),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        val amountString = when (tx.type) {
                            TransactionType.INCOME -> CurrencyFormatter.formatRupees(tx.amount, showPlusSign = true)
                            TransactionType.EXPENSE -> CurrencyFormatter.formatRupees(-tx.amount)
                            TransactionType.TRANSFER -> CurrencyFormatter.formatRupees(tx.amount)
                        }
                        Text(
                            text = amountString,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = LedgerNumberFont,
                                fontWeight = FontWeight.Bold
                            ),
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "शेष ${CurrencyFormatter.formatRupees(entry.balanceAfter)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = LedgerNumberFont
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (tx.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tx.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onEdit != null || onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ledgerColors.ruledLine)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onEdit != null) {
                            IconButton(
                                onClick = { onEdit(tx) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("edit_entry_${tx.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "एंट्री बदलें",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(
                                onClick = { onDelete(tx) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_entry_${tx.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "एंट्री हटाएं",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
