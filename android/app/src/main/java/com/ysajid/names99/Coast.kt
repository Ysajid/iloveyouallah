package com.ysajid.names99

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The coastline of an island, as points on a unit circle.
 *
 * A circle plus three harmonics, with the island's own seed setting the
 * phases, so every build draws the same archipelago and the kids learn the
 * shape of their own map.
 *
 * This is a deliberate mirror of coastRadius/coastPoints in src/islands.js.
 * The two are checked against each other by CoastTest, which holds values
 * taken from the JavaScript: if either drifts, the test fails rather than the
 * two apps quietly disagreeing about where the land is.
 */
object Coast {

    /** How far out the shore lies at this angle, around a radius of 1. */
    fun radius(angle: Double, seed: Int): Double {
        val s = seed * 1.7
        return 1.0 +
            0.11 * sin(angle * 2 - s) +
            0.07 * sin(angle * 3 + s * 1.3) +
            0.035 * sin(angle * 5 - s * 0.7)
    }

    /** The outline as `count` points, going clockwise on screen. */
    fun points(seed: Int, count: Int): List<Offset> = List(count) { k ->
        val a = PI * 2.0 * k / count
        val r = radius(a, seed)
        Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
    }
}
