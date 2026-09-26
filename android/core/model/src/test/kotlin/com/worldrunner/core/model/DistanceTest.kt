package com.worldrunner.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DistanceTest {
    @Test
    fun `typed miles convert to whole metres`() =
        assertEquals(Distance(8_047), Distance.of(5.0, DistanceUnit.Miles))

    @Test
    fun `formats in the Runner's unit`() {
        val d = Distance(10_000)
        assertEquals("10.0 km", d.format(DistanceUnit.Kilometres, Locale.US))
        assertEquals("6.2 mi", d.format(DistanceUnit.Miles, Locale.US))
    }
}
