package com.mosque.prayer.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var cityId: String
        get() = prefs.getString(KEY_CITY_ID, DEFAULT_CITY_ID) ?: DEFAULT_CITY_ID
        set(value) = prefs.edit().putString(KEY_CITY_ID, value).apply()

    var use24Hour: Boolean
        get() = prefs.getBoolean(KEY_24H, true)
        set(value) = prefs.edit().putBoolean(KEY_24H, value).apply()

    var bgMode: String
        get() = prefs.getString(KEY_BG_MODE, BG_IMAGE) ?: BG_IMAGE
        set(value) = prefs.edit().putString(KEY_BG_MODE, value).apply()

    var activationCode: String
        get() = prefs.getString(KEY_ACTIVATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACTIVATION, value).apply()

    var hadithUpdateMode: String
        get() = prefs.getString(KEY_HADITH_UPDATE_MODE, HADITH_UPDATE_HOURLY) ?: HADITH_UPDATE_HOURLY
        set(value) = prefs.edit().putString(KEY_HADITH_UPDATE_MODE, value).apply()

    var screenFlip: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_FLIP, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_FLIP, value).apply()

    var screenTemplate: String
        get() = prefs.getString(KEY_SCREEN_TEMPLATE, TEMPLATE_A) ?: TEMPLATE_A
        set(value) = prefs.edit().putString(KEY_SCREEN_TEMPLATE, value).apply()

    var hadithIndex: Int
        get() = prefs.getInt(KEY_HADITH_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_HADITH_INDEX, value).apply()

    var hadithLastUpdateMillis: Long
        get() = prefs.getLong(KEY_HADITH_LAST_UPDATE, 0L)
        set(value) = prefs.edit().putLong(KEY_HADITH_LAST_UPDATE, value).apply()

    var selectedDateYear: Int
        get() = prefs.getInt(KEY_SELECTED_DATE_YEAR, 0)
        set(value) = prefs.edit().putInt(KEY_SELECTED_DATE_YEAR, value).apply()

    var selectedDateMonth: Int
        get() = prefs.getInt(KEY_SELECTED_DATE_MONTH, 0)
        set(value) = prefs.edit().putInt(KEY_SELECTED_DATE_MONTH, value).apply()

    var selectedDateDay: Int
        get() = prefs.getInt(KEY_SELECTED_DATE_DAY, 0)
        set(value) = prefs.edit().putInt(KEY_SELECTED_DATE_DAY, value).apply()

    var selectedTimeHour: Int
        get() = prefs.getInt(KEY_SELECTED_TIME_HOUR, -1)
        set(value) = prefs.edit().putInt(KEY_SELECTED_TIME_HOUR, value).apply()

    var selectedTimeMinute: Int
        get() = prefs.getInt(KEY_SELECTED_TIME_MINUTE, -1)
        set(value) = prefs.edit().putInt(KEY_SELECTED_TIME_MINUTE, value).apply()

    var hadithCustomIntervalMinutes: Int
        get() = prefs.getInt(KEY_HADITH_CUSTOM_INTERVAL_MINUTES, 60)
        set(value) = prefs.edit().putInt(KEY_HADITH_CUSTOM_INTERVAL_MINUTES, value).apply()

    var migratedDuhaIqamaTo10: Boolean
        get() = prefs.getBoolean(KEY_MIGR_DUHA_IQAMA_10, false)
        set(value) = prefs.edit().putBoolean(KEY_MIGR_DUHA_IQAMA_10, value).apply()

    var calcMethod: String
        get() = prefs.getString(KEY_CALC_METHOD, CALC_AUTO) ?: CALC_AUTO
        set(value) = prefs.edit().putString(KEY_CALC_METHOD, value).apply()

    fun getIqamaMinutes(prayerKey: String, defaultMinutes: Int): Int {
        return prefs.getInt(KEY_IQAMA_PREFIX + prayerKey, defaultMinutes)
    }

    fun setIqamaMinutes(prayerKey: String, minutes: Int) {
        prefs.edit().putInt(KEY_IQAMA_PREFIX + prayerKey, minutes).apply()
    }

    fun getAdhanOffsetMinutes(name: String): Int {
        return prefs.getInt(KEY_ADHAN_OFFSET_PREFIX + name, 0)
    }

    fun setAdhanOffsetMinutes(name: String, minutes: Int) {
        prefs.edit().putInt(KEY_ADHAN_OFFSET_PREFIX + name, minutes).apply()
    }

    companion object {
        private const val FILE_NAME = "mosque_prayer_prefs"
        private const val KEY_CITY_ID = "city_id"
        private const val KEY_24H = "use_24h"
        private const val KEY_BG_MODE = "bg_mode"
        private const val KEY_ACTIVATION = "activation_code"
        private const val KEY_IQAMA_PREFIX = "iqama_"

        private const val KEY_HADITH_UPDATE_MODE = "hadith_update_mode"
        private const val KEY_SCREEN_FLIP = "screen_flip"
        private const val KEY_SCREEN_TEMPLATE = "screen_template"
        private const val KEY_HADITH_INDEX = "hadith_index"
        private const val KEY_HADITH_LAST_UPDATE = "hadith_last_update"

        private const val KEY_SELECTED_DATE_YEAR = "selected_date_year"
        private const val KEY_SELECTED_DATE_MONTH = "selected_date_month"
        private const val KEY_SELECTED_DATE_DAY = "selected_date_day"

        private const val KEY_SELECTED_TIME_HOUR = "selected_time_hour"
        private const val KEY_SELECTED_TIME_MINUTE = "selected_time_minute"

        private const val KEY_HADITH_CUSTOM_INTERVAL_MINUTES = "hadith_custom_interval_minutes"
        private const val KEY_MIGR_DUHA_IQAMA_10 = "migr_duha_iqama_to_10"

        private const val KEY_CALC_METHOD = "calc_method"

        private const val KEY_ADHAN_OFFSET_PREFIX = "adhan_offset_"

        const val BG_IMAGE = "image"
        const val BG_PLAIN = "plain"

        const val HADITH_UPDATE_EVERY_PRAYER = "every_prayer"
        const val HADITH_UPDATE_HOURLY = "hourly"
        const val HADITH_UPDATE_30_MIN = "30_min"
        const val HADITH_UPDATE_MANUAL = "manual"

        const val TEMPLATE_A = "A"
        const val TEMPLATE_B = "B"

        const val DEFAULT_CITY_ID = "jeddah"

        const val CALC_AUTO = "auto"
        const val CALC_UMM_AL_QURA = "umm_al_qura"
        const val CALC_MWL = "mwl"
        const val CALC_EGYPTIAN = "egyptian"
        const val CALC_KARACHI = "karachi"
        const val CALC_ISNA = "isna"
        const val CALC_DIYANET = "diyanet"
    }
}
