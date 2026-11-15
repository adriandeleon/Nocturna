package com.adl.nocturna.data

import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

class BackupTest {
    private val settings = Settings(
        use24h = false,
        color = Settings.COLORS[3],
        dayMode = DayMode.SCHEDULED,
        dayModeOverrideUntil = 1_789_000_000_000L,
        dayPhotoVersion = 1_794_590_985_000L,
        weatherCity = "Monterrey",
        weatherLat = 25.67,
        weatherLon = -100.31,
        stations = listOf(Station("D99", "https://example.com/d99"), Station("Jazz", "https://example.com/jazz")),
        selectedStation = 1,
    )
    private val alarms = listOf(
        Alarm(1, 6, 40, days = 0b0011111, label = "Work", station = Station("D99", "https://example.com/d99")),
        Alarm(2, 9, 0, enabled = false, skipDate = LocalDate(2026, 11, 16)),
    )
    private val backup = Backup("1.4.0", settings, alarms)

    private fun encoded(b: Backup = backup): JsonObject = Json.parseToJsonElement(BackupCodec.encode(b)).jsonObject

    private fun problem(json: String) = assertFailsWith<BackupException> { BackupCodec.decode(json) }.problem

    @Test fun aBackupSurvivesTheRoundTripWithoutThePhonesOwnSettings() {
        val read = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals("1.4.0", read.appVersion)
        assertEquals(alarms, read.alarms)
        assertEquals(settings.copy(dayModeOverrideUntil = 0L, dayPhotoVersion = 0L), read.settings)
    }

    @Test fun theFileSaysWhatItIs() {
        val json = encoded()
        assertEquals(JsonPrimitive("Nocturna"), json["app"])
        assertEquals(JsonPrimitive(BackupCodec.FORMAT), json["format"])
        val stored = json["settings"]!!.jsonObject
        assertNull(stored["dayPhotoVersion"])
        assertNull(stored["dayModeOverrideUntil"])
    }

    @Test fun importingKeepsThisPhonesPhotoAndDropsTheOverride() {
        val current = Settings(dayPhotoVersion = 42L, dayModeOverrideUntil = 99L)
        val imported = BackupCodec.decode(BackupCodec.encode(backup)).settingsOver(current)
        assertEquals(42L, imported.dayPhotoVersion)
        assertEquals(0L, imported.dayModeOverrideUntil)
        assertEquals(settings.stations, imported.stations)
    }

    @Test fun unknownFieldsAreIgnored() {
        val json = encoded().let { root ->
            val settings = JsonObject(root["settings"]!!.jsonObject + ("aSettingFromTheFuture" to JsonPrimitive(true)))
            val alarms = JsonArray((root["alarms"] as JsonArray).map { JsonObject(it.jsonObject + ("snoozeLimit" to JsonPrimitive(3))) })
            JsonObject(root + mapOf("exportedBy" to JsonPrimitive("phone"), "settings" to settings, "alarms" to alarms))
        }
        val read = BackupCodec.decode(json.toString())
        assertEquals(alarms, read.alarms)
        assertEquals(settings.stations, read.settings.stations)
    }

    @Test fun anOlderBackupWithoutNewerFieldsLoadsWithDefaults() {
        val old = """{"app":"Nocturna","format":1,"settings":{"use24h":false,"stationsVersion":2,""" +
            """"stations":[{"name":"Jazz","url":"https://example.com/jazz"}]},""" +
            """"alarms":[{"id":7,"hour":7,"minute":15}]}"""
        val read = BackupCodec.decode(old)
        assertEquals("", read.appVersion)
        assertEquals(Settings(use24h = false, stations = listOf(Station("Jazz", "https://example.com/jazz"))), read.settings)
        assertEquals(listOf(Alarm(7, 7, 15)), read.alarms)
    }

    @Test fun garbageIsNotABackup() {
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem(""))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem("\u0089PNG\r\n"))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem("{not json"))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem("[1, 2, 3]"))
    }

    @Test fun anotherAppsJsonIsNotABackup() {
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem("""{"format":1,"settings":{},"alarms":[]}"""))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem("""{"app":"Other","format":1,"settings":{},"alarms":[]}"""))
        // Nocturna's stored settings on their own, not a backup.
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem(SettingsCodec.encodeToString(settings)))
    }

    @Test fun aWrongFormatIsRefused() {
        fun withFormat(format: JsonElement?) =
            JsonObject(if (format == null) encoded() - "format" else encoded() + ("format" to format)).toString()
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem(withFormat(null)))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem(withFormat(JsonPrimitive(0))))
        assertEquals(BackupException.Problem.NOT_A_BACKUP, problem(withFormat(JsonPrimitive("one"))))
    }

    @Test fun aNewerFormatIsRefused() =
        assertEquals(
            BackupException.Problem.NEWER_FORMAT,
            problem(JsonObject(encoded() + ("format" to JsonPrimitive(BackupCodec.FORMAT + 1))).toString()),
        )

    @Test fun aDamagedBackupIsRefused() {
        fun without(key: String) = JsonObject(encoded() - key).toString()
        fun with(key: String, value: JsonElement) = JsonObject(encoded() + (key to value)).toString()
        assertEquals(BackupException.Problem.DAMAGED, problem(without("settings")))
        assertEquals(BackupException.Problem.DAMAGED, problem(without("alarms")))
        assertEquals(BackupException.Problem.DAMAGED, problem(with("alarms", JsonPrimitive("none"))))
        assertEquals(BackupException.Problem.DAMAGED, problem(with("alarms", Json.parseToJsonElement("""[{"hour":7}]"""))))
        assertEquals(BackupException.Problem.DAMAGED, problem(with("settings", Json.parseToJsonElement("""{"stations":[1]}"""))))
        val twins = Json.parseToJsonElement("""[{"id":1,"hour":7,"minute":0},{"id":1,"hour":8,"minute":0}]""")
        assertEquals(BackupException.Problem.DAMAGED, problem(with("alarms", twins)))
    }

    @Test fun theSummaryCountsAlarmsAndStations() {
        assertEquals("2 alarms, 2 radio stations", backup.summary(testLocale))
        assertEquals("1 alarm, 1 radio station", Backup("", Settings(stations = settings.stations.take(1)), alarms.take(1)).summary(testLocale))
        assertEquals("0 alarms, 0 radio stations", Backup("", Settings(stations = emptyList()), emptyList()).summary(testLocale))
    }

    @Test fun theFileIsNamedAfterTheDay() =
        assertEquals("nocturna-backup-2026-11-15.json", BackupCodec.fileName(LocalDate(2026, 11, 15)))

    @Test fun replacingTheAlarmsSavesThemCancelsASnoozeAndReschedules() {
        val now = Instant.parse("2026-11-15T22:00:00Z")
        val clock = object : Clock {
            override fun now() = now
        }
        var reschedules = 0
        val store = MemoryStore()
        AlarmStore.init(store, clock) { reschedules++ }
        val old = AlarmStore.add(5, 0)
        AlarmStore.setSnooze(old.id, now.toEpochMilliseconds() + 60_000)
        reschedules = 0

        AlarmStore.replaceAll(alarms.reversed())

        assertEquals(alarms, AlarmStore.alarms.value)
        assertEquals(alarms, AlarmCodec.decode(store.getString("alarms")!!))
        assertNull(AlarmStore.snooze.value)
        assertFalse("snoozeUntil" in store.values)
        assertEquals(1, reschedules)
    }
}
