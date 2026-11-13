package com.example.domain.util

import com.example.domain.model.RecurrenceFrequency
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val shortMonthsHi = arrayOf(
        "जन", "फर", "मार्च", "अप्रै", "मई", "जून",
        "जुला", "अग", "सित", "अक्टू", "नव", "दिस"
    )

    private val fullMonthsHi = arrayOf(
        "जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून",
        "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर"
    )

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return toDateKey(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun currentTimeMinutes(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatTimeMinutes(timeMinutes: Int): String {
        val clamped = timeMinutes.coerceIn(0, 1439)
        val hour24 = clamped / 60
        val minute = clamped % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    fun toDateKey(year: Int, month1Based: Int, day: Int): Int {
        return year * 10000 + month1Based * 100 + day
    }

    fun yearOf(dateKey: Int): Int = dateKey / 10000
    fun monthOf(dateKey: Int): Int = (dateKey / 100) % 100
    fun dayOf(dateKey: Int): Int = dateKey % 100

    fun monthKeyFromDateKey(dateKey: Int): Int = dateKey / 100 // yyyyMM

    fun dateKeyFromUtcPickerMillis(utcMillis: Long): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        return toDateKey(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun utcPickerMillisFromDateKey(dateKey: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, yearOf(dateKey))
            set(Calendar.MONTH, (monthOf(dateKey) - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, dayOf(dateKey).coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatDateHindi(dateKey: Int): String {
        val y = yearOf(dateKey)
        val m = monthOf(dateKey)
        val d = dayOf(dateKey)
        if (m !in 1..12 || d !in 1..31) return dateKey.toString()
        return "$d ${shortMonthsHi[m - 1]} $y"
    }

    fun formatShortDayMonth(dateKey: Int): String {
        val m = monthOf(dateKey)
        val d = dayOf(dateKey)
        if (m !in 1..12) return dateKey.toString()
        return "$d ${shortMonthsHi[m - 1]}"
    }

    fun formatMonthYearHindi(monthKey: Int): String {
        val y = monthKey / 100
        val m = monthKey % 100
        if (m !in 1..12) return monthKey.toString()
        return "${fullMonthsHi[m - 1]} $y"
    }

    fun formatShortMonthHindi(monthKey: Int): String {
        val m = monthKey % 100
        if (m !in 1..12) return monthKey.toString()
        return shortMonthsHi[m - 1]
    }

    fun formatShortMonthIndexHindi(month1Based: Int): String {
        if (month1Based !in 1..12) return month1Based.toString()
        return shortMonthsHi[month1Based - 1]
    }

    fun formatIsoDate(dateKey: Int): String {
        val y = yearOf(dateKey)
        val m = monthOf(dateKey)
        val d = dayOf(dateKey)
        return "%04d-%02d-%02d".format(Locale.US, y, m, d)
    }

    fun parseIsoOrFlexibleDateKey(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val isoMatch = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").matchEntire(trimmed)
        if (isoMatch != null) {
            val y = isoMatch.groupValues[1].toIntOrNull() ?: return null
            val m = isoMatch.groupValues[2].toIntOrNull() ?: return null
            val d = isoMatch.groupValues[3].toIntOrNull() ?: return null
            if (y in 1970..2100 && m in 1..12 && d in 1..31) return toDateKey(y, m, d)
        }
        val compact = trimmed.toIntOrNull()
        if (compact != null && compact in 19700101..21001231) {
            val m = monthOf(compact)
            val d = dayOf(compact)
            if (m in 1..12 && d in 1..31) return compact
        }
        return null
    }

    fun shiftDays(dateKey: Int, deltaDays: Int): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcPickerMillisFromDateKey(dateKey)
            add(Calendar.DAY_OF_MONTH, deltaDays)
        }
        return toDateKey(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun shiftMonths(monthKey: Int, deltaMonths: Int): Int {
        val y = monthKey / 100
        val m = (monthKey % 100) - 1
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1)
    }

    fun advanceRecurringDate(dateKey: Int, frequency: RecurrenceFrequency): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcPickerMillisFromDateKey(dateKey)
            when (frequency) {
                RecurrenceFrequency.DAILY -> add(Calendar.DAY_OF_MONTH, 1)
                RecurrenceFrequency.WEEKLY -> add(Calendar.DAY_OF_MONTH, 7)
                RecurrenceFrequency.MONTHLY -> add(Calendar.MONTH, 1)
                RecurrenceFrequency.YEARLY -> add(Calendar.YEAR, 1)
            }
        }
        return toDateKey(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
