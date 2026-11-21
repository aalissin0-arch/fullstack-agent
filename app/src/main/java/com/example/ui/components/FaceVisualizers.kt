package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.FaceStyle
import com.example.model.VoiceBusState
import com.example.ui.theme.CircuitAmber
import com.example.ui.theme.CircuitCyan
import com.example.ui.theme.CircuitEmerald
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AgentFaceVisualizer(
    faceStyle: FaceStyle,
    agentName: String,
    busState: VoiceBusState,
    waveformEnergy: Float,
    waveformBars: List<Float>,
    wiringHealthy: Boolean,
    modifier: Modifier = Modifier
) {
    val effectiveBusState = if (wiringHealthy) busState else VoiceBusState.IDLE
    val accentColor = when (effectiveBusState) {
        VoiceBusState.IDLE -> CircuitCyan.copy(alpha = 0.72f)
        VoiceBusState.LISTENING -> CircuitCyan
        VoiceBusState.THINKING -> CircuitAmber
        VoiceBusState.SPEAKING -> CircuitEmerald
    }

    val infiniteTransition = rememberInfiniteTransition(label = "face_pulse")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.14f),
                        Color(0xFF0A0F16),
                        Color(0xFF06090E)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        when (faceStyle) {
            FaceStyle.BOARD -> BoardCircuitCanvas(
                tick = tick,
                accentColor = accentColor,
                busState = effectiveBusState,
                energy = waveformEnergy,
                modifier = Modifier.fillMaxSize()
            )
            FaceStyle.RADIAL -> RadialWaveCanvas(
                tick = tick,
                accentColor = accentColor,
                bars = waveformBars,
                energy = waveformEnergy,
                modifier = Modifier.fillMaxSize()
            )
            FaceStyle.RAIN -> RainCascadeCanvas(
                tick = tick,
                accentColor = accentColor,
                busState = effectiveBusState,
                energy = waveformEnergy,
                modifier = Modifier.fillMaxSize()
            )
            FaceStyle.NEURAL -> NeuralMeshCanvas(
                tick = tick,
                accentColor = accentColor,
                busState = effectiveBusState,
                energy = waveformEnergy,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (faceStyle == FaceStyle.BOARD) {
            // Central silicon chip carrying the agent's name
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 156.dp, height = 96.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0E1520).copy(alpha = 0.94f),
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = accentColor
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = agentName.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF8FAFC)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (wiringHealthy) effectiveBusState.label.uppercase() else "BUS DISCONNECTED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                color = accentColor
                            )
                        )
                    }
                }
            }
        } else {
            // Subtle floating badge for Radial / Rain / Neural faces
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0E1520).copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$agentName · ${if (wiringHealthy) effectiveBusState.label else "Bus Stale"}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0))
                    )
                }
            }
        }

        // Top-right face pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0E1520).copy(alpha = 0.8f)
        ) {
            Text(
                text = faceStyle.id.uppercase(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(color = accentColor)
            )
        }
    }
}

@Composable
private fun BoardCircuitCanvas(
    tick: Float,
    accentColor: Color,
    busState: VoiceBusState,
    energy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val traceBase = Color(0xFF1A293B)
        val activeAlpha = (0.35f + energy * 0.65f).coerceIn(0.3f, 1f)
        val activeTrace = accentColor.copy(alpha = activeAlpha)

        val directions = listOf(
            Offset(-1f, -0.55f), Offset(-1f, -0.2f), Offset(-1f, 0.2f), Offset(-1f, 0.55f),
            Offset(1f, -0.55f), Offset(1f, -0.2f), Offset(1f, 0.2f), Offset(1f, 0.55f),
            Offset(-0.45f, -1f), Offset(0f, -1f), Offset(0.45f, -1f),
            Offset(-0.45f, 1f), Offset(0f, 1f), Offset(0.45f, 1f)
        )

        directions.forEachIndexed { idx, dir ->
            val startOffset = Offset(
                cx + dir.x * size.width * 0.14f,
                cy + dir.y * size.height * 0.14f
            )
            val kneeOffset = Offset(
                cx + dir.x * size.width * 0.29f,
                cy + dir.y * size.height * 0.29f
            )
            val endOffset = Offset(
                if (kotlin.math.abs(dir.x) >= kotlin.math.abs(dir.y)) {
                    if (dir.x < 0) 14f else size.width - 14f
                } else {
                    kneeOffset.x
                },
                if (kotlin.math.abs(dir.y) > kotlin.math.abs(dir.x)) {
                    if (dir.y < 0) 14f else size.height - 14f
                } else {
                    kneeOffset.y
                }
            )

            val path = Path().apply {
                moveTo(startOffset.x, startOffset.y)
                lineTo(kneeOffset.x, kneeOffset.y)
                lineTo(endOffset.x, endOffset.y)
            }

            drawPath(
                path = path,
                color = traceBase,
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = path,
                color = activeTrace.copy(alpha = 0.35f + (energy * 0.55f)),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Solder pad at end
            drawCircle(
                color = accentColor.copy(alpha = 0.75f),
                radius = 4.5f,
                center = endOffset
            )

            // Traveling signal packet
            val speedFactor = when (busState) {
                VoiceBusState.IDLE -> 1f
                VoiceBusState.LISTENING -> 2f
                VoiceBusState.THINKING -> 3f
                VoiceBusState.SPEAKING -> 4f
            }
            val progress = ((tick * speedFactor + idx * 0.13f) % 1f)
            val pulsePos = if (progress < 0.5f) {
                val p = progress * 2f
                Offset(
                    startOffset.x + (kneeOffset.x - startOffset.x) * p,
                    startOffset.y + (kneeOffset.y - startOffset.y) * p
                )
            } else {
                val p = (progress - 0.5f) * 2f
                Offset(
                    kneeOffset.x + (endOffset.x - kneeOffset.x) * p,
                    kneeOffset.y + (endOffset.y - kneeOffset.y) * p
                )
            }
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.5f + energy * 2.5f,
                center = pulsePos
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.4f),
                radius = 9f + energy * 5f,
                center = pulsePos
            )
        }

        // Glow ring around central chip
        drawRoundRect(
            color = accentColor.copy(alpha = 0.18f + energy * 0.35f),
            topLeft = Offset(cx - 88.dp.toPx(), cy - 56.dp.toPx()),
            size = Size(176.dp.toPx(), 112.dp.toPx()),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun RadialWaveCanvas(
    tick: Float,
    accentColor: Color,
    bars: List<Float>,
    energy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension * 0.19f

        // Concentric harmonic rings
        for (ring in 1..4) {
            val r = baseRadius * (ring * 0.52f) + energy * 18f * ring
            drawCircle(
                color = accentColor.copy(alpha = (0.48f - ring * 0.09f).coerceAtLeast(0.1f)),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
        }

        // Waveform spokes
        val count = bars.size.coerceAtLeast(1)
        bars.forEachIndexed { idx, amp ->
            val angle = (idx.toFloat() / count) * (2f * PI.toFloat()) + tick * 2f * PI.toFloat()
            val inner = baseRadius * 0.9f
            val outer = inner + 16f + amp * size.minDimension * 0.22f
            val p1 = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner)
            val p2 = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer)
            drawLine(
                color = accentColor.copy(alpha = 0.5f + amp * 0.5f),
                start = p1,
                end = p2,
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = accentColor.copy(alpha = 0.28f + energy * 0.3f),
            radius = baseRadius * 0.65f,
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun RainCascadeCanvas(
    tick: Float,
    accentColor: Color,
    busState: VoiceBusState,
    energy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val columns = 18
        val colWidth = size.width / columns
        val speed = when (busState) {
            VoiceBusState.IDLE -> 1f
            VoiceBusState.LISTENING -> 1.8f
            VoiceBusState.THINKING -> 2.6f
            VoiceBusState.SPEAKING -> 3.4f
        }
        for (i in 0 until columns) {
            val x = colWidth * (i + 0.5f)
            val phase = (tick * speed + i * 0.19f) % 1f
            val headY = phase * (size.height + 80f) - 40f
            val tailLen = 50f + energy * 90f

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.85f)),
                    startY = headY - tailLen,
                    endY = headY
                ),
                start = Offset(x, headY - tailLen),
                end = Offset(x, headY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = 3.2f,
                center = Offset(x, headY)
            )
        }
    }
}

@Composable
private fun NeuralMeshCanvas(
    tick: Float,
    accentColor: Color,
    busState: VoiceBusState,
    energy: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val nodes = List(9) { idx ->
            val angle = (idx / 9f) * 2f * PI.toFloat() + tick * 0.8f
            val ringScale = if (idx % 2 == 0) 0.34f else 0.22f
            Offset(
                cx + cos(angle) * size.minDimension * ringScale,
                cy + sin(angle) * size.minDimension * ringScale
            )
        } + Offset(cx, cy)

        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val dist = (nodes[i] - nodes[j]).getDistance()
                if (dist < size.minDimension * 0.42f) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.16f + energy * 0.38f),
                        start = nodes[i],
                        end = nodes[j],
                        strokeWidth = 2f
                    )
                }
            }
        }

        val nodeRadius = when (busState) {
            VoiceBusState.SPEAKING -> 7f + energy * 6f
            VoiceBusState.THINKING -> 6.5f + energy * 4f
            else -> 5.5f
        }
        nodes.forEachIndexed { idx, pt ->
            val isCenter = idx == nodes.lastIndex
            drawCircle(
                color = if (isCenter) Color.White else accentColor,
                radius = if (isCenter) nodeRadius * 1.6f else nodeRadius,
                center = pt
            )
        }
    }
}
