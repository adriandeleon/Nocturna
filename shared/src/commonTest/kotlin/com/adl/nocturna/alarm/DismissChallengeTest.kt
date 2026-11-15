package com.adl.nocturna.alarm

import com.adl.nocturna.text.testLocale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DismissChallengeTest {
    @Test fun theSameSeedGivesTheSameSums() =
        assertEquals(sums(42).take(10).toList(), sums(42).take(10).toList())

    @Test fun differentSeedsGiveDifferentSums() =
        assertNotEquals(sums(1).take(5).toList(), sums(2).take(5).toList())

    @Test fun sumsAreOfTwoTwoDigitNumbers() =
        sums(7).take(500).forEach {
            assertTrue(it.a in 12..89 && it.b in 12..89, "$it")
            assertEquals(it.a + it.b, it.answer)
        }

    @Test fun neverTheSameSumTwiceInARow() =
        sums(3).take(1000).zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }

    @Test fun showsAndSpeaksTheSum() {
        assertEquals("37 + 48", Sum(37, 48).text)
        assertEquals("37 plus 48", Sum(37, 48).spoken(testLocale))
    }

    @Test fun checksTheAnswer() {
        val sum = Sum(37, 48)
        assertTrue(isAnswer(sum, "85"))
        assertTrue(isAnswer(sum, " 85 "))
        assertFalse(isAnswer(sum, "84"))
        assertFalse(isAnswer(sum, ""))
        assertFalse(isAnswer(sum, "8 5"))
    }

    @Test fun rightAnswersCountUntilEnoughAreDone() {
        val seed = 99L
        var p = SumProgress.start(3)
        repeat(3) {
            assertFalse(p.done)
            p = p.answer(seed, p.sum(seed).answer.toString())
        }
        assertTrue(p.done)
        assertEquals(3, p.solved)
    }

    @Test fun aWrongAnswerGivesANewSumWithoutCountingAndNoLockout() {
        val seed = 5L
        val start = SumProgress.start(1)
        val wrong = start.answer(seed, (start.sum(seed).answer + 1).toString())
        assertEquals(0, wrong.solved)
        assertTrue(wrong.lastWrong)
        assertNotEquals(start.sum(seed), wrong.sum(seed))
        // Straight away, the new sum can be answered.
        val right = wrong.answer(seed, wrong.sum(seed).answer.toString())
        assertTrue(right.done)
        assertFalse(right.lastWrong)
    }

    @Test fun theNumberOfSumsIsHeldToOneToThree() {
        assertEquals(1, SumProgress.start(0).needed)
        assertEquals(3, SumProgress.start(9).needed)
        assertEquals(2, SumProgress.start(2).needed)
    }

    @Test fun holdingFillsUpOverThreeSeconds() {
        assertEquals(0f, holdProgress(0))
        assertEquals(0.5f, holdProgress(1_500))
        assertEquals(1f, holdProgress(3_000))
        assertEquals(1f, holdProgress(10_000))
        assertEquals(0f, holdProgress(-5))
    }

    @Test fun unknownChallengesAreATap() {
        assertEquals(DismissChallenge.SUM, DismissChallenge.of("sum"))
        assertEquals(DismissChallenge.HOLD, DismissChallenge.of("hold"))
        assertEquals(DismissChallenge.TAP, DismissChallenge.of("riddle"))
        assertEquals(DismissChallenge.TAP, DismissChallenge.of(null))
    }

    @Test fun aFailedChallengeFallsBackToATap() {
        assertEquals(DismissChallenge.SUM, challengeToShow(DismissChallenge.SUM, failed = false))
        assertEquals(DismissChallenge.TAP, challengeToShow(DismissChallenge.SUM, failed = true))
        assertEquals(DismissChallenge.TAP, challengeToShow(DismissChallenge.HOLD, failed = true))
    }
}
