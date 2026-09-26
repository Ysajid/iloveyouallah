package com.ysajid.names99

import androidx.compose.ui.geometry.Offset

/**
 * Where an island actually is, for the purposes of being tapped.
 *
 * The tap target used to be the whole square box the island is drawn in, with
 * Compose's default ripple painting that whole rectangle. On an irregular
 * island sitting in open water that is wrong twice over: taps land on water
 * and still count, and the feedback is a rectangle flashing around something
 * that is not a rectangle.
 *
 * So the hit test asks the real question: is this point on the land? The land
 * is the coastline swept upward by the island's height — the top face, the
 * walls, and everything between — which is tested by checking the point and a
 * few samples below it against the coastline at the waterline.
 */
object IslandTouch {

    /** Vertical samples between the top face and the waterline. */
    private const val SAMPLES = 5

    /**
     * @param local tap position within the island's art box
     * @param box   size of that box, in the same units
     */
    fun hitsLand(local: Offset, box: Float, outline: List<Offset>): Boolean {
        if (outline.isEmpty()) return false

        val r = box * 0.41f
        val h = r * ISLE_H_FRACTION
        val cx = box / 2f
        val cy = box * 0.44f

        // the coastline at the waterline, in box coordinates
        val shore = outline.map { Offset(cx + it.x * r, cy + it.y * r * SQUASH_FACTOR) }

        // the land is that shore swept up by h: test the point, the point
        // pushed all the way down, and a few places in between
        for (k in 0..SAMPLES) {
            val lift = h * k / SAMPLES
            if (inside(Offset(local.x, local.y + lift), shore)) return true
        }
        return false
    }

    /** Standard ray crossing test. */
    fun inside(point: Offset, polygon: List<Offset>): Boolean {
        var hit = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            if ((a.y > point.y) != (b.y > point.y)) {
                val x = a.x + (point.y - a.y) / (b.y - a.y) * (b.x - a.x)
                if (point.x < x) hit = !hit
            }
            j = i
        }
        return hit
    }
}

/** Shared with the drawing so the hit test and the picture cannot disagree. */
const val SQUASH_FACTOR = 0.54f
const val ISLE_H_FRACTION = 0.46f
