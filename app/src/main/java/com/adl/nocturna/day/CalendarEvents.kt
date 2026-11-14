package com.adl.nocturna.day

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Reads event occurrences from the calendars on the device (Android's calendar provider). */
object CalendarEvents {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /**
     * Occurrences from [first] to [last], with a day more on either side because all-day events are
     * stored at UTC midnight. [covers] picks out the ones that really fall on a given day.
     */
    fun between(context: Context, first: LocalDate, last: LocalDate, zone: ZoneId): List<CalendarEvent> {
        val from = first.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val to = last.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = Instances.CONTENT_URI.buildUpon()
            .also { ContentUris.appendId(it, from); ContentUris.appendId(it, to) }
            .build()
        val projection = arrayOf(Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY, Instances.DISPLAY_COLOR)
        // Hidden calendars and invitations the user declined don't belong on the desk.
        val selection = "${Instances.VISIBLE} = 1 AND ${Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        val events = mutableListOf<CalendarEvent>()
        context.contentResolver.query(uri, projection, selection, null, "${Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                events += CalendarEvent(
                    title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(No title)",
                    begin = c.getLong(1),
                    end = c.getLong(2),
                    allDay = c.getInt(3) != 0,
                    color = if (c.isNull(4)) null else c.getInt(4),
                )
            }
        }
        return events
    }
}

/**
 * Event occurrences from [first] to [last], kept current as the calendars change; null when [enabled]
 * is off or calendar access isn't granted.
 */
@Composable
fun rememberCalendarEvents(enabled: Boolean, first: LocalDate, last: LocalDate, zone: ZoneId): State<List<CalendarEvent>?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<List<CalendarEvent>?>(null) }
    val changes = remember { mutableIntStateOf(0) }

    DisposableEffect(context, enabled) {
        if (!enabled) return@DisposableEffect onDispose { }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes.intValue++
            }
        }
        val registered = runCatching {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        }.isSuccess
        onDispose { if (registered) context.contentResolver.unregisterContentObserver(observer) }
    }

    LaunchedEffect(context, enabled, first, last, zone, changes.intValue) {
        while (true) {
            state.value = if (enabled && CalendarEvents.hasPermission(context)) {
                withContext(Dispatchers.IO) { runCatching { CalendarEvents.between(context, first, last, zone) }.getOrDefault(emptyList()) }
            } else {
                null
            }
            // The observer catches most edits; this catches access granted or revoked meanwhile.
            delay(5 * 60_000L)
        }
    }
    return state
}
