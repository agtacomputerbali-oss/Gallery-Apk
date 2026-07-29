package com.gallery.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PHashCalculatorTest {

    @Test
    fun hammingDistance_identicalHashes_returnsZero() {
        val hash = "a3f01b9e82c74d0f"
        val distance = PHashCalculator.hammingDistance(hash, hash)
        assertEquals(0, distance)
    }

    @Test
    fun hammingDistance_oppositeHashes_returns64() {
        val hash1 = "0000000000000000"
        val hash2 = "ffffffffffffffff"
        val distance = PHashCalculator.hammingDistance(hash1, hash2)
        assertEquals(64, distance)
    }

    @Test
    fun hammingDistance_knownBitDifference_returnsCorrectDistance() {
        // 0000 vs 0001 -> 1 bit diff in hex '1' (0000 vs 0001)
        val hash1 = "0000000000000000"
        val hash2 = "0000000000000001"
        val distance = PHashCalculator.hammingDistance(hash1, hash2)
        assertEquals(1, distance)
    }
}
