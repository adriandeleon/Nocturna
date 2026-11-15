package com.adl.nocturna.data

import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** What a backup file holds: the settings, radio stations included, and the alarms. */
data class Backup(val appVersion: String, val settings: Settings, val alarms: List<Alarm>) {
    /**
     * The settings to save when this backup is imported over [current]. The Day mode photo stays on the phone
     * and isn't in the backup, so whether there is one is kept from [current]; a Day mode override from the
     * clock's button is dropped, so the imported schedule takes over.
     */
    fun settingsOver(current: Settings): Settings =
        settings.copy(dayPhotoVersion = current.dayPhotoVersion, dayModeOverrideUntil = 0L)

    /** For the import dialog, such as "3 alarms, 6 radio stations". */
    val summary: String
        get() = "${count(alarms.size, "alarm")}, ${count(settings.stations.size, "radio station")}"

    private fun count(n: Int, noun: String) = if (n == 1) "1 $noun" else "$n ${noun}s"
}

/** Why a file couldn't be imported; [message] is shown to the user as it is. */
class BackupException(val problem: Problem, cause: Throwable? = null) : Exception(problem.message, cause) {
    enum class Problem(val message: String) {
        NOT_A_BACKUP("This file isn't a Nocturna backup."),
        NEWER_FORMAT("This backup was made by a newer version of Nocturna. Update the app, then import it again."),
        DAMAGED("This backup is damaged and can't be imported."),
    }
}

/**
 * A backup as one JSON file:
 * `{"app":"Nocturna","format":1,"appVersion":"…","settings":{…},"alarms":[…]}`, with the settings and alarms
 * in the same JSON as [SettingsCodec] and [AlarmCodec] store them, so a backup reads the way stored settings
 * do: unknown keys are ignored and missing ones get their defaults. Raise [FORMAT] only for a change that an
 * older version would read wrongly; it then refuses the file instead.
 */
object BackupCodec {
    const val FORMAT = 1
    private const val APP = "Nocturna"

    /** Keys of the stored settings that belong to this phone and stay out of a backup. */
    private val LOCAL_SETTINGS = setOf("dayPhotoVersion", "dayModeOverrideUntil")

    fun fileName(date: LocalDate): String = "nocturna-backup-$date.json"

    fun encode(backup: Backup): String = buildJsonObject {
        put("app", APP)
        put("format", FORMAT)
        put("appVersion", backup.appVersion)
        put("settings", JsonObject(SettingsCodec.encode(backup.settings) - LOCAL_SETTINGS))
        put("alarms", Json.parseToJsonElement(AlarmCodec.encode(backup.alarms)))
    }.toString()

    /** Reads what [encode] wrote, by this version or an older one; throws [BackupException] for anything else. */
    fun decode(json: String): Backup {
        val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject
            ?: throw BackupException(BackupException.Problem.NOT_A_BACKUP)
        if (root.optString("app") != APP) throw BackupException(BackupException.Problem.NOT_A_BACKUP)
        val format = root.primitive("format")?.asLong() ?: throw BackupException(BackupException.Problem.NOT_A_BACKUP)
        when {
            format > FORMAT -> throw BackupException(BackupException.Problem.NEWER_FORMAT)
            format < 1 -> throw BackupException(BackupException.Problem.NOT_A_BACKUP)
        }
        return try {
            val settings = SettingsCodec.decode(root.getObject("settings"))
            val alarms = AlarmCodec.decode((root["alarms"] as? JsonArray ?: throw IllegalArgumentException("No alarms")).toString())
            require(alarms.map { it.id }.toSet().size == alarms.size) { "Two alarms share an id" }
            Backup(root.optString("appVersion"), settings, alarms)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupException.Problem.DAMAGED, e)
        }
    }
}
