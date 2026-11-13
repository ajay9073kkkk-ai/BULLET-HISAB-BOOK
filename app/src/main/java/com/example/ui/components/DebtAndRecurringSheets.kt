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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurringEntry
import com.example.domain.model.TransactionType
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormSheet(
    initialType: DebtType,
    editingDebt: DebtEntry?,
    todayDateKey: Int,
    onDismiss: () -> Unit,
    onSave: (
        type: DebtType,
        personName: String,
        amountText: String,
        dateKey: Int,
        dueDateKey: Int?,
        notes: String
    ) -> Boolean
) {
    val ledgerColors = LocalLedgerColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEditing = editingDebt != null

    var selectedType by remember(editingDebt, initialType) {
        mutableStateOf(editingDebt?.type ?: initialType)
    }
    var personName by remember(editingDebt) { mutableStateOf(editingDebt?.personName.orEmpty()) }
    var amountText by remember(editingDebt) {
        val raw = editingDebt?.amount?.let { amt ->
            if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        }.orEmpty()
        mutableStateOf(raw)
    }
    var dateKey by remember(editingDebt, todayDateKey) {
        mutableIntStateOf(editingDebt?.dateKey ?: todayDateKey)
    }
    var dueDateKey by remember(editingDebt) {
        mutableStateOf<Int?>(editingDebt?.dueDateKey)
    }
    var notes by remember(editingDebt) { mutableStateOf(editingDebt?.notes.orEmpty()) }

    var personError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePickerForDue by remember { mutableStateOf<Boolean?>(null) }

    val accentColor = if (selectedType == DebtType.LENT) ledgerColors.income else ledgerColors.expense

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
                    text = if (isEditing) "उधार रिकॉर्ड बदलें" else "नया उधार लिखें",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "बंद करें")
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DebtType.entries.forEachIndexed { index, dt ->
                    SegmentedButton(
                        selected = selectedType == dt,
                        onClick = { selectedType = dt },
                        shape = SegmentedButtonDefaults.itemShape(index, DebtType.entries.size),
                        modifier = Modifier.testTag("debt_form_type_${dt.name.lowercase()}")
                    ) {
                        Text(dt.labelHi, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it; personError = null },
                label = { Text("व्यक्ति का नाम") },
                placeholder = { Text("जैसे: सुरेश शर्मा") },
                singleLine = true,
                isError = personError != null,
                supportingText = personError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_person_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; amountError = null },
                label = { Text("रकम (₹ में)") },
                prefix = {
                    Text(
                        "₹ ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = LedgerNumberFont,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_amount_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showDatePickerForDue = false },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("तारीख: ${DateUtils.formatShortDayMonth(dateKey)}")
                }
                OutlinedButton(
                    onClick = { showDatePickerForDue = true },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        dueDateKey?.let { "वापसी: ${DateUtils.formatShortDayMonth(it)}" }
                            ?: "वापसी तारीख (वैकल्पिक)"
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("नोट्स (वैकल्पिक)") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (personName.isBlank()) {
                        personError = "कृपया व्यक्ति का नाम लिखें"
                        return@Button
                    }
                    if (CurrencyFormatter.parseValidTransactionAmount(amountText) == null) {
                        amountError = "कृपया सही रकम दर्ज करें (0 से अधिक)"
                        return@Button
                    }
                    onSave(selectedType, personName, amountText, dateKey, dueDateKey, notes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_debt_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isEditing) "उधार अपडेट करें" else "उधार सेव करें",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    showDatePickerForDue?.let { isDue ->
        val initial = if (isDue) (dueDateKey ?: todayDateKey) else dateKey
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcPickerMillisFromDateKey(initial)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerForDue = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = DateUtils.dateKeyFromUtcPickerMillis(millis)
                            if (isDue) dueDateKey = picked else dateKey = picked
                        }
                        showDatePickerForDue = null
                    }
                ) { Text("चुनें") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForDue = null }) { Text("रद्द करें") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecurringFormSheet(
    editingRecurring: RecurringEntry?,
    todayDateKey: Int,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        type: TransactionType,
        amountText: String,
        account: PaymentAccount,
        expenseCategory: ExpenseCategory?,
        incomeCategory: IncomeCategory?,
        frequency: RecurrenceFrequency,
        nextDueDateKey: Int,
        notes: String
    ) -> Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(editingRecurring) { mutableStateOf(editingRecurring?.title.orEmpty()) }
    var selectedType by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.type ?: TransactionType.EXPENSE)
    }
    var amountText by remember(editingRecurring) {
        val raw = editingRecurring?.amount?.let { amt ->
            if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
        }.orEmpty()
        mutableStateOf(raw)
    }
    var selectedAccount by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.account ?: PaymentAccount.BANK)
    }
    var expenseCategory by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.expenseCategory ?: ExpenseCategory.HOME)
    }
    var incomeCategory by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.incomeCategory ?: IncomeCategory.SALARY)
    }
    var frequency by remember(editingRecurring) {
        mutableStateOf(editingRecurring?.frequency ?: RecurrenceFrequency.MONTHLY)
    }
    var nextDueDateKey by remember(editingRecurring, todayDateKey) {
        mutableIntStateOf(editingRecurring?.nextDueDateKey ?: todayDateKey)
    }
    var notes by remember(editingRecurring) { mutableStateOf(editingRecurring?.notes.orEmpty()) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingRecurring != null) "याददाश्त बदलें" else "नियमित याददाश्त जोड़ें",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "बंद करें")
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(TransactionType.INCOME, TransactionType.EXPENSE)
                options.forEachIndexed { idx, t ->
                    SegmentedButton(
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                        shape = SegmentedButtonDefaults.itemShape(idx, options.size)
                    ) { Text(t.labelHi) }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = null },
                label = { Text("नाम / शीर्षक") },
                placeholder = { Text("जैसे: मकान किराया, मोबाइल रिचार्ज, वेतन") },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_title_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; amountError = null },
                label = { Text("रकम (₹ में)") },
                singleLine = true,
                isError = amountError != null,
                supportingText = amountError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recurring_amount_input")
            )

            Text("दोहराने की अवधि", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecurrenceFrequency.entries.forEach { freq ->
                    FilterChip(
                        selected = frequency == freq,
                        onClick = { frequency = freq },
                        label = { Text(freq.labelHi) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            Text("खाता", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentAccount.entries.forEach { acct ->
                    FilterChip(
                        selected = selectedAccount == acct,
                        onClick = { selectedAccount = acct },
                        label = { Text(acct.labelHi) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                }
            }

            if (selectedType == TransactionType.EXPENSE) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = expenseCategory == cat,
                            onClick = { expenseCategory = cat },
                            label = { Text(cat.labelHi) }
                        )
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IncomeCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = incomeCategory == cat,
                            onClick = { incomeCategory = cat },
                            label = { Text(cat.labelHi) }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("अगली तारीख: ${DateUtils.formatDateHindi(nextDueDateKey)}")
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("नोट्स (वैकल्पिक)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "कृपया नाम लिखें"
                        return@Button
                    }
                    if (CurrencyFormatter.parseValidTransactionAmount(amountText) == null) {
                        amountError = "कृपया सही रकम दर्ज करें"
                        return@Button
                    }
                    onSave(
                        title,
                        selectedType,
                        amountText,
                        selectedAccount,
                        if (selectedType == TransactionType.EXPENSE) expenseCategory else null,
                        if (selectedType == TransactionType.INCOME) incomeCategory else null,
                        frequency,
                        nextDueDateKey,
                        notes
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_recurring_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("याददाश्त सेव करें", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcPickerMillisFromDateKey(nextDueDateKey)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            nextDueDateKey = DateUtils.dateKeyFromUtcPickerMillis(millis)
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
