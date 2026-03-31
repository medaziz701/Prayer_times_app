package com.mosque.prayer.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.mosque.prayer.databinding.ActivityMainBinding
import com.mosque.prayer.data.CitiesRepository
import com.mosque.prayer.data.PrayerKey
import com.mosque.prayer.data.AppPreferences
import com.mosque.prayer.utils.HijriCalendarUtils
import com.mosque.prayer.utils.TimeFormatUtils
import com.mosque.prayer.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    private lateinit var prefs: AppPreferences

    private val handler = Handler(Looper.getMainLooper())
    private val adapter = PrayerRowAdapter()

    private var tz: TimeZone = TimeZone.getDefault()
    private val logTag = "MainActivity"
    private var lastRecalcAt: Long = 0L
    // While a special override (Adhan/Jumuah/Athkar-after-Iqama) is active,
    // ignore LiveData hadith updates so UI keeps showing the override text.
    private var contentOverrideUntilMs: Long = 0L
    // Simulation anchors so that selected time advances in real-time
    private var simAnchorRealMs: Long = 0L
    private var simAnchorSimMs: Long = 0L
    private var simLastHour: Int = -1
    private var simLastMinute: Int = -1
    private var simLastDateKey: String? = null

    private val tick = object : Runnable {
        override fun run() {
            vm.refreshHadithManual()
            renderClockAndCountdowns()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(logTag, "onCreate: setContentView done")

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = AppPreferences(this)

        // Hide the footer link when the bright tick segment passes under
        // the bottom center area of the frame, so text is not overlapped.
        binding.animatedTicks.onBottomCoveredChanged = { _ ->
            binding.txtFooterLink.visibility = View.VISIBLE
        }

        binding.rvPrayers.layoutManager = LinearLayoutManager(this)
        binding.rvPrayers.adapter = adapter
        binding.rvPrayers.isNestedScrollingEnabled = false

        binding.animatedTicks.setNowProvider { currentSimulatedNow() }

        vm.city.observe(this, Observer { city ->
            tz = TimeZone.getTimeZone(city.timeZoneId)
            // Refresh all sections once for the selected city
            renderDates()
            renderTable()
            renderClockAndCountdowns()
        })
        vm.use24h.observe(this, Observer {
            // Only time and table depend on 12h/24h
            renderClockAndCountdowns()
            renderTable()
        })
        vm.bgMode.observe(this, Observer { mode ->
            val alpha = if (mode == AppPreferences.BG_PLAIN) 0f else 1f
            binding.imgBackground.alpha = alpha
            Log.d(logTag, "bgMode changed: mode=$mode alpha=$alpha")
        })
        vm.prayerTimes.observe(this, Observer {
            // Update rows, date (background), and counters when times change
            renderTable()
            renderDates()
            renderClockAndCountdowns()
        })
        vm.nextPrayer.observe(this, Observer {
            // Only counters depend on the next prayer selection
            renderClockAndCountdowns()
        })
        vm.hadithText.observe(this, Observer { txt ->
            val now = currentSimulatedNow()
            if (now >= contentOverrideUntilMs) {
                binding.txtHadith.text = txt
                // Restore default title when no override is active
                binding.txtHadithTitle.text = getString(com.mosque.prayer.R.string.hadith_title)
                binding.txtHadithTitle.visibility = View.VISIBLE
            }
        })
        vm.screenFlip.observe(this, Observer { flip ->
            // When "flip screen" is enabled, use reverse portrait (180°) instead of landscape
            requestedOrientation = if (flip == true) {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            binding.root.rotation = 0f
        })

        // Small subtle settings button (top-right icon)
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Day navigation arrows around the prayers table
        binding.btnPrevDay.setOnClickListener { shiftSelectedDate(-1) }
        binding.btnNextDay.setOnClickListener { shiftSelectedDate(+1) }

        if (!vm.isActivated()) {
            ActivationDialog.show(this, onActivated = { code ->
                vm.setActivationCode(code)
            }, onExit = {
                finish()
            })
        }

        vm.reload()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
        // Reload settings (city, format, etc.) in case they were changed
        // in SettingsActivity while this activity was paused.
        vm.reload()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun shiftSelectedDate(days: Int) {
        val cal = Calendar.getInstance(tz)
        val y = prefs.selectedDateYear
        val m = prefs.selectedDateMonth
        val d = prefs.selectedDateDay
        val hasDate = (y > 0 && m in 1..12 && d > 0)
        if (hasDate) {
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m - 1)
            cal.set(Calendar.DAY_OF_MONTH, d)
        } else {
            cal.timeInMillis = System.currentTimeMillis()
        }
        cal.add(Calendar.DAY_OF_MONTH, days)
        prefs.selectedDateYear = cal.get(Calendar.YEAR)
        prefs.selectedDateMonth = cal.get(Calendar.MONTH) + 1
        prefs.selectedDateDay = cal.get(Calendar.DAY_OF_MONTH)
        // Recalculate for the newly selected date
        vm.recalcToday(System.currentTimeMillis())
    }

    private fun renderAll() {
        Log.d(logTag, "renderAll")
        renderClockAndCountdowns()
        renderDates()
        renderTable()
    }

    private fun renderDates() {
        val city = vm.city.value ?: CitiesRepository.findById(AppPreferences.DEFAULT_CITY_ID)
        val nowMillis = currentSimulatedNow()
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = nowMillis
        val dayName = HijriCalendarUtils.dayNameAr(cal)
        binding.dayName.text = dayName

        // Example output: "27 ديسمبر (12)"
        val gSmall = SimpleDateFormat("dd MMMM (MM)", Locale("ar"))
        gSmall.timeZone = tz
        binding.txtGregorianSmall.text = gSmall.format(cal.time).toLatinDigits()
        binding.txtGregorianYear.text = cal.get(Calendar.YEAR).toString()

        val hijri = HijriCalendarUtils.gregorianToHijri(cal.time, tz)
        val hijriMonthName = HijriCalendarUtils.hijriMonthNameAr(hijri.month)
        // Example: "5 رجب (7)"
        binding.txtHijriSmall.text = "${hijri.day} $hijriMonthName (${hijri.month})"
        binding.txtHijriYear.text = hijri.year.toString()

        updateBackgroundForDay(cal)
    }

    private fun renderTable() {
        val pt = vm.prayerTimes.value ?: return
        val use24 = vm.use24h.value ?: true
        val next = vm.nextPrayer.value
        val iq = vm.iqamaMinutes.value ?: emptyMap()

        val rows = pt.asList().map { (k, t) ->
            val adhan = TimeFormatUtils.formatTime(t, use24, tz)
            val iqMin = iq[k.key] ?: defaultIqama(k)
            val iqamaTime = t + iqMin * 60_000L
            val iqama = TimeFormatUtils.formatTime(iqamaTime, use24, tz)
            PrayerRowUi(
                key = k,
                name = k.nameAr,
                adhan = adhan,
                iqama = iqama,
                isNext = (next == k)
            )
        }

        adapter.submit(rows)
    }

    private fun renderClockAndCountdowns() {
        val now = currentSimulatedNow()
        val use24 = vm.use24h.value ?: true
        binding.txtTime.text = TimeFormatUtils.formatTime(now, use24, tz)

        val pt = vm.prayerTimes.value ?: return
        val next = vm.nextPrayer.value ?: return
        val iqMap = vm.iqamaMinutes.value ?: emptyMap()

        // Detect if we are in the window between Adhan and Iqama for any prayer
        var activeBetweenKey: com.mosque.prayer.data.PrayerKey? = null
        for ((k, t) in pt.asList()) {
            val iqMinK = iqMap[k.key] ?: defaultIqama(k)
            val iqTimeK = t + iqMinK * 60_000L
            if (now >= t && now < iqTimeK) {
                activeBetweenKey = k
                break
            }
        }
        val rawNextTime = pt.get(next)
        // If we are after Isha and ViewModel still returns FAJR from today's table,
        // treat FAJR as tomorrow by adding 24h for display purposes only.
        val oneDayMs = 24L * 60L * 60L * 1000L
        val nextTime = if (next == com.mosque.prayer.data.PrayerKey.FAJR && rawNextTime <= now) {
            rawNextTime + oneDayMs
        } else {
            rawNextTime
        }
        val secToNext = if (activeBetweenKey != null) 0L else (nextTime - now) / 1000L
        binding.txtNextCountdown.text = TimeFormatUtils.formatCountdownHms(secToNext)

        val keyForIqama = activeBetweenKey ?: next
        val baseAdhanForIq = pt.get(keyForIqama)
        val iqMin = (iqMap)[keyForIqama.key] ?: defaultIqama(keyForIqama)
        val iqTime = baseAdhanForIq + iqMin * 60_000L
        val secToIqama = (iqTime - now) / 1000L
        val showIqama = (now >= baseAdhanForIq) && (now < iqTime)
        if (showIqama) {
            if (secToIqama < 60) {
                // In the last minute, show seconds: 59..00
                binding.txtIqamaCountdown.text = String.format(Locale.US, "%02d", secToIqama)
            } else {
                // Otherwise show remaining whole minutes
                val minsOnly = (secToIqama / 60L)
                binding.txtIqamaCountdown.text = String.format(Locale.US, "%02d", minsOnly)
            }
        } else {
            // Keep any previous value; view will be hidden below
        }
        
        binding.labelIqamaAfter.visibility = if (showIqama) View.VISIBLE else View.GONE
        binding.txtIqamaCountdown.visibility = if (showIqama) View.VISIBLE else View.GONE
        if (showIqama) {
            binding.iqamaContainer.visibility = View.VISIBLE
            binding.nextContainer.visibility = View.GONE
            binding.rowCounters.weightSum = 1f
            val lpIq = binding.iqamaContainer.layoutParams as LinearLayout.LayoutParams
            lpIq.weight = 1f
            lpIq.marginEnd = 0
            binding.iqamaContainer.layoutParams = lpIq
        } else {
            binding.iqamaContainer.visibility = View.GONE
            binding.nextContainer.visibility = View.VISIBLE
            binding.rowCounters.weightSum = 1f
            val lpNext = binding.nextContainer.layoutParams as LinearLayout.LayoutParams
            lpNext.weight = 1f
            lpNext.marginStart = 0
            binding.nextContainer.layoutParams = lpNext
        }

        renderHadithForTime(now, pt, iqMap)

        // Trigger a recalculation only when we pass a non-FAJR prayer time.
        // For FAJR displayed as tomorrow (rawNextTime <= now), do not recalc
        // to avoid a tight loop.
        if (now > rawNextTime && next != com.mosque.prayer.data.PrayerKey.FAJR) {
            // Throttle recalc to at most once every 30s to avoid loops
            if (now - lastRecalcAt >= 30_000L) {
                lastRecalcAt = now
                vm.recalcAtExactNow(now)
            }
        }

        // Also trigger a recalc shortly after the Iqama time of the previous prayer passes,
        // so that the "next prayer" logic advances right after Iqama if desired.
        var prevKey: com.mosque.prayer.data.PrayerKey? = null
        var prevAdhan: Long = 0L
        for ((k, t) in pt.asList()) {
            if (t <= now) {
                prevKey = k
                prevAdhan = t
            } else break
        }
        if (prevKey != null) {
            val prevIqMin = iqMap[prevKey!!.key] ?: defaultIqama(prevKey!!)
            val prevIqTime = prevAdhan + prevIqMin * 60_000L
            if (now > prevIqTime && now - lastRecalcAt >= 1_000L) {
                lastRecalcAt = now
                vm.recalcAtExactNow(now)
            }
        }
    }

    private fun currentSimulatedNow(): Long {
        val nowReal = System.currentTimeMillis()
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = nowReal

        // Appliquer la date sélectionnée si présente
        val y = prefs.selectedDateYear
        val m = prefs.selectedDateMonth
        val d = prefs.selectedDateDay
        val hasDate = (y > 0 && m in 1..12 && d > 0)
        if (hasDate) {
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m - 1)
            cal.set(Calendar.DAY_OF_MONTH, d)
        }

        // Si une heure est sélectionnée, ancrer la simulation pour qu'elle avance avec le temps réel
        val h = prefs.selectedTimeHour
        val min = prefs.selectedTimeMinute
        val hasTime = (h in 0..23 && min in 0..59)
        if (hasTime) {
            val dateKey = if (hasDate) "$y-$m-$d" else "today"
            if (simAnchorRealMs == 0L || simLastHour != h || simLastMinute != min || simLastDateKey != dateKey) {
                // Recréer l'ancrage
                val base = Calendar.getInstance(tz)
                base.timeInMillis = nowReal
                if (hasDate) {
                    base.set(Calendar.YEAR, y)
                    base.set(Calendar.MONTH, m - 1)
                    base.set(Calendar.DAY_OF_MONTH, d)
                }
                base.set(Calendar.HOUR_OF_DAY, h)
                base.set(Calendar.MINUTE, min)
                base.set(Calendar.SECOND, 0)
                base.set(Calendar.MILLISECOND, 0)
                simAnchorSimMs = base.timeInMillis
                simAnchorRealMs = nowReal
                simLastHour = h
                simLastMinute = min
                simLastDateKey = dateKey
            }
            val delta = nowReal - simAnchorRealMs
            return simAnchorSimMs + delta
        } else {
            // Pas de simulation -> réinitialiser l'ancrage
            simAnchorRealMs = 0L
            simLastHour = -1
            simLastMinute = -1
            simLastDateKey = null
            return cal.timeInMillis
        }
    }

    private fun defaultIqama(k: PrayerKey): Int {
        return when (k) {
            PrayerKey.FAJR -> 25
            PrayerKey.DUHA -> 10
            PrayerKey.DHUHR -> 20
            PrayerKey.ASR -> 20
            PrayerKey.MAGHRIB -> 10
            PrayerKey.ISHA -> 20
        }
    }

    private fun renderHadithForTime(nowMillis: Long, pt: com.mosque.prayer.data.PrayerTimes, iqMap: Map<String, Int>) {
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = nowMillis
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isFriday = (dayOfWeek == Calendar.FRIDAY)

        var overrideText: String? = null
        var overrideTitleRes: Int? = null
        var hideTitle = false

        // Friday: one hour before Dhuhr (Jumuah) show the Friday ayah with reminder title
        if (isFriday) {
            val dhuhrTime = pt.get(PrayerKey.DHUHR)
            val oneHourBefore = dhuhrTime - 60L * 60_000L
            if (nowMillis in oneHourBefore until dhuhrTime) {
                overrideText = getString(com.mosque.prayer.R.string.hadith_friday_pre_jumuah)
                overrideTitleRes = com.mosque.prayer.R.string.hadith_reminder_title
                hideTitle = false
                contentOverrideUntilMs = dhuhrTime
            }
        }

        // Adhan windows: for one minute starting at each prayer time
        if (overrideText == null) {
            for ((key, adhanTime) in pt.asList()) {
                val start = adhanTime
                val end = adhanTime + 60_000L
                if (nowMillis in start until end) {
                    overrideText = getString(com.mosque.prayer.R.string.hadith_adhan_time)
                    overrideTitleRes = com.mosque.prayer.R.string.hadith_reminder_title
                    hideTitle = false
                    contentOverrideUntilMs = end
                    break
                }
            }
        }

        if (overrideText == null) {
            for ((key, adhanTime) in pt.asList()) {
                val iqMin = iqMap[key.key] ?: defaultIqama(key)
                val iqTime = adhanTime + iqMin * 60_000L
                val start = iqTime
                val end = iqTime + 60_000L
                if (nowMillis in start until end) {
                    overrideText = getString(com.mosque.prayer.R.string.hadith_iqama_time)
                    overrideTitleRes = com.mosque.prayer.R.string.hadith_reminder_title
                    hideTitle = false
                    contentOverrideUntilMs = end
                    break
                }
            }
        }

        // Iqama window: show Athkar overlay starting 7 minutes after Iqama and keep for 10 minutes
        if (overrideText == null) {
            for ((key, adhanTime) in pt.asList()) {
                val iqMin = iqMap[key.key] ?: defaultIqama(key)
                val iqTime = adhanTime + iqMin * 60_000L
                val start = iqTime + 7L * 60_000L
                val end = start + 10L * 60_000L
                if (nowMillis in start until end) {
                    overrideText = getString(com.mosque.prayer.R.string.athkar_after_prayer_body)
                    overrideTitleRes = com.mosque.prayer.R.string.athkar_after_prayer_title
                    hideTitle = false
                    contentOverrideUntilMs = end
                    break
                }
            }
        }

        // Defensive fallback: if we decided to show the reminder title but
        // somehow did not set a content text (should not happen), provide a
        // default content so the hadith card is not an empty thin capsule.
        if ((overrideTitleRes == com.mosque.prayer.R.string.hadith_reminder_title) && overrideText.isNullOrBlank()) {
            overrideText = getString(com.mosque.prayer.R.string.hadith_adhan_time)
        }

        if (overrideText != null) {
            val isAthkar = (overrideTitleRes == com.mosque.prayer.R.string.athkar_after_prayer_title)
            if (isAthkar) {
                // Show full-screen Athkar overlay and hide normal content
                binding.overlayAthkar.visibility = View.VISIBLE
                binding.overlayAthkarBody.text = styleAthkarBullets(overrideText)
                binding.timeBox.visibility = View.GONE
                binding.cardDate.visibility = View.GONE
                binding.cardTable.visibility = View.GONE
                binding.rowCounters.visibility = View.GONE
                binding.txtHadithTitle.visibility = View.GONE
                binding.cardHadith.visibility = View.GONE
                binding.btnPrevDay.visibility = View.GONE
                binding.btnNextDay.visibility = View.GONE
                binding.btnSettings.visibility = View.GONE
            } else {
                // No Athkar overlay -> ensure overlay is hidden and show default hadith area
                binding.overlayAthkar.visibility = View.GONE
                binding.timeBox.visibility = View.VISIBLE
                binding.cardDate.visibility = View.VISIBLE
                binding.cardTable.visibility = View.VISIBLE
                binding.rowCounters.visibility = View.VISIBLE
                binding.cardHadith.visibility = View.VISIBLE
                binding.txtFooterLink.visibility = View.VISIBLE
                binding.btnPrevDay.visibility = View.VISIBLE
                binding.btnNextDay.visibility = View.VISIBLE
                binding.btnSettings.visibility = View.VISIBLE

                binding.txtHadith.text = overrideText
                if (hideTitle) {
                    binding.txtHadithTitle.visibility = View.GONE
                } else {
                    binding.txtHadithTitle.text = getString(overrideTitleRes ?: com.mosque.prayer.R.string.hadith_title)
                    binding.txtHadithTitle.visibility = View.VISIBLE
                }
            }
        } else {
            // No override at all -> hide overlay and restore normal layout
            binding.overlayAthkar.visibility = View.GONE
            binding.timeBox.visibility = View.VISIBLE
            binding.cardDate.visibility = View.VISIBLE
            binding.cardTable.visibility = View.VISIBLE
            binding.rowCounters.visibility = View.VISIBLE
            binding.txtFooterLink.visibility = View.VISIBLE
            binding.btnPrevDay.visibility = View.VISIBLE
            binding.btnNextDay.visibility = View.VISIBLE
            binding.btnSettings.visibility = View.VISIBLE

            val base = vm.hadithText.value?.trim()
            if (!base.isNullOrEmpty()) {
                binding.cardHadith.visibility = View.VISIBLE
                binding.txtHadith.text = base
                binding.txtHadithTitle.text = getString(com.mosque.prayer.R.string.hadith_title)
                binding.txtHadithTitle.visibility = View.VISIBLE
            } else {
                // If there is no hadith content, hide the empty card to avoid a blank capsule at bottom
                binding.cardHadith.visibility = View.GONE
                binding.txtHadithTitle.visibility = View.GONE
            }
            // No override active; allow hadith observer to resume updates naturally
        }
    }

    private fun updateBackgroundForDay(cal: Calendar) {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isFriday = (dayOfWeek == Calendar.FRIDAY)
        if (isFriday) {
            // Choisir l'image selon l'heure réelle de la journée.
            // Si les horaires de prière sont disponibles, considérer
            // la journée entre Duha (après lever) et Maghrib (coucher).
            // Sinon, repli sur une fenêtre horaire 06:00–17:59.
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val nowMs = cal.timeInMillis
            val isDayTime = vm.prayerTimes.value?.let { pt ->
                nowMs >= pt.duha && nowMs < pt.maghrib
            } ?: (hour in 6..17)

            val resId = if (isDayTime) {
                com.mosque.prayer.R.drawable.vendredi2
            } else {
                com.mosque.prayer.R.drawable.vendredi
            }
            binding.imgBackground.setImageResource(resId)
            Log.d(logTag, "updateBackgroundForDay: Friday hour=${hour} isDayTime=${isDayTime} resId=${resId}")
        } else {
            binding.imgBackground.setImageResource(com.mosque.prayer.R.drawable.arriereplan)
            Log.d(logTag, "updateBackgroundForDay: not Friday -> arriereplan")
        }
    }
}

private fun MainActivity.styleAthkarBullets(text: String): CharSequence {
    // Make leading bullets bigger and bold on each bullet line, while keeping RTL order.
    val lines = text.split('\n')
    val out = SpannableStringBuilder()
    var first = true
    for (line in lines) {
        if (!first) out.append('\n') else first = false
        if (line.startsWith("•") || line.startsWith("●")) {
            // Keep the regular bullet but make it slightly larger
            out.append('•')
            val start = out.length - 1
            val end = out.length
            out.setSpan(RelativeSizeSpan(2.0f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Append the rest of the original line after the bullet character
            out.append(line.substring(1))
        } else {
            out.append(line)
        }
    }
    return out
}

private fun String.toLatinDigits(): String {
    val arabicIndic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val easternArabicIndic = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val builder = StringBuilder(length)
    for (ch in this) {
        val indexArabic = arabicIndic.indexOf(ch)
        val indexEastern = if (indexArabic == -1) easternArabicIndic.indexOf(ch) else -1
        val idx = if (indexArabic != -1) indexArabic else indexEastern
        if (idx != -1) {
            builder.append('0' + idx)
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}
