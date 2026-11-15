package com.adl.nocturna.clock

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class NightDndTest {
    private val monterrey = TimeZone.of("America/Monterrey") // UTC-6, no daylight saving
    private val tokyo = TimeZone.of("Asia/Tokyo")

    private fun at(text: String, zone: TimeZone = monterrey): Instant = LocalDateTime.parse(text).toInstant(zone)

    private fun wanted(
        time: String,
        enabled: Boolean = true,
        showing: Boolean = true,
        day: Boolean = false,
        night: Boolean = false,
        alarmsAllowed: Boolean = true,
    ) = shouldDndBeOn(enabled, showing, day, night, alarmsAllowed, at("2026-11-13T$time"), monterrey)

    // The night window

    @Test fun nightWindowRunsFromTenAtNightToSevenInTheMorning() {
        assertFalse(inNightWindow(at("2026-11-13T21:59"), monterrey))
        assertTrue(inNightWindow(at("2026-11-13T22:00"), monterrey))
        assertTrue(inNightWindow(at("2026-11-14T03:00"), monterrey))
        assertTrue(inNightWindow(at("2026-11-14T06:59"), monterrey))
        assertFalse(inNightWindow(at("2026-11-14T07:00"), monterrey))
    }

    @Test fun nightWindowIsReadInTheZoneGiven() {
        // 23:00 in Monterrey is 14:00 the next day in Tokyo.
        val instant = at("2026-11-13T23:00")
        assertTrue(inNightWindow(instant, monterrey))
        assertFalse(inNightWindow(instant, tokyo))
    }

    // Whether DND is wanted

    @Test fun wantedInTheNightWindowWhileTheClockShows() {
        assertTrue(wanted("23:30"))
        assertTrue(wanted("05:00"))
    }

    @Test fun notWantedWhenOffNotShowingOrInDayMode() {
        assertFalse(wanted("23:30", enabled = false))
        assertFalse(wanted("23:30", showing = false))
        assertFalse(wanted("23:30", day = true))
        assertFalse(wanted("23:30", day = true, night = true))
    }

    @Test fun notWantedWhenTheDndSettingsWouldSilenceAlarms() =
        assertFalse(wanted("23:30", alarmsAllowed = false))

    @Test fun nightModeStartsItEarlyInTheEvening() {
        assertFalse(wanted("20:30"))
        assertTrue(wanted("20:30", night = true))
        assertTrue(wanted("12:00", night = true))
    }

    @Test fun nightModeAloneDoesNotKeepItOnPastTheMorningEdge() {
        assertTrue(wanted("06:59", night = true))
        assertFalse(wanted("07:00", night = true))
        assertFalse(wanted("11:59", night = true))
    }

    // The steps

    private val fresh = DndMemory()
    private val ours = DndMemory(setByUs = true)
    private val handsOff = DndMemory(standingDown = true)

    @Test fun turnsOnFromOffAndRemembersIt() =
        assertEquals(DndStep(DndAction.TURN_ON, ours), dndStep(wanted = true, DndFilter.OFF, fresh))

    @Test fun keepsItsOwnDndWhileStillWanted() =
        assertEquals(DndStep(DndAction.NONE, ours), dndStep(wanted = true, DndFilter.PRIORITY, ours))

    @Test fun restoresItsOwnDndWhenNoLongerWanted() =
        assertEquals(DndStep(DndAction.RESTORE, fresh), dndStep(wanted = false, DndFilter.PRIORITY, ours))

    @Test fun leavesDndThatWasAlreadyOnAlone() {
        for (current in listOf(DndFilter.PRIORITY, DndFilter.OTHER)) {
            assertEquals(DndStep(DndAction.NONE, handsOff), dndStep(wanted = true, current, fresh))
        }
    }

    @Test fun doesNotTakeOverWhenTheUserLaterTurnsTheirDndOff() =
        assertEquals(DndStep(DndAction.NONE, handsOff), dndStep(wanted = true, DndFilter.OFF, handsOff))

    @Test fun userTurningItsDndOffMeansNotSettingItAgainTonight() {
        val step = dndStep(wanted = true, DndFilter.OFF, ours)
        assertEquals(DndStep(DndAction.NONE, handsOff), step)
        assertEquals(DndAction.NONE, dndStep(wanted = true, DndFilter.OFF, step.memory).action)
    }

    @Test fun userChangingItsDndIsNeverUndone() {
        for (current in listOf(DndFilter.OFF, DndFilter.OTHER)) {
            assertEquals(DndStep(DndAction.NONE, fresh), dndStep(wanted = false, current, ours))
        }
    }

    @Test fun handsOffEndsWhenDndIsNoLongerWanted() {
        val step = dndStep(wanted = false, DndFilter.OFF, handsOff)
        assertEquals(DndStep(DndAction.NONE, fresh), step)
        // The next night starts afresh.
        assertEquals(DndAction.TURN_ON, dndStep(wanted = true, DndFilter.OFF, step.memory).action)
    }

    @Test fun nothingToDoWhenNotWantedAndNotOurs() {
        for (current in DndFilter.entries) {
            assertEquals(DndStep(DndAction.NONE, fresh), dndStep(wanted = false, current, fresh))
        }
    }

    @Test fun aWholeNight() {
        var memory = fresh
        var filter = DndFilter.OFF
        fun step(time: String, showing: Boolean = true) {
            val s = dndStep(wanted(time, showing = showing), filter, memory)
            memory = s.memory
            when (s.action) {
                DndAction.TURN_ON -> filter = DndFilter.PRIORITY
                DndAction.RESTORE -> filter = DndFilter.OFF
                DndAction.NONE -> Unit
            }
        }
        step("21:00")
        assertEquals(DndFilter.OFF, filter)
        step("22:00")
        assertEquals(DndFilter.PRIORITY, filter)
        // The app is killed and started again: the stored memory still knows the DND is Nocturna's.
        memory = DndMemory(setByUs = memory.setByUs, standingDown = memory.standingDown)
        step("03:00")
        assertEquals(DndFilter.PRIORITY, filter)
        step("07:00")
        assertEquals(DndFilter.OFF, filter)
        assertEquals(fresh, memory)
    }

    @Test fun clockLeavingTheScreenRestoresIt() {
        val on = dndStep(wanted("23:00"), DndFilter.OFF, fresh)
        assertEquals(DndAction.TURN_ON, on.action)
        assertEquals(DndAction.RESTORE, dndStep(wanted("23:05", showing = false), DndFilter.PRIORITY, on.memory).action)
    }
}
