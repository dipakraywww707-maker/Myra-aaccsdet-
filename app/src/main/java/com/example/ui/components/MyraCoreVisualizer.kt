package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraEmerald
import com.example.ui.theme.MyraPurple
import com.example.ui.theme.MyraViolet
import kotlin.math.cos
import kotlin.math.sin

enum class MyraState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun MyraCoreVisualizer(
    state: MyraState,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MyraOrb")

    // Breathing pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = when (state) {
            MyraState.LISTENING -> 1.18f
            MyraState.SPEAKING -> 1.12f
            MyraState.THINKING -> 1.05f
            MyraState.IDLE -> 0.98f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    MyraState.LISTENING -> 600
                    MyraState.SPEAKING -> 800
                    MyraState.THINKING -> 500
                    MyraState.IDLE -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Continuous rotation for gyroscope rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    MyraState.THINKING -> 2000
                    MyraState.LISTENING -> 4000
                    MyraState.SPEAKING -> 5000
                    MyraState.IDLE -> 9000
                },
                easing = LinearEasing
            )
        ),
        label = "Rotation"
    )

    // Wave ripple
    val ripple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == MyraState.SPEAKING || state == MyraState.LISTENING) 1200 else 2400,
                easing = LinearEasing
            )
        ),
        label = "Ripple"
    )

    val coreColors = when (state) {
        MyraState.LISTENING -> listOf(MyraCyan, Color(0xFF00B0FF), MyraEmerald)
        MyraState.THINKING -> listOf(MyraViolet, MyraPurple, MyraCyan)
        MyraState.SPEAKING -> listOf(MyraCyan, MyraViolet, Color(0xFFFF4081))
        MyraState.IDLE -> listOf(MyraCyan, MyraViolet, MyraPurple)
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.45f

            // Outer expanding ripple for speaking or listening
            if (state == MyraState.SPEAKING || state == MyraState.LISTENING) {
                val rippleRadius = baseRadius + (ripple * baseRadius * 0.9f)
                val rippleAlpha = (1f - ripple).coerceIn(0f, 0.7f)
                drawCircle(
                    color = coreColors[0].copy(alpha = rippleAlpha * 0.5f),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Outer Orbit Ring 1
            rotate(rotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(coreColors[0].copy(alpha = 0.8f), Color.Transparent, coreColors[1], Color.Transparent)
                    ),
                    radius = baseRadius * 1.35f * pulse,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Outer Orbit Ring 2 (counter-rotating)
            rotate(-rotation * 1.3f, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, coreColors[1].copy(alpha = 0.7f), Color.Transparent, coreColors[2].copy(alpha = 0.8f))
                    ),
                    radius = baseRadius * 1.15f * pulse,
                    center = center,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // Inner holographic core glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColors[0].copy(alpha = 0.95f),
                        coreColors[1].copy(alpha = 0.7f),
                        coreColors[2].copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.1f * pulse
                ),
                radius = baseRadius * 1.1f * pulse,
                center = center
            )

            // Central solid luminous nucleus
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreColors[0].copy(alpha = 0.9f),
                        coreColors[1].copy(alpha = 0.6f)
                    ),
                    center = center,
                    radius = baseRadius * 0.5f * pulse
                ),
                radius = baseRadius * 0.5f * pulse,
                center = center
            )

            // Satellites / Orbiting particles
            val particleCount = 4
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((rotation * 2 + (i * (360.0 / particleCount))).toDouble())
                val orbitDist = baseRadius * (0.85f + (i * 0.12f))
                val px = center.x + (orbitDist * cos(angle)).toFloat()
                val py = center.y + (orbitDist * sin(angle)).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = 3.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
