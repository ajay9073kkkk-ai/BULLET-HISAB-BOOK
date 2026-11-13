package com.example.domain.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.domain.model.LedgerEntry
import com.example.domain.model.LedgerSummary
import com.example.domain.model.ReportSummary
import com.example.domain.model.TransactionType
import java.io.OutputStream
import java.util.Locale

object PdfReportExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    fun writeAccountReportPdf(
        outputStream: OutputStream,
        summary: LedgerSummary,
        report: ReportSummary,
        entries: List<LedgerEntry>,
        todayDateKey: Int
    ) {
        val nativeSuccess = tryWriteWithAndroidPdfDocument(
            outputStream = outputStream,
            summary = summary,
            report = report,
            entries = entries,
            todayDateKey = todayDateKey
        )
        if (!nativeSuccess) {
            writeStandardPdf14Fallback(
                outputStream = outputStream,
                summary = summary,
                report = report,
                entries = entries,
                todayDateKey = todayDateKey
            )
        }
    }

    private fun tryWriteWithAndroidPdfDocument(
        outputStream: OutputStream,
        summary: LedgerSummary,
        report: ReportSummary,
        entries: List<LedgerEntry>,
        todayDateKey: Int
    ): Boolean {
        val pdfDocument = PdfDocument()
        return try {
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E3A2F")
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#555555")
                textSize = 10f
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E3A2F")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F2421")
                textSize = 9.5f
            }
            val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F2421")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#157A46")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#C0392B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#D8D0C0")
                strokeWidth = 1f
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#F4EDE0")
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var y = 44f

            canvas.drawText("BULLET HISAB BOOK - Account Statement", MARGIN_X, y, titlePaint)
            y += 16f
            canvas.drawText(
                "Report Period: ${report.period.labelHi} (${report.selectedYear})  |  Generated: ${DateUtils.formatIsoDate(todayDateKey)}",
                MARGIN_X,
                y,
                subtitlePaint
            )
            y += 12f
            canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, linePaint)
            y += 18f

            canvas.drawRect(MARGIN_X, y - 12f, PAGE_WIDTH - MARGIN_X, y + 66f, headerBgPaint)
            canvas.drawText("Account Wealth Summary", MARGIN_X + 10f, y + 2f, sectionPaint)
            y += 18f
            canvas.drawText(
                "Total Wealth: ${CurrencyFormatter.formatRupees(summary.currentBalance)}   |   Opening: ${CurrencyFormatter.formatRupees(summary.openingBalance)}",
                MARGIN_X + 10f,
                y,
                boldBodyPaint
            )
            y += 15f
            val acctText = summary.accountBalances.joinToString("   |   ") {
                "${it.account.key}: ${CurrencyFormatter.formatRupees(it.currentBalance)}"
            }
            canvas.drawText(acctText, MARGIN_X + 10f, y, bodyPaint)
            y += 15f
            canvas.drawText(
                "Period Received: ${CurrencyFormatter.formatRupees(report.totalReceived)}   |   Spent: ${CurrencyFormatter.formatRupees(report.totalSpent)}   |   Savings: ${CurrencyFormatter.formatRupees(report.periodNet)}",
                MARGIN_X + 10f,
                y,
                bodyPaint
            )
            y += 28f

            fun drawTableHeader() {
                canvas.drawRect(MARGIN_X, y - 11f, PAGE_WIDTH - MARGIN_X, y + 7f, headerBgPaint)
                canvas.drawText("Date & Time", MARGIN_X + 4f, y, boldBodyPaint)
                canvas.drawText("Party / Notes", MARGIN_X + 95f, y, boldBodyPaint)
                canvas.drawText("Acct / Cat", MARGIN_X + 255f, y, boldBodyPaint)
                canvas.drawText("Amount", MARGIN_X + 365f, y, boldBodyPaint)
                canvas.drawText("Balance", MARGIN_X + 455f, y, boldBodyPaint)
                y += 16f
            }

            canvas.drawText("Chronological Ledger Entries (${entries.size})", MARGIN_X, y, sectionPaint)
            y += 18f
            drawTableHeader()

            val chronological = entries.reversed()
            for (entry in chronological) {
                if (y > PAGE_HEIGHT - 56f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 44f
                    canvas.drawText("BULLET HISAB BOOK (Page $pageNumber)", MARGIN_X, y, subtitlePaint)
                    y += 20f
                    drawTableHeader()
                }

                val tx = entry.transaction
                val dateTimeStr = "${DateUtils.formatIsoDate(tx.dateKey)} ${DateUtils.formatTimeMinutes(tx.timeMinutes)}"
                val partySummary = if (tx.notes.isNotBlank()) {
                    "${tx.partyName} (${tx.notes})".take(28)
                } else {
                    tx.partyName.take(28)
                }
                val acctCat = if (tx.type == TransactionType.TRANSFER) {
                    "${tx.account.key}->${tx.toAccount?.key ?: ""}"
                } else {
                    "${tx.account.key} / ${tx.displayCategoryLabel}".take(20)
                }
                val amtPaint = when (tx.type) {
                    TransactionType.INCOME -> incomePaint
                    TransactionType.EXPENSE -> expensePaint
                    TransactionType.TRANSFER -> boldBodyPaint
                }
                val amtText = when (tx.type) {
                    TransactionType.INCOME -> CurrencyFormatter.formatRupees(tx.amount, showPlusSign = true)
                    TransactionType.EXPENSE -> CurrencyFormatter.formatRupees(-tx.amount)
                    TransactionType.TRANSFER -> CurrencyFormatter.formatRupees(tx.amount)
                }

                canvas.drawText(dateTimeStr, MARGIN_X + 4f, y, bodyPaint)
                canvas.drawText(partySummary, MARGIN_X + 95f, y, bodyPaint)
                canvas.drawText(acctCat, MARGIN_X + 255f, y, bodyPaint)
                canvas.drawText(amtText, MARGIN_X + 365f, y, amtPaint)
                canvas.drawText(CurrencyFormatter.formatRupees(entry.balanceAfter), MARGIN_X + 455f, y, boldBodyPaint)

                y += 6f
                canvas.drawLine(MARGIN_X, y, MARGIN_X + CONTENT_WIDTH, y, linePaint)
                y += 14f
            }

            pdfDocument.finishPage(page)
            pdfDocument.writeTo(outputStream)
            true
        } catch (_: IllegalStateException) {
            false
        } catch (_: UnsatisfiedLinkError) {
            false
        } finally {
            runCatching { pdfDocument.close() }
        }
    }

    private fun writeStandardPdf14Fallback(
        outputStream: OutputStream,
        summary: LedgerSummary,
        report: ReportSummary,
        entries: List<LedgerEntry>,
        todayDateKey: Int
    ) {
        val lines = mutableListOf(
            "BULLET HISAB BOOK - Account Statement",
            "Generated: ${DateUtils.formatIsoDate(todayDateKey)} | Period: ${report.period.name} (${report.selectedYear})",
            "Total Wealth: INR ${String.format(Locale.US, "%.2f", summary.currentBalance)} | Opening: INR ${String.format(Locale.US, "%.2f", summary.openingBalance)}",
            "Accounts: " + summary.accountBalances.joinToString(" | ") {
                "${it.account.key}: INR ${String.format(Locale.US, "%.2f", it.currentBalance)}"
            },
            "Received: INR ${String.format(Locale.US, "%.2f", report.totalReceived)} | Spent: INR ${String.format(Locale.US, "%.2f", report.totalSpent)} | Savings: INR ${String.format(Locale.US, "%.2f", report.periodNet)}",
            "------------------------------------------------------------------------",
            "Date       Time     Type     Acct   Amount        Balance"
        )
        entries.reversed().take(42).forEach { entry ->
            val tx = entry.transaction
            val dateStr = DateUtils.formatIsoDate(tx.dateKey)
            val timeStr = DateUtils.formatTimeMinutes(tx.timeMinutes)
            val typeStr = tx.type.name.padEnd(8).take(8)
            val acctStr = tx.account.key.padEnd(6).take(6)
            val amtStr = String.format(Locale.US, "%.2f", tx.amount).padStart(10)
            val balStr = String.format(Locale.US, "%.2f", entry.balanceAfter).padStart(10)
            lines.add("$dateStr $timeStr $typeStr $acctStr $amtStr $balStr")
        }

        val streamContent = buildString {
            append("BT\n/F1 10 Tf\n36 800 Td\n14 TL\n")
            lines.forEachIndexed { idx, line ->
                val escaped = line
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace(Regex("[^\\x20-\\x7E]"), "?")
                if (idx == 0) {
                    append("($escaped) Tj\n")
                } else {
                    append("T* ($escaped) Tj\n")
                }
            }
            append("ET\n")
        }

        val streamBytes = streamContent.toByteArray(Charsets.US_ASCII)
        val objects = listOf(
            "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n",
            "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n",
            "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R /Resources << /Font << /F1 4 0 R >> >> >>\nendobj\n",
            "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n",
            "5 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n$streamContent\nendstream\nendobj\n"
        )

        val pdf = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        for (obj in objects) {
            offsets.add(pdf.length)
            pdf.append(obj)
        }
        val xrefOffset = pdf.length
        pdf.append("xref\n0 ${objects.size + 1}\n")
        pdf.append("0000000000 65535 f \n")
        for (i in 1..objects.size) {
            pdf.append(String.format(Locale.US, "%010d 00000 n \n", offsets[i]))
        }
        pdf.append("trailer\n<< /Size ${objects.size + 1} /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n")
        outputStream.write(pdf.toString().toByteArray(Charsets.US_ASCII))
    }
}
