package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.DebtEntry
import com.example.domain.model.HisabTransaction
import com.example.domain.model.RecurringEntry
import com.example.domain.util.BackupPayload
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.domain.util.PinHashUtil

private fun formatEditableAmount(value: Double): String {
    return if (value == 0.0) {
        "0"
    } else if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

@Composable
fun OpeningBalanceDialog(
    currentOpenings: AccountOpeningBalances,
    onDismiss: () -> Unit,
    onSave: (cashText: String, bankText: String, upiText: String) -> Boolean
) {
    var cashText by remember(currentOpenings) { mutableStateOf(formatEditableAmount(currentOpenings.cash)) }
    var bankText by remember(currentOpenings) { mutableStateOf(formatEditableAmount(currentOpenings.bank)) }
    var upiText by remember(currentOpenings) { mutableStateOf(formatEditableAmount(currentOpenings.upi)) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val totalPreview = (CurrencyFormatter.parseValidOpeningBalance(cashText.ifBlank { "0" }) ?: 0.0) +
        (CurrencyFormatter.parseValidOpeningBalance(bankText.ifBlank { "0" }) ?: 0.0) +
        (CurrencyFormatter.parseValidOpeningBalance(upiText.ifBlank { "0" }) ?: 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "शुरुआती बैलेंस सेट करें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "नकद, बैंक और UPI का शुरुआती बैलेंस लिखें (कुल: ${CurrencyFormatter.formatRupees(totalPreview)})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cashText,
                    onValueChange = {
                        cashText = it
                        errorText = null
                    },
                    label = { Text("नकद शुरुआती बैलेंस (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opening_balance_input")
                )
                OutlinedTextField(
                    value = bankText,
                    onValueChange = {
                        bankText = it
                        errorText = null
                    },
                    label = { Text("बैंक खाता शुरुआती बैलेंस (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opening_bank_input")
                )
                OutlinedTextField(
                    value = upiText,
                    onValueChange = {
                        upiText = it
                        errorText = null
                    },
                    label = { Text("UPI शुरुआती बैलेंस (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("opening_upi_input")
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = CurrencyFormatter.parseValidOpeningBalance(cashText.ifBlank { "0" })
                    val b = CurrencyFormatter.parseValidOpeningBalance(bankText.ifBlank { "0" })
                    val u = CurrencyFormatter.parseValidOpeningBalance(upiText.ifBlank { "0" })
                    if (c == null || b == null || u == null) {
                        errorText = "कृपया सभी खातों में 0 या उससे अधिक सही रकम लिखें"
                    } else {
                        onSave(cashText, bankText, upiText)
                    }
                },
                modifier = Modifier.testTag("save_opening_balance_button")
            ) {
                Text("सेव करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun PinSetupDialog(
    isPinCurrentlyEnabled: Boolean,
    onDismiss: () -> Unit,
    onEnablePin: (String) -> Boolean,
    onDisablePin: () -> Unit
) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPinCurrentlyEnabled) "ऐप पिन बदलें / बंद करें" else "4-अंकों का ऐप पिन सेट करें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "पिन लॉक चालू करने से ऐप खोलते समय 4 अंकों का पिन पूछा जाएगा ताकि आपका हिसाब गोपनीय रहे।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin1,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { pin1 = it; errorText = null } },
                    label = { Text("नया 4-अंकीय पिन") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_setup_input")
                )
                OutlinedTextField(
                    value = pin2,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) { pin2 = it; errorText = null } },
                    label = { Text("पिन दोबारा लिखें") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_confirm_input")
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isPinCurrentlyEnabled) {
                    OutlinedButton(
                        onClick = onDisablePin,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("disable_pin_button")
                    ) {
                        Text("पिन बंद करें")
                    }
                }
                Button(
                    onClick = {
                        when {
                            !PinHashUtil.isValidPinFormat(pin1) -> errorText = "कृपया 4 अंकों का पिन दर्ज करें"
                            pin1 != pin2 -> errorText = "दोनों पिन एक जैसे होने चाहिए"
                            else -> onEnablePin(pin1)
                        }
                    },
                    modifier = Modifier.testTag("save_pin_button")
                ) {
                    Text("पिन सेव करें")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    transaction: HisabTransaction,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "एंट्री हटाएं?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "क्या आप वाकई इस एंट्री को हटाना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${transaction.partyName} • ${CurrencyFormatter.formatRupees(transaction.amount)} (${DateUtils.formatDateHindi(transaction.dateKey)})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("हाँ, हटाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun DeleteDebtConfirmDialog(
    debt: DebtEntry,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("उधार रिकॉर्ड हटाएं?") },
        text = {
            Text("${debt.personName} • ${CurrencyFormatter.formatRupees(debt.amount)} (${debt.type.labelHi}) को हटाना चाहते हैं?")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_debt_button")
            ) {
                Text("हाँ, हटाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}

@Composable
fun DeleteRecurringConfirmDialog(
    entry: RecurringEntry,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नियमित याददाश्त हटाएं?") },
        text = {
            Text("\"${entry.title}\" (${CurrencyFormatter.formatRupees(entry.amount)} • ${entry.frequency.labelHi}) को हटाना चाहते हैं?")
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_recurring_button")
            ) {
                Text("हाँ, हटाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("रद्द करें") }
        }
    )
}

@Composable
fun RestoreConfirmDialog(
    payload: BackupPayload,
    onDismiss: () -> Unit,
    onReplaceExisting: () -> Unit,
    onMergeWithExisting: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "बैकअप रीस्टोर करें",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "फ़ाइल में ${payload.transactions.size} हिसाब एंट्री, ${payload.debts.size} उधार रिकॉर्ड और ${payload.recurringEntries.size} नियमित याददाश्त मिली हैं।",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (payload.openingBalance != null) {
                    Text(
                        text = "कुल शुरुआती बैलेंस: ${CurrencyFormatter.formatRupees(payload.openingBalance)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (payload.skippedInvalidRows > 0) {
                    Text(
                        text = "नोट: ${payload.skippedInvalidRows} अमान्य पंक्तियाँ सुरक्षा के लिए छोड़ी जाएंगी।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = "आप पुराना हिसाब हटाकर नया डेटा लाना चाहते हैं या पुराने हिसाब में जोड़ना चाहते हैं?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onMergeWithExisting,
                    modifier = Modifier.testTag("restore_merge_button")
                ) {
                    Text("जोड़ें (Merge)")
                }
                Button(
                    onClick = onReplaceExisting,
                    modifier = Modifier.testTag("restore_replace_button")
                ) {
                    Text("बदलें (Replace)")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}

@Composable
fun ClearAllConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmClear: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("सारा डेटा साफ़ करें?") },
        text = {
            Text(
                "इससे आपकी सभी आमदनी, खर्च, ट्रांसफ़र, उधार, याददाश्त और शुरुआती बैलेंस हमेशा के लिए हट जाएंगे। चाहें तो पहले JSON या CSV बैकअप सेव कर लें।"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.testTag("confirm_clear_all_button")
            ) {
                Text("हाँ, सब हटाएं")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें")
            }
        }
    )
}
