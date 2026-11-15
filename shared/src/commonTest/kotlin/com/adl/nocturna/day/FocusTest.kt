package com.adl.nocturna.day

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocusTest {
    private val lengths = FocusLengths(focus = 25, shortBreak = 5, longBreak = 15)
    private val t0 = 1_763_200_000_000L // any instant: the timer only counts from its own start

    private fun min(m: Int, s: Int = 0) = m * 60_000L + s * 1000L

    @Test fun theCycleHasALongBreakAfterEveryFourthBlock() {
        val order = (0 until 10).map(::focusPhase)
        assertEquals(
            listOf(
                FocusPhase.FOCUS, FocusPhase.BREAK, FocusPhase.FOCUS, FocusPhase.BREAK,
                FocusPhase.FOCUS, FocusPhase.BREAK, FocusPhase.FOCUS, FocusPhase.LONG_BREAK,
                FocusPhase.FOCUS, FocusPhase.BREAK,
            ),
            order,
        )
        assertEquals(FocusPhase.LONG_BREAK, focusPhase(15))
    }

    @Test fun startsWithAWholeFocusBlock() {
        val s = FocusTimer.start(t0).status(t0, lengths)
        assertEquals(0, s.step)
        assertEquals(FocusPhase.FOCUS, s.phase)
        assertEquals(min(25), s.left)
        assertEquals(0f, s.progress)
        assertEquals("Focus · 25:00 left", focusLabel(s))
    }

    @Test fun timeLeftFollowsTheClock() {
        val s = FocusTimer.start(t0).status(t0 + min(6, 18), lengths)
        assertEquals("Focus · 18:42 left", focusLabel(s))
        assertEquals(0.252f, s.progress, 0.001f)
    }

    @Test fun movesToABreakThenTheNextBlock() {
        val timer = FocusTimer.start(t0)
        assertEquals("Focus · 0:01 left", focusLabel(timer.status(t0 + min(25) - 1, lengths)))
        val brk = timer.status(t0 + min(26, 50), lengths)
        assertEquals(FocusPhase.BREAK, brk.phase)
        assertEquals("Break · 3:10 left", focusLabel(brk))
        val next = timer.status(t0 + min(30), lengths)
        assertEquals(2, next.step)
        assertEquals("Focus · 25:00 left", focusLabel(next))
    }

    @Test fun theFourthBreakIsLong() {
        // Four blocks and three short breaks: 4 × 25 + 3 × 5 = 115 minutes.
        val s = FocusTimer.start(t0).status(t0 + min(115), lengths)
        assertEquals(7, s.step)
        assertEquals("Long break · 15:00 left", focusLabel(s))
        // Then the cycle starts over with a focus block.
        val over = FocusTimer.start(t0).status(t0 + min(130), lengths)
        assertEquals(8, over.step)
        assertEquals(FocusPhase.FOCUS, over.phase)
    }

    @Test fun aTimerLeftRunningForHoursStillKnowsWhereItIs() {
        // A cycle is 130 minutes; ten of them and 27 minutes more is two minutes into the first break.
        val s = FocusTimer.start(t0).status(t0 + min(1300 + 27), lengths)
        assertEquals(81, s.step)
        assertEquals("Break · 3:00 left", focusLabel(s))
    }

    @Test fun lengthsComeFromTheSettings() {
        val short = FocusLengths(focus = 50, shortBreak = 10, longBreak = 30)
        assertEquals("Focus · 45:00 left", focusLabel(FocusTimer.start(t0).status(t0 + min(5), short)))
        assertEquals("Break · 10:00 left", focusLabel(FocusTimer.start(t0).status(t0 + min(50), short)))
        // Zero would stall the cycle: a step lasts at least a minute.
        assertEquals(60_000L, FocusLengths(shortBreak = 0).millis(FocusPhase.BREAK))
    }

    @Test fun aPauseStopsTheCountdownAndResumingPicksItUp() {
        val paused = FocusTimer.start(t0).pause(t0 + min(10))
        assertTrue(paused.paused)
        val during = paused.status(t0 + min(40), lengths)
        assertEquals("Focus paused · 15:00 left", focusLabel(during))
        assertEquals(paused, paused.pause(t0 + min(20)))
        // Resumed after 30 minutes paused, it goes on from 15:00.
        val resumed = paused.resume(t0 + min(40))
        assertEquals("Focus · 15:00 left", focusLabel(resumed.status(t0 + min(40), lengths)))
        assertEquals("Focus · 10:00 left", focusLabel(resumed.status(t0 + min(45), lengths)))
        assertEquals(FocusPhase.BREAK, resumed.status(t0 + min(55), lengths).phase)
    }

    @Test fun skipStartsTheNextStepNow() {
        val timer = FocusTimer.start(t0)
        val skipped = timer.skip(t0 + min(3), lengths)
        assertEquals("Break · 5:00 left", focusLabel(skipped.status(t0 + min(3), lengths)))
        assertEquals("Focus · 25:00 left", focusLabel(skipped.skip(t0 + min(4), lengths).status(t0 + min(4), lengths)))
        // A paused timer skips to the next step, still paused.
        val pausedSkip = timer.pause(t0 + min(1)).skip(t0 + min(2), lengths)
        assertEquals("Break paused · 5:00 left", focusLabel(pausedSkip.status(t0 + min(9), lengths)))
    }

    @Test fun chimesOnceAtEachSwitchTheClockRunsInto() {
        val timer = FocusTimer.start(t0)
        assertNull(timer.chimeDue(t0, lengths))
        assertNull(timer.chimeDue(t0 + min(24, 59), lengths))
        assertEquals(1, timer.chimeDue(t0 + min(25), lengths))
        val heard = timer.copy(chimed = 1)
        assertNull(heard.chimeDue(t0 + min(26), lengths))
        assertEquals(2, heard.chimeDue(t0 + min(30), lengths))
    }

    @Test fun skippingAndPausingDontChime() {
        val timer = FocusTimer.start(t0)
        assertNull(timer.skip(t0 + min(3), lengths).chimeDue(t0 + min(3), lengths))
        assertNull(timer.pause(t0 + min(30)).chimeDue(t0 + min(30), lengths))
    }

    @Test fun timeLeftText() {
        assertEquals("0:00", focusTimeLeft(0))
        assertEquals("0:01", focusTimeLeft(1))
        assertEquals("3:10", focusTimeLeft(min(3, 10)))
        assertEquals("59:59", focusTimeLeft(min(59, 59)))
        assertEquals("1:05:00", focusTimeLeft(min(65)))
    }

    @Test fun survivesSavingForRotation() {
        val running = FocusTimer(3, t0, chimed = 3)
        val paused = FocusTimer(5, t0, pausedAt = t0 + 1234, chimed = 4)
        assertEquals(running, focusTimerFromSaved(running.toSaved()))
        assertEquals(paused, focusTimerFromSaved(paused.toSaved()))
        assertNull(focusTimerFromSaved(listOf(1L, 2L)))
    }
}
