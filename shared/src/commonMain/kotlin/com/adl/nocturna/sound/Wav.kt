package com.adl.nocturna.sound

import kotlin.math.roundToInt

/** Mono 16-bit PCM WAV files. */
object Wav {
    const val HEADER_BYTES = 44
    const val BITS_PER_SAMPLE = 16

    /** [samples] in -1..1 (anything outside is clipped) as a complete WAV file at [sampleRate]. */
    fun encode(samples: FloatArray, sampleRate: Int): ByteArray {
        require(sampleRate > 0) { "Sample rate must be positive" }
        val dataBytes = samples.size * 2
        val out = ByteArray(HEADER_BYTES + dataBytes)
        var at = 0
        fun ascii(s: String) = s.forEach { out[at++] = it.code.toByte() }
        fun int16(v: Int) {
            out[at++] = v.toByte()
            out[at++] = (v shr 8).toByte()
        }
        fun int32(v: Int) {
            int16(v)
            int16(v shr 16)
        }

        ascii("RIFF")
        int32(HEADER_BYTES - 8 + dataBytes)
        ascii("WAVE")
        ascii("fmt ")
        int32(16) // fmt chunk size
        int16(1) // PCM
        int16(1) // mono
        int32(sampleRate)
        int32(sampleRate * 2) // bytes per second
        int16(2) // bytes per frame
        int16(BITS_PER_SAMPLE)
        ascii("data")
        int32(dataBytes)
        for (s in samples) int16(toPcm16(s))
        return out
    }

    fun toPcm16(sample: Float): Int = (sample * Short.MAX_VALUE).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
}
