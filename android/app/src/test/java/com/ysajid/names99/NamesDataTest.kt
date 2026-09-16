package com.ysajid.names99

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The content itself: 99 names, none missing, none doubled, nothing left blank.
 * Reads the same asset the app ships, so a bad edit to src/names.js fails here.
 */
class NamesDataTest {

    private val journey: Journey by lazy {
        val asset = File("src/main/assets/names.json")
        assertTrue("assets/names.json missing — run `node build.js`", asset.exists())
        Journey.parse(asset.readText())
    }

    @Test
    fun `there are ninety-nine names on eleven islands`() {
        assertEquals(99, journey.names.size)
        assertEquals(11, journey.islands.size)
    }

    @Test
    fun `every position in the traditional order appears exactly once`() {
        val positions = journey.names.map { it.n }.sorted()
        assertEquals((1..99).toList(), positions)
    }

    @Test
    fun `every island has exactly nine names`() {
        journey.islands.forEach { island ->
            assertEquals(
                "island ${island.i} (${island.title(Lang.EN)})",
                NAMES_PER_ISLAND,
                journey.namesOf(island.i).size,
            )
        }
    }

    @Test
    fun `every name carries every line in both languages`() {
        journey.names.forEach { entry ->
            assertTrue("name ${entry.n} has no Arabic", entry.arabic.isNotBlank())
            assertTrue("name ${entry.n} has no transliteration", entry.translit.isNotBlank())
            listOf(Lang.BN, Lang.EN).forEach { lang ->
                val lines = entry.lines(lang)
                assertTrue("name ${entry.n} $lang meaning", lines.meaning.isNotBlank())
                assertTrue("name ${entry.n} $lang about", lines.about.isNotBlank())
                assertTrue("name ${entry.n} $lang today", lines.today.isNotBlank())
            }
        }
    }

    @Test
    fun `the arabic is arabic and the bangla is bangla`() {
        val arabic = Regex("[\\u0600-\\u06FF]")
        val bangla = Regex("[\\u0980-\\u09FF]")
        journey.names.forEach { entry ->
            assertTrue("name ${entry.n} arabic script", arabic.containsMatchIn(entry.arabic))
            val bn = entry.lines(Lang.BN)
            assertTrue(
                "name ${entry.n} bangla script",
                bangla.containsMatchIn(bn.meaning + bn.about + bn.today),
            )
        }
        journey.islands.forEach { island ->
            assertTrue(
                "island ${island.i} bangla title",
                bangla.containsMatchIn(island.title(Lang.BN)),
            )
        }
    }

    @Test
    fun `each island points at a real island number`() {
        val known = journey.islands.map { it.i }.toSet()
        journey.names.forEach { entry ->
            assertTrue("name ${entry.n} on island ${entry.island}", entry.island in known)
        }
    }

    @Test
    fun `the full list runs one to ninety-nine in order`() {
        assertEquals(1, journey.inTraditionalOrder.first().n)
        assertEquals(99, journey.inTraditionalOrder.last().n)
        assertEquals(
            journey.inTraditionalOrder.map { it.n },
            journey.inTraditionalOrder.map { it.n }.sorted(),
        )
    }
}
