package com.ysajid.names99

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap

/**
 * The ocean, edge to edge, behind the whole map.
 *
 * Two layers of the same seamless tile scrolling left to right at different
 * speeds. The far one is larger and slower, and that difference is what makes
 * the water read as having depth rather than sliding past as one flat sheet.
 *
 * The tile is drawn by build.js and checked there for seamlessness, so it can
 * scroll for ever without a repeating scar.
 */
@Composable
fun Ocean(modifier: Modifier = Modifier) {
    val waves: ImageBitmap = ImageBitmap.imageResource(R.drawable.waves)

    val near = remember(waves) { ShaderBrush(ImageShader(waves, TileMode.Repeated, TileMode.Repeated)) }
    val far = remember(waves) { ShaderBrush(ImageShader(waves, TileMode.Repeated, TileMode.Repeated)) }

    val clock = rememberInfiniteTransition(label = "ocean")
    val nearRun by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "near",
    )
    val farRun by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(54_000, easing = LinearEasing), RepeatMode.Restart),
        label = "far",
    )

    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFFCDEAF1),
                0.26f to Color(0xFFA9DCE8),
                0.62f to Color(0xFF7EC9DA),
                1f to Color(0xFF5FB7CC),
            )
        )

        // each layer is drawn one tile wider than the screen and slid across,
        // so the scroll never exposes an edge
        fun layer(brush: ShaderBrush, tile: Float, run: Float, alpha: Float) {
            val shift = run * tile
            withTransform({ translate(shift - tile, 0f) }) {
                drawRect(
                    brush = brush,
                    topLeft = Offset.Zero,
                    size = Size(size.width + tile * 2f, size.height),
                    alpha = alpha,
                )
            }
        }

        layer(far, 760f, farRun, 0.55f)
        layer(near, 420f, nearRun, 1f)
    }
}
