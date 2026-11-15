package com.adl.nocturna.alarm

/**
 * Decides when a radio alarm has been silent long enough to give up on the stream and ring the alarm
 * tone instead. Silence counts from [start] (the stream hasn't started yet) and again from any moment
 * playback stops, so a stream that dies or stalls mid-alarm is caught too. Times are in milliseconds
 * on a monotonic clock.
 */
class SilenceWatchdog(private val timeoutMs: Long, start: Long) {
    private var silentSince: Long? = start

    fun update(playing: Boolean, now: Long) {
        silentSince = if (playing) null else silentSince ?: now
    }

    fun expired(now: Long): Boolean = silentSince?.let { now - it >= timeoutMs } ?: false
}
