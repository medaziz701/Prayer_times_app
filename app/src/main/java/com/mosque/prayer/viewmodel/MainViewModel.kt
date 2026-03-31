package com.mosque.prayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.mosque.prayer.R
import com.mosque.prayer.data.AppPreferences
import com.mosque.prayer.data.CitiesRepository
import com.mosque.prayer.data.City
import com.mosque.prayer.data.PrayerKey
import com.mosque.prayer.data.PrayerTimes
import com.mosque.prayer.utils.PrayerTimesCalculator
import java.util.Calendar
import java.util.TimeZone

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPreferences(app)
    private val calculator = PrayerTimesCalculator()

    private val _city = MutableLiveData<City>()
    val city: LiveData<City> = _city

    private val _use24h = MutableLiveData<Boolean>()
    val use24h: LiveData<Boolean> = _use24h

    private val _bgMode = MutableLiveData<String>()
    val bgMode: LiveData<String> = _bgMode

    private val _prayerTimes = MutableLiveData<PrayerTimes>()
    val prayerTimes: LiveData<PrayerTimes> = _prayerTimes

    private val _nextPrayer = MutableLiveData<PrayerKey>()
    val nextPrayer: LiveData<PrayerKey> = _nextPrayer

    private val _iqamaMinutes = MutableLiveData<Map<String, Int>>()
    val iqamaMinutes: LiveData<Map<String, Int>> = _iqamaMinutes

    private val _hadithUpdateMode = MutableLiveData<String>()
    val hadithUpdateMode: LiveData<String> = _hadithUpdateMode

    private val _screenFlip = MutableLiveData<Boolean>()
    val screenFlip: LiveData<Boolean> = _screenFlip

    private val _screenTemplate = MutableLiveData<String>()
    val screenTemplate: LiveData<String> = _screenTemplate

    private val _hadithText = MutableLiveData<String>()
    val hadithText: LiveData<String> = _hadithText

    private var lastNextPrayer: PrayerKey? = null

    private val defaultHadithPool: List<String> = listOf(
        "عن أبي هريرة رضي الله عنه أن النبي ﷺ قال: \"كلمتان خفيفتان على اللسان ثقيلتان في الميزان حبيبتان إلى الرحمن: سبحان الله وبحمده سبحان الله العظيم\"",
        "قال رسول الله ﷺ: \"من سلك طريقًا يلتمس فيه علمًا سهّل الله له به طريقًا إلى الجنة\"",
        "قال رسول الله ﷺ: \"إنما الأعمال بالنيات، وإنما لكل امرئ ما نوى\"",
        "قال رسول الله ﷺ: \"أحب الأعمال إلى الله أدومها وإن قل\"",
        "قال رسول الله ﷺ: \"الدين النصيحة\""
    )

    private var hadithPool: List<String> = emptyList()

    private fun loadHadithPoolFromRaw(): List<String> {
        return try {
            val text = getApplication<Application>().resources
                .openRawResource(R.raw.hadiths)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val normalized = text.replace("\r\n", "\n")
            parseHadithText(normalized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHadithText(normalized: String): List<String> {
        val markerRegex = Regex("(?m)^---\\s*$")
        val paraRegex = Regex("\n\\s*\n+")
        val numberHeadRegex = Regex("(?m)^\\s*(?:[0-9]+|[٠-٩]+)[\\).،\\-–—\\.]\\s+")

        val primary = if (markerRegex.containsMatchIn(normalized)) {
            normalized.split(markerRegex)
        } else listOf(normalized)

        val out = mutableListOf<String>()
        for (chunk in primary) {
            val trimmed = chunk.trim()
            if (trimmed.isEmpty()) continue

            if (numberHeadRegex.containsMatchIn(trimmed)) {
                out += trimmed.split(numberHeadRegex)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                continue
            }

            if (paraRegex.containsMatchIn(trimmed)) {
                out += trimmed.split(paraRegex)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                continue
            }

            out += trimmed
        }

        return out.map { stripLeadingNumber(it) }
    }

    private fun stripLeadingNumber(text: String): String {
        // Remove one or more leading enumerations like "1.", "1)", "١.", including accidental repeats and leading spaces
        return text.replaceFirst(Regex("^\\s*(?:(?:[0-9]+|[٠-٩]+)[\\).،\\-–—\\.]\\s*)+"), "").trim()
    }

    private fun ensureHadithPoolLoaded() {
        if (hadithPool.isEmpty()) {
            val items = loadHadithPoolFromRaw()
            hadithPool = if (items.isNotEmpty()) items else defaultHadithPool
        }
    }

    fun reload() {
        val c = CitiesRepository.findById(prefs.cityId)
        _city.value = c
        _use24h.value = prefs.use24Hour
        _bgMode.value = prefs.bgMode
        // One-time migration: set DUHA iqama to +10 minutes if it was previously the old default (30)
        if (!prefs.migratedDuhaIqamaTo10) {
            val current = prefs.getIqamaMinutes(PrayerKey.DUHA.key, 10)
            if (current == 30) {
                prefs.setIqamaMinutes(PrayerKey.DUHA.key, 10)
            }
            prefs.migratedDuhaIqamaTo10 = true
        }
        _iqamaMinutes.value = loadIqamaMap()
        _hadithUpdateMode.value = prefs.hadithUpdateMode
        _screenFlip.value = prefs.screenFlip
        _screenTemplate.value = prefs.screenTemplate
        ensureHadithPoolLoaded()
        ensureHadithInitialized()
        recalcToday()
    }

    fun setCity(id: String) {
        prefs.cityId = id
        reload()
    }

    fun setUse24h(use: Boolean) {
        prefs.use24Hour = use
        _use24h.value = use
    }

    fun setBgMode(mode: String) {
        // Ignore requested mode and always keep BG_IMAGE
        prefs.bgMode = AppPreferences.BG_IMAGE
        _bgMode.value = AppPreferences.BG_IMAGE
    }

    fun setHadithUpdateMode(mode: String) {
        prefs.hadithUpdateMode = mode
        _hadithUpdateMode.value = mode
        maybeUpdateHadith(System.currentTimeMillis(), force = (mode != AppPreferences.HADITH_UPDATE_MANUAL))
    }

    fun setScreenFlip(flip: Boolean) {
        prefs.screenFlip = flip
        _screenFlip.value = flip
    }

    fun setScreenTemplate(template: String) {
        prefs.screenTemplate = template
        _screenTemplate.value = template
    }

    fun refreshHadithManual() {
        if ((prefs.hadithUpdateMode) == AppPreferences.HADITH_UPDATE_MANUAL) {
            maybeUpdateHadith(System.currentTimeMillis())
        }
    }

    fun isActivated(): Boolean = prefs.activationCode.isNotBlank()

    fun setActivationCode(code: String) {
        prefs.activationCode = code
    }

    fun setIqamaMinutes(prayerKey: String, minutes: Int) {
        prefs.setIqamaMinutes(prayerKey, minutes)
        _iqamaMinutes.value = loadIqamaMap()
    }

    fun getIqamaMinutes(prayerKey: String, defaultMinutes: Int): Int {
        return prefs.getIqamaMinutes(prayerKey, defaultMinutes)
    }

    fun recalcToday(nowMillis: Long = System.currentTimeMillis()) {
        val c = _city.value ?: CitiesRepository.findById(prefs.cityId)
        val tz = TimeZone.getTimeZone(c.timeZoneId)
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = nowMillis

        // Si une date a été sélectionnée dans les paramètres, l'utiliser pour le calcul des horaires
        val selYear = prefs.selectedDateYear
        val selMonth = prefs.selectedDateMonth
        val selDay = prefs.selectedDateDay
        if (selYear > 0 && selMonth in 1..12 && selDay > 0) {
            cal.set(Calendar.YEAR, selYear)
            cal.set(Calendar.MONTH, selMonth - 1)
            cal.set(Calendar.DAY_OF_MONTH, selDay)
        }

        // Si une heure a été sélectionnée, l'utiliser comme "maintenant" pour les compteurs
        val selHour = prefs.selectedTimeHour
        val selMinute = prefs.selectedTimeMinute
        if (selHour in 0..23 && selMinute in 0..59) {
            cal.set(Calendar.HOUR_OF_DAY, selHour)
            cal.set(Calendar.MINUTE, selMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        val effectiveNow = cal.timeInMillis

        val base = calculator.calculate(cal, c.latitude, c.longitude, tz)
        val pt = applyAdhanOffsets(base)
        _prayerTimes.value = pt

        val next = computeNextPrayer(effectiveNow, pt)
        _nextPrayer.value = next

        val nextChanged = lastNextPrayer != next
        lastNextPrayer = next
        maybeUpdateHadith(effectiveNow, prayerChanged = nextChanged)
    }

    // Recalculate using the exact supplied timestamp without overriding with
    // selected date/time from preferences. This keeps ViewModel in sync with
    // the simulated clock used by the Activity (e.g., after Iqama has passed).
    fun recalcAtExactNow(exactNowMillis: Long) {
        val c = _city.value ?: CitiesRepository.findById(prefs.cityId)
        val tz = TimeZone.getTimeZone(c.timeZoneId)
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = exactNowMillis

        val base = calculator.calculate(cal, c.latitude, c.longitude, tz)
        val pt = applyAdhanOffsets(base)
        _prayerTimes.value = pt

        val next = computeNextPrayer(exactNowMillis, pt)
        val nextChanged = lastNextPrayer != next
        lastNextPrayer = next
        _nextPrayer.value = next

        maybeUpdateHadith(exactNowMillis, prayerChanged = nextChanged)
    }

    private fun computeNextPrayer(nowMillis: Long, pt: PrayerTimes): PrayerKey {
        for ((k, t) in pt.asList()) {
            if (t > nowMillis) return k
        }
        return PrayerKey.FAJR
    }

    private fun loadIqamaMap(): Map<String, Int> {
        val fajr = prefs.getIqamaMinutes(PrayerKey.FAJR.key, 25)
        val duha = prefs.getIqamaMinutes(PrayerKey.DUHA.key, 10)
        val dhuhr = prefs.getIqamaMinutes(PrayerKey.DHUHR.key, 20)
        val asr = prefs.getIqamaMinutes(PrayerKey.ASR.key, 20)
        val maghrib = prefs.getIqamaMinutes(PrayerKey.MAGHRIB.key, 10)
        val isha = prefs.getIqamaMinutes(PrayerKey.ISHA.key, 20)

        return mapOf(
            PrayerKey.FAJR.key to fajr,
            PrayerKey.DUHA.key to duha,
            PrayerKey.DHUHR.key to dhuhr,
            PrayerKey.ASR.key to asr,
            PrayerKey.MAGHRIB.key to maghrib,
            PrayerKey.ISHA.key to isha
        )
    }

    private fun applyAdhanOffsets(base: PrayerTimes): PrayerTimes {
        val m = 60_000L
        val offFajr = prefs.getAdhanOffsetMinutes("fajr") * m
        val offSunrise = prefs.getAdhanOffsetMinutes("sunrise") * m
        val offDhuhr = prefs.getAdhanOffsetMinutes("dhuhr") * m
        val offAsr = prefs.getAdhanOffsetMinutes("asr") * m
        val offMaghrib = prefs.getAdhanOffsetMinutes("maghrib") * m
        val offIsha = prefs.getAdhanOffsetMinutes("isha") * m

        return PrayerTimes(
            fajr = base.fajr - offFajr,
            // Duha equals sunrise + 15 minutes; apply sunrise offset to keep relation
            // Positive offset means advance (earlier) -> subtract minutes
            duha = base.duha - offSunrise,
            dhuhr = base.dhuhr - offDhuhr,
            asr = base.asr - offAsr,
            maghrib = base.maghrib - offMaghrib,
            isha = base.isha - offIsha
        )
    }

    private fun ensureHadithInitialized() {
        if (hadithPool.isEmpty()) return
        if (prefs.hadithIndex < 0 || prefs.hadithIndex >= hadithPool.size) {
            prefs.hadithIndex = 0
        }
        if (prefs.hadithLastUpdateMillis <= 0L) {
            prefs.hadithLastUpdateMillis = System.currentTimeMillis()
        }
        _hadithText.value = hadithPool[prefs.hadithIndex]
    }

    private fun maybeUpdateHadith(nowMillis: Long, force: Boolean = false, prayerChanged: Boolean = false) {
        if (hadithPool.isEmpty()) return
        val mode = _hadithUpdateMode.value ?: prefs.hadithUpdateMode

        if (force) {
            advanceHadithAndPublish(nowMillis)
            return
        }

        when (mode) {
            AppPreferences.HADITH_UPDATE_MANUAL -> {
                val intervalMin = prefs.hadithCustomIntervalMinutes.coerceAtLeast(1)
                val last = prefs.hadithLastUpdateMillis
                if (nowMillis - last >= intervalMin * 60_000L) {
                    advanceHadithAndPublish(nowMillis)
                } else {
                    _hadithText.value = hadithPool[prefs.hadithIndex]
                }
            }

            AppPreferences.HADITH_UPDATE_EVERY_PRAYER -> {
                if (prayerChanged) {
                    advanceHadithAndPublish(nowMillis)
                } else {
                    _hadithText.value = hadithPool[prefs.hadithIndex]
                }
            }

            AppPreferences.HADITH_UPDATE_30_MIN -> {
                val last = prefs.hadithLastUpdateMillis
                if (nowMillis - last >= 10L * 60_000L) {
                    advanceHadithAndPublish(nowMillis)
                } else {
                    _hadithText.value = hadithPool[prefs.hadithIndex]
                }
            }

            else -> {
                val last = prefs.hadithLastUpdateMillis
                if (nowMillis - last >= 60L * 60_000L) {
                    advanceHadithAndPublish(nowMillis)
                } else {
                    _hadithText.value = hadithPool[prefs.hadithIndex]
                }
            }
        }
    }

    private fun advanceHadithAndPublish(nowMillis: Long) {
        if (hadithPool.isEmpty()) return
        val nextIdx = (prefs.hadithIndex + 1) % hadithPool.size
        prefs.hadithIndex = nextIdx
        prefs.hadithLastUpdateMillis = nowMillis
        _hadithText.value = hadithPool[nextIdx]
    }
}
