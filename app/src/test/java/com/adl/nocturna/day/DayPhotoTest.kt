package com.adl.nocturna.day

import org.junit.Assert.assertEquals
import org.junit.Test

class DayPhotoTest {
    @Test fun smallPhotosAreKeptWhole() = assertEquals(1, sampleSize(1920, 1080, 2560))

    @Test fun phonePhotoIsHalved() = assertEquals(2, sampleSize(4000, 3000, 1920))

    @Test fun neverGoesBelowTheTargetSize() {
        // 12000 / 4 = 3000 still covers 2560; 12000 / 8 = 1500 would not.
        assertEquals(4, sampleSize(9000, 12000, 2560))
    }
}
