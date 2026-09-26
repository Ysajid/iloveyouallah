package com.ysajid.names99

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot

/**
 * The coastline maths is duplicated in two languages — GDScript aside, the web
 * build has it in src/islands.js and the app has it in Coast.kt. Duplicated
 * maths drifts silently, and the symptom would be the two apps drawing
 * different islands under the same name.
 *
 * These values were taken from the JavaScript. If either side changes, this
 * fails instead.
 */
class CoastTest {

    private data class Golden(val seed: Int, val index: Int, val radius: Float)

    private val fromJavaScript = listOf(
        Golden(0, 0, 1.000000000f),
        Golden(0, 5, 1.153228580f),
        Golden(0, 11, 0.965000000f),
        Golden(0, 23, 0.978991405f),
        Golden(0, 37, 0.923560696f),
        Golden(1, 0, 0.914603932f),
        Golden(1, 5, 0.929096996f),
        Golden(1, 11, 1.163850181f),
        Golden(1, 23, 0.873624748f),
        Golden(1, 37, 1.011868528f),
        Golden(2, 0, 0.936927836f),
        Golden(2, 5, 0.950492678f),
        Golden(2, 11, 0.966736497f),
        Golden(2, 23, 1.101204291f),
        Golden(2, 37, 1.123169181f),
        Golden(3, 0, 1.140172678f),
        Golden(3, 5, 1.098165316f),
        Golden(3, 11, 0.800491174f),
        Golden(3, 23, 1.070297693f),
        Golden(3, 37, 0.860502989f),
        Golden(4, 0, 1.019248867f),
        Golden(4, 5, 0.984438579f),
        Golden(4, 11, 1.114386652f),
        Golden(4, 23, 0.936378256f),
        Golden(4, 37, 0.917094088f),
    )

    @Test
    fun `the coastline matches the web build point for point`() {
        fromJavaScript.forEach { g ->
            val angle = PI * 2.0 * g.index / 44
            assertEquals(
                "seed ${g.seed}, point ${g.index}",
                g.radius.toDouble(),
                Coast.radius(angle, g.seed),
                1e-6,
            )
        }
    }

    @Test
    fun `an island is irregular but never pinched or ballooned`() {
        (0..4).forEach { seed ->
            val radii = Coast.points(seed, 44).map { hypot(it.x, it.y) }
            val min = radii.min()
            val max = radii.max()
            assertTrue("seed $seed collapses to $min", min > 0.6f)
            assertTrue("seed $seed balloons to $max", max < 1.4f)
            assertTrue("seed $seed is a plain circle", max - min > 0.1f)
        }
    }

    @Test
    fun `the outline closes on itself`() {
        val pts = Coast.points(2, 44)
        assertEquals(44, pts.size)
        // the last point and the first should be neighbours, not a jump
        val step = hypot(pts.first().x - pts.last().x, pts.first().y - pts.last().y)
        assertTrue("gap of $step between the last point and the first", step < 0.25f)
    }

    @Test
    fun `the same seed always draws the same island`() {
        assertEquals(Coast.points(3, 44), Coast.points(3, 44))
    }

    @Test
    fun `different islands get different coastlines`() {
        val a = Coast.points(0, 44)
        val b = Coast.points(3, 44)
        assertTrue("seeds 0 and 3 drew the same shape", a != b)
    }
}
