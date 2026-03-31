package com.mosque.prayer.utils

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.floor

object HijriCalendarUtils {

    data class HijriDate(val day: Int, val month: Int, val year: Int)

    // Kuwaiti algorithm (tabular/civil approximation) - offline.
    fun gregorianToHijri(date: Date, tz: TimeZone): HijriDate {
        val cal = GregorianCalendar(tz)
        cal.time = date
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)

        val jd = gregorianToJulianDay(y, m, d)
        return julianDayToHijri(jd)
    }

    fun hijriMonthNameAr(month: Int): String {
        return when (month) {
            1 -> "محرم"
            2 -> "صفر"
            3 -> "ربيع الأول"
            4 -> "ربيع الآخر"
            5 -> "جمادى الأولى"
            6 -> "جمادى الآخرة"
            7 -> "رجب"
            8 -> "شعبان"
            9 -> "رمضان"
            10 -> "شوال"
            11 -> "ذو القعدة"
            12 -> "ذو الحجة"
            else -> ""
        }
    }

    fun dayNameAr(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "السبت"
            Calendar.SUNDAY -> "الأحد"
            Calendar.MONDAY -> "الإثنين"
            Calendar.TUESDAY -> "الثلاثاء"
            Calendar.WEDNESDAY -> "الأربعاء"
            Calendar.THURSDAY -> "الخميس"
            Calendar.FRIDAY -> "الجمعة"
            else -> ""
        }
    }

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun julianDayToHijri(jd: Double): HijriDate {
        val iYear: Int
        val iMonth: Int
        val iDay: Int

        val jdAdj = floor(jd) + 0.5
        val daysSince = jdAdj - 1948439.5
        val hYear = floor((30.0 * daysSince + 10646.0) / 10631.0)
        val yearStart = hijriToJulianDay(hYear.toInt(), 1, 1)
        var dayOfYear = (jdAdj - yearStart + 1).toInt()
        var month = 1
        var monthDays: Int
        while (true) {
            monthDays = hijriMonthLength(hYear.toInt(), month)
            if (dayOfYear <= monthDays) break
            dayOfYear -= monthDays
            month += 1
        }

        iYear = hYear.toInt()
        iMonth = month
        iDay = dayOfYear
        return HijriDate(iDay, iMonth, iYear)
    }

    private fun hijriToJulianDay(year: Int, month: Int, day: Int): Double {
        return day +
            ceil(29.5 * (month - 1)) +
            (year - 1) * 354 +
            floor((3 + 11 * year) / 30.0) +
            1948439.5 - 1
    }

    private fun hijriMonthLength(year: Int, month: Int): Int {
        // Civil/tabular: months alternate 30/29, with leap years adding a day to last month.
        val isLeap = isHijriLeapYear(year)
        return when (month) {
            1, 3, 5, 7, 9, 11 -> 30
            2, 4, 6, 8, 10 -> 29
            12 -> if (isLeap) 30 else 29
            else -> 29
        }
    }

    private fun isHijriLeapYear(year: Int): Boolean {
        val mod = (11 * year + 14) % 30
        return mod < 11
    }

    private fun ceil(x: Double): Double = kotlin.math.ceil(x)
}
