package com.adl.nocturna.clock

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/*
 * Do Not Disturb while the clock shows at night (Settings.dndWhileShowing). Nocturna turns the phone's
 * Do Not Disturb to priority only, which still lets alarms through, and later puts it back to off. It only
 * ever undoes its own change: DND that was already on, or that the user changed after Nocturna did, is
 * left alone until the night is over.
 */

/** The night window: from [NIGHT_START_HOUR]:00 to the morning edge at [MORNING_HOUR]:00. */
const val NIGHT_START_HOUR = 22
const val MORNING_HOUR = 7

/** From this hour on, night mode (a dark room) starts the night early; before it, it's morning. */
const val EVENING_START_HOUR = 12

/** The phone's Do Not Disturb setting, as far as Nocturna cares. */
enum class DndFilter {
    /** DND is off: every notification comes through. */
    OFF,

    /** Priority only, which is what Nocturna sets. */
    PRIORITY,

    /** Alarms only, total silence, or anything Nocturna can't read. */
    OTHER,
}

/** What Nocturna remembers about its own change to DND; stored, so it survives the app being killed. */
data class DndMemory(
    /** Nocturna turned priority-only DND on from off, and hasn't put it back yet. */
    val setByUs: Boolean = false,
    /** DND was already on, or the user changed it after Nocturna did: hands off until it's no longer wanted. */
    val standingDown: Boolean = false,
)

enum class DndAction { NONE, TURN_ON, RESTORE }

/** What to do to the phone's DND now, and what to remember afterwards. */
data class DndStep(val action: DndAction, val memory: DndMemory)

/** Whether [now] is between [NIGHT_START_HOUR]:00 and the morning edge at [MORNING_HOUR]:00 in [zone]. */
fun inNightWindow(now: Instant, zone: TimeZone): Boolean {
    val hour = now.toLocalDateTime(zone).hour
    return hour >= NIGHT_START_HOUR || hour < MORNING_HOUR
}

/**
 * Whether Nocturna wants DND on: the setting is [enabled], the clock is [showing] (the app or the screen
 * saver), day mode is off, and it is night, either inside the night window or, from [EVENING_START_HOUR]
 * on, in night mode ([night]: the clock's dim red look). Night mode alone never keeps DND on past the
 * morning edge. When the phone's DND settings would silence alarms (![alarmsAllowed]), never.
 */
fun shouldDndBeOn(
    enabled: Boolean,
    showing: Boolean,
    day: Boolean,
    night: Boolean,
    alarmsAllowed: Boolean,
    now: Instant,
    zone: TimeZone,
): Boolean {
    if (!enabled || !showing || day || !alarmsAllowed) return false
    return inNightWindow(now, zone) || (night && now.toLocalDateTime(zone).hour >= EVENING_START_HOUR)
}

/**
 * The next step, given whether DND is [wanted] (see [shouldDndBeOn]), the phone's [current] DND, and what
 * Nocturna did before ([memory]). It turns DND on only from off, and restores only while DND is still
 * what it set; any other change to DND hands it back to the user until DND is no longer wanted.
 */
fun dndStep(wanted: Boolean, current: DndFilter, memory: DndMemory): DndStep = when {
    // Someone else changed DND since Nocturna set it: theirs now, and not set again tonight.
    memory.setByUs && current != DndFilter.PRIORITY -> DndStep(DndAction.NONE, DndMemory(standingDown = wanted))
    memory.setByUs && wanted -> DndStep(DndAction.NONE, memory)
    memory.setByUs -> DndStep(DndAction.RESTORE, DndMemory())
    !wanted -> DndStep(DndAction.NONE, DndMemory())
    memory.standingDown -> DndStep(DndAction.NONE, memory)
    current == DndFilter.OFF -> DndStep(DndAction.TURN_ON, DndMemory(setByUs = true))
    // DND was already on: leave it, and don't take it over if the user turns it off later.
    else -> DndStep(DndAction.NONE, DndMemory(standingDown = true))
}
