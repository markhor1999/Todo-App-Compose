package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme

/**
 * The waveform is Murmur's hero surface, so it gets light: each bar carries an
 * amplitude-driven amber→coral color and a soft under-glow (a wider translucent
 * pass beneath the crisp bar — cheap, no blur).
 */
@Composable
fun LiveWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barWidth: Dp = 3.5.dp,
    gap: Dp = 3.dp,
) {
    val density = LocalDensity.current
    val active = VnTheme.extended.waveActive
    val hot = VnTheme.extended.waveHot

    Canvas(modifier = modifier) {
        val barPx = with(density) { barWidth.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val slot = barPx + gapPx
        val maxBars = (size.width / slot).toInt().coerceAtLeast(1)
        val bars = amplitudes.takeLast(maxBars)
        val centerY = size.height / 2f

        bars.forEachIndexed { index, raw ->
            val amp = raw.coerceIn(0f, 1f)
            val h = (amp * size.height * 0.92f).coerceAtLeast(barPx)
            val x = size.width - (bars.size - index) * slot
            val recency = index.toFloat() / bars.size // older bars fade
            val color = lerp(active, hot, amp * amp)

            // under-glow
            drawRoundRect(
                color = color.copy(alpha = 0.18f * (0.35f + 0.65f * recency)),
                topLeft = Offset(x - barPx * 0.9f, centerY - h * 0.62f),
                size = Size(barPx * 2.8f, h * 1.24f),
                cornerRadius = CornerRadius(barPx * 1.4f, barPx * 1.4f),
            )
            // crisp bar
            drawRoundRect(
                color = color.copy(alpha = 0.35f + 0.65f * recency),
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barPx, h),
                cornerRadius = CornerRadius(barPx / 2f, barPx / 2f),
            )
        }
    }
}

/**
 * Playback waveform: played bars are lit (amber, amplitude-tinted toward coral)
 * with a soft glow; the remainder stays idle. Tap or drag to seek.
 */
@Composable
fun StaticWaveform(
    bars: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    onSeek: ((Float) -> Unit)? = null,
    barWidth: Dp = 3.dp,
    gap: Dp = 2.dp,
) {
    val idle = VnTheme.extended.waveIdle
    val active = VnTheme.extended.waveActive
    val hot = VnTheme.extended.waveHot
    val playhead = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current

    val gestures = if (onSeek != null) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek(offset.x / size.width) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek(change.position.x / size.width)
                }
            }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(gestures)) {
        if (bars.isEmpty()) return@Canvas
        val barPx = with(density) { barWidth.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val slot = size.width / bars.size
        val actualBar = (slot - gapPx).coerceAtLeast(barPx * 0.5f).coerceAtMost(barPx)
        val centerY = size.height / 2f
        val playedX = size.width * progress.coerceIn(0f, 1f)

        bars.forEachIndexed { index, raw ->
            val amp = raw.coerceIn(0f, 1f)
            val h = (amp * size.height * 0.9f).coerceAtLeast(actualBar)
            val x = index * slot
            val played = x <= playedX
            if (played) {
                val color = lerp(active, hot, amp * amp)
                drawRoundRect(
                    color = color.copy(alpha = 0.20f),
                    topLeft = Offset(x - actualBar * 0.8f, centerY - h * 0.62f),
                    size = Size(actualBar * 2.6f, h * 1.24f),
                    cornerRadius = CornerRadius(actualBar * 1.3f, actualBar * 1.3f),
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, centerY - h / 2f),
                    size = Size(actualBar, h),
                    cornerRadius = CornerRadius(actualBar / 2f, actualBar / 2f),
                )
            } else {
                drawRoundRect(
                    color = idle,
                    topLeft = Offset(x, centerY - h / 2f),
                    size = Size(actualBar, h),
                    cornerRadius = CornerRadius(actualBar / 2f, actualBar / 2f),
                )
            }
        }
        // playhead: a taller luminous pill at the boundary
        if (progress > 0.001f && progress < 0.999f) {
            drawRoundRect(
                color = playhead.copy(alpha = 0.9f),
                topLeft = Offset(playedX - 1.2f, centerY - size.height * 0.5f),
                size = Size(2.4f, size.height),
                cornerRadius = CornerRadius(1.2f, 1.2f),
            )
        }
    }
}

/** Fills available space; useful as a decorative glyph in empty states. */
@Composable
fun DecorativeWaveform(
    modifier: Modifier = Modifier,
    bars: List<Float> = listOf(0.2f, 0.5f, 0.9f, 0.6f, 1f, 0.7f, 0.35f, 0.55f, 0.25f),
) {
    StaticWaveform(
        bars = bars,
        progress = 1f,
        modifier = modifier.fillMaxSize(),
        barWidth = 6.dp,
        gap = 5.dp,
    )
}
