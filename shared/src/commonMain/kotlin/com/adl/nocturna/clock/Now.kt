package com.adl.nocturna.clock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/** A moment and the time zone it's shown in: what the clock reads each second. */
data class Moment(val instant: Instant, val zone: TimeZone) {
    /** The date and time on the clock. */
    val local: LocalDateTime = instant.toLocalDateTime(zone)
    val date: LocalDate get() = local.date
    val time: LocalTime get() = local.time
    val hour: Int get() = local.hour
    val epochMillis: Long get() = instant.toEpochMilliseconds()

    companion object {
        /** Now, in the phone's time zone. */
        fun now(clock: Clock = Clock.System): Moment = Moment(clock.now(), TimeZone.currentSystemDefault())
    }
}

/** Current time, updated exactly on each second boundary. */
@Composable
fun rememberNow(): State<Moment> = produceState(Moment.now()) {
    while (true) {
        val now = Moment.now()
        value = now
        delay(1000L - now.local.nanosecond / 1_000_000 + 5)
    }
}
