package com.ysajid.names99

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that decide whether a night counted. If these break, a child loses
 * an island they already earned, so they are worth pinning down.
 */
class ProgressRulesTest {

    private fun readAllNine(start: Progress = Progress(), island: Int = 1): Progress =
        (1..NAMES_PER_ISLAND).fold(start) { acc, n -> acc.seeing(island, n) }

    @Test
    fun `island one is open and the rest are not`() {
        val fresh = Progress()
        assertTrue(fresh.unlocked(1))
        assertFalse(fresh.unlocked(2))
        assertFalse(fresh.unlocked(11))
    }

    @Test
    fun `reading all nine cards earns exactly one star and does not finish the island`() {
        val after = readAllNine()
        assertEquals(9, after.of(1).seen.size)
        assertEquals(1, after.of(1).stars)
        assertFalse(after.of(1).done)
        assertFalse("reading alone must not open the next island", after.unlocked(2))
    }

    @Test
    fun `eight cards is not enough for a star`() {
        val after = (1..8).fold(Progress()) { acc, n -> acc.seeing(1, n) }
        assertEquals(0, after.of(1).stars)
    }

    @Test
    fun `re-reading a card changes nothing`() {
        val once = readAllNine()
        val again = once.seeing(1, 3).seeing(1, 7)
        assertEquals(once, again)
        assertTrue("an unchanged read should not allocate", once === again)
    }

    @Test
    fun `a clean game earns three stars and opens the next island`() {
        val after = readAllNine().finishing(1, mistakes = 0)
        assertEquals(3, after.of(1).stars)
        assertTrue(after.of(1).done)
        assertTrue(after.unlocked(2))
        assertFalse("only the very next island opens", after.unlocked(3))
    }

    @Test
    fun `a game with a mistake earns two stars`() {
        val after = readAllNine().finishing(1, mistakes = 1)
        assertEquals(2, after.of(1).stars)
        assertTrue(after.of(1).done)
    }

    @Test
    fun `replaying worse never takes a star away`() {
        val perfect = readAllNine().finishing(1, mistakes = 0)
        val sloppyReplay = perfect.finishing(1, mistakes = 4)
        assertEquals("three stars already earned must stick", 3, sloppyReplay.of(1).stars)
        assertTrue(sloppyReplay.of(1).done)
    }

    @Test
    fun `islands light up one at a time across the whole journey`() {
        var progress = Progress()
        for (island in 1..11) {
            assertTrue("island $island should be open by now", progress.unlocked(island))
            progress = readAllNine(progress, island).finishing(island, mistakes = 0)
            assertEquals(island, progress.lit())
        }
        assertEquals(11, progress.lit())
    }

    @Test
    fun `progress survives being written out and read back`() {
        val before = readAllNine().finishing(1, mistakes = 1).seeing(2, 14)
        val after = Progress.fromJson(before.toJson())
        assertEquals(before, after)
        assertEquals(2, after.of(1).stars)
        assertEquals(setOf(14), after.of(2).seen)
        assertTrue(after.unlocked(2))
    }

    @Test
    fun `nonsense in storage costs stars but does not crash bedtime`() {
        assertEquals(Progress(), Progress.fromJson("not json at all"))
        assertEquals(Progress(), Progress.fromJson(""))
        assertEquals(Progress(), Progress.fromJson(null))
    }

    @Test
    fun `bangla digits are used for bangla and western for english`() {
        assertEquals("৯৯", 99.inLang(Lang.BN))
        assertEquals("১১", 11.inLang(Lang.BN))
        assertEquals("০", 0.inLang(Lang.BN))
        assertEquals("99", 99.inLang(Lang.EN))
    }
}
