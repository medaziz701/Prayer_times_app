package com.mosque.prayer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog      // <-- AJOUTER CETTE LIGNE
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mosque.prayer.data.AppPreferences
import com.mosque.prayer.data.CitiesRepository
import com.mosque.prayer.databinding.ActivitySettingsBinding
import com.mosque.prayer.viewmodel.MainViewModel
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val vm: MainViewModel by viewModels()

    private lateinit var prefs: AppPreferences

    private var year = 0
    private var month = 0
    private var day = 0

    private var hour = 0
    private var minute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        prefs = AppPreferences(this)

        // Load city dataset from res/raw/cities.txt (once per process)
        CitiesRepository.ensureLoaded(this)

        vm.reload()
        val city = vm.city.value ?: CitiesRepository.findById(AppPreferences.DEFAULT_CITY_ID)
        binding.btnCity.text = city.nameAr

        binding.btnCity.setOnClickListener {
            // Toujours utiliser la ville *actuelle* comme sélectionnée dans le popup
            val currentCityId = vm.city.value?.id ?: AppPreferences.DEFAULT_CITY_ID
            CityPickerDialog.show(this, currentCityId) { selected ->
                vm.setCity(selected.id)
                binding.btnCity.text = selected.nameAr
            }
        }

        // Initialiser la date affichée soit avec la date sauvegardée, soit avec la date actuelle
        val savedYear = prefs.selectedDateYear
        val savedMonth = prefs.selectedDateMonth
        val savedDay = prefs.selectedDateDay

        if (savedYear > 0 && savedMonth in 1..12 && savedDay > 0) {
            year = savedYear
            month = savedMonth - 1
            day = savedDay
        } else {
            val cal = Calendar.getInstance()
            year = cal.get(Calendar.YEAR)
            month = cal.get(Calendar.MONTH)
            day = cal.get(Calendar.DAY_OF_MONTH)
        }
        updateDateViews()

        // Ouvrir un DatePicker au clic sur n'importe quel champ de date
        binding.edtDay.setOnClickListener { showDatePicker() }
        binding.edtMonth.setOnClickListener { showDatePicker() }
        binding.edtYear.setOnClickListener { showDatePicker() }

        // Initialiser l'heure affichée soit avec l'heure sauvegardée, soit avec l'heure actuelle
        val savedHour = prefs.selectedTimeHour
        val savedMinute = prefs.selectedTimeMinute
        if (savedHour in 0..23 && savedMinute in 0..59) {
            hour = savedHour
            minute = savedMinute
        } else {
            val nowCal = Calendar.getInstance()
            hour = nowCal.get(Calendar.HOUR_OF_DAY)
            minute = nowCal.get(Calendar.MINUTE)
        }
        updateTimeViews()

        // Ouvrir un TimePicker au clic sur l'heure ou les minutes
        binding.edtHour.setOnClickListener { showTimePicker() }
        binding.edtMinute.setOnClickListener { showTimePicker() }

        binding.btn24.setOnClickListener { vm.setUse24h(true); updateFormatButtons() }
        binding.btn12.setOnClickListener { vm.setUse24h(false); updateFormatButtons() }

        // Background mode is now fixed to BG_IMAGE and the row is hidden,
        // so these buttons should not change anything.
        binding.btnBgImage.setOnClickListener { }
        binding.btnBgPlain.setOnClickListener { }

        binding.btnHadithManual.setOnClickListener {
            showHadithIntervalDialog()
        }
        binding.btnHadithEveryPrayer.setOnClickListener {
            vm.setHadithUpdateMode(AppPreferences.HADITH_UPDATE_EVERY_PRAYER)
            updateHadithButtons()
        }

        // New "ضبط" button: opens Delay Times dialog (per-prayer minute offset)
        binding.btnDelay.setOnClickListener {
            DelayTimesDialog.show(this) {
                // Recalculate with new offsets
                vm.recalcToday()
            }
        }

        // Single flip button that toggles the current state and rotates the screen
        binding.btnFlipOn.setOnClickListener {
            val current = vm.screenFlip.value ?: false
            val newFlip = !current
            vm.setScreenFlip(newFlip)
            updateFlipButtons()

            // Apply the same orientation immediately in the settings screen
            requestedOrientation = if (newFlip) {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        binding.btnSave.setOnClickListener {
            finish()
        }

        updateFormatButtons()
        updateBgButtons()
        updateHadithButtons()
        updateFlipButtons()

        binding.btnCity.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun updateFormatButtons() {
        val use24 = vm.use24h.value ?: true
        // Always keep both format pills fully opaque (no greyed-out effect)
        binding.btn24.alpha = 1f
        binding.btn12.alpha = 1f
        setChecked(binding.btn24, use24)
        setChecked(binding.btn12, !use24)
    }

    private fun updateBgButtons() {
        val mode = vm.bgMode.value ?: AppPreferences.BG_IMAGE
        binding.btnBgImage.alpha = if (mode == AppPreferences.BG_IMAGE) 1f else 0.85f
        binding.btnBgPlain.alpha = if (mode == AppPreferences.BG_PLAIN) 1f else 0.85f
        binding.imgBackgroundSettings.alpha = if (mode == AppPreferences.BG_PLAIN) 0f else 1f
        setChecked(binding.btnBgImage, mode == AppPreferences.BG_IMAGE)
        setChecked(binding.btnBgPlain, mode == AppPreferences.BG_PLAIN)
    }

    private fun updateHadithButtons() {
        val mode = vm.hadithUpdateMode.value ?: AppPreferences.HADITH_UPDATE_HOURLY

        val isManual = mode == AppPreferences.HADITH_UPDATE_MANUAL
        // Any non-manual mode (including legacy 30-min / hourly) is visually mapped
        // to the "every prayer" pill so one of the two pills is always active.
        val isEveryPrayer = mode == AppPreferences.HADITH_UPDATE_EVERY_PRAYER || !isManual

        // Always keep both pills fully opaque (no greyed-out effect)
        binding.btnHadithManual.alpha = 1f
        binding.btnHadithEveryPrayer.alpha = 1f

        setChecked(binding.btnHadithManual, isManual)
        setChecked(binding.btnHadithEveryPrayer, isEveryPrayer)
    }

    private fun updateFlipButtons() {
        val flip = vm.screenFlip.value ?: false
        // Keep the flip pill fully opaque; use only the check icon to show state
        binding.btnFlipOn.alpha = 1f
        setChecked(binding.btnFlipOn, flip)
    }

    private fun setChecked(pill: TextView, checked: Boolean) {
        val density = resources.displayMetrics.density
        val iconSize = (14 * density).toInt() // smaller check icon
        val edgeGap = (10 * density).toInt()  // keep icon near the outer edge

        if (checked) {
            val d = ContextCompat.getDrawable(this, com.mosque.prayer.R.drawable.ic_check_circle)
            d?.mutate()
            d?.alpha = 255
            // Scale icon smaller and place it at visual START (right in RTL)
            if (d != null) {
                d.setBounds(0, 0, iconSize, iconSize)
            }
            pill.setCompoundDrawablesRelative(d, null, null, null)
            pill.compoundDrawablePadding = 0

            // Keep text centered: total space at START (icon + gap) == total space at END (gap)
            val padStart = edgeGap
            val padEnd = edgeGap + iconSize
            pill.setPaddingRelative(padStart, pill.paddingTop, padEnd, pill.paddingBottom)
        } else {
            // Remove drawable entirely; no extra width is reserved
            pill.setCompoundDrawablesRelative(null, null, null, null)
            pill.compoundDrawablePadding = 0
            // No artificial padding; keep it perfectly centered
            pill.setPaddingRelative(0, pill.paddingTop, 0, pill.paddingBottom)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply {
            if (year > 0) set(Calendar.YEAR, year)
            if (month >= 0) set(Calendar.MONTH, month)
            if (day > 0) set(Calendar.DAY_OF_MONTH, day)
        }

        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                year = y
                month = m
                day = d
                // Mettre à jour l'affichage
                updateDateViews()
                // Sauvegarder la date choisie pour que l'écran principal puisse l'utiliser
                prefs.selectedDateYear = year
                prefs.selectedDateMonth = month + 1
                prefs.selectedDateDay = day
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    private fun updateDateViews() {
        binding.edtDay.text = String.format("%02d", day)
        binding.edtMonth.text = String.format("%02d", month + 1)
        binding.edtYear.text = year.toString()
    }

    private fun showTimePicker() {
        val use24 = vm.use24h.value ?: true
        val dialog = TimePickerDialog(
            this,
            { _, h, m ->
                hour = h
                minute = m
                updateTimeViews()
                prefs.selectedTimeHour = hour
                prefs.selectedTimeMinute = minute
            },
            hour,
            minute,
            use24
        )

        dialog.show()
    }

    private fun updateTimeViews() {
        binding.edtHour.text = String.format("%02d", hour)
        binding.edtMinute.text = String.format("%02d", minute)
    }

    private fun showHadithIntervalDialog() {
        val current = prefs.hadithCustomIntervalMinutes
        HadithIntervalDialog.show(this, current) { chosen ->
            prefs.hadithCustomIntervalMinutes = chosen
            vm.setHadithUpdateMode(AppPreferences.HADITH_UPDATE_MANUAL)
            updateHadithButtons()
        }
    }
}
