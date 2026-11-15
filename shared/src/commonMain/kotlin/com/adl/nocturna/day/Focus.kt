package com.adl.nocturna.day

import com.adl.nocturna.text.PlatformLocale
import com.adl.nocturna.text.strings

/** After this many focus blocks, the break is a long one. */
const val FOCUS_BLOCKS_PER_LONG_BREAK = 4

/** The steps of the focus timer's cycle: focus, break, focus, break, ..., with a long break after every 4th block. */
enum class FocusPhase { FOCUS, BREAK, LONG_BREAK }

/** How long each step of the focus timer lasts, in minutes (Settings › Day mode). */
data class FocusLengths(val focus: Int = 25, val shortBreak: Int = 5, val longBreak: Int = 15) {
    /** [phase]'s length in millis; never under a minute, so the cycle always moves on. */
    fun millis(phase: FocusPhase): Long = when (phase) {
        FocusPhase.FOCUS -> focus
        FocusPhase.BREAK -> shortBreak
        FocusPhase.LONG_BREAK -> longBreak
    }.coerceAtLeast(1) * 60_000L

    /** One whole cycle: [FOCUS_BLOCKS_PER_LONG_BREAK] blocks, the short breaks between them and the long one. */
    val cycleMillis: Long
        get() = (0 until CYCLE_STEPS).sumOf { millis(focusPhase(it)) }
}

/** Steps in one whole cycle: a focus block and a break for each block. */
private const val CYCLE_STEPS = FOCUS_BLOCKS_PER_LONG_BREAK * 2

/** The [step]th step of the cycle, counting from 0: even steps are focus blocks, odd ones breaks. */
fun focusPhase(step: Int): FocusPhase = when {
    step % 2 == 0 -> FocusPhase.FOCUS
    (step / 2 + 1) % FOCUS_BLOCKS_PER_LONG_BREAK == 0 -> FocusPhase.LONG_BREAK
    else -> FocusPhase.BREAK
}

/**
 * A running focus timer, as stored: not a ticking counter, but the [step] it was on at [since] (epoch
 * millis), so where it is now follows from the clock. A pause sets [pausedAt]; resuming moves [since]
 * on by the time spent paused. [chimed] is the last step whose start has chimed, so each switch chimes
 * once.
 */
data class FocusTimer(
    val step: Int,
    val since: Long,
    val pausedAt: Long? = null,
    val chimed: Int = step,
) {
    val paused: Boolean get() = pausedAt != null

    /** Where the timer is at [now] (epoch millis): the steps that ran out since [since] are passed over. */
    fun status(now: Long, lengths: FocusLengths): FocusStatus {
        var elapsed = ((pausedAt ?: now) - since).coerceAtLeast(0)
        var s = step
        while (true) {
            // Whole cycles at once, so a timer left running for hours doesn't walk every step.
            if (s % CYCLE_STEPS == 0) {
                val cycles = elapsed / lengths.cycleMillis
                s += (cycles * CYCLE_STEPS).toInt()
                elapsed -= cycles * lengths.cycleMillis
            }
            val length = lengths.millis(focusPhase(s))
            if (elapsed < length) return FocusStatus(s, length - elapsed, length, paused)
            elapsed -= length
            s++
        }
    }

    fun pause(now: Long): FocusTimer = if (paused) this else copy(pausedAt = now)

    fun resume(now: Long): FocusTimer =
        pausedAt?.let { copy(since = since + (now - it).coerceAtLeast(0), pausedAt = null) } ?: this

    /** Ends the current step at [now] and starts the next one; a paused timer stays paused. No chime. */
    fun skip(now: Long, lengths: FocusLengths): FocusTimer {
        val next = status(now, lengths).step + 1
        return FocusTimer(next, now, pausedAt = if (paused) now else null, chimed = next)
    }

    /** The step whose start should chime at [now], or null: a switch the clock ran into, not a skip, once. */
    fun chimeDue(now: Long, lengths: FocusLengths): Int? =
        status(now, lengths).step.takeIf { it > chimed && !paused }

    companion object {
        /** A timer starting its first focus block at [now]. */
        fun start(now: Long) = FocusTimer(0, now)
    }
}

/** Where a [FocusTimer] is: its [step] of the cycle and the millis [left] of [length]. */
data class FocusStatus(val step: Int, val left: Long, val length: Long, val paused: Boolean) {
    val phase: FocusPhase get() = focusPhase(step)

    /** How much of this step has gone, 0 to 1, for the progress ring. */
    val progress: Float get() = (1f - left.toFloat() / length).coerceIn(0f, 1f)
}

/** What the desk clock shows, e.g. "Focus · 18:42 left", "Break · 3:10 left" or "Focus paused · 18:42 left". */
fun focusLabel(status: FocusStatus, locale: PlatformLocale): String {
    val t = strings(locale)
    val name = when (status.phase) {
        FocusPhase.FOCUS -> t.focus
        FocusPhase.BREAK -> t.focusBreak
        FocusPhase.LONG_BREAK -> t.focusLongBreak
    }
    return t.focusLabel(name, status.paused, focusTimeLeft(status.left))
}

/** [millis] as "18:42", or "1:05:00" from an hour up; rounded up to the second, so it reads 0:00 only at the end. */
fun focusTimeLeft(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0) + 999) / 1000
    val h = seconds / 3600
    val m = seconds / 60 % 60
    val s = seconds % 60
    return if (h > 0) "$h:${m.pad()}:${s.pad()}" else "$m:${s.pad()}"
}

private fun Long.pad() = toString().padStart(2, '0')

/** A [FocusTimer] as numbers, so `rememberSaveable` can keep it across rotation; [focusTimerFromSaved] reads it back. */
fun FocusTimer.toSaved(): List<Long> = listOf(step.toLong(), since, pausedAt ?: -1L, chimed.toLong())

fun focusTimerFromSaved(saved: List<Long>): FocusTimer? {
    if (saved.size != 4) return null
    return FocusTimer(saved[0].toInt(), saved[1], saved[2].takeIf { it >= 0 }, saved[3].toInt())
}
