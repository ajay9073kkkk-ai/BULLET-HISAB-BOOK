package com.example.domain.util

import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurringEntry
import com.example.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val openingBalance: Double?,
    val accountOpenings: AccountOpeningBalances? = null,
    val transactions: List<HisabTransaction>,
    val debts: List<DebtEntry> = emptyList(),
    val recurringEntries: List<RecurringEntry> = emptyList(),
    val skippedInvalidRows: Int = 0
)

object BackupSerializer {
    private const val JSON_VERSION = 2

    fun toJson(
        openingBalance: Double,
        transactions: List<HisabTransaction>
    ): String {
        return toJson(
            accountOpenings = AccountOpeningBalances(cash = openingBalance),
            transactions = transactions,
            debts = emptyList(),
            recurringEntries = emptyList()
        )
    }

    fun toJson(
        accountOpenings: AccountOpeningBalances,
        transactions: List<HisabTransaction>,
        debts: List<DebtEntry> = emptyList(),
        recurringEntries: List<RecurringEntry> = emptyList()
    ): String {
        val root = JSONObject()
        root.put("app", "Bullet Hisab Book")
        root.put("version", JSON_VERSION)
        root.put("exportedAtMillis", System.currentTimeMillis())
        root.put("openingBalance", accountOpenings.total)

        val openingsObj = JSONObject().apply {
            put("cash", accountOpenings.cash)
            put("bank", accountOpenings.bank)
            put("upi", accountOpenings.upi)
        }
        root.put("accountOpenings", openingsObj)

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("dateKey", tx.dateKey)
            obj.put("dateIso", DateUtils.formatIsoDate(tx.dateKey))
            obj.put("timeMinutes", tx.timeMinutes)
            obj.put("account", tx.account.key)
            if (tx.toAccount != null) {
                obj.put("toAccount", tx.toAccount.key)
            }
            obj.put("partyName", tx.partyName)
            obj.put(
                "category",
                when (tx.type) {
                    TransactionType.EXPENSE -> (tx.category ?: ExpenseCategory.OTHER).key
                    TransactionType.INCOME -> (tx.incomeCategory ?: IncomeCategory.OTHER).key
                    TransactionType.TRANSFER -> "TRANSFER"
                }
            )
            obj.put("notes", tx.notes)
            obj.put("createdAtMillis", tx.createdAtMillis)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val debtArray = JSONArray()
        for (debt in debts) {
            val obj = JSONObject()
            obj.put("type", debt.type.name)
            obj.put("personName", debt.personName)
            obj.put("amount", debt.amount)
            obj.put("dateKey", debt.dateKey)
            if (debt.dueDateKey != null) obj.put("dueDateKey", debt.dueDateKey)
            obj.put("status", debt.status.name)
            if (debt.settledDateKey != null) obj.put("settledDateKey", debt.settledDateKey)
            obj.put("notes", debt.notes)
            obj.put("createdAtMillis", debt.createdAtMillis)
            debtArray.put(obj)
        }
        root.put("debts", debtArray)

        val recArray = JSONArray()
        for (rec in recurringEntries) {
            val obj = JSONObject()
            obj.put("title", rec.title)
            obj.put("type", rec.type.name)
            obj.put("amount", rec.amount)
            obj.put("account", rec.account.key)
            obj.put(
                "category",
                if (rec.type == TransactionType.EXPENSE) {
                    (rec.expenseCategory ?: ExpenseCategory.OTHER).key
                } else {
                    (rec.incomeCategory ?: IncomeCategory.OTHER).key
                }
            )
            obj.put("frequency", rec.frequency.key)
            obj.put("nextDueDateKey", rec.nextDueDateKey)
            obj.put("notes", rec.notes)
            obj.put("isActive", rec.isActive)
            obj.put("createdAtMillis", rec.createdAtMillis)
            recArray.put(obj)
        }
        root.put("recurringEntries", recArray)

        return root.toString(2)
    }

    fun fromJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson)
        val hasAppHeader = root.has("transactions") || root.has("openingBalance") || root.has("accountOpenings")
        require(hasAppHeader) { "Invalid backup schema" }

        val accountOpeningsObj = root.optJSONObject("accountOpenings")
        val accountOpenings = if (accountOpeningsObj != null) {
            val cash = accountOpeningsObj.optDouble("cash", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            val bank = accountOpeningsObj.optDouble("bank", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            val upi = accountOpeningsObj.optDouble("upi", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            AccountOpeningBalances(cash = cash, bank = bank, upi = upi)
        } else {
            null
        }

        val opening = accountOpenings?.total ?: if (root.has("openingBalance") && !root.isNull("openingBalance")) {
            root.optDouble("openingBalance", Double.NaN).takeIf { it.isFinite() && it >= 0.0 }
        } else {
            null
        }

        var skipped = 0
        val txArray = root.optJSONArray("transactions") ?: JSONArray()
        val txList = mutableListOf<HisabTransaction>()
        for (i in 0 until txArray.length()) {
            val obj = txArray.optJSONObject(i)
            if (obj == null) {
                skipped++
                continue
            }
            val typeStr = obj.optString("type", "")
            val type = when {
                typeStr.equals("INCOME", ignoreCase = true) -> TransactionType.INCOME
                typeStr.equals("EXPENSE", ignoreCase = true) -> TransactionType.EXPENSE
                typeStr.equals("TRANSFER", ignoreCase = true) -> TransactionType.TRANSFER
                else -> {
                    skipped++
                    continue
                }
            }
            val amount = obj.optDouble("amount", Double.NaN)
            if (!amount.isFinite() || amount <= 0.0 || amount > 99_99_99_999.0) {
                skipped++
                continue
            }
            val dateKey = obj.optInt("dateKey", 0).takeIf { it in 19700101..21001231 }
                ?: DateUtils.parseIsoOrFlexibleDateKey(obj.optString("dateIso", ""))
            if (dateKey == null) {
                skipped++
                continue
            }

            val timeMinutes = obj.optInt("timeMinutes", 720).coerceIn(0, 1439)
            val account = PaymentAccount.fromKeyOrLabel(obj.optString("account", "CASH"))
            val toAccount = if (type == TransactionType.TRANSFER) {
                val dest = PaymentAccount.fromKeyOrLabel(obj.optString("toAccount", ""))
                if (dest == account) {
                    skipped++
                    continue
                }
                dest
            } else {
                null
            }

            val partyName = obj.optString("partyName", "").trim().ifBlank { type.labelHi }
            val notes = obj.optString("notes", "").trim()
            val catStr = obj.optString("category", "")
            val expenseCategory = if (type == TransactionType.EXPENSE) {
                ExpenseCategory.fromKeyOrLabel(catStr)
            } else null
            val incomeCategory = if (type == TransactionType.INCOME) {
                IncomeCategory.fromKeyOrLabel(catStr)
            } else null
            val createdAt = obj.optLong("createdAtMillis", System.currentTimeMillis())

            val candidate = HisabTransaction(
                id = 0L,
                type = type,
                amount = amount,
                dateKey = dateKey,
                partyName = partyName,
                category = expenseCategory,
                notes = notes,
                createdAtMillis = createdAt,
                timeMinutes = timeMinutes,
                account = account,
                toAccount = toAccount,
                incomeCategory = incomeCategory
            )
            if (!HisabCalculator.isDuplicateTransaction(candidate, txList)) {
                txList.add(candidate)
            }
        }

        val debtArray = root.optJSONArray("debts") ?: JSONArray()
        val debtList = mutableListOf<DebtEntry>()
        for (i in 0 until debtArray.length()) {
            val obj = debtArray.optJSONObject(i) ?: continue
            val type = when {
                obj.optString("type").equals("BORROWED", ignoreCase = true) -> DebtType.BORROWED
                obj.optString("type").equals("LENT", ignoreCase = true) -> DebtType.LENT
                else -> continue
            }
            val person = obj.optString("personName", "").trim()
            if (person.isBlank()) continue
            val amount = obj.optDouble("amount", Double.NaN)
            if (!amount.isFinite() || amount <= 0.0) continue
            val dateKey = obj.optInt("dateKey", 0).takeIf { it in 19700101..21001231 } ?: continue
            val dueDate = obj.optInt("dueDateKey", 0).takeIf { it in 19700101..21001231 }
            val status = if (obj.optString("status").equals("SETTLED", ignoreCase = true)) {
                DebtStatus.SETTLED
            } else DebtStatus.PENDING
            val settledDate = obj.optInt("settledDateKey", 0).takeIf { it in 19700101..21001231 }
            debtList.add(
                DebtEntry(
                    id = 0L,
                    type = type,
                    personName = person,
                    amount = amount,
                    dateKey = dateKey,
                    dueDateKey = dueDate,
                    status = status,
                    settledDateKey = settledDate,
                    notes = obj.optString("notes", "").trim(),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }

        val recArray = root.optJSONArray("recurringEntries") ?: JSONArray()
        val recList = mutableListOf<RecurringEntry>()
        for (i in 0 until recArray.length()) {
            val obj = recArray.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) continue
            val type = when {
                obj.optString("type").equals("INCOME", ignoreCase = true) -> TransactionType.INCOME
                obj.optString("type").equals("EXPENSE", ignoreCase = true) -> TransactionType.EXPENSE
                else -> continue
            }
            val amount = obj.optDouble("amount", Double.NaN)
            if (!amount.isFinite() || amount <= 0.0) continue
            val nextDue = obj.optInt("nextDueDateKey", 0).takeIf { it in 19700101..21001231 } ?: continue
            val catStr = obj.optString("category", "")
            recList.add(
                RecurringEntry(
                    id = 0L,
                    title = title,
                    type = type,
                    amount = amount,
                    account = PaymentAccount.fromKeyOrLabel(obj.optString("account", "CASH")),
                    expenseCategory = if (type == TransactionType.EXPENSE) ExpenseCategory.fromKeyOrLabel(catStr) else null,
                    incomeCategory = if (type == TransactionType.INCOME) IncomeCategory.fromKeyOrLabel(catStr) else null,
                    frequency = RecurrenceFrequency.fromKey(obj.optString("frequency", "MONTHLY")),
                    nextDueDateKey = nextDue,
                    notes = obj.optString("notes", "").trim(),
                    isActive = obj.optBoolean("isActive", true),
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }

        return BackupPayload(
            openingBalance = opening,
            accountOpenings = accountOpenings ?: opening?.let { AccountOpeningBalances(cash = it) },
            transactions = txList,
            debts = debtList,
            recurringEntries = recList,
            skippedInvalidRows = skipped
        )
    }

    fun toCsv(openingBalance: Double, transactions: List<HisabTransaction>): String {
        return toCsv(AccountOpeningBalances(cash = openingBalance), transactions)
    }

    fun toCsv(
        accountOpenings: AccountOpeningBalances,
        transactions: List<HisabTransaction>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("#BULLET_HISAB_BOOK_OPENING_BALANCE,${accountOpenings.total}")
        sb.appendLine("#BULLET_HISAB_BOOK_ACCOUNTS,${accountOpenings.cash},${accountOpenings.bank},${accountOpenings.upi}")
        sb.appendLine("date,time,type,account,to_account,category,party_name,amount,notes,created_at_millis")
        for (tx in transactions.sortedWith(compareBy({ it.dateKey }, { it.timeMinutes }, { it.createdAtMillis }))) {
            val fields = listOf(
                DateUtils.formatIsoDate(tx.dateKey),
                DateUtils.formatTimeMinutes(tx.timeMinutes),
                tx.type.name,
                tx.account.key,
                tx.toAccount?.key ?: "",
                tx.displayCategoryLabel,
                tx.partyName,
                tx.amount.toString(),
                tx.notes,
                tx.createdAtMillis.toString()
            )
            sb.appendLine(fields.joinToString(",") { escapeCsv(it) })
        }
        return sb.toString()
    }

    fun fromCsv(rawCsv: String): BackupPayload {
        val lines = rawCsv.lines().filter { it.isNotBlank() }
        var openingBalance: Double? = null
        var accountOpenings: AccountOpeningBalances? = null
        val list = mutableListOf<HisabTransaction>()
        var isV2Format = false
        var skipped = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#BULLET_HISAB_BOOK_OPENING_BALANCE,")) {
                val rawBal = trimmed.substringAfter(",").trim().toDoubleOrNull()
                if (rawBal != null && rawBal.isFinite() && rawBal >= 0.0) {
                    openingBalance = rawBal
                }
                continue
            }
            if (trimmed.startsWith("#BULLET_HISAB_BOOK_ACCOUNTS,")) {
                val parts = trimmed.substringAfter(",").split(",")
                if (parts.size >= 3) {
                    val cash = parts[0].trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    val bank = parts[1].trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    val upi = parts[2].trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                    accountOpenings = AccountOpeningBalances(cash, bank, upi)
                    openingBalance = accountOpenings.total
                }
                continue
            }
            if (trimmed.startsWith("date,time,type,", ignoreCase = true)) {
                isV2Format = true
                continue
            }
            if (trimmed.startsWith("date,type,", ignoreCase = true)) {
                isV2Format = false
                continue
            }

            val cols = parseCsvLine(line)
            if (isV2Format || cols.size >= 8) {
                if (cols.size < 8) {
                    skipped++
                    continue
                }
                val dateKey = DateUtils.parseIsoOrFlexibleDateKey(cols[0])
                val timeMinutes = parseTimeMinutes(cols[1]) ?: 720
                val type = parseTransactionType(cols[2])
                val account = PaymentAccount.fromKeyOrLabel(cols[3])
                val toAccount = if (type == TransactionType.TRANSFER) {
                    PaymentAccount.fromKeyOrLabel(cols[4]).takeIf { it != account }
                } else null
                val catRaw = cols[5].trim()
                val partyName = cols[6].trim()
                val amount = cols[7].trim().toDoubleOrNull()
                if (dateKey == null || type == null || amount == null || !amount.isFinite() || amount <= 0.0 ||
                    (type == TransactionType.TRANSFER && toAccount == null)
                ) {
                    skipped++
                    continue
                }
                val notes = cols.getOrNull(8)?.trim().orEmpty()
                val createdAt = cols.getOrNull(9)?.trim()?.toLongOrNull() ?: System.currentTimeMillis()
                val tx = HisabTransaction(
                    id = 0L,
                    type = type,
                    amount = amount,
                    dateKey = dateKey,
                    partyName = partyName.ifBlank { type.labelHi },
                    category = if (type == TransactionType.EXPENSE) ExpenseCategory.fromKeyOrLabel(catRaw) else null,
                    notes = notes,
                    createdAtMillis = createdAt,
                    timeMinutes = timeMinutes,
                    account = account,
                    toAccount = toAccount,
                    incomeCategory = if (type == TransactionType.INCOME) IncomeCategory.fromKeyOrLabel(catRaw) else null
                )
                if (!HisabCalculator.isDuplicateTransaction(tx, list)) {
                    list.add(tx)
                }
            } else {
                // Backward compatible v1 CSV: date,type,category,party_name,amount,notes,created_at_millis
                if (cols.size < 5) {
                    skipped++
                    continue
                }
                val dateKey = DateUtils.parseIsoOrFlexibleDateKey(cols[0])
                val type = parseTransactionType(cols[1])
                val catRaw = cols[2].trim()
                val partyName = cols[3].trim()
                val amount = cols[4].trim().toDoubleOrNull()
                if (dateKey == null || type == null || amount == null || !amount.isFinite() || amount <= 0.0) {
                    skipped++
                    continue
                }
                val notes = cols.getOrNull(5)?.trim().orEmpty()
                val createdAt = cols.getOrNull(6)?.trim()?.toLongOrNull() ?: System.currentTimeMillis()
                val tx = HisabTransaction(
                    id = 0L,
                    type = type,
                    amount = amount,
                    dateKey = dateKey,
                    partyName = partyName.ifBlank { type.labelHi },
                    category = if (type == TransactionType.EXPENSE) ExpenseCategory.fromKeyOrLabel(catRaw) else null,
                    notes = notes,
                    createdAtMillis = createdAt
                )
                if (!HisabCalculator.isDuplicateTransaction(tx, list)) {
                    list.add(tx)
                }
            }
        }
        return BackupPayload(
            openingBalance = openingBalance,
            accountOpenings = accountOpenings ?: openingBalance?.let { AccountOpeningBalances(cash = it) },
            transactions = list,
            skippedInvalidRows = skipped
        )
    }

    private fun parseTransactionType(raw: String): TransactionType? {
        val t = raw.trim()
        return when {
            t.equals("INCOME", ignoreCase = true) || t == TransactionType.INCOME.labelHi -> TransactionType.INCOME
            t.equals("EXPENSE", ignoreCase = true) || t == TransactionType.EXPENSE.labelHi -> TransactionType.EXPENSE
            t.equals("TRANSFER", ignoreCase = true) || t == TransactionType.TRANSFER.labelHi -> TransactionType.TRANSFER
            else -> null
        }
    }

    private fun parseTimeMinutes(raw: String): Int? {
        val t = raw.trim().uppercase()
        val amPmMatch = Regex("""^(\d{1,2}):(\d{2})\s*(AM|PM)$""").matchEntire(t)
        if (amPmMatch != null) {
            var hour = amPmMatch.groupValues[1].toIntOrNull() ?: return null
            val min = amPmMatch.groupValues[2].toIntOrNull() ?: return null
            val period = amPmMatch.groupValues[3]
            if (hour !in 1..12 || min !in 0..59) return null
            if (period == "AM" && hour == 12) hour = 0
            if (period == "PM" && hour != 12) hour += 12
            return hour * 60 + min
        }
        val h24Match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(t)
        if (h24Match != null) {
            val hour = h24Match.groupValues[1].toIntOrNull() ?: return null
            val min = h24Match.groupValues[2].toIntOrNull() ?: return null
            if (hour in 0..23 && min in 0..59) return hour * 60 + min
        }
        return null
    }

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuote) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    current.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        result.add(current.toString())
                        current.clear()
                    }
                    else -> current.append(c)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
