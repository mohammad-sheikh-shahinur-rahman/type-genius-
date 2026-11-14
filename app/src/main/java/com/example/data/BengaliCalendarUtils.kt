package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BanglaDateInfo(
    val day: Int,
    val monthIndex: Int, // 0..11 (বৈশাখ..চৈত্র)
    val monthName: String,
    val year: Int,
    val weekdayName: String,
    val seasonPeriodBn: String
) {
    val formattedFull: String
        get() = "${BengaliCalendarUtils.toBengaliDigits(day.toString())} $monthName ${BengaliCalendarUtils.toBengaliDigits(year.toString())} বঙ্গাব্দ"

    val formattedWithWeekday: String
        get() = "$weekdayName, $formattedFull"

    val formattedShort: String
        get() {
            val d = BengaliCalendarUtils.toBengaliDigits(day.toString().padStart(2, '0'))
            val m = BengaliCalendarUtils.toBengaliDigits((monthIndex + 1).toString().padStart(2, '0'))
            val y = BengaliCalendarUtils.toBengaliDigits(year.toString())
            return "$d/$m/$y বঙ্গাব্দ"
        }
}

object BengaliCalendarUtils {

    val BANGLA_MONTHS = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    val BANGLA_WEEKDAYS = mapOf(
        Calendar.SATURDAY to "শনিবার",
        Calendar.SUNDAY to "রবিবার",
        Calendar.MONDAY to "সোমবার",
        Calendar.TUESDAY to "মঙ্গলবার",
        Calendar.WEDNESDAY to "বুধবার",
        Calendar.THURSDAY to "বৃহস্পতিবার",
        Calendar.FRIDAY to "শুক্রবার"
    )

    fun toBengaliDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return buildString(input.length) {
            for (c in input) {
                if (c in '0'..'9') append(bnDigits[c - '0']) else append(c)
            }
        }
    }

    private fun isGregorianLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Converts a Gregorian [Date] to Revised Bangla Academy Calendar (বঙ্গাব্দ):
     * - ১ বৈশাখ = 14 April
     * - বৈশাখ–ভাদ্র (5 months) = 31 days each (revised rule; আশ্বিন = 31 in 2019+ revision, ফাল্গুন = 29 or 30 in leap year)
     * - Specifically (Bangla Academy 2019 revision used in Bangladesh):
     *   বৈশাখ(31), জ্যৈষ্ঠ(31), আষাঢ়(31), শ্রাবণ(31), ভাদ্র(31), আশ্বিন(31),
     *   কার্তিক(30), অগ্রহায়ণ(30), পৌষ(30), মাঘ(30), ফাল্গুন(29/30), চৈত্র(30).
     */
    fun toBanglaDate(date: Date = Date()): BanglaDateInfo {
        val cal = Calendar.getInstance().apply { time = date }
        val gYear = cal.get(Calendar.YEAR)
        val gMonth = cal.get(Calendar.MONTH) + 1 // 1..12
        val gDay = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = BANGLA_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)] ?: ""

        // Start of Bangla year for this Gregorian date: April 14 of gYear (or gYear - 1 if before April 14)
        val pohelaYear = if (gMonth > 4 || (gMonth == 4 && gDay >= 14)) gYear else gYear - 1
        val banglaYear = pohelaYear - 593

        val pohelaCal = Calendar.getInstance().apply {
            clear()
            set(pohelaYear, Calendar.APRIL, 14)
        }
        val targetCal = Calendar.getInstance().apply {
            clear()
            set(gYear, gMonth - 1, gDay)
        }

        val diffDays = kotlin.math.round((targetCal.timeInMillis - pohelaCal.timeInMillis) / 86_400_000.0).toInt()
        val falgunDays = if (isGregorianLeapYear(pohelaYear + 1)) 30 else 29
        val monthLengths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, falgunDays, 30)

        var remaining = diffDays.coerceAtLeast(0)
        var monthIdx = 0
        while (monthIdx < 11 && remaining >= monthLengths[monthIdx]) {
            remaining -= monthLengths[monthIdx]
            monthIdx++
        }
        val banglaDay = remaining + 1

        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val period = when (hour24) {
            in 4..5 -> "ভোর"
            in 6..11 -> "সকাল"
            in 12..14 -> "দুপুর"
            in 15..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }

        return BanglaDateInfo(
            day = banglaDay,
            monthIndex = monthIdx,
            monthName = BANGLA_MONTHS[monthIdx],
            year = banglaYear,
            weekdayName = weekday,
            seasonPeriodBn = period
        )
    }

    fun formatBanglaTime(date: Date = Date()): String {
        val cal = Calendar.getInstance().apply { time = date }
        val info = toBanglaDate(date)
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val minute = cal.get(Calendar.MINUTE)
        val hh = toBengaliDigits(hour12.toString().padStart(2, '0'))
        val mm = toBengaliDigits(minute.toString().padStart(2, '0'))
        return "${info.seasonPeriodBn} $hh:$mm"
    }

    fun formatEnglishDate(date: Date = Date()): String =
        SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)

    fun formatIsoDate(date: Date = Date()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)

    fun formatEnglishTime(date: Date = Date()): String =
        SimpleDateFormat("hh:mm a", Locale.US).format(date)

    fun formatEnglishWeekday(date: Date = Date()): String =
        SimpleDateFormat("EEEE", Locale.US).format(date)
}
