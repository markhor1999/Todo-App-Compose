package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnMotion
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme

/**
 * The app's hero control. Idle: coral gradient circle with a mic. Recording: morphs
 * toward a rounded "stop" square while a halo breathes with the live microphone
 * amplitude — the button itself is the level meter.
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
) {
    val extended = VnTheme.extended
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = VnMotion.snappy(),
        label = "recordPressScale",
    )
    val level by animateFloatAsState(
        targetValue = amplitude.coerceIn(0f, 1f),
        animationSpec = VnMotion.smooth(),
        label = "recordLevel",
    )
    val pulse by rememberInfiniteTransition(label = "recordPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "recordPulseValue",
    )
    val corner by animateDpAsState(
        targetValue = if (isRecording) size * 0.28f else size / 2,
        animationSpec = VnMotion.bouncy(),
        label = "recordCorner",
    )
    val innerStopCorner by animateDpAsState(
        targetValue = if (isRecording) 7.dp else size / 2,
        animationSpec = VnMotion.bouncy(),
        label = "recordInnerCorner",
    )
    val haloColor = extended.record
    val haloStrength by animateFloatAsState(
        targetValue = if (isRecording) 1f else 0f,
        animationSpec = VnMotion.smooth(),
        label = "recordHalo",
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = if (isRecording) "Stop recording" else "Record" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .drawBehind {
                    if (haloStrength > 0f) {
                        val base = this.size.minDimension / 2f
                        val breath = base * (0.16f + level * 0.55f + pulse * 0.10f) * haloStrength
                        drawCircle(
                            color = haloColor.copy(alpha = 0.16f * haloStrength),
                            radius = base + breath,
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                        )
                        drawCircle(
                            color = haloColor.copy(alpha = 0.10f * haloStrength),
                            radius = base + breath * 1.9f,
                            center = Offset(this.size.width / 2f, this.size.height / 2f),
                        )
                    }
                }
                .scale(pressScale)
                .clip(RoundedCornerShape(corner))
                .background(
                    Brush.linearGradient(
                        colors = listOf(extended.record, extended.recordGradientEnd),
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(size * 0.32f)
                        .clip(RoundedCornerShape(innerStopCorner))
                        .background(Color.White),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.42f),
                )
            }
        }
    }
}
