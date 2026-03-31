package com.mosque.prayer.utils

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.data.DateComponents
import com.mosque.prayer.data.PrayerTimes
import java.util.Calendar
import java.util.TimeZone
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes

/**
 * Offline prayer times calculator (approx.) based on solar position.
 * Designed for stable offline usage.
 */
class PrayerTimesCalculator {

    fun calculate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        timeZone: TimeZone
    ): PrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(year, month, day)

        val params = CalculationMethod.UMM_AL_QURA.getParameters().apply {
            madhab = Madhab.SHAFI
            // keep zero adjustments; official timetables already account for Umm al-Qura method
        }

        // Ensure calculations honor the intended timezone (for cross‑TZ devices)
        val oldTz = TimeZone.getDefault()
        val adhanTimes = try {
            if (oldTz.id != timeZone.id) TimeZone.setDefault(timeZone)
            AdhanPrayerTimes(coordinates, dateComponents, params)
        } finally {
            if (TimeZone.getDefault().id != oldTz.id) TimeZone.setDefault(oldTz)
        }

        // Round to minute: Fajr is ceiled to next minute (matches public timetables like Google);
        // others are rounded to nearest minute.
        val fajrMillis = ceilToMinute(adhanTimes.fajr.time)
        val sunriseMillis = roundToNearestMinute(adhanTimes.sunrise.time)
        val dhuhrMillis = roundToNearestMinute(adhanTimes.dhuhr.time)
        val asrMillis = roundToNearestMinute(adhanTimes.asr.time)
        val maghribMillis = roundToNearestMinute(adhanTimes.maghrib.time)
        val ishaMillis = roundToNearestMinute(adhanTimes.isha.time)

        val duhaMillis = sunriseMillis + 15 * 60_000L

        return PrayerTimes(
            fajr = fajrMillis,
            duha = duhaMillis,
            dhuhr = dhuhrMillis,
            asr = asrMillis,
            maghrib = maghribMillis,
            isha = ishaMillis
        )
    }

    private fun roundToNearestMinute(millis: Long): Long {
        val secs = ((millis / 1000L) % 60L).toInt()
        val base = millis - (secs * 1000L)
        return if (secs >= 30) base + 60_000L else base
    }

    private fun ceilToMinute(millis: Long): Long {
        val secs = ((millis / 1000L) % 60L).toInt()
        val base = millis - (secs * 1000L)
        return if (secs > 0) base + 60_000L else base
    }
}
