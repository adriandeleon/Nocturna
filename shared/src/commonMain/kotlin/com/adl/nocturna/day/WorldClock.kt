package com.adl.nocturna.day

import com.adl.nocturna.data.WorldZone
import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.formatTime
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The longest label the desk clock shows; longer ones end in "…". */
const val WORLD_LABEL_MAX = 16

/**
 * How many calendar days [other]'s date is ahead of [home]'s at [now]: 1 when it is already tomorrow
 * there, -1 when it is still yesterday, 0 on the same day.
 */
fun dayOffset(now: Instant, home: TimeZone, other: TimeZone): Int =
    now.toLocalDateTime(home).date.daysUntil(now.toLocalDateTime(other).date)

/** "+1" or "−1" (a real minus sign) for a different calendar day, and nothing for the same one. */
fun dayOffsetMark(offset: Int): String = when {
    offset > 0 -> "+$offset"
    offset < 0 -> "−${-offset}"
    else -> ""
}

/** [label] trimmed and cut to [max] characters, so one long name doesn't crowd the desk clock. */
fun shortLabel(label: String, max: Int = WORLD_LABEL_MAX): String {
    val trimmed = label.trim()
    return if (trimmed.length <= max) trimmed else trimmed.take(max - 1).trimEnd() + "…"
}

/** [id]'s zone, or null when this phone's time zone data doesn't know it. */
fun timeZoneOrNull(id: String): TimeZone? = try {
    TimeZone.of(id)
} catch (_: IllegalTimeZoneException) {
    null
}

/**
 * [zone]'s time at [now] as the desk clock shows it, in the user's 12/24 h format, e.g. "London 19:42" or
 * "Tokyo 04:42 +1", with the day offset from [home]. Null when the zone is unknown on this phone.
 */
fun worldTime(zone: WorldZone, now: Instant, home: TimeZone, use24h: Boolean, locale: PlatformLocale): String? {
    val tz = timeZoneOrNull(zone.zoneId) ?: return null
    val time = formatTime(now.toLocalDateTime(tz).time, use24h, locale)
    val mark = dayOffsetMark(dayOffset(now, home, tz))
    return listOf(shortLabel(zone.label), time, mark).filter { it.isNotEmpty() }.joinToString(" ")
}

/**
 * The desk clock's world clock line, e.g. "London 19:42 · Tokyo 04:42 +1", or null with nothing to show.
 * Each zone's own spaces don't break, so a narrow clock wraps the line only between zones.
 */
fun worldClockLine(zones: List<WorldZone>, now: Instant, home: TimeZone, use24h: Boolean, locale: PlatformLocale): String? =
    zones.mapNotNull { worldTime(it, now, home, use24h, locale)?.replace(' ', ' ') }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(" · ")

/** Whether day mode's desk clock keeps its world clock line, and whether the sticky note shows under it. */
data class DeskFit(val worldLine: Boolean, val note: Boolean)

/**
 * Landscape: the desk clock is [withLine] tall with its world clock line and [without] tall without it, and
 * the sticky note at its smallest is [noteMin] tall (null when there's no note to show), all to fit in [max]
 * with a [gap] before the note. The note keeps the room it had before the world clock, so when only one
 * of them fits the line is the one left out; with no note the line shows whenever the clock still fits.
 */
fun deskFit(withLine: Int, without: Int, gap: Int, noteMin: Int?, max: Int): DeskFit = when {
    noteMin != null && noteFits(withLine, gap, noteMin, max) -> DeskFit(worldLine = true, note = true)
    noteMin != null && noteFits(without, gap, noteMin, max) -> DeskFit(worldLine = false, note = true)
    else -> DeskFit(worldLine = withLine <= max, note = false)
}

/** The most of portrait day mode's height the desk clock may take with its world clock line. */
const val PORTRAIT_CLOCK_SHARE = 0.4f

/**
 * Portrait: whether the world clock line stays, the desk clock being [withLine] tall with it out of [max],
 * so the calendar page below keeps most of the screen.
 */
fun worldLineFitsPortrait(withLine: Int, max: Int): Boolean = withLine <= max * PORTRAIT_CLOCK_SHARE

/** A city in the world clock's picker: what it's called, its country, and its IANA zone. */
data class City(val name: String, val country: String, val zoneId: String)

/** One row of the picker's search results; [detail] is the country, or the zone ID for a zone found by ID. */
data class ZoneChoice(val label: String, val detail: String, val zoneId: String) {
    fun toWorldZone() = WorldZone(shortLabel(label), zoneId)
}

/**
 * The picker's results for [query]: cities from [CITIES] whose name (best) or country matches, or whose
 * zone ID does when the query has a "/" in it; then, for anything else, zone IDs from [allZoneIds] that contain it, labelled with their last part
 * ("America/Argentina/Cordoba" is "Cordoba"). A blank query lists every city. Letters match without
 * their accents, so "sao" finds São Paulo.
 */
fun searchZones(query: String, allZoneIds: Collection<String>, limit: Int = 50): List<ZoneChoice> {
    val q = fold(query)
    if (q.isEmpty()) return CITIES.sortedBy { fold(it.name) }.map { ZoneChoice(it.name, it.country, it.zoneId) }
    // Cities match by zone only when an ID is typed, so "london" doesn't bring Edinburgh along.
    val idTyped = '/' in query
    fun rank(city: City): Int? {
        val name = fold(city.name)
        return when {
            name.startsWith(q) -> 0
            name.split(' ').any { it.startsWith(q) } -> 1
            name.contains(q) -> 2
            fold(city.country).startsWith(q) -> 3
            idTyped && fold(city.zoneId).contains(q) -> 4
            else -> null
        }
    }
    val cities = CITIES.mapNotNull { c -> rank(c)?.let { c to it } }
        .sortedWith(compareBy({ it.second }, { fold(it.first.name) }))
        .map { (c, _) -> ZoneChoice(c.name, c.country, c.zoneId) }
    val listed = cities.map { it.zoneId }.toSet()
    val byId = allZoneIds.asSequence()
        .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") && it !in listed && fold(it).contains(q) }
        .sorted()
        .map { ZoneChoice(it.substringAfterLast('/').replace('_', ' '), it, it) }
        .toList()
    return (cities + byId).take(limit)
}

/** [text] lower-cased, without accents, punctuation or extra spaces, for matching names however they're typed. */
private fun fold(text: String): String {
    val out = StringBuilder()
    for (ch in text.lowercase()) {
        val c = ACCENTS[ch] ?: ch
        when {
            c.isLetterOrDigit() -> out.append(c)
            c == ' ' || c == '_' || c == '/' || c == '-' -> if (out.isNotEmpty() && out.last() != ' ') out.append(' ')
        }
    }
    return out.toString().trimEnd()
}

private val ACCENTS: Map<Char, Char> = buildMap {
    fun put(from: String, to: Char) = from.forEach { put(it, to) }
    put("áàâäãåā", 'a')
    put("éèêëē", 'e')
    put("íìîïī", 'i')
    put("óòôöõøō", 'o')
    put("úùûüū", 'u')
    put("ñ", 'n')
    put("ç", 'c')
    put("ş", 's')
    put("ý", 'y')
}

/** About 150 big cities, by IANA zone; names that share a zone are listed apart so each can be found. */
val CITIES: List<City> = listOf(
    // North America
    City("New York", "United States", "America/New_York"),
    City("Washington", "United States", "America/New_York"),
    City("Boston", "United States", "America/New_York"),
    City("Miami", "United States", "America/New_York"),
    City("Atlanta", "United States", "America/New_York"),
    City("Detroit", "United States", "America/Detroit"),
    City("Chicago", "United States", "America/Chicago"),
    City("Houston", "United States", "America/Chicago"),
    City("Dallas", "United States", "America/Chicago"),
    City("Denver", "United States", "America/Denver"),
    City("Phoenix", "United States", "America/Phoenix"),
    City("Los Angeles", "United States", "America/Los_Angeles"),
    City("San Francisco", "United States", "America/Los_Angeles"),
    City("Seattle", "United States", "America/Los_Angeles"),
    City("Las Vegas", "United States", "America/Los_Angeles"),
    City("Anchorage", "United States", "America/Anchorage"),
    City("Honolulu", "United States", "Pacific/Honolulu"),
    City("Toronto", "Canada", "America/Toronto"),
    City("Montreal", "Canada", "America/Toronto"),
    City("Vancouver", "Canada", "America/Vancouver"),
    City("Calgary", "Canada", "America/Edmonton"),
    City("Edmonton", "Canada", "America/Edmonton"),
    City("Winnipeg", "Canada", "America/Winnipeg"),
    City("Halifax", "Canada", "America/Halifax"),
    City("St. John's", "Canada", "America/St_Johns"),
    City("Mexico City", "Mexico", "America/Mexico_City"),
    City("Guadalajara", "Mexico", "America/Mexico_City"),
    City("Monterrey", "Mexico", "America/Monterrey"),
    City("Cancún", "Mexico", "America/Cancun"),
    City("Tijuana", "Mexico", "America/Tijuana"),
    City("Chihuahua", "Mexico", "America/Chihuahua"),
    City("Hermosillo", "Mexico", "America/Hermosillo"),
    City("Mazatlán", "Mexico", "America/Mazatlan"),
    // Central America and the Caribbean
    City("Guatemala City", "Guatemala", "America/Guatemala"),
    City("San Salvador", "El Salvador", "America/El_Salvador"),
    City("Tegucigalpa", "Honduras", "America/Tegucigalpa"),
    City("Managua", "Nicaragua", "America/Managua"),
    City("San José", "Costa Rica", "America/Costa_Rica"),
    City("Panama City", "Panama", "America/Panama"),
    City("Havana", "Cuba", "America/Havana"),
    City("Santo Domingo", "Dominican Republic", "America/Santo_Domingo"),
    City("San Juan", "Puerto Rico", "America/Puerto_Rico"),
    City("Kingston", "Jamaica", "America/Jamaica"),
    // South America
    City("Bogotá", "Colombia", "America/Bogota"),
    City("Caracas", "Venezuela", "America/Caracas"),
    City("Quito", "Ecuador", "America/Guayaquil"),
    City("Lima", "Peru", "America/Lima"),
    City("La Paz", "Bolivia", "America/La_Paz"),
    City("Santiago", "Chile", "America/Santiago"),
    City("Buenos Aires", "Argentina", "America/Argentina/Buenos_Aires"),
    City("Montevideo", "Uruguay", "America/Montevideo"),
    City("Asunción", "Paraguay", "America/Asuncion"),
    City("São Paulo", "Brazil", "America/Sao_Paulo"),
    City("Rio de Janeiro", "Brazil", "America/Sao_Paulo"),
    City("Brasília", "Brazil", "America/Sao_Paulo"),
    City("Manaus", "Brazil", "America/Manaus"),
    // Europe
    City("London", "United Kingdom", "Europe/London"),
    City("Manchester", "United Kingdom", "Europe/London"),
    City("Edinburgh", "United Kingdom", "Europe/London"),
    City("Dublin", "Ireland", "Europe/Dublin"),
    City("Reykjavík", "Iceland", "Atlantic/Reykjavik"),
    City("Lisbon", "Portugal", "Europe/Lisbon"),
    City("Madrid", "Spain", "Europe/Madrid"),
    City("Barcelona", "Spain", "Europe/Madrid"),
    City("Paris", "France", "Europe/Paris"),
    City("Brussels", "Belgium", "Europe/Brussels"),
    City("Amsterdam", "Netherlands", "Europe/Amsterdam"),
    City("Luxembourg", "Luxembourg", "Europe/Luxembourg"),
    City("Zurich", "Switzerland", "Europe/Zurich"),
    City("Geneva", "Switzerland", "Europe/Zurich"),
    City("Berlin", "Germany", "Europe/Berlin"),
    City("Munich", "Germany", "Europe/Berlin"),
    City("Frankfurt", "Germany", "Europe/Berlin"),
    City("Hamburg", "Germany", "Europe/Berlin"),
    City("Vienna", "Austria", "Europe/Vienna"),
    City("Rome", "Italy", "Europe/Rome"),
    City("Milan", "Italy", "Europe/Rome"),
    City("Copenhagen", "Denmark", "Europe/Copenhagen"),
    City("Oslo", "Norway", "Europe/Oslo"),
    City("Stockholm", "Sweden", "Europe/Stockholm"),
    City("Helsinki", "Finland", "Europe/Helsinki"),
    City("Tallinn", "Estonia", "Europe/Tallinn"),
    City("Riga", "Latvia", "Europe/Riga"),
    City("Vilnius", "Lithuania", "Europe/Vilnius"),
    City("Warsaw", "Poland", "Europe/Warsaw"),
    City("Prague", "Czechia", "Europe/Prague"),
    City("Budapest", "Hungary", "Europe/Budapest"),
    City("Zagreb", "Croatia", "Europe/Zagreb"),
    City("Belgrade", "Serbia", "Europe/Belgrade"),
    City("Bucharest", "Romania", "Europe/Bucharest"),
    City("Sofia", "Bulgaria", "Europe/Sofia"),
    City("Athens", "Greece", "Europe/Athens"),
    City("Istanbul", "Türkiye", "Europe/Istanbul"),
    // Older time zone data on some phones knows Kyiv only by its old spelling.
    City("Kyiv", "Ukraine", "Europe/Kiev"),
    City("Minsk", "Belarus", "Europe/Minsk"),
    City("Moscow", "Russia", "Europe/Moscow"),
    City("Saint Petersburg", "Russia", "Europe/Moscow"),
    // Africa
    City("Casablanca", "Morocco", "Africa/Casablanca"),
    City("Algiers", "Algeria", "Africa/Algiers"),
    City("Tunis", "Tunisia", "Africa/Tunis"),
    City("Cairo", "Egypt", "Africa/Cairo"),
    City("Dakar", "Senegal", "Africa/Dakar"),
    City("Accra", "Ghana", "Africa/Accra"),
    City("Lagos", "Nigeria", "Africa/Lagos"),
    City("Kinshasa", "DR Congo", "Africa/Kinshasa"),
    City("Luanda", "Angola", "Africa/Luanda"),
    City("Addis Ababa", "Ethiopia", "Africa/Addis_Ababa"),
    City("Nairobi", "Kenya", "Africa/Nairobi"),
    City("Kampala", "Uganda", "Africa/Kampala"),
    City("Dar es Salaam", "Tanzania", "Africa/Dar_es_Salaam"),
    City("Johannesburg", "South Africa", "Africa/Johannesburg"),
    City("Cape Town", "South Africa", "Africa/Johannesburg"),
    // Middle East
    City("Jerusalem", "Israel", "Asia/Jerusalem"),
    City("Tel Aviv", "Israel", "Asia/Jerusalem"),
    City("Beirut", "Lebanon", "Asia/Beirut"),
    City("Amman", "Jordan", "Asia/Amman"),
    City("Baghdad", "Iraq", "Asia/Baghdad"),
    City("Riyadh", "Saudi Arabia", "Asia/Riyadh"),
    City("Jeddah", "Saudi Arabia", "Asia/Riyadh"),
    City("Kuwait City", "Kuwait", "Asia/Kuwait"),
    City("Doha", "Qatar", "Asia/Qatar"),
    City("Dubai", "United Arab Emirates", "Asia/Dubai"),
    City("Abu Dhabi", "United Arab Emirates", "Asia/Dubai"),
    City("Tehran", "Iran", "Asia/Tehran"),
    // Asia
    City("Tashkent", "Uzbekistan", "Asia/Tashkent"),
    City("Almaty", "Kazakhstan", "Asia/Almaty"),
    City("Karachi", "Pakistan", "Asia/Karachi"),
    City("Lahore", "Pakistan", "Asia/Karachi"),
    City("Delhi", "India", "Asia/Kolkata"),
    City("Mumbai", "India", "Asia/Kolkata"),
    City("Bengaluru", "India", "Asia/Kolkata"),
    City("Chennai", "India", "Asia/Kolkata"),
    City("Kolkata", "India", "Asia/Kolkata"),
    City("Colombo", "Sri Lanka", "Asia/Colombo"),
    City("Kathmandu", "Nepal", "Asia/Kathmandu"),
    City("Dhaka", "Bangladesh", "Asia/Dhaka"),
    City("Yangon", "Myanmar", "Asia/Yangon"),
    City("Bangkok", "Thailand", "Asia/Bangkok"),
    City("Hanoi", "Vietnam", "Asia/Ho_Chi_Minh"),
    City("Ho Chi Minh City", "Vietnam", "Asia/Ho_Chi_Minh"),
    City("Kuala Lumpur", "Malaysia", "Asia/Kuala_Lumpur"),
    City("Singapore", "Singapore", "Asia/Singapore"),
    City("Jakarta", "Indonesia", "Asia/Jakarta"),
    City("Manila", "Philippines", "Asia/Manila"),
    City("Hong Kong", "China", "Asia/Hong_Kong"),
    City("Shanghai", "China", "Asia/Shanghai"),
    City("Beijing", "China", "Asia/Shanghai"),
    City("Shenzhen", "China", "Asia/Shanghai"),
    City("Taipei", "Taiwan", "Asia/Taipei"),
    City("Seoul", "South Korea", "Asia/Seoul"),
    City("Tokyo", "Japan", "Asia/Tokyo"),
    City("Osaka", "Japan", "Asia/Tokyo"),
    City("Ulaanbaatar", "Mongolia", "Asia/Ulaanbaatar"),
    City("Yekaterinburg", "Russia", "Asia/Yekaterinburg"),
    City("Novosibirsk", "Russia", "Asia/Novosibirsk"),
    City("Vladivostok", "Russia", "Asia/Vladivostok"),
    // Oceania
    City("Perth", "Australia", "Australia/Perth"),
    City("Darwin", "Australia", "Australia/Darwin"),
    City("Adelaide", "Australia", "Australia/Adelaide"),
    City("Brisbane", "Australia", "Australia/Brisbane"),
    City("Sydney", "Australia", "Australia/Sydney"),
    City("Melbourne", "Australia", "Australia/Melbourne"),
    City("Hobart", "Australia", "Australia/Hobart"),
    City("Auckland", "New Zealand", "Pacific/Auckland"),
    City("Wellington", "New Zealand", "Pacific/Auckland"),
    City("Suva", "Fiji", "Pacific/Fiji"),
    City("Port Moresby", "Papua New Guinea", "Pacific/Port_Moresby"),
    City("Guam", "United States", "Pacific/Guam"),
    City("Papeete", "French Polynesia", "Pacific/Tahiti"),
    City("UTC", "Coordinated Universal Time", "UTC"),
)
