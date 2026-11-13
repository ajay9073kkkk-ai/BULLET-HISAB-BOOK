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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.domain.model.ReportPeriod
import com.example.domain.util.CurrencyFormatter
import com.example.ui.components.BulletSectionHeader
import com.example.ui.components.CategoryBreakdownList
import com.example.ui.components.IncomeVsExpenseBarChart
import com.example.ui.components.NotebookRuledCard
import com.example.ui.components.bulletGridBackground
import com.example.ui.state.HisabUiState
import com.example.ui.theme.LedgerNumberFont
import com.example.ui.theme.LocalLedgerColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: HisabUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onShiftYear: (Int) -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ledgerColors = LocalLedgerColors.current
    val report = uiState.reportSummary

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
                BulletSectionHeader(title = "हिसाब रिपोर्ट और चार्ट")
            }

            // Period Selector (Daily / Weekly / Monthly / Yearly)
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ReportPeriod.entries.forEachIndexed { index, period ->
                        SegmentedButton(
                            selected = uiState.reportPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ReportPeriod.entries.size
                            ),
                            modifier = Modifier.testTag("report_period_${period.name.lowercase()}")
                        ) {
                            Text(
                                text = period.labelHi,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Year Selector when Yearly report is active
            if (uiState.reportPeriod == ReportPeriod.YEARLY) {
                item {
                    NotebookRuledCard(showMarginRule = false) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onShiftYear(-1) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("report_prev_year_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                    contentDescription = "पिछला साल"
                                )
                            }
                            Text(
                                text = "साल ${uiState.reportSelectedYear} का वार्षिक हिसाब",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { onShiftYear(1) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("report_next_year_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = "अगला साल"
                                )
                            }
                        }
                    }
                }
            }

            // Summary Cards: Total Received, Total Spent, Period Savings & Overall Balance
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricCard(
                            title = "कुल प्राप्त",
                            amount = report.totalReceived,
                            containerColor = ledgerColors.incomeContainer,
                            contentColor = ledgerColors.income,
                            tag = "report_total_received",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "कुल खर्च",
                            amount = report.totalSpent,
                            containerColor = ledgerColors.expenseContainer,
                            contentColor = ledgerColors.expense,
                            tag = "report_total_spent",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportMetricCard(
                            title = "${report.period.labelHi} बचत",
                            amount = report.periodNet,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (report.periodNet >= 0) ledgerColors.income else ledgerColors.expense,
                            tag = "report_period_net",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricCard(
                            title = "कुल संपत्ति शेष",
                            amount = report.remainingOverallBalance,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            tag = "report_overall_balance",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Export Report Actions (PDF & CSV)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reports_export_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF डाउनलोड", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reports_export_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CSV डाउनलोड", maxLines = 1)
                    }
                }
            }

            // Income vs Expense Chart
            item {
                val periodDesc = when (report.period) {
                    ReportPeriod.DAILY -> "पिछले 7 दिन — आमदनी बनाम खर्च"
                    ReportPeriod.WEEKLY -> "पिछले 4 सप्ताह — आमदनी बनाम खर्च"
                    ReportPeriod.MONTHLY -> "पिछले 6 महीने — आमदनी बनाम खर्च"
                    ReportPeriod.YEARLY -> "साल ${report.selectedYear} — मासिक आमदनी बनाम खर्च"
                }
                NotebookRuledCard(showMarginRule = false) {
                    Text(
                        text = periodDesc,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    val hasChartData = report.buckets.any { it.income > 0.0 || it.expense > 0.0 }
                    if (hasChartData) {
                        IncomeVsExpenseBarChart(buckets = report.buckets)
                    } else {
                        Text(
                            text = "इस अवधि में कोई आमदनी या खर्च दर्ज नहीं है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                }
            }

            // Category Breakdown
            item {
                NotebookRuledCard(showMarginRule = false) {
                    Text(
                        text = "श्रेणी के अनुसार खर्च (${report.period.labelHi})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (report.categoryTotals.isEmpty()) {
                        Text(
                            text = "इस अवधि में अभी कोई खर्च नहीं लिखा गया है।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                        )
                    } else {
                        CategoryBreakdownList(items = report.categoryTotals)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    amount: Double,
    containerColor: Color,
    contentColor: Color,
    tag: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.85f)
            )
            Text(
                text = CurrencyFormatter.formatRupees(amount),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = LedgerNumberFont,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                modifier = Modifier.testTag(tag)
            )
        }
    }
}
