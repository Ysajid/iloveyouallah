package com.ysajid.names99

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
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.random.Random

private data class Blob(val x: Float, val y: Float, val r: Float, val tint: Color, val phase: Float)

/**
 * Soft shapes drifting behind everything, like light through a window.
 *
 * Deliberately very faint: in a large empty area anything stronger reads as
 * a stain rather than as light. Replaces the star field, which needed a dark
 * screen to read at all.
 */
@Composable
fun SoftLight(count: Int = 10) {
    val tints = remember {
        listOf(
            Color(0xFFF7CDB0), Color(0xFFCDBFE0), Color(0xFFA9DCC4),
            Color(0xFFF5CE63), Color(0xFF9FD8E4), Color(0xFFF0907A),
        )
    }
    val blobs = remember {
        val rng = Random(99)
        List(count) { k ->
            Blob(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = 70f + (k % 5) * 40f,
                tint = tints[k % tints.size],
                phase = rng.nextFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "drift")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t",
    )

    Canvas(Modifier.fillMaxSize()) {
        blobs.forEachIndexed { k, blob ->
            val phase = (t + blob.phase) % 1f
            // triangle wave, so the rise and fall are the same speed
            val lift = (1f - abs(phase * 2f - 1f)) * 14f * density
            drawCircle(
                color = blob.tint.copy(alpha = if (k % 3 == 0) 0.13f else 0.09f),
                radius = blob.r * density,
                center = Offset(blob.x * size.width, blob.y * size.height - lift),
            )
        }
    }
}
