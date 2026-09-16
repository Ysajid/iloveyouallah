package com.ysajid.names99

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

/** Same night sky as the web version, so the two do not feel like different apps. */
object Palette {
    val night1 = Color(0xFF0B1026)
    val night2 = Color(0xFF141A3A)
    val night3 = Color(0xFF1D1240)

    val ink = Color(0xFFF4F1EA)
    val inkSoft = Color(0xFFB9B6CC)
    val inkFaint = Color(0xFF7B7897)

    val gold = Color(0xFFF4C95D)
    val goldDeep = Color(0xFFC99A2E)
    val green = Color(0xFF5FCF8C)
    val rose = Color(0xFFEF7D7D)

    val card = Color(0x12FFFFFF)
    val cardLine = Color(0x24FFFFFF)

    /** Each island gets its own colour, from the hue carried in the data. */
    fun islandFill(hue: Float) = Color.hsl(hue, 0.50f, 0.18f)
    fun islandEdge(hue: Float) = Color.hsl(hue, 0.60f, 0.45f, 0.35f)
    fun islandBadge(hue: Float) = Color.hsl(hue, 0.60f, 0.50f, 0.18f)
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

val NightSky = Brush.linearGradient(
    colors = listOf(Palette.night3, Palette.night2, Palette.night1),
    start = Offset.Zero,
    end = Offset(0f, Float.POSITIVE_INFINITY),
)

@Composable
fun NightBackdrop(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(NightSky)) {
        StarField()
        content()
    }
}
