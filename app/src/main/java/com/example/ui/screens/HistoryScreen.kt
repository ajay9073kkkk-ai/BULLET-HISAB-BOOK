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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.PaymentAccount
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HisabUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleShowAllMonths: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onAccountFilterChange: (PaymentAccount?) -> Unit,
    onCategoryFilterChange: (ExpenseCategory?) -> Unit,
    onDateFilterChange: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onEditTransaction: (HisabTransaction) -> Unit,
    onDeleteTransaction: (HisabTransaction) -> Unit,
    onQuickAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val filters = uiState.filterState
    val notebook = uiState.monthlyNotebookSummary
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredIncome = uiState.filteredEntries
        .filter { it.transaction.type == TransactionType.INCOME }
        .sumOf { it.transaction.amount }
    val filteredExpense = uiState.filteredEntries
        .filter { it.transaction.type == TransactionType.EXPENSE }
        .sumOf { it.transaction.amount }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BulletSectionHeader(
                    title = "मासिक हिसाब डायरी (${uiState.filteredEntries.size})",
                    trailing = {
                        if (filters.hasActiveFilters || uiState.showAllMonthsInDiary) {
                            TextButton(
                                onClick = onClearFilters,
                                modifier = Modifier.testTag("clear_filters_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterAltOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("फ़िल्टर हटाएं")
                            }
                        }
                    }
                )
            }

            // Monthly Notebook Switcher (< Previous | Month Year | Next >) + Summary
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousMonth,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "पिछला महीना"
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (uiState.showAllMonthsInDiary || filters.query.isNotBlank()) {
                                    "सभी महीनों का हिसाब"
                                } else {
                                    DateUtils.formatMonthYearHindi(uiState.selectedNotebookMonthKey)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("notebook_month_title")
                            )
                        }

                        IconButton(
                            onClick = onNextMonth,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "अगला महीना"
                            )
                        }
                    }

                    if (!uiState.showAllMonthsInDiary && filters.query.isBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = ledgerColors.ruledLine)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NotebookStatColumn(
                                label = "पिछला बचा",
                                value = CurrencyFormatter.formatRupees(notebook.broughtForwardBalance),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            NotebookStatColumn(
                                label = "आमदनी",
                                value = CurrencyFormatter.formatRupees(notebook.monthIncome, showPlusSign = true),
                                color = ledgerColors.income
                            )
                            NotebookStatColumn(
                                label = "खर्च",
                                value = CurrencyFormatter.formatRupees(notebook.monthExpense),
                                color = ledgerColors.expense
                            )
                            NotebookStatColumn(
                                label = "मासिक शेष",
                                value = CurrencyFormatter.formatRupees(notebook.closingBalance),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Search Bar (name, amount, date, description)
            item {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("नाम, रकम, तारीख या विवरण खोजें...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "खोजें"
                        )
                    },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "खोज साफ़ करें"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            // Type + All Months + Date Filters
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = uiState.showAllMonthsInDiary,
                            onClick = { onToggleShowAllMonths(!uiState.showAllMonthsInDiary) },
                            label = { Text("सभी महीने") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("toggle_all_months_chip")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == null,
                            onClick = { onTypeFilterChange(null) },
                            label = { Text("सभी प्रकार") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_type_all")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == TransactionType.INCOME,
                            onClick = {
                                val next = if (filters.typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                onTypeFilterChange(next)
                            },
                            label = { Text("आमदनी") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_type_income")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == TransactionType.EXPENSE,
                            onClick = {
                                val next = if (filters.typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                onTypeFilterChange(next)
                            },
                            label = { Text("खर्च") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_type_expense")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.typeFilter == TransactionType.TRANSFER,
                            onClick = {
                                val next = if (filters.typeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER
                                onTypeFilterChange(next)
                            },
                            label = { Text("ट्रांसफ़र") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_type_transfer")
                        )
                    }
                    item {
                        FilterChip(
                            selected = filters.dateKeyFilter != null,
                            onClick = {
                                if (filters.dateKeyFilter != null) {
                                    onDateFilterChange(null)
                                } else {
                                    showDatePicker = true
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    filters.dateKeyFilter?.let { DateUtils.formatDateHindi(it) }
                                        ?: "तारीख चुनें"
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_date_chip")
                        )
                    }
                }
            }

            // Account & Expense Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PaymentAccount.entries) { acct ->
                        val selected = filters.accountFilter == acct
                        FilterChip(
                            selected = selected,
                            onClick = { onAccountFilterChange(if (selected) null else acct) },
                            leadingIcon = {
                                Icon(
                                    imageVector = acct.iconVector(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(acct.labelHi) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_acct_${acct.key.lowercase()}")
                        )
                    }
                    items(ExpenseCategory.entries) { category ->
                        val selected = filters.categoryFilter == category
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onCategoryFilterChange(if (selected) null else category)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.iconVector(),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(category.labelHi) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_cat_${category.key.lowercase()}")
                        )
                    }
                }
            }

            // Filtered Summary Strip
            if (uiState.allEntries.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "दिखाई गई आमदनी: ${CurrencyFormatter.formatRupees(filteredIncome)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = LedgerNumberFont),
                                color = ledgerColors.income
                            )
                            Text(
                                text = "खर्च: ${CurrencyFormatter.formatRupees(filteredExpense)}",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = LedgerNumberFont),
                                color = ledgerColors.expense
                            )
                        }
                    }
                }
            }

            // Entries or Empty State
            if (uiState.filteredEntries.isEmpty()) {
                item {
                    NotebookRuledCard(showMarginRule = false) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (uiState.allEntries.isEmpty()) {
                                    "अभी कोई हिसाब दर्ज नहीं है"
                                } else {
                                    "इस महीने या फ़िल्टर के अनुसार कोई एंट्री नहीं मिली"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (uiState.allEntries.isEmpty()) {
                                    "नीचे + बटन दबाकर अपनी पहली आमदनी या खर्च लिखें।"
                                } else {
                                    "दूसरा महीना चुनें या खोज शब्द/फ़िल्टर बदलें।"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (filters.hasActiveFilters) {
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = onClearFilters) {
                                    Text("सभी फ़िल्टर हटाएं")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.filteredEntries,
                    key = { it.transaction.id }
                ) { entry ->
                    LedgerEntryRow(
                        entry = entry,
                        onEdit = onEditTransaction,
                        onDelete = onDeleteTransaction
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onQuickAddExpense,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("history_fab_add")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "नई एंट्री जोड़ें"
            )
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcPickerMillisFromDateKey(
                filters.dateKeyFilter ?: uiState.todayDateKey
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateFilterChange(DateUtils.dateKeyFromUtcPickerMillis(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("चुनें") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("रद्द करें") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun NotebookStatColumn(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = LedgerNumberFont,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}
