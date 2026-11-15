package com.adl.nocturna.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the Settings pages need from the phone beyond what the clock does ([com.adl.nocturna.clock.ClockPlatform],
 * which they use too): what the system allows, asking for permissions, the system's own settings pages,
 * the Day mode photo, backup files and starting the clock on charge. Each app provides one through [LocalSettingsPlatform].
 */
interface SettingsPlatform {
    /** What the system lets Nocturna do; read again whenever the screen comes back to the front. */
    @Composable
    fun access(): SystemAccess

    /** Whether [permission] is granted right now. */
    fun isGranted(permission: Permission): Boolean

    /**
     * A function that asks the user for [permission], then calls [onResult] with whether it was granted.
     * Where the system doesn't ask for it (notifications on older Android), it does nothing.
     */
    @Composable
    fun rememberPermissionRequest(permission: Permission, onResult: (granted: Boolean) -> Unit): () -> Unit

    /** Opens one of the system's settings pages; nothing where the system has no such page. */
    fun open(page: SystemPage)

    /**
     * A function that lets the user pick a photo and keeps a copy as Day mode's background, then calls
     * [onSaved] with whether it worked. Nothing happens when the user picks nothing.
     */
    @Composable
    fun rememberPhotoPicker(onSaved: (Boolean) -> Unit): () -> Unit

    /** Deletes the copy of Day mode's photo. */
    fun clearDayPhoto()

    /** Starts or stops opening the clock when the charger is connected. */
    fun setOpenOnCharge(on: Boolean)

    /** Sets the next alarm with the system again, after a change the alarm list doesn't see (days off). */
    fun rescheduleAlarms()

    /**
     * A function that asks the user where to save a file, suggesting its name, and writes the text there;
     * then [onResult] says whether it was saved. Nothing happens when the user picks no place.
     */
    @Composable
    fun rememberSaveFile(mimeType: String, onResult: (saved: Boolean) -> Unit): (fileName: String, text: String) -> Unit

    /**
     * A function that lets the user pick a file and reads it as text, then calls [onResult]. Nothing happens
     * when the user picks no file.
     */
    @Composable
    fun rememberOpenFile(maxBytes: Int, onResult: (OpenedFile) -> Unit): () -> Unit

    /** A short message that goes away by itself, such as "Backup saved". */
    fun showMessage(text: String)

    /** The app's version name, "0.1.0", which backups record. */
    val versionName: String

    /** "Version 0.1.0 (1)", as [versionLabel] writes it. */
    val versionLabel: String
}

val LocalSettingsPlatform = staticCompositionLocalOf<SettingsPlatform> { error("No SettingsPlatform provided") }

/** What the system lets Nocturna do, from settings the user changes outside the app. */
data class SystemAccess(
    /** May post notifications, which the alarm's screen over the lock screen needs. */
    val notifications: Boolean,
    /** May read notifications, for their icons on the clock. */
    val notificationAccess: Boolean,
    /** May open over other apps, which opening the clock on charge needs. */
    val overlay: Boolean,
    /** May fill the locked screen with a ringing alarm; true where the system doesn't ask. */
    val fullScreen: Boolean,
    val calendar: Boolean,
)

/** A file the user picked with [SettingsPlatform.rememberOpenFile]. */
sealed interface OpenedFile {
    data class Text(val text: String) : OpenedFile

    /** Bigger than asked for, so it wasn't read. */
    data object TooBig : OpenedFile

    /** It couldn't be opened or read. */
    data object Unreadable : OpenedFile
}

/** Permissions the Settings pages ask for. */
enum class Permission { CALENDAR, LOCATION, NOTIFICATIONS }

/** The system's own settings pages that the Settings pages send the user to. */
enum class SystemPage { NOTIFICATIONS, NOTIFICATION_ACCESS, OVERLAY, FULL_SCREEN_ALARMS, SCREEN_SAVER }
