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

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

/** Seventy slow stars. They never flash, because this gets used at bedtime. */
@Composable
fun StarField(count: Int = 70) {
    val stars = remember {
        val rng = Random(99)
        List(count) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                r = if (rng.nextFloat() < 0.15f) 2.2f else 1.5f,
                phase = rng.nextFloat(),
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "twinkle")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t",
    )

    Canvas(Modifier.fillMaxSize()) {
        stars.forEach { star ->
            // triangle wave, so the fade in and out are the same speed
            val phase = (t + star.phase) % 1f
            val alpha = 0.18f + 0.57f * (1f - abs(phase * 2f - 1f))
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.r * density,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}
