package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.components.BulletSectionHeader
import com.example.ui.components.CategoryPill
import com.example.ui.components.NotebookRuledCard
import com.example.ui.components.bulletGridBackground
import com.example.ui.state.HisabUiState
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@Composable
fun DebtScreen(
    uiState: HisabUiState,
    onFilterType: (DebtType?) -> Unit,
    onAddDebt: (DebtType) -> Unit,
    onEditDebt: (DebtEntry) -> Unit,
    onToggleSettled: (DebtEntry) -> Unit,
    onDeleteDebt: (DebtEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val summary = uiState.debtSummary

    Box(
        modifier = modifier
            .fillMaxSize()
            .bulletGridBackground(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BulletSectionHeader(title = "उधार / लेन-देन खाता")
            }

            // Separation Rule Notice
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "उधार खाता आपकी आमदनी और खर्च से अलग रखा जाता है ताकि दोहरी गिनती न हो।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // Summary Cards: To Receive vs To Pay
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ledgerColors.incomeContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "लेना बाकी (दिया उधार)",
                                style = MaterialTheme.typography.labelMedium,
                                color = ledgerColors.income
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatRupees(summary.pendingToReceive),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = LedgerNumberFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ledgerColors.income,
                                modifier = Modifier.testTag("debt_to_receive_text")
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ledgerColors.expenseContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "देना बाकी (लिया उधार)",
                                style = MaterialTheme.typography.labelMedium,
                                color = ledgerColors.expense
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatRupees(summary.pendingToPay),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = LedgerNumberFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ledgerColors.expense,
                                modifier = Modifier.testTag("debt_to_pay_text")
                            )
                        }
                    }
                }
            }

            // Add Lent / Add Borrowed Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onAddDebt(DebtType.LENT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ledgerColors.income,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_debt_lent_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("उधार दिया", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAddDebt(DebtType.BORROWED) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ledgerColors.expense,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("add_debt_borrowed_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("उधार लिया", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Type Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.debtTypeFilter == null,
                        onClick = { onFilterType(null) },
                        label = { Text("सभी (${uiState.debts.size})") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                    FilterChip(
                        selected = uiState.debtTypeFilter == DebtType.LENT,
                        onClick = { onFilterType(DebtType.LENT) },
                        label = { Text("उधार दिया") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                    FilterChip(
                        selected = uiState.debtTypeFilter == DebtType.BORROWED,
                        onClick = { onFilterType(DebtType.BORROWED) },
                        label = { Text("उधार लिया") },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            if (uiState.filteredDebts.isEmpty()) {
                item {
                    NotebookRuledCard(showMarginRule = false) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "कोई उधार रिकॉर्ड नहीं है",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "किसी को दिया हुआ या किसी से लिया हुआ उधार यहाँ लिखकर याद रख सकते हैं।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = uiState.filteredDebts,
                    key = { it.id }
                ) { debt ->
                    DebtEntryCard(
                        debt = debt,
                        onToggleSettled = { onToggleSettled(debt) },
                        onEdit = { onEditDebt(debt) },
                        onDelete = { onDeleteDebt(debt) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DebtEntryCard(
    debt: DebtEntry,
    onToggleSettled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ledgerColors = LocalLedgerColors.current
    val isLent = debt.type == DebtType.LENT
    val isSettled = debt.status == DebtStatus.SETTLED
    val accent = if (isLent) ledgerColors.income else ledgerColors.expense
    val container = if (isLent) ledgerColors.incomeContainer else ledgerColors.expenseContainer

    NotebookRuledCard(
        modifier = Modifier.testTag("debt_card_${debt.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debt.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryPill(
                        label = debt.type.labelHi,
                        icon = null,
                        containerColor = container,
                        contentColor = accent
                    )
                    CategoryPill(
                        label = debt.status.labelHi,
                        icon = null,
                        containerColor = if (isSettled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            ledgerColors.brassContainer
                        },
                        contentColor = if (isSettled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            ledgerColors.brass
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("तारीख: ${DateUtils.formatDateHindi(debt.dateKey)}")
                        if (debt.dueDateKey != null) {
                            append(" • वापसी: ${DateUtils.formatDateHindi(debt.dueDateKey)}")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (debt.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = debt.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.formatRupees(debt.amount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LedgerNumberFont,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = ledgerColors.ruledLine)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onToggleSettled,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_debt_settled_${debt.id}")
            ) {
                Icon(
                    imageVector = if (isSettled) Icons.Rounded.Undo else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isSettled) "बाकी करें" else "चुकाया मार्क करें")
            }

            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("edit_debt_${debt.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "उधार बदलें",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_debt_${debt.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "उधार हटाएं",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
