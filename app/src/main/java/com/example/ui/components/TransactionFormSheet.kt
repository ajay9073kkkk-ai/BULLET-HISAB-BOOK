package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.TransactionType
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormSheet(
    initialType: TransactionType,
    editingTransaction: HisabTransaction?,
    todayDateKey: Int,
    defaultTimeMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amountText: String,
        dateKey: Int,
        partyName: String,
        category: ExpenseCategory?,
        notes: String,
        timeMinutes: Int,
        account: PaymentAccount,
        toAccount: PaymentAccount?,
        incomeCategory: IncomeCategory?
    ) -> Boolean
) {
    val ledgerColors = LocalLedgerColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingTransaction != null

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.type ?: initialType)
    }
    var amountText by remember(editingTransaction) {
        val raw = editingTransaction?.amount?.let { amt ->
            if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        }.orEmpty()
        mutableStateOf(raw)
    }
    var dateKey by remember(editingTransaction, todayDateKey) {
        mutableIntStateOf(editingTransaction?.dateKey ?: todayDateKey)
    }
    var timeMinutes by remember(editingTransaction, defaultTimeMinutes) {
        mutableIntStateOf(editingTransaction?.timeMinutes ?: defaultTimeMinutes)
    }
    var selectedAccount by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.account ?: PaymentAccount.CASH)
    }
    var selectedToAccount by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.toAccount ?: PaymentAccount.BANK)
    }
    var partyName by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.partyName.orEmpty())
    }
    var selectedExpenseCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.category ?: ExpenseCategory.FOOD)
    }
    var selectedIncomeCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.incomeCategory ?: IncomeCategory.SALARY)
    }
    var notes by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.notes.orEmpty())
    }

    var amountError by remember { mutableStateOf<String?>(null) }
    var partyError by remember { mutableStateOf<String?>(null) }
    var transferError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val accentColor = when (selectedType) {
        TransactionType.INCOME -> ledgerColors.income
        TransactionType.EXPENSE -> ledgerColors.expense
        TransactionType.TRANSFER -> ledgerColors.brass
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isEditing -> "${selectedType.labelHi} बदलें"
                        selectedType == TransactionType.INCOME -> "नई आमदनी लिखें"
                        selectedType == TransactionType.EXPENSE -> "नया खर्च लिखें"
                        else -> "खाता ट्रांसफ़र लिखें"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "बंद करें"
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            transferError = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionType.entries.size
                        ),
                        modifier = Modifier.testTag("form_type_${type.name.lowercase()}")
                    ) {
                        Text(
                            text = type.labelHi,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { value ->
                    amountText = value
                    amountError = null
                },
                label = { Text("रकम (₹ में)") },
                prefix = {
                    Text(
                        text = "₹ ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LedgerNumberFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LedgerNumberFont,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input")
            )

            // Date & Time selectors
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "तारीख और समय",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .weight(1.1f)
                            .minimumInteractiveComponentSize()
                            .testTag("transaction_date_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "तारीख चुनें",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatDateHindi(dateKey),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("transaction_time_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = "समय चुनें",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatTimeMinutes(timeMinutes),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Payment Method / Account Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) {
                        "किस खाते से (From Account)"
                    } else {
                        "भुगतान का माध्यम / खाता"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentAccount.entries.forEach { acct ->
                        FilterChip(
                            selected = selectedAccount == acct,
                            onClick = {
                                selectedAccount = acct
                                if (selectedType == TransactionType.TRANSFER && selectedToAccount == acct) {
                                    selectedToAccount = PaymentAccount.entries.first { it != acct }
                                }
                                transferError = null
                            },
                            label = { Text(acct.labelHi) },
                            leadingIcon = {
                                Icon(
                                    imageVector = acct.iconVector(),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("account_chip_${acct.key.lowercase()}")
                        )
                    }
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "किस खाते में (To Account)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentAccount.entries.forEach { acct ->
                            FilterChip(
                                selected = selectedToAccount == acct,
                                onClick = {
                                    selectedToAccount = acct
                                    transferError = null
                                },
                                label = { Text(acct.labelHi) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = acct.iconVector(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("to_account_chip_${acct.key.lowercase()}")
                            )
                        }
                    }
                    if (transferError != null) {
                        Text(
                            text = transferError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Category Selection (both Expense & Income)
            if (selectedType == TransactionType.EXPENSE) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "खर्च की श्रेणी चुनें",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ExpenseCategory.entries.forEach { category ->
                            val selected = selectedExpenseCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedExpenseCategory = category },
                                label = { Text(category.labelHi) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.iconVector(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ledgerColors.expenseContainer,
                                    selectedLabelColor = ledgerColors.expense,
                                    selectedLeadingIconColor = ledgerColors.expense
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("category_chip_${category.key.lowercase()}")
                            )
                        }
                    }
                }
            } else if (selectedType == TransactionType.INCOME) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "आमदनी की श्रेणी चुनें",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IncomeCategory.entries.forEach { category ->
                            val selected = selectedIncomeCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { selectedIncomeCategory = category },
                                label = { Text(category.labelHi) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = category.iconVector(),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ledgerColors.incomeContainer,
                                    selectedLabelColor = ledgerColors.income,
                                    selectedLeadingIconColor = ledgerColors.income
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("income_category_chip_${category.key.lowercase()}")
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = partyName,
                onValueChange = {
                    partyName = it
                    partyError = null
                },
                label = {
                    Text(
                        when (selectedType) {
                            TransactionType.INCOME -> "किससे पैसा आया (नाम / स्रोत)"
                            TransactionType.EXPENSE -> "किसको दिया / दुकान का नाम"
                            TransactionType.TRANSFER -> "ट्रांसफ़र विवरण / व्यक्ति (वैकल्पिक)"
                        }
                    )
                },
                placeholder = {
                    Text(
                        when (selectedType) {
                            TransactionType.INCOME -> "जैसे: सैलरी, रमेश जी, दुकान बिक्री"
                            TransactionType.EXPENSE -> "जैसे: किराना स्टोर, बस टिकट, चाय"
                            TransactionType.TRANSFER -> "जैसे: बैंक से नकद निकासी"
                        }
                    )
                },
                singleLine = true,
                isError = partyError != null,
                supportingText = partyError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_party_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("विवरण / नोट्स (वैकल्पिक)") },
                placeholder = { Text("कोई जरूरी जानकारी लिखें...") },
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_notes_input")
            )

            Button(
                onClick = {
                    val validAmount = CurrencyFormatter.parseValidTransactionAmount(amountText)
                    if (validAmount == null) {
                        amountError = "कृपया सही रकम दर्ज करें (0 से अधिक)"
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER && selectedToAccount == selectedAccount) {
                        transferError = "कृपया दो अलग-अलग खाते चुनें"
                        return@Button
                    }
                    if (selectedType != TransactionType.TRANSFER && partyName.isBlank()) {
                        partyError = "कृपया नाम या स्रोत दर्ज करें"
                        return@Button
                    }
                    onSave(
                        selectedType,
                        amountText,
                        dateKey,
                        partyName,
                        if (selectedType == TransactionType.EXPENSE) selectedExpenseCategory else null,
                        notes,
                        timeMinutes,
                        selectedAccount,
                        if (selectedType == TransactionType.TRANSFER) selectedToAccount else null,
                        if (selectedType == TransactionType.INCOME) selectedIncomeCategory else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "बदलाव सेव करें" else "हिसाब सेव करें",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcPickerMillisFromDateKey(dateKey)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateKey = DateUtils.dateKeyFromUtcPickerMillis(millis)
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

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeMinutes / 60,
            initialMinute = timeMinutes % 60
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("समय चुनें") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text("चुनें") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("रद्द करें") }
            }
        )
    }
}
