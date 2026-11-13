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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecurringEntry
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.ui.components.BulletSectionHeader
import com.example.ui.components.ClearAllConfirmDialog
import com.example.ui.components.NotebookRuledCard
import com.example.ui.components.bulletGridBackground
import com.example.ui.state.HisabUiState
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@Composable
fun SettingsScreen(
    uiState: HisabUiState,
    onEditOpeningBalance: () -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (RecurringEntry) -> Unit,
    onToggleRecurringActive: (RecurringEntry) -> Unit,
    onDeleteRecurring: (RecurringEntry) -> Unit,
    onRecordRecurring: (RecurringEntry) -> Unit,
    onOpenPinDialog: () -> Unit,
    onLockAppNow: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onImportBackup: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    var showClearDialog by remember { mutableStateOf(false) }

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
            item {
                BulletSectionHeader(title = "सेटिंग्स, याददाश्त और सुरक्षा")
            }

            // Opening Balances Section (Cash, Bank, UPI)
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ledgerColors.brass,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "खातों का शुरुआती बैलेंस",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.formatRupees(uiState.summary.openingBalance),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = LedgerNumberFont,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            val openings = uiState.summary.accountOpenings
                            Text(
                                text = "नकद ${CurrencyFormatter.formatRupees(openings.cash)} • बैंक ${CurrencyFormatter.formatRupees(openings.bank)} • UPI ${CurrencyFormatter.formatRupees(openings.upi)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = onEditOpeningBalance,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("settings_opening_balance_button")
                        ) {
                            Text("बदलें")
                        }
                    }
                }
            }

            // Recurring Income & Expense Reminders Section
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = ledgerColors.brass,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "नियमित याददाश्त (${uiState.recurringEntries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = onAddRecurring,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_recurring_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("जोड़ें")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "किराया, वेतन, बिजली बिल या रिचार्ज जैसी नियमित एंट्री की याददाश्त सेट करें।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.recurringEntries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.recurringEntries.forEach { entry ->
                                HorizontalDivider(color = ledgerColors.ruledLine)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${entry.title} (${entry.type.labelHi})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatRupees(entry.amount)} • ${entry.frequency.labelHi} • अगली: ${DateUtils.formatDateHindi(entry.nextDueDateKey)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = entry.isActive,
                                        onCheckedChange = { onToggleRecurringActive(entry) }
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onRecordRecurring(entry) }) {
                                        Text("अभी दर्ज करें")
                                    }
                                    IconButton(
                                        onClick = { onEditRecurring(entry) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "बदलें")
                                    }
                                    IconButton(
                                        onClick = { onDeleteRecurring(entry) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            Icons.Outlined.DeleteOutline,
                                            contentDescription = "हटाएं",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Privacy & App PIN Lock Section
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ledgerColors.income,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "प्राइवेसी और ऐप पिन लॉक",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (uiState.isPinEnabled) {
                            "4-अंकीय पिन लॉक चालू है। आपका पूरा हिसाब केवल आपके फ़ोन में सुरक्षित रहता है।"
                        } else {
                            "अपने हिसाब को सुरक्षित रखने के लिए 4-अंकीय ऐप पिन लॉक चालू कर सकते हैं। कोई लॉगिन जरूरी नहीं है।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onOpenPinDialog,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manage_pin_button")
                        ) {
                            Text(if (uiState.isPinEnabled) "पिन बदलें / बंद करें" else "पिन लॉक सेट करें")
                        }
                        if (uiState.isPinEnabled) {
                            OutlinedButton(
                                onClick = onLockAppNow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("lock_now_button")
                            ) {
                                Text("अभी लॉक करें")
                            }
                        }
                    }
                }
            }

            // Backup, Export (PDF/CSV/JSON) & Restore Section
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "बैकअप, PDF और रीस्टोर",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "अपना पूरा हिसाब JSON बैकअप, CSV (Excel) या PDF स्टेटमेंट में डाउनलोड करें, या बैकअप फ़ाइल से वापस लाएं।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExportJson,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_json_button")
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("JSON पूरा बैकअप डाउनलोड करें")
                        }

                        OutlinedButton(
                            onClick = onExportPdf,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_pdf_button")
                        ) {
                            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF अकाउंट रिपोर्ट डाउनलोड करें")
                        }

                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CSV (Excel) डाउनलोड करें")
                        }

                        OutlinedButton(
                            onClick = onImportBackup,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_backup_button")
                        ) {
                            Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("बैकअप से रीस्टोर करें (JSON / CSV)")
                        }
                    }
                }
            }

            // Clear Data Section
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "नई शुरुआत / डेटा साफ़ करें",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "यदि आप पूरी हिसाब डायरी खाली करना चाहते हैं, तो नीचे दिए बटन का उपयोग करें।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_data_button")
                    ) {
                        Text("सभी डेटा साफ़ करें")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showClearDialog) {
        ClearAllConfirmDialog(
            onDismiss = { showClearDialog = false },
            onConfirmClear = {
                showClearDialog = false
                onClearAllData()
            }
        )
    }
}
