package io.swiftfolio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalOrderTest {
    @Test fun `numbers compare by value`() {
        val sorted = listOf("img10.png", "img2.png", "img1.png", "IMG3.png").sortedWith(NaturalOrder)
        assertEquals(listOf("img1.png", "img2.png", "IMG3.png", "img10.png"), sorted)
    }

    @Test fun `leading zeros and case ties are deterministic`() {
        assertTrue(NaturalOrder.compare("a01", "a1") > 0)
        assertTrue(NaturalOrder.compare("A", "a") < 0)
        assertEquals(0, NaturalOrder.compare("same", "same"))
    }

    @Test fun `handles empty strings and very long numbers`() {
        assertTrue(NaturalOrder.compare("", "a") < 0)
        assertTrue(NaturalOrder.compare("x99999999999999999999", "x100000000000000000000") < 0)
    }
}
