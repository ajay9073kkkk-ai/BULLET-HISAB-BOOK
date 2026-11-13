package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AccountBalanceSummary
import com.example.domain.model.HisabTransaction
import com.example.domain.model.RecurringEntry
import com.example.domain.model.TransactionType
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.components.BulletSectionHeader
import com.example.ui.components.LedgerEntryRow
import com.example.ui.components.NotebookRuledCard
import com.example.ui.components.bulletGridBackground
import com.example.ui.components.iconVector
import com.example.ui.state.HisabUiState
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@Composable
fun DashboardScreen(
    uiState: HisabUiState,
    onAddTransaction: (TransactionType) -> Unit,
    onEditOpeningBalance: () -> Unit,
    onEditTransaction: (HisabTransaction) -> Unit,
    onDeleteTransaction: (HisabTransaction) -> Unit,
    onRecordRecurring: (RecurringEntry) -> Unit,
    onSkipRecurring: (RecurringEntry) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val summary = uiState.summary

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Diary Title Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bullet Hisab Book",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "आज: ${DateUtils.formatDateHindi(uiState.todayDateKey)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onEditOpeningBalance,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ledgerColors.brass.copy(alpha = 0.7f)),
                        modifier = Modifier.testTag("opening_balance_chip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "शुरुआती बैलेंस बदलें",
                            tint = ledgerColors.brass,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "शुरुआती: ${CurrencyFormatter.formatRupees(summary.openingBalance)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Hero Total Wealth Ledger Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_balance_card"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "बची हुई कुल रकम (Total Wealth)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }

                            Text(
                                text = CurrencyFormatter.formatRupees(summary.currentBalance),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = LedgerNumberFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("current_balance_text")
                            )

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                HeroStatItem(
                                    label = "कुल आमदनी",
                                    amount = summary.totalIncome,
                                    tag = "total_income_text",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatItem(
                                    label = "कुल खर्च",
                                    amount = summary.totalExpense,
                                    tag = "total_expense_text",
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatItem(
                                    label = "शुरुआती बैलेंस",
                                    amount = summary.openingBalance,
                                    tag = "opening_balance_text",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Multiple Accounts Breakdown (Cash, Bank, UPI)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BulletSectionHeader(title = "मेरे खाते (नकद • बैंक • UPI)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        summary.accountBalances.forEach { acctSummary ->
                            AccountBalanceChipCard(
                                summary = acctSummary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick Action Buttons (Add Income / Add Expense / Transfer)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddTransaction(TransactionType.INCOME) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ledgerColors.income,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_add_income_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "आमदनी",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { onAddTransaction(TransactionType.EXPENSE) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ledgerColors.expense,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_add_expense_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "खर्च",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { onAddTransaction(TransactionType.TRANSFER) },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, ledgerColors.brass),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("quick_add_transfer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = ledgerColors.brass,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ट्रांसफ़र",
                            style = MaterialTheme.typography.labelLarge,
                            color = ledgerColors.brass,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Due Recurring Reminders Banner
            if (uiState.dueRecurringEntries.isNotEmpty()) {
                item {
                    NotebookRuledCard(showMarginRule = false) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = ledgerColors.brass,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "नियमित याददाश्त (${uiState.dueRecurringEntries.size} बाकी)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.dueRecurringEntries.forEach { rec ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatRupees(rec.amount)} • ${rec.frequency.labelHi} • ${rec.account.labelHi}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TextButton(onClick = { onSkipRecurring(rec) }) {
                                            Text("छोड़ें")
                                        }
                                        Button(
                                            onClick = { onRecordRecurring(rec) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("record_due_recurring_${rec.id}")
                                        ) {
                                            Text("दर्ज करें")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Today & Monthly Summary Cards
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 520.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PeriodSummaryNotebookCard(
                                title = "आज का हिसाब",
                                icon = Icons.Outlined.Today,
                                income = summary.todayIncome,
                                expense = summary.todayExpense,
                                modifier = Modifier.weight(1f)
                            )
                            PeriodSummaryNotebookCard(
                                title = "इस महीने का हिसाब",
                                icon = Icons.Outlined.CalendarMonth,
                                income = summary.monthIncome,
                                expense = summary.monthExpense,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PeriodSummaryNotebookCard(
                                title = "आज का हिसाब",
                                icon = Icons.Outlined.Today,
                                income = summary.todayIncome,
                                expense = summary.todayExpense
                            )
                            PeriodSummaryNotebookCard(
                                title = "इस महीने का हिसाब",
                                icon = Icons.Outlined.CalendarMonth,
                                income = summary.monthIncome,
                                expense = summary.monthExpense
                            )
                        }
                    }
                }
            }

            // Recent Entries Header
            item {
                BulletSectionHeader(
                    title = "हाल की एंट्री",
                    trailing = {
                        if (uiState.allEntries.isNotEmpty()) {
                            TextButton(
                                onClick = onNavigateToHistory,
                                modifier = Modifier.testTag("view_all_history_button")
                            ) {
                                Text("सभी देखें")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }

            if (uiState.recentEntries.isEmpty()) {
                item {
                    EmptyLedgerCard(
                        onAddIncome = { onAddTransaction(TransactionType.INCOME) },
                        onAddExpense = { onAddTransaction(TransactionType.EXPENSE) }
                    )
                }
            } else {
                items(
                    items = uiState.recentEntries,
                    key = { it.transaction.id }
                ) { entry ->
                    LedgerEntryRow(
                        entry = entry,
                        onEdit = onEditTransaction,
                        onDelete = onDeleteTransaction
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AccountBalanceChipCard(
    summary: AccountBalanceSummary,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    Card(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .testTag("account_card_${summary.account.key.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ledgerColors.paperCard),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = summary.account.iconVector(),
                    contentDescription = summary.account.labelHi,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = summary.account.labelHi,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = CurrencyFormatter.formatRupees(summary.currentBalance),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = LedgerNumberFont,
                    fontWeight = FontWeight.Bold
                ),
                color = if (summary.currentBalance >= 0) MaterialTheme.colorScheme.onSurface else ledgerColors.expense,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    amount: Double,
    tag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = CurrencyFormatter.formatRupees(amount),
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = LedgerNumberFont,
                fontWeight = FontWeight.Bold
            ),
            color = Color.White,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun PeriodSummaryNotebookCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    income: Double,
    expense: Double,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    NotebookRuledCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ledgerColors.brass,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "आमदनी",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatRupees(income, showPlusSign = true),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LedgerNumberFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ledgerColors.income
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "खर्च",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatRupees(expense),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = LedgerNumberFont,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ledgerColors.expense
                )
            }
        }
    }
}

@Composable
private fun EmptyLedgerCard(
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit
) {
    NotebookRuledCard(showMarginRule = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_ledger),
                contentDescription = "खाली हिसाब डायरी चित्र",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Text(
                text = "हिसाब डायरी खाली है",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "आपकी डायरी में अभी कोई एंट्री नहीं है। शुरुआत करने के लिए अपनी पहली आमदनी या खर्च लिखें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = onAddIncome,
                    shape = RoundedCornerShape(12.dp),
                    color = LocalLedgerColors.current.incomeContainer
                ) {
                    Text(
                        text = "+ पहली आमदनी",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalLedgerColors.current.income,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                Surface(
                    onClick = onAddExpense,
                    shape = RoundedCornerShape(12.dp),
                    color = LocalLedgerColors.current.expenseContainer
                ) {
                    Text(
                        text = "- पहला खर्च",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalLedgerColors.current.expense,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
