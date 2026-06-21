package com.saavdhan.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the resilient package fetch — the safety net that stops a budget phone with many apps
 * from silently failing the whole scan. These run on your computer in milliseconds, no phone needed,
 * because [resilientPackageFetch] is generic and Android-free. We use String as the stand-in for
 * Android's PackageInfo (which can't be created off-device) and a plain RuntimeException as the
 * stand-in for the TransactionTooLargeException a real phone would throw.
 */
class ResilientPackageFetchTest {

    @Test
    fun `fast path - trusts the bulk result when it returns everything`() {
        var perNameCalls = 0
        val out = resilientPackageFetch(
            names = { listOf("a", "b", "c") },
            bulk = { listOf("a", "b", "c") },
            perName = {
                perNameCalls++
                it
            }
        )
        assertEquals(listOf("a", "b", "c"), out.items)
        assertFalse(out.partial)
        assertEquals(0, perNameCalls) // the slow per-package path must NOT run on the fast path
    }

    @Test
    fun `falls back per-package when the bulk call throws (TransactionTooLargeException)`() {
        val perNameCalls = mutableListOf<String>()
        val out = resilientPackageFetch(
            names = { listOf("a", "b", "c") },
            bulk = { throw RuntimeException("android.os.TransactionTooLargeException") },
            perName = {
                perNameCalls += it
                it
            }
        )
        assertEquals(listOf("a", "b", "c"), out.items)
        assertFalse(out.partial) // recovered every app, so not partial
        assertEquals(listOf("a", "b", "c"), perNameCalls) // proves the fallback path ran
    }

    @Test
    fun `marks partial when some apps cannot be fetched on the fallback path`() {
        val out = resilientPackageFetch(
            names = { listOf("a", "b", "c") },
            bulk = { throw RuntimeException("boom") },
            perName = { if (it == "b") null else it } // b fails to fetch
        )
        assertEquals(listOf("a", "c"), out.items)
        assertTrue(out.partial)
    }

    @Test
    fun `detects a silently-truncated bulk result and falls back`() {
        // Some OEM ROMs return fewer apps instead of throwing. The count is far below the names
        // count, so we must distrust it and recover the full set per-package.
        val out = resilientPackageFetch(
            names = { listOf("a", "b", "c", "d") },
            bulk = { listOf("a") }, // 1 of 4 is below the 0.95 trust ratio
            perName = { it }
        )
        assertEquals(listOf("a", "b", "c", "d"), out.items)
        assertFalse(out.partial)
    }

    @Test
    fun `tolerates a tiny count difference (install race) without falling back`() {
        // One app vanished between the two listings; 4 of 5 is still above the trust ratio, so the
        // fast result is kept (no needless slow path).
        var perNameCalls = 0
        val out = resilientPackageFetch(
            names = { listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j") }, // 10
            bulk = { listOf("a", "b", "c", "d", "e", "f", "g", "h", "i") }, // 9 of 10 = 0.9 ... below
            perName = {
                perNameCalls++
                it
            },
            trustRatio = 0.8 // explicit: 0.9 >= 0.8, so trust the bulk result
        )
        assertEquals(9, out.items.size)
        assertFalse(out.partial)
        assertEquals(0, perNameCalls)
    }

    @Test
    fun `reports partial and empty when even the cheap names listing fails`() {
        val out = resilientPackageFetch<String>(
            names = { throw RuntimeException("cannot list packages") },
            bulk = { listOf("a") },
            perName = { it }
        )
        assertTrue(out.items.isEmpty())
        assertTrue(out.partial)
    }
}
