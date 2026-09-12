package com.mosque.prayer.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormatUtils {

    fun formatTime(millis: Long, use24Hour: Boolean, timeZone: TimeZone): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = timeZone
        return sdf.format(Date(millis))
    }

    fun formatTimeWithAmPm(millis: Long, use24Hour: Boolean, timeZone: TimeZone): String {
        val pattern = if (use24Hour) "HH:mm" else "hh:mm"
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = timeZone
        val base = sdf.format(Date(millis))
        return base
    }

    fun formatCountdownHms(totalSeconds: Long): String {
        val s = if (totalSeconds < 0) 0 else totalSeconds
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60

        return if (h > 0) {
            // 1 heure ou plus : afficher HH:MM:SS, ex : 01:20:30
            String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
        } else {
            // Moins d'1 heure : toujours afficher MM:SS, ex : 39:45 ou 00:30
            String.format(Locale.US, "%02d:%02d", m, sec)
        }
    }

    fun formatCountdownMmOrSs(totalSeconds: Long): String {
        val s = if (totalSeconds < 0) 0 else totalSeconds
        return if (s >= 60) {
            val m = s / 60
            String.format(Locale.US, "%02d", m)
        } else {
            String.format(Locale.US, "%02d", s)
        }
    }
}
