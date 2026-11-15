package com.adl.nocturna.alarm

import kotlin.random.Random

/**
 * What it takes to stop a ringing alarm, chosen per alarm (*To stop it*). Snoozing is always one tap,
 * whatever this says. [key] is how it is stored.
 */
enum class DismissChallenge(val key: String, val label: String) {
    TAP("tap", "Just tap"),
    SUM("sum", "Solve a sum"),
    HOLD("hold", "Hold the button 3 s"),
    ;

    companion object {
        /** The challenge stored as [key]; anything unknown is [TAP], so an alarm can always be stopped. */
        fun of(key: String?): DismissChallenge = entries.firstOrNull { it.key == key } ?: TAP
    }
}

/** How long *Hold the button* must be held to stop the alarm. */
const val HOLD_TO_STOP_MILLIS = 3_000L

/** How many sums *Solve a sum* asks for, from easy to hard. */
val SUM_COUNTS = 1..3

/** An addition of two two-digit numbers, such as 37 + 48. */
data class Sum(val a: Int, val b: Int) {
    val answer: Int get() = a + b

    /** As shown: "37 + 48". */
    val text: String get() = "$a + $b"

    /** As TalkBack reads it: "37 plus 48". */
    val spoken: String get() = "$a plus $b"
}

/**
 * The sums one ringing alarm asks, in order: the same for the same [seed], each with both numbers from 12
 * to 89 (so it takes a carry or two to get wrong), and never the same sum twice in a row.
 */
fun sums(seed: Long): Sequence<Sum> = sequence {
    val random = Random(seed)
    var last: Sum? = null
    while (true) {
        val next = Sum(random.nextInt(12, 90), random.nextInt(12, 90))
        if (next == last) continue
        last = next
        yield(next)
    }
}

/** Whether [typed] (digits from the keypad, perhaps with spaces) is the answer to [sum]. */
fun isAnswer(sum: Sum, typed: String): Boolean = typed.trim().toIntOrNull() == sum.answer

/**
 * Where *Solve a sum* has got to: [solved] of [needed] sums done, and [round] the index into [sums] of the
 * one showing. A wrong answer moves on to a new sum without counting, so there is never a lockout.
 */
data class SumProgress(val needed: Int, val solved: Int = 0, val round: Int = 0, val lastWrong: Boolean = false) {
    val done: Boolean get() = solved >= needed

    /** The sum showing now, from the alarm's [seed]. */
    fun sum(seed: Long): Sum = sums(seed).elementAt(round)

    /** After answering the sum showing with [typed]. */
    fun answer(seed: Long, typed: String): SumProgress =
        if (isAnswer(sum(seed), typed)) copy(solved = solved + 1, round = round + 1, lastWrong = false)
        else copy(round = round + 1, lastWrong = true)

    companion object {
        /** A fresh start, asking for [count] sums, held to [SUM_COUNTS]. */
        fun start(count: Int) = SumProgress(needed = count.coerceIn(SUM_COUNTS))
    }
}

/** How far through holding the button [heldMillis] is, from 0 to 1 at [HOLD_TO_STOP_MILLIS]. */
fun holdProgress(heldMillis: Long): Float = (heldMillis.toFloat() / HOLD_TO_STOP_MILLIS).coerceIn(0f, 1f)

/**
 * The challenge to put up for an alarm set to [chosen]: [DismissChallenge.TAP] once the challenge screen
 * has [failed], so a ringing alarm can always be stopped.
 */
fun challengeToShow(chosen: DismissChallenge, failed: Boolean): DismissChallenge =
    if (failed) DismissChallenge.TAP else chosen
