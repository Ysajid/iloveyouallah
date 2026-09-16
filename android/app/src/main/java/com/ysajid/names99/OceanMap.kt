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
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        val drift by clock.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(26000), RepeatMode.Restart),
            label = "drift",
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(heightDp.dp)
                .clip(RoundedCornerShape(26.dp))
                .border(BorderStroke(1.dp, Color(0xFFCFE3E8)), RoundedCornerShape(26.dp)),
        ) {
            Sea(state, drift)

            state.journey.islands.forEach { isl ->
                IslandOnChart(state, isl, widthDp, heightDp, tide)
            }

            Boat(state, widthDp, heightDp, tide)
        }
    }
}

/* ---------------- water, sunlight, swell and the route ---------------- */

@Composable
private fun Sea(state: JourneyState, drift: Float) {
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.linearGradient(
                0f to WaterTop, 0.30f to WaterMid, 0.68f to WaterLow, 1f to WaterDeep,
                start = Offset(0f, 0f),
                end = Offset(w * 0.25f, h),
            )
        )

        // sunlight off the top corner
        drawCircle(
            brush = Brush.radialGradient(
                0f to Sun.copy(alpha = 0.72f),
                0.55f to Color(0xFFFFF6D8).copy(alpha = 0.22f),
                1f to Color(0xFFFFF6D8).copy(alpha = 0f),
                center = Offset(w * 0.20f, h * 0.033f),
                radius = w * 0.74f,
            ),
            radius = w * 0.74f,
            center = Offset(w * 0.20f, h * 0.033f),
        )

        swell(w, h, drift)
        route(state, w, h)
    }
}

/** Foam lines across the whole chart. Cheap, and they read as water. */
private fun DrawScope.swell(w: Float, h: Float, drift: Float) {
    val shift = drift * 8f / Chart.SEA_W * w
    var y = 4f
    while (y < Chart.SEA_H) {
        val yPx = y / Chart.SEA_H * h
        val path = Path().apply {
            moveTo(-12f / Chart.SEA_W * w + shift, yPx)
            var x = -12f
            while (x <= Chart.SEA_W + 12f) {
                val lift = (((x + y).toInt() % 3) - 1) * 0.5f / Chart.SEA_H * h
                relativeQuadraticTo(
                    4f / Chart.SEA_W * w, lift,
                    8f / Chart.SEA_W * w, 0f,
                )
                x += 8f
            }
        }
        drawPath(
            path = path,
            color = Foam.copy(alpha = 0.30f),
            style = Stroke(width = if (y % 11f < 5.5f) 0.26f / Chart.SEA_W * w else 0.15f / Chart.SEA_W * w),
        )
        y += 5.5f
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

    val art = Chart.artSizeDp(isl, widthDp)
    val plateW = Chart.plateWidthDp(isl, widthDp)
    val (cx, cy) = Chart.centreDp(isl, widthDp, chartHeightDp)

    // triangle wave, so the rise and fall are the same speed; each island
    // starts at its own point in the cycle so they do not bob in unison
    val phase = (tide + isl.seed * 0.17f) % 1f
    val bob = if (open) (1f - abs(phase * 2f - 1f) - 0.5f) * 0.03f * art else 0f

    val coast = remember(isl.i) { state.journey.coastFor(isl) }

    Column(
        Modifier
            .offset(x = (cx - plateW / 2f).dp, y = (cy - art / 2f).dp)
            .width(plateW.dp)
            .then(
                if (open) Modifier.clickable { state.openIsland(isl.i) } else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(art.dp)
                .offset(y = bob.dp),
            contentAlignment = Alignment.Center,
        ) {
            IslandArt(isl, coast, open)
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

        // the label, tucked up over the shore. Deliberately not animated: it
        // holds still so the Bangla stays readable and taps land where aimed.
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

/**
 * The land. Depth is faked, not modelled: a lit top face over a darker copy
 * of the same coastline pushed down, which is the cliff side.
 */
@Composable
private fun IslandArt(isl: Island, coast: String, open: Boolean) {
    val land = remember(coast) {
        if (coast.isEmpty()) Path() else PathParser().parsePathString(coast).toPath()
    }

    Canvas(Modifier.fillMaxSize()) {
        if (coast.isEmpty()) return@Canvas
        // the art is drawn on a 132-unit box so the shallows and the cliff
        // have room to spill outside the 100-unit coastline
        val k = size.minDimension / 132f

        withTransform({
            translate(16f * k, 10f * k)
            scale(k, k, pivot = Offset.Zero)
        }) {
            // the paler ring of water an island sits in
            val ringW = if (open) 128f else 100f
            val ringH = if (open) 88f else 68f
            drawOval(
                brush = if (open) Brush.radialGradient(
                    0.38f to Color(0xFFE6F7F3).copy(alpha = 0f),
                    0.60f to Color(0xFFDFF5F0).copy(alpha = 0.70f),
                    1f to Color(0xFFCFEEF0).copy(alpha = 0f),
                    center = Offset(50f, 52f),
                    radius = ringW / 2f,
                ) else Brush.radialGradient(
                    0.35f to Color.White.copy(alpha = 0.62f),
                    1f to Color.White.copy(alpha = 0f),
                    center = Offset(50f, 52f),
                    radius = ringW / 2f,
                ),
                topLeft = Offset(50f - ringW / 2f, 52f - ringH / 2f),
                size = Size(ringW, ringH),
            )

            // the cliff side
            withTransform({ translate(0f, 10f) }) {
                drawPath(
                    path = land,
                    color = if (open) shade(isl, -26f, 6f) else HazeSkirt.copy(alpha = 0.55f),
                )
            }
            // a pale beach where land meets water
            withTransform({ translate(0f, 3.5f) }) {
                drawPath(
                    path = land,
                    color = Beach.copy(alpha = if (open) 0.85f else 0.30f),
                    style = Stroke(width = 3.4f),
                )
            }
            // the lit top face
            drawPath(
                path = land,
                brush = if (open) Brush.linearGradient(
                    0f to shade(isl, 8f),
                    0.55f to shade(isl, 0f),
                    1f to shade(isl, -9f),
                    start = Offset(20f, 0f),
                    end = Offset(80f, 100f),
                ) else Brush.linearGradient(
                    0f to HazeLand.copy(alpha = 0.72f),
                    1f to HazeLand.copy(alpha = 0.72f),
                ),
            )
            drawPath(
                path = land,
                color = if (open) shade(isl, 12f, -6f).copy(alpha = 0.7f)
                        else HazeRim.copy(alpha = 0.4f),
                style = Stroke(width = 2f),
            )
        }
    }
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
