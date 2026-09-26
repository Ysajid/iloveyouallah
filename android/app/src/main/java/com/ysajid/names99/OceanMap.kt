package com.ysajid.names99

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.hypot

/* ---------------- colours of the water ---------------- */

private val WaterTop = Color(0xFFCDEAF1)
private val WaterMid = Color(0xFFA9DCE8)
private val WaterLow = Color(0xFF7EC9DA)
private val WaterDeep = Color(0xFF5FB7CC)
private val Sun = Color(0xFFFFFDF0)
private val Foam = Color.White
private val Sailed = Color(0xFFE08C2C)
private val Beach = Color(0xFFFDF3DD)
private val HazeLand = Color(0xFFC6D2D8)
private val HazeSkirt = Color(0xFFA9B6BD)
private val HazeRim = Color(0xFFDFE7EA)

/** One island colour, lightened or darkened; every face stays on one hue. */
private fun shade(isl: Island, dl: Float, ds: Float = 0f): Color = Color.hsl(
    hue = isl.hue,
    saturation = (isl.sat + ds).coerceIn(0f, 100f) / 100f,
    lightness = (isl.light + dl).coerceIn(0f, 100f) / 100f,
)

/* ---------------- the chart ---------------- */

@Composable
fun OceanChart(state: JourneyState) {
    val words = state.words

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        val widthDp = maxWidth.value
        val heightDp = Chart.chartHeightDp(widthDp, state.journey.islands)

        // one clock for the whole chart; each island reads it at its own phase
        val clock = rememberInfiniteTransition(label = "chart")
        val tide by clock.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(7500), RepeatMode.Restart),
            label = "tide",
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
        ) {
            // only the route is drawn here; the water is the page background
            RouteLayer(state)

            state.journey.islands.forEach { isl ->
                IslandOnChart(state, isl, widthDp, heightDp, tide)
            }

            Boat(state, widthDp, heightDp, tide)
        }
    }
}

/* ---------------- water, sunlight, swell and the route ---------------- */

@Composable
private fun RouteLayer(state: JourneyState) {
    Canvas(Modifier.fillMaxSize()) {
        route(state, size.width, size.height)
    }
}

/** The sailing route, leg by leg: apricot behind you, white ahead. */
private fun DrawScope.route(state: JourneyState, w: Float, h: Float) {
    val islands = state.journey.islands
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(2.4f / Chart.SEA_W * w, 3.2f / Chart.SEA_W * w)
    )

    for (k in 0 until islands.size - 1) {
        val from = islands[k]
        val to = islands[k + 1]
        val x1 = from.x * w; val y1 = from.y * h
        val x2 = to.x * w; val y2 = to.y * h
        val mx = (x1 + x2) / 2f; val my = (y1 + y2) / 2f

        // push the midpoint sideways, alternating, for a lazy S down the chart
        val nx = -(y2 - y1); val ny = x2 - x1
        val len = hypot(nx, ny).takeIf { it > 0f } ?: 1f
        val pull = Chart.legPull(k) / Chart.SEA_W * w

        val sailed = state.progress.of(from.i).done
        val path = Path().apply {
            moveTo(x1, y1)
            quadraticTo(mx + nx / len * pull, my + ny / len * pull, x2, y2)
        }
        drawPath(
            path = path,
            color = if (sailed) Sailed.copy(alpha = 0.9f) else Foam.copy(alpha = 0.6f),
            style = Stroke(
                width = (if (sailed) 0.75f else 0.55f) / Chart.SEA_W * w,
                pathEffect = dash,
            ),
        )
    }
}

/* ---------------- one island ---------------- */

@Composable
private fun IslandOnChart(
    state: JourneyState,
    isl: Island,
    widthDp: Float,
    chartHeightDp: Float,
    tide: Float,
) {
    val words = state.words
    val open = state.progress.unlocked(isl.i)
    val here = state.progress.of(isl.i)
    val isCurrent = open && !here.done
    val justLit = state.justLit == isl.i

    val art = Chart.artSizeDp(isl, widthDp)
    val plateW = Chart.plateWidthDp(isl, widthDp)
    val (cx, cy) = Chart.centreDp(isl, widthDp, chartHeightDp)

    val outline = remember(isl.seed) { Coast.points(isl.seed, SAMPLES) }

    // Pressing an island should feel like pushing something solid: it squashes
    // and settles. Compose's default ripple is switched off — the island is
    // not a rectangle, and a rectangle flashing around it looked wrong.
    var pressed by remember { mutableStateOf(false) }
    val squashX by animateFloatAsState(
        if (pressed) 1.05f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "squashX",
    )
    val squashY by animateFloatAsState(
        if (pressed) 0.87f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 900f),
        label = "squashY",
    )

    // the island they are up to breathes, so the eye finds it
    val breathe = if (isCurrent) 1f + 0.035f * sin(tide * PI.toFloat() * 2f) else 1f

    // one celebration when an island has just been finished
    val lit = remember(justLit) { Animatable(if (justLit) 0.72f else 1f) }
    LaunchedEffect(justLit) {
        if (justLit) {
            lit.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 320f))
            state.clearJustLit()
        }
    }

    val bobPhase = (tide + isl.seed * 0.17f) % 1f
    val bob = if (open) (1f - abs(bobPhase * 2f - 1f) - 0.5f) * 0.03f * art else 0f

    Column(
        Modifier
            .offset(x = (cx - plateW / 2f).dp, y = (cy - art / 2f).dp)
            .width(plateW.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(art.dp)
                .offset(y = bob.dp)
                .graphicsLayer {
                    scaleX = squashX * lit.value * breathe
                    scaleY = squashY * lit.value * breathe
                }
                .then(
                    if (open) Modifier.pointerInput(isl.i, art) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val boxPx = size.width.toFloat()
                                // only the land counts, not the water around it
                                if (!IslandTouch.hitsLand(down.position, boxPx, outline)) continue
                                pressed = true
                                val up = waitForUpOrCancellation()
                                pressed = false
                                if (up != null &&
                                    IslandTouch.hitsLand(up.position, boxPx, outline)
                                ) {
                                    state.openIsland(isl.i)
                                }
                            }
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            IslandArt(isl, open)
            Text(
                if (open) isl.emoji else "🔒",
                fontSize = (art * 0.26f).coerceIn(15f, 30f).sp,
                modifier = Modifier.offset(y = -(art * 0.04f).dp),
            )
            if (here.done) {
                Box(
                    Modifier
                        .offset(x = (art * 0.26f).dp, y = -(art * 0.34f).dp)
                        .size((art * 0.24f).coerceIn(16f, 26f).dp)
                        .clip(RoundedCornerShape(50))
                        .background(Palette.green),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✓",
                        color = Color.White,
                        fontSize = (art * 0.14f).coerceIn(10f, 15f).sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        // the label holds still on purpose: the Bangla stays readable and the
        // island alone carries the motion
        Column(
            Modifier
                .offset(y = -(art * Chart.PLATE_TUCK).dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.card.copy(alpha = if (open) 0.94f else 0.80f))
                .border(
                    BorderStroke(
                        1.dp,
                        if (here.done) Palette.green.copy(alpha = 0.5f) else Palette.line,
                    ),
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                words.nth(isl.i),
                color = Palette.inkFaint,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                isl.title(state.lang),
                color = if (open) Palette.ink else Palette.inkFaint,
                fontSize = if (widthDp >= 560f) 15.sp else 13.sp,
                lineHeight = if (widthDp >= 560f) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (open) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { k ->
                        Text(
                            "★",
                            color = if (k < here.stars) Palette.goldDeep else Palette.emptyStar,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                Text(
                    words.locked,
                    color = Palette.inkFaint,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/* ---------------- one island, as an isometric solid ---------------- */

/** Points around the coastline. */
private const val SAMPLES = 44

/** The sun, upper-left, on the ground plane. */
private const val LIGHT_X = -0.55f
private const val LIGHT_Y = -0.83f

/**
 * The land.
 *
 * No 3D here: the coastline is a shape lying on the ground, and an oblique
 * projection tilts that ground away from the viewer —
 *
 *     screen.x = x
 *     screen.y = y * SQUASH_FACTOR - z
 *
 * — so an outline becomes a solid. Every coastline edge on the near side is
 * extruded into a wall and shaded by which way it faces, and that per-face
 * shading is what makes an island read as an object rather than a sticker.
 *
 * Same maths and the same seeds as the web build, so both draw one archipelago.
 */
@Composable
private fun IslandArt(isl: Island, open: Boolean) {
    val outline = remember(isl.seed) { Coast.points(isl.seed, SAMPLES) }

    // worked out once per island rather than per frame
    val sand = Color(0xFFF3E2BD)
    val hazeLand = Color(0xFFCFD9DE)
    val hazeSand = Color(0xFFDFE6E9)

    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension * 0.41f
        val h = r * ISLE_H_FRACTION
        val cx = size.width / 2f
        val cy = size.height * 0.44f

        fun project(p: Offset, scale: Float, z: Float, dx: Float = 0f, dy: Float = 0f) =
            Offset(cx + p.x * r * scale + dx, cy + p.y * r * scale * SQUASH_FACTOR - z + dy)

        val top = outline.map { project(it, 1f, h) }
        val base = outline.map { project(it, 1f, 0f) }
        val sandTop = outline.map { project(it, 1f, h * 0.26f) }

        // the shadow it casts on the water
        drawPath(
            polygonOf(outline.map { project(it, 1.02f, 0f, dx = h * 0.55f, dy = h * 0.30f) }),
            Color(0xFF175668).copy(alpha = if (open) 0.20f else 0.09f),
        )

        // the shallows it sits in, two soft rings instead of a blur
        drawPath(polygonOf(outline.map { project(it, 1.34f, 0f) }), Color.White.copy(alpha = 0.30f))
        drawPath(polygonOf(outline.map { project(it, 1.18f, 0f) }), Color.White.copy(alpha = 0.34f))

        // the walls, near side only, each shaded by the way it faces
        for (k in outline.indices) {
            val j = (k + 1) % outline.size
            var nx = (outline[k].x + outline[j].x) / 2f
            var ny = (outline[k].y + outline[j].y) / 2f
            val len = hypot(nx, ny).takeIf { it > 0f } ?: 1f
            nx /= len; ny /= len
            if (ny <= 0f) continue              // facing away: hidden behind the top

            val lit = 0.5f + 0.5f * (nx * LIGHT_X + ny * LIGHT_Y)
            drawPath(
                polygonOf(listOf(top[k], top[j], base[j], base[k])),
                if (open) shade(isl, -16f - 16f * (1f - lit), 4f)
                else hazeLand.copy(alpha = 0.75f),
            )
        }

        // a band of sand just above the waterline
        for (k in outline.indices) {
            val j = (k + 1) % outline.size
            val nx = (outline[k].x + outline[j].x) / 2f
            val ny = (outline[k].y + outline[j].y) / 2f
            val len = hypot(nx, ny).takeIf { it > 0f } ?: 1f
            if (ny / len <= 0f) continue
            drawPath(
                polygonOf(listOf(sandTop[k], sandTop[j], base[j], base[k])),
                if (open) sand else hazeSand,
            )
        }

        // the top face, lit from the upper left
        drawPath(
            polygonOf(top),
            brush = if (open) Brush.linearGradient(
                0f to shade(isl, 10f),
                0.6f to shade(isl, 0f),
                1f to shade(isl, -7f),
                start = Offset(cx - r, cy - h - r * SQUASH_FACTOR),
                end = Offset(cx + r, cy + r * SQUASH_FACTOR),
            ) else Brush.linearGradient(listOf(hazeLand, hazeLand)),
        )

        // a softer crown, so the top does not read as a flat card
        drawPath(
            polygonOf(outline.map { project(it, 0.62f, h, dy = -r * 0.055f) }),
            (if (open) shade(isl, 9f) else Color(0xFFDAE2E6)).copy(alpha = 0.55f),
        )
    }
}

/** A closed polygon through the given screen points. */
private fun polygonOf(points: List<Offset>): Path = Path().apply {
    points.forEachIndexed { index, p ->
        if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/* ---------------- the boat marks where they are up to ---------------- */

@Composable
private fun Boat(state: JourneyState, widthDp: Float, chartHeightDp: Float, tide: Float) {
    val next = state.journey.islands.firstOrNull {
        state.progress.unlocked(it.i) && !state.progress.of(it.i).done
    } ?: return

    val (cx, cy) = Chart.centreDp(next, widthDp, chartHeightDp)
    val side = if (next.x > 0.5f) -0.19f else 0.19f
    val phase = (tide * 1.4f) % 1f
    val rock = (1f - abs(phase * 2f - 1f) - 0.5f) * 8f

    Text(
        "⛵",
        fontSize = (widthDp * 0.045f).coerceIn(15f, 24f).sp,
        modifier = Modifier.offset(
            x = (cx + side * widthDp - 12f).dp,
            y = (cy - 12f + rock).dp,
        ),
    )
}
