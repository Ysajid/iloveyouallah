package com.ysajid.names99

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Colours from the playroom: cream walls, the rainbow arch (teal, butter,
 * apricot, peach) and the rug. Everything sits a little below full strength —
 * pastel reads as calm, the same hues at full saturation read as a toy shop.
 *
 * Kept in step with src/styles.css so the two builds look like one app.
 */
object Palette {
    // surfaces, warm rather than white
    val paper = Color(0xFFFBF6EC)
    val sand = Color(0xFFF3E9D8)
    val card = Color(0xFFFFFDF8)
    val line = Color(0xFFE7DCC7)
    val cardLine = Color(0xFFE7DCC7)

    // ink. Warm dark brown, not black: softer under a bedside lamp.
    // inkFaint was darkened until it cleared WCAG AA on cream.
    val ink = Color(0xFF3B332A)
    val inkSoft = Color(0xFF6E6455)
    val inkFaint = Color(0xFF736A5E)

    // the arch and the rug
    val gold = Color(0xFFF5CE63)        // butter
    val goldDeep = Color(0xFFF0A24E)    // apricot
    val onGold = Color(0xFF4A3208)      // text that sits on top of either
    val teal = Color(0xFF2E7D8A)        // the Arabic
    val sky = Color(0xFF63C0D1)
    val green = Color(0xFF6BB886)       // an island finished
    val rose = Color(0xFFDD6459)        // a wrong match
    val emptyStar = Color(0xFFDCCFB6)

    /** The three faces of one island, all on its own hue, lit from one side. */
    fun islandFill(isl: Island) = Color.hsl(isl.hue, isl.sat / 100f, isl.light / 100f)
    fun islandEdge(isl: Island) = Color.hsl(isl.hue, (isl.sat - 6).coerceIn(0f, 100f) / 100f, ((isl.light + 12) / 100f).coerceAtMost(1f), 0.9f)
    fun islandBadge(isl: Island) = Color.hsl(isl.hue, isl.sat / 100f, ((isl.light + 8) / 100f).coerceAtMost(1f))
}

/**
 * Arabic and Bangla both come from the system fonts, which every Android
 * device since 5.0 ships with. Nothing to bundle, nothing to download.
 */
val AppTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontSize = 18.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
    ),
)

val DaySky = Brush.linearGradient(
    colors = listOf(Color(0xFFFFFDF6), Palette.paper, Palette.sand),
    start = Offset.Zero,
    end = Offset(0f, Float.POSITIVE_INFINITY),
)

@Composable
fun DayBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(DaySky)) {
        SoftLight()
        content()
    }
}
