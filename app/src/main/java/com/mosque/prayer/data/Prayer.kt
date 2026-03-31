package com.mosque.prayer.data

enum class PrayerKey(val key: String, val nameAr: String) {
    FAJR("fajr", "الفجر"),
    DUHA("duha", "الضحى"),
    DHUHR("dhuhr", "الظهر"),
    ASR("asr", "العصر"),
    MAGHRIB("maghrib", "المغرب"),
    ISHA("isha", "العشاء");
}

data class PrayerTimes(
    val fajr: Long,
    val duha: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
) {
    fun get(prayer: PrayerKey): Long {
        return when (prayer) {
            PrayerKey.FAJR -> fajr
            PrayerKey.DUHA -> duha
            PrayerKey.DHUHR -> dhuhr
            PrayerKey.ASR -> asr
            PrayerKey.MAGHRIB -> maghrib
            PrayerKey.ISHA -> isha
        }
    }

    fun asList(): List<Pair<PrayerKey, Long>> {
        return listOf(
            PrayerKey.FAJR to fajr,
            PrayerKey.DUHA to duha,
            PrayerKey.DHUHR to dhuhr,
            PrayerKey.ASR to asr,
            PrayerKey.MAGHRIB to maghrib,
            PrayerKey.ISHA to isha
        )
    }
}
