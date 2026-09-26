package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisArcGlow
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDeepBlue
import com.example.ui.theme.JarvisHoloLight
import com.example.ui.theme.StatusOnline
import com.example.ui.theme.StatusWarning
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    SCANNING
}

@Composable
fun JarvisOrb(
    orbState: OrbState,
    audioRms: Float = 0f,
    size: Dp = 230.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_transition")

    // Slow continuous outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    // Counter rotation for inner cyber ring
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    // Base pulsing breath
    val pulseBreath by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_breath"
    )

    // Rapid energy flare when thinking
    val energyFlare by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (orbState == OrbState.THINKING) 400 else 1400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "energy_flare"
    )

    // Smooth reactive audio expansion
    val animatedRms = remember { Animatable(0f) }
    LaunchedEffect(audioRms) {
        animatedRms.animateTo(
            targetValue = audioRms,
            animationSpec = tween(100)
        )
    }

    val stateColor = when (orbState) {
        OrbState.IDLE -> JarvisCyan
        OrbState.LISTENING -> JarvisArcGlow
        OrbState.THINKING -> JarvisBlue
        OrbState.SPEAKING -> StatusOnline
        OrbState.SCANNING -> StatusWarning
    }

    val stateLabel = when (orbState) {
        OrbState.IDLE -> "JARVIS // STANDBY"
        OrbState.LISTENING -> "JARVIS // LISTENING..."
        OrbState.THINKING -> "JARVIS // PROCESSING..."
        OrbState.SPEAKING -> "JARVIS // SYNTHESIZING..."
        OrbState.SCANNING -> "JARVIS // OPTICAL SCAN..."
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = this.size.minDimension / 2.6f

                // Dynamic scale based on state & sound
                val soundFactor = if (orbState == OrbState.LISTENING) {
                    1f + (animatedRms.value * 0.45f)
                } else if (orbState == OrbState.SPEAKING) {
                    pulseBreath * 1.15f
                } else {
                    pulseBreath
                }

                val currentRadius = baseRadius * soundFactor

                // 1. Outermost Glowing Ambient Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            stateColor.copy(alpha = 0.45f),
                            JarvisBlue.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius * 1.55f
                    ),
                    center = center,
                    radius = currentRadius * 1.55f
                )

                // 2. Outer Rotating Telemetry HUD Ring with tick marks
                val outerHudRadius = currentRadius * 1.25f
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f, 32f, 18f), outerRotation)
                drawCircle(
                    color = JarvisCyan.copy(alpha = 0.65f),
                    center = center,
                    radius = outerHudRadius,
                    style = Stroke(width = 2.5f, pathEffect = dashEffect)
                )

                // Outer Ring Orbiting Telemetry Marks
                val angleRad = Math.toRadians(outerRotation.toDouble())
                val orbitX = center.x + (outerHudRadius * cos(angleRad)).toFloat()
                val orbitY = center.y + (outerHudRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    center = Offset(orbitX, orbitY),
                    radius = 4.5f
                )

                val orbitX2 = center.x + (outerHudRadius * cos(angleRad + PI)).toFloat()
                val orbitY2 = center.y + (outerHudRadius * sin(angleRad + PI)).toFloat()
                drawCircle(
                    color = JarvisCyan,
                    center = Offset(orbitX2, orbitY2),
                    radius = 3.5f
                )

                // 3. Counter-rotating Intermediate Cyber Ring
                val innerHudRadius = currentRadius * 1.08f
                val innerDashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 14f, 4f, 14f), innerRotation)
                drawCircle(
                    color = JarvisBlue.copy(alpha = 0.75f),
                    center = center,
                    radius = innerHudRadius,
                    style = Stroke(width = 1.8f, pathEffect = innerDashEffect)
                )

                // 4. Arc Reactor Plasma Core Sphere (Layered Gradients)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JarvisHoloLight,
                            stateColor,
                            JarvisBlue,
                            JarvisDeepBlue,
                            Color(0xFF030D20)
                        ),
                        center = center,
                        radius = currentRadius
                    ),
                    center = center,
                    radius = currentRadius
                )

                // 5. Bright Central Arc Reactor Core
                val coreRadius = currentRadius * 0.38f * (if (orbState == OrbState.THINKING) energyFlare else 1.0f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            JarvisArcGlow,
                            stateColor.copy(alpha = 0.9f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreRadius
                    ),
                    center = center,
                    radius = coreRadius
                )

                // 6. Reactive Sound Wave Rings (when listening or speaking)
                if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING) {
                    val waveCount = 3
                    for (i in 1..waveCount) {
                        val waveRadius = currentRadius + (i * 12f * (animatedRms.value + 0.3f))
                        val waveAlpha = (1f - (i.toFloat() / (waveCount + 1))) * 0.8f
                        drawCircle(
                            color = stateColor.copy(alpha = waveAlpha),
                            center = center,
                            radius = waveRadius,
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stateLabel,
            color = stateColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}
