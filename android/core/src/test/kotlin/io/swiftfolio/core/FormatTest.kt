package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset

class FormatTest {
    @Test fun `byte format matches the web app`() {
        assertEquals("0 B", ByteFormat.format(0))
        assertEquals("0 B", ByteFormat.format(-1))
        assertEquals("512 B", ByteFormat.format(512))
        assertEquals("1 KB", ByteFormat.format(1024))
        assertEquals("1.5 KB", ByteFormat.format(1536))
        assertEquals("2.4 MB", ByteFormat.format(2_516_582))
        assertEquals("1 GB", ByteFormat.format(1L shl 30))
        assertEquals("1 PB", ByteFormat.format(1L shl 50))
    }

    @Test fun `percent is clamped`() {
        assertEquals(0, ByteFormat.percent(5, 0))
        assertEquals(50, ByteFormat.percent(5, 10))
        assertEquals(100, ByteFormat.percent(20, 10))
    }

    @Test fun `time format is ISO-like`() {
        val millis = 1_792_400_000_000L
        assertEquals("2026-10-19 08:53", TimeFormat.format(millis, ZoneOffset.UTC))
        assertNull(TimeFormat.format(null, ZoneOffset.UTC))
    }
}
