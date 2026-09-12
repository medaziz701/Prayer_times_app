package com.mosque.prayer.data

import android.content.Context
import com.mosque.prayer.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

object CitiesRepository {

    @Volatile
    private var initialized = false
    private var cities: List<City> = fallbackCities()

    // Internal parsed row to select a single central entry per city
    private data class CityRow(
        val id: String,
        val nameAr: String,
        val lat: Double,
        val lon: Double,
        val tz: String,
        val fcode: String,
        val admin1: String,
        val population: Long
    )

    private fun fallbackCities(): List<City> = listOf(
        City("jeddah", "جدة", 21.4858, 39.1925, "Asia/Riyadh"),
        City("riyadh", "الرياض", 24.7136, 46.6753, "Asia/Riyadh"),
        City("dammam", "الشرقية", 26.4207, 50.0888, "Asia/Riyadh"),
        City("taif", "الطائف", 21.2703, 40.4158, "Asia/Riyadh"),
        City("tabuk", "تبوك", 28.3835, 36.5662, "Asia/Riyadh")
    )

    fun ensureLoaded(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val rows = mutableListOf<CityRow>()
            try {
                context.resources.openRawResource(R.raw.cities).use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).useLines { lines ->
                        lines.forEach { line ->
                            parseLine(line)?.let { rows.add(it) }
                        }
                    }
                }
                if (rows.isNotEmpty()) {
                    // Deduplicate by (admin1 + normalized Arabic name) and
                    // prefer central entries: PPLC>PPLA>PPLA2>PPLA3>PPLA4>PPL(PPLX excluded), then highest population
                    val best = LinkedHashMap<String, CityRow>()
                    for (r in rows) {
                        val key = r.admin1 + ":" + normalizeArabicName(r.nameAr)
                        val current = best[key]
                        if (current == null || score(r) > score(current)) {
                            best[key] = r
                        }
                    }
                    val deduped = best.values.map { City(it.id, it.nameAr, it.lat, it.lon, it.tz) }
                    cities = deduped.sortedBy { it.nameAr }
                }
            } catch (_: Exception) {
                // Keep fallback list on any error
            } finally {
                initialized = true
            }
        }
    }

    private fun parseLine(line: String): CityRow? {
        // GeoNames allCountries.tsv format (tab-separated)
        // 0:id 1:name 2:asciiname 3:alternatenames 4:lat 5:lon 6:fclass 7:fcode 8:country 10:admin1 11:admin2 14:population 17:timezone
        val parts = line.split('\t')
        if (parts.size < 18) return null
        val country = parts[8]
        if (country != "SA") return null
        val fclass = parts[6]
        if (fclass != "P") return null // keep only populated places
        val fcode = parts[7]
        if (fcode == "PPLX") return null // exclude sections of populated places

        val name = parts[1]
        val ascii = parts[2]
        val alt = parts[3]
        val lat = parts[4].toDoubleOrNull() ?: return null
        val lon = parts[5].toDoubleOrNull() ?: return null
        val admin1 = parts.getOrNull(10) ?: ""
        val pop = parts.getOrNull(14)?.toLongOrNull() ?: 0L
        val tzRaw = parts[17]
        val tz = if (tzRaw.isBlank()) "Asia/Riyadh" else tzRaw

        val arFromAlt = extractArabic(alt)
        val nameAr = when {
            arFromAlt.isNotBlank() -> arFromAlt
            containsArabic(name) -> name
            else -> name
        }.trim()

        val id = makeId(ascii)
        if (id.isBlank()) return null
        return CityRow(
            id = id,
            nameAr = nameAr.ifBlank { name },
            lat = lat,
            lon = lon,
            tz = tz,
            fcode = fcode,
            admin1 = admin1,
            population = pop
        )
    }

    private fun containsArabic(s: String): Boolean = s.any { it in '\u0600'..'\u06FF' }

    private fun extractArabic(alt: String): String {
        if (alt.isBlank()) return ""
        val items = alt.split(',')
        return items.firstOrNull { containsArabic(it) }?.trim() ?: ""
    }

    private fun makeId(ascii: String): String {
        val slug = ascii.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return if (slug.isNotBlank()) slug else ascii.lowercase(Locale.ROOT)
    }

    private fun normalizeArabicName(s: String): String {
        val noTashkeel = s.replace(Regex("[\u064B-\u0652\u0670\u0640]"), "")
        return noTashkeel.trim()
            .replace("\u200F", "")
            .replace("\u200E", "")
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private fun fcodeRank(code: String): Int = when (code) {
        "PPLC" -> 6 // capital
        "PPLA" -> 5 // admin1 seat
        "PPLA2" -> 4
        "PPLA3" -> 3
        "PPLA4" -> 2
        "PPL" -> 1 // generic populated place
        else -> 0
    }

    private fun score(r: CityRow): Long {
        val rank = fcodeRank(r.fcode)
        return rank * 1_000_000_000L + r.population.coerceAtLeast(0L)
    }

    fun all(): List<City> = cities

    fun findById(id: String): City = cities.firstOrNull { it.id == id } ?: cities.first()

    fun search(query: String): List<City> {
        val q = query.trim()
        if (q.isEmpty()) return cities
        return cities.filter { it.nameAr.contains(q, ignoreCase = true) || it.id.contains(q.lowercase(Locale.ROOT)) }
    }
}
