package com.codingwithsalman.voicenotes.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.codingwithsalman.voicenotes.core.designsystem.theme.VnTheme

/**
 * Murmur's own glyphs: everything is drawn from the same DNA as the waveform —
 * vertical pills with round caps, 2.2dp stroke. No stock Material silhouettes
 * on brand surfaces.
 */
object VnIcons {

    private fun icon(
        name: String,
        block: androidx.compose.ui.graphics.vector.ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    private fun androidx.compose.ui.graphics.vector.ImageVector.Builder.stroke(
        pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ) = path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2.2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        fill = null,
    ) { pathData() }

    /** Arrow dropping into a waveform — "bring audio in". */
    val ImportAudio: ImageVector by lazy {
        icon("VnImportAudio") {
            stroke { moveTo(12f, 3.2f); lineTo(12f, 11.8f) }          // shaft
            stroke { moveTo(8.4f, 8.6f); lineTo(12f, 12.2f); lineTo(15.6f, 8.6f) } // chevron
            stroke { moveTo(4.6f, 18.2f); lineTo(4.6f, 20.2f) }       // wave pills
            stroke { moveTo(8.3f, 16.9f); lineTo(8.3f, 21.4f) }
            stroke { moveTo(12f, 15.6f); lineTo(12f, 22.6f) }
            stroke { moveTo(15.7f, 16.9f); lineTo(15.7f, 21.4f) }
            stroke { moveTo(19.4f, 18.2f); lineTo(19.4f, 20.2f) }
        }
    }

    /** Three tuning sliders — settings, in an audio dialect. */
    val Tune: ImageVector by lazy {
        icon("VnTune") {
            stroke { moveTo(4f, 7f); lineTo(20f, 7f) }
            stroke { moveTo(4f, 12f); lineTo(20f, 12f) }
            stroke { moveTo(4f, 17f); lineTo(20f, 17f) }
            // knobs (short fat pills read as dots)
            stroke { moveTo(9f, 5.6f); lineTo(9f, 8.4f) }
            stroke { moveTo(15f, 10.6f); lineTo(15f, 13.4f) }
            stroke { moveTo(7f, 15.6f); lineTo(7f, 18.4f) }
        }
    }

    /** Magnifier built from a circle + pill handle, matching stroke DNA. */
    val Search: ImageVector by lazy {
        icon("VnSearch") {
            stroke {
                moveTo(15.2f, 10.4f)
                arcTo(4.8f, 4.8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 15.199f, 10.39f)
            }
            stroke { moveTo(14.2f, 14.2f); lineTo(19.4f, 19.4f) }
        }
    }
}

/** Circular chip around an icon action — the designed top-bar cluster. */
@Composable
fun VnIconChip(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .border(BorderStroke(1.dp, VnTheme.extended.cardStroke), CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}
