package com.ysajid.names99

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The chart is laid out by arithmetic, so the arithmetic is worth pinning down
 * — especially label collision, which is what actually went wrong when this
 * was built for the browser. There is no emulator here, so this is the closest
 * thing to seeing it.
 */
class ChartGeometryTest {

    private val journey: Journey by lazy {
        Journey.parse(File("src/main/assets/names.json").readText())
    }

    /** A plate is roughly this tall once the number, name and stars are in it. */
    private val plateHeight = 58f

    private fun overlaps(a: FloatArray, b: FloatArray): Boolean =
        a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3]

    private fun collisionsAt(widthDp: Float): List<Pair<Int, Int>> {
        val h = Chart.chartHeightDp(widthDp, journey.islands, plateHeight)
        val boxes = journey.islands.map { it.i to Chart.boundsDp(it, widthDp, h, plateHeight) }
        val hits = mutableListOf<Pair<Int, Int>>()
        for (i in boxes.indices) {
            for (j in i + 1 until boxes.size) {
                if (overlaps(boxes[i].second, boxes[j].second)) {
                    hits += boxes[i].first to boxes[j].first
                }
            }
        }
        return hits
    }

    @Test
    fun `no two islands collide at any plausible screen width`() {
        // small phone, big phone, small tablet, big tablet, and the breakpoints
        listOf(320f, 360f, 380f, 411f, 480f, 559f, 560f, 600f, 699f, 700f, 800f, 1000f)
            .forEach { w ->
                val hits = collisionsAt(w)
                assertTrue("collisions at ${w}dp: $hits", hits.isEmpty())
            }
    }

    @Test
    fun `the chart grows longer, relative to its width, as the screen narrows`() {
        val narrow = Chart.chartHeightDp(360f, journey.islands) / 360f
        val mid = Chart.chartHeightDp(600f, journey.islands) / 600f
        val wide = Chart.chartHeightDp(900f, journey.islands) / 900f
        assertTrue("narrow $narrow should exceed mid $mid", narrow > mid)
        assertTrue("mid $mid should exceed wide $wide", mid > wide)
    }

    @Test
    fun `every island has room for its own label before the next island`() {
        listOf(320f, 360f, 411f, 480f, 600f, 800f, 1000f).forEach { w ->
            val h = Chart.chartHeightDp(w, journey.islands, plateHeight)
            journey.islands.zipWithNext { above, below ->
                val gapDp = (below.y - above.y) * h
                val box = Chart.boxHeightDp(above, w, plateHeight)
                assertTrue(
                    "at ${w}dp island ${above.i} label ($box) reaches island ${below.i} (gap $gapDp)",
                    gapDp >= box,
                )
            }
        }
    }

    @Test
    fun `the chart stays a sensible length to scroll`() {
        // over-correcting is its own bug: endless scrolling to reach island 11
        listOf(320f, 360f, 600f, 1000f).forEach { w ->
            val screens = Chart.chartHeightDp(w, journey.islands, plateHeight) / w
            assertTrue("at ${w}dp the chart is $screens screens tall", screens < 7f)
        }
    }

    @Test
    fun `islands stop growing on a big screen`() {
        val biggest = journey.islands.maxByOrNull { it.size }!!
        val onTablet = Chart.artSizeDp(biggest, 1000f)
        assertTrue("an island should not be $onTablet dp across", onTablet <= Chart.ART_MAX_DP * biggest.size)
        // but it should still scale on a phone
        assertTrue(Chart.artSizeDp(biggest, 360f) < Chart.artSizeDp(biggest, 560f))
    }

    @Test
    fun `every island lands inside the chart`() {
        listOf(360f, 600f, 900f).forEach { w ->
            val h = Chart.chartHeightDp(w, journey.islands)
            journey.islands.forEach { isl ->
                val (cx, cy) = Chart.centreDp(isl, w, h)
                assertTrue("island ${isl.i} x at ${w}dp", cx > 0f && cx < w)
                assertTrue("island ${isl.i} y at ${w}dp", cy > 0f && cy < h)
            }
        }
    }

    @Test
    fun `islands run top to bottom in journey order`() {
        val ys = journey.islands.map { it.y }
        assertEquals(ys.sorted(), ys)
    }

    @Test
    fun `every island has a coastline to wear`() {
        assertEquals(5, journey.coasts.size)
        journey.islands.forEach { isl ->
            val coast = journey.coastFor(isl)
            assertTrue("island ${isl.i} coast", coast.startsWith("M") && coast.endsWith("Z"))
        }
    }

    @Test
    fun `plates never fall below a readable width`() {
        journey.islands.forEach { isl ->
            assertTrue(
                "island ${isl.i} plate too narrow on a small phone",
                Chart.plateWidthDp(isl, 320f) >= Chart.PLATE_MIN_DP,
            )
        }
    }

    @Test
    fun `route legs alternate the way they bend`() {
        assertTrue(Chart.legPull(0) < 0f)
        assertTrue(Chart.legPull(1) > 0f)
        assertTrue(Chart.legPull(2) < 0f)
    }
}
